package org.sunbird.learner.actors;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.actorutil.location.impl.LocationClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.EmailValidator;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsIndex;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.Slug;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.Util;
import org.sunbird.models.location.Location;
import org.sunbird.models.organisation.OrgTypeEnum;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.validator.location.LocationRequestValidator;
import scala.concurrent.Future;

/**
 * This actor will handle organisation related operation .
 *
 * @author Arvind
 */
@ActorConfig(
  tasks = {
    "createOrg",
    "updateOrg",
    "updateOrgStatus",
    "getOrgDetails",
    "addMemberOrganisation",
    "removeMemberOrganisation",
    "assignKeys"
  },
  asyncTasks = {}
)
public class OrganisationManagementActor extends BaseActor {
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final LocationRequestValidator validator = new LocationRequestValidator();
  private final EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private LocationClient locationClient = LocationClientImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.ORGANISATION);
    if (request.getOperation().equalsIgnoreCase(ActorOperations.CREATE_ORG.getValue())) {
      createOrg(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_ORG_STATUS.getValue())) {
      updateOrgStatus(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_ORG.getValue())) {
      updateOrgData(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_ORG_DETAILS.getValue())) {
      getOrgDetails(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ADD_MEMBER_ORGANISATION.getValue())) {
      addMemberOrganisation(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue())) {
      removeMemberOrganisation(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.ASSIGN_KEYS.getValue())) {
      assignKey(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  /** Method to create an Organisation . */
  @SuppressWarnings("unchecked")
  private void createOrg(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "OrgManagementActor: Create org method call start");
    // object of telemetry event...
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    try {
      actorMessage.toLower();
      Map<String, Object> request = actorMessage.getRequest();
      if (request.containsKey(JsonKey.EMAIL)
          && !EmailValidator.isEmailValid((String) request.get(JsonKey.EMAIL))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.emailFormatError);
      }
      validateOrgType((String) request.get(JsonKey.ORG_TYPE), JsonKey.CREATE);
      channelMandatoryValidation(request);
      validateChannel(request, actorMessage.getRequestContext());

      Boolean isRootOrg = (Boolean) request.get(JsonKey.IS_ROOT_ORG);

      if (request.containsKey(JsonKey.CHANNEL)) {
        String slug = Slug.makeSlug((String) request.getOrDefault(JsonKey.CHANNEL, ""), true);
        if (null != isRootOrg && isRootOrg) {
          boolean bool = isSlugUnique(slug, actorMessage.getRequestContext());
          if (bool) {
            request.put(JsonKey.SLUG, slug);
          } else {
            sender().tell(ProjectUtil.createClientException(ResponseCode.slugIsNotUnique), self());
            return;
          }
        } else {
          request.put(JsonKey.SLUG, slug);
        }
      }

      validateOrgLocation(request, actorMessage.getRequestContext());

      String passedExternalId = (String) request.get(JsonKey.EXTERNAL_ID);
      if (StringUtils.isNotBlank(passedExternalId)) {
        passedExternalId = passedExternalId.toLowerCase();
        String channel = (String) request.get(JsonKey.CHANNEL);
        if (!validateChannelExternalIdUniqueness(
            channel, passedExternalId, null, actorMessage.getRequestContext())) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorDuplicateEntry,
              MessageFormat.format(
                  ResponseCode.errorDuplicateEntry.getErrorMessage(),
                  passedExternalId,
                  JsonKey.EXTERNAL_ID));
        }
        request.put(JsonKey.EXTERNAL_ID, passedExternalId);
        request.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
      } else {
        request.remove(JsonKey.EXTERNAL_ID);
        request.remove(JsonKey.PROVIDER);
      }

      String createdBy = (String) actorMessage.getRequest().get(JsonKey.REQUESTED_BY);
      request.put(JsonKey.CREATED_BY, createdBy);
      request.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
      request.put(JsonKey.ID, uniqueId);
      // set root org on basis of whether the org itself is root org or not ...
      if (null != isRootOrg && isRootOrg) {
        request.put(JsonKey.ROOT_ORG_ID, uniqueId);
      }
      if (JsonKey.BULK_ORG_UPLOAD.equalsIgnoreCase(callerId)) {
        if (null == request.get(JsonKey.STATUS)) {
          request.put(JsonKey.STATUS, ProjectUtil.OrgStatus.ACTIVE.getValue());
        }
      } else {
        request.put(JsonKey.STATUS, ProjectUtil.OrgStatus.ACTIVE.getValue());
      }

      if (null != request.get(JsonKey.ADDRESS)) {
        Map<String, Object> addressReq = (Map<String, Object>) request.get(JsonKey.ADDRESS);
        request.remove(JsonKey.ADDRESS);
        // update address if present in request
        if (MapUtils.isNotEmpty(addressReq)) {
          String addressId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
          addressReq.put(JsonKey.ID, addressId);
          addressReq.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());

          if (!(StringUtils.isBlank(createdBy))) {
            addressReq.put(JsonKey.CREATED_BY, createdBy);
          }
          upsertAddress(addressReq);
          request.put(JsonKey.ADDRESS_ID, addressId);
          telemetryGenerationForOrgAddress(addressReq, request, false, actorMessage.getContext());
        }
      }

      // adding one extra filed for tag.
      if (StringUtils.isNotBlank(((String) request.get(JsonKey.HASHTAGID)))) {
        request.put(
            JsonKey.HASHTAGID,
            validateHashTagId(
                ((String) request.get(JsonKey.HASHTAGID)),
                JsonKey.CREATE,
                "",
                actorMessage.getRequestContext()));
      } else {
        request.put(JsonKey.HASHTAGID, uniqueId);
      }

      if (null != isRootOrg && isRootOrg) {
        boolean bool = Util.registerChannel(request, actorMessage.getRequestContext());
        request.put(
            JsonKey.IS_SSO_ROOTORG_ENABLED,
            request.containsKey(JsonKey.IS_SSO_ROOTORG_ENABLED)
                ? request.get(JsonKey.IS_SSO_ROOTORG_ENABLED)
                : false);
        if (!bool) {
          sender().tell(ProjectUtil.createServerError(ResponseCode.channelRegFailed), self());
          return;
        }
      } else {
        request.put(JsonKey.IS_ROOT_ORG, false);
        request.put(JsonKey.IS_SSO_ROOTORG_ENABLED, false);
      }
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      // This will remove all extra unnecessary parameter from request
      ObjectMapper mapper = new ObjectMapper();
      Organisation org = mapper.convertValue(request, Organisation.class);
      request = mapper.convertValue(org, Map.class);
      Response result =
          cassandraOperation.insertRecord(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              request,
              actorMessage.getRequestContext());

      if (StringUtils.isNotBlank(passedExternalId)) {
        String channel = (String) request.get(JsonKey.CHANNEL);
        createOrgExternalIdRecord(
            channel, passedExternalId, uniqueId, actorMessage.getRequestContext());
      }
      logger.info(
          actorMessage.getRequestContext(),
          "OrgManagementActor : createOrg : Created org id is ----." + uniqueId);
      result.getResult().put(JsonKey.ORGANISATION_ID, uniqueId);
      sender().tell(result, self());
      Request orgReq = new Request();
      orgReq.setRequestContext(actorMessage.getRequestContext());
      orgReq.getRequest().put(JsonKey.ORGANISATION, request);
      orgReq.setOperation(ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue());
      logger.info(
          actorMessage.getRequestContext(),
          "OrganisationManagementActor:createOrg: Calling background job to sync org data "
              + uniqueId);
      tellToAnother(orgReq);
      targetObject =
          TelemetryUtil.generateTargetObject(uniqueId, JsonKey.ORGANISATION, JsonKey.CREATE, null);
      TelemetryUtil.generateCorrelatedObject(
          uniqueId, JsonKey.ORGANISATION, null, correlatedObject);
      TelemetryUtil.telemetryProcessingCall(
          (Map<String, Object>) actorMessage.getRequest().get(JsonKey.ORGANISATION),
          targetObject,
          correlatedObject,
          actorMessage.getContext());
    } catch (ProjectCommonException e) {
      logger.error(
          actorMessage.getRequestContext(),
          "OrganisationManagementActor:createOrg: Error occurred = " + e.getMessage(),
          e);
      sender().tell(e, self());
      return;
    }
  }

  private void createOrgExternalIdRecord(
      String channel, String externalId, String orgId, RequestContext context) {
    Map<String, Object> orgExtIdRequest = new HashMap<String, Object>();
    orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
    orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
    orgExtIdRequest.put(JsonKey.ORG_ID, orgId);
    cassandraOperation.insertRecord(
        JsonKey.SUNBIRD, JsonKey.ORG_EXT_ID_DB, orgExtIdRequest, context);
  }

  private void deleteOrgExternalIdRecord(
      String channel, String externalId, RequestContext context) {
    Map<String, String> orgExtIdRequest = new HashMap<String, String>();
    orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
    orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
    cassandraOperation.deleteRecord(
        JsonKey.SUNBIRD, JsonKey.ORG_EXT_ID_DB, orgExtIdRequest, context);
  }

  private String validateHashTagId(
      String hashTagId, String opType, String orgId, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.HASHTAGID, hashTagId);
    SearchDTO searchDto = new SearchDTO();
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> resultF =
        esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
      if (!dataMapList.isEmpty()) {
        throw new ProjectCommonException(
            ResponseCode.invalidHashTagId.getErrorCode(),
            ResponseCode.invalidHashTagId.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else if (opType.equalsIgnoreCase(JsonKey.UPDATE) && !dataMapList.isEmpty()) {
      Map<String, Object> orgMap = dataMapList.get(0);
      if (!(((String) orgMap.get(JsonKey.ID)).equalsIgnoreCase(orgId))) {
        throw new ProjectCommonException(
            ResponseCode.invalidHashTagId.getErrorCode(),
            ResponseCode.invalidHashTagId.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return hashTagId;
  }

  private void validateOrgType(String orgType, String operation) {
    if (StringUtils.isBlank(orgType) && operation.equalsIgnoreCase(JsonKey.CREATE)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.ORG_TYPE),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    List<String> orgTypeList = new ArrayList<>();
    for (OrgTypeEnum type : OrgTypeEnum.values()) {
      orgTypeList.add(type.getType());
    }
    if (!orgTypeList.contains(orgType)) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue.getErrorCode(),
          MessageFormat.format(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, orgType, orgTypeList),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /** Updates the status of the Organisation */
  @SuppressWarnings("unchecked")
  private void updateOrgStatus(Request actorMessage) {

    // object of telemetry event...
    Map<String, Object> targetObject = null;
    try {
      actorMessage.toLower();
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Map<String, Object> request = actorMessage.getRequest();
      if (!(validateOrgRequest(request, actorMessage.getRequestContext()))) {
        logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID");
        return;
      }
      Map<String, Object> orgDao;
      Map<String, Object> updateOrgDao = new HashMap<>();
      String updatedBy = (String) request.get(JsonKey.REQUESTED_BY);
      String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
      Response result =
          cassandraOperation.getRecordById(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              orgId,
              actorMessage.getRequestContext());
      List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (!(list.isEmpty())) {
        orgDao = list.get(0);
      } else {
        logger.info(actorMessage.getRequestContext(), "Invalid Org Id");
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidRequestData.getErrorCode(),
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }

      Integer currentStatus = (Integer) orgDao.get(JsonKey.STATUS);
      Integer nextStatus = (Integer) request.get(JsonKey.STATUS);
      if (!(Util.checkOrgStatusTransition(currentStatus, nextStatus))) {
        logger.info(actorMessage.getRequestContext(), "Invalid Org State transation");
        sender().tell(ProjectUtil.createClientException(ResponseCode.invalidRequestData), self());
        return;
      }

      if (!(StringUtils.isBlank(updatedBy))) {
        updateOrgDao.put(JsonKey.UPDATED_BY, updatedBy);
      }
      updateOrgDao.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      updateOrgDao.put(JsonKey.ID, orgDao.get(JsonKey.ID));
      updateOrgDao.put(JsonKey.STATUS, nextStatus);
      Response response =
          cassandraOperation.updateRecord(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              updateOrgDao,
              actorMessage.getRequestContext());
      response.getResult().put(JsonKey.ORGANISATION_ID, orgDao.get(JsonKey.ID));
      sender().tell(response, self());

      // update the ES --
      Request orgRequest = new Request();
      orgRequest.setRequestContext(actorMessage.getRequestContext());
      orgRequest.getRequest().put(JsonKey.ORGANISATION, updateOrgDao);
      orgRequest.setOperation(ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue());
      tellToAnother(orgRequest);

      targetObject =
          TelemetryUtil.generateTargetObject(orgId, JsonKey.ORGANISATION, JsonKey.UPDATE, null);
      Map<String, Object> telemetryAction = new HashMap<>();
      telemetryAction.put("updateOrgStatus", "org status updated.");
      TelemetryUtil.telemetryProcessingCall(
          telemetryAction, targetObject, new ArrayList<>(), actorMessage.getContext());
      return;
    } catch (ProjectCommonException e) {
      logger.error(actorMessage.getRequestContext(), e.getMessage(), e);
      sender().tell(e, self());
      return;
    }
  }

  /** Update the Organisation data */
  @SuppressWarnings("unchecked")
  private void updateOrgData(Request actorMessage) {
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    try {
      actorMessage.toLower();
      Map<String, Object> request = actorMessage.getRequest();
      if (request.containsKey(JsonKey.EMAIL)
          && !EmailValidator.isEmailValid((String) request.get(JsonKey.EMAIL))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.emailFormatError);
      }
      validateOrgType((String) request.get(JsonKey.ORG_TYPE), JsonKey.UPDATE);
      if (!(validateOrgRequest(request, actorMessage.getRequestContext()))) {
        logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID for Org update");
        return;
      }
      String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
      OrgService orgService = new OrgServiceImpl();
      Map<String, Object> dbOrgDetails =
          orgService.getOrgById(orgId, actorMessage.getRequestContext());
      if (MapUtils.isEmpty(dbOrgDetails)) {
        logger.info(
            actorMessage.getRequestContext(),
            "OrganisationManagementActor: updateOrgData invalid orgId");
        sender().tell(ProjectUtil.createClientException(ResponseCode.invalidRequestData), self());
        return;
      }

      String existingExternalId = (String) dbOrgDetails.get(JsonKey.EXTERNAL_ID);
      validateOrgLocation(request, actorMessage.getRequestContext());
      if (request.containsKey(JsonKey.CHANNEL)) {
        boolean flag =
            validateChannelUniqueness(
                (String) request.get(JsonKey.CHANNEL),
                (String) request.get(JsonKey.ORGANISATION_ID),
                actorMessage.getRequestContext());
        if (!flag) {
          logger.info(actorMessage.getRequestContext(), "Channel validation failed");
          ProjectCommonException.throwClientErrorException(ResponseCode.channelUniquenessInvalid);
        }
      }
      // allow lower case values for source and externalId to the database
      if (request.get(JsonKey.PROVIDER) != null) {
        request.put(JsonKey.PROVIDER, ((String) request.get(JsonKey.PROVIDER)).toLowerCase());
      }

      String passedExternalId = (String) request.get(JsonKey.EXTERNAL_ID);
      if (StringUtils.isNotBlank(passedExternalId)) {
        passedExternalId = passedExternalId.toLowerCase();
        String channel = (String) request.get(JsonKey.CHANNEL);
        if (StringUtils.isBlank(channel)) {
          channel = (String) dbOrgDetails.get(JsonKey.CHANNEL);
        }
        if (!validateChannelExternalIdUniqueness(
            channel,
            passedExternalId,
            (String) request.get(JsonKey.ORGANISATION_ID),
            actorMessage.getRequestContext())) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorDuplicateEntry,
              MessageFormat.format(
                  ResponseCode.errorDuplicateEntry.getErrorMessage(),
                  passedExternalId,
                  JsonKey.EXTERNAL_ID));
        }
        request.put(JsonKey.EXTERNAL_ID, passedExternalId);
      } else {
        request.remove(JsonKey.EXTERNAL_ID);
      }
      Map<String, Object> updateOrgDao = new HashMap<>();
      updateOrgDao.putAll(request);
      updateOrgDao.remove(JsonKey.ORGANISATION_ID);
      updateOrgDao.remove(JsonKey.IS_APPROVED);
      updateOrgDao.remove(JsonKey.APPROVED_BY);
      updateOrgDao.remove(JsonKey.APPROVED_DATE);
      updateOrgDao.remove(JsonKey.CONTACT_DETAILS);
      if (JsonKey.BULK_ORG_UPLOAD.equalsIgnoreCase(callerId)) {
        if (null == request.get(JsonKey.STATUS)) {
          updateOrgDao.remove(JsonKey.STATUS);
        }
      } else {
        updateOrgDao.remove(JsonKey.STATUS);
      }

      String updatedBy = (String) actorMessage.getRequest().get(JsonKey.REQUESTED_BY);
      Map<String, Object> addressReq = null;
      if (null != request.get(JsonKey.ADDRESS)) {
        addressReq = (Map<String, Object>) request.get(JsonKey.ADDRESS);
        request.remove(JsonKey.ADDRESS);
        boolean isAddressUpdated = false;
        // update address if present in request
        if (null != addressReq && addressReq.size() == 1) {
          if (dbOrgDetails.get(JsonKey.ADDRESS_ID) != null) {
            String addressId = (String) dbOrgDetails.get(JsonKey.ADDRESS_ID);
            addressReq.put(JsonKey.ID, addressId);
            isAddressUpdated = true;
          }
          // add new address record
          else {
            String addressId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
            addressReq.put(JsonKey.ID, addressId);
            dbOrgDetails.put(JsonKey.ADDRESS_ID, addressId);
          }
          if (!(StringUtils.isBlank(updatedBy))) {
            addressReq.put(JsonKey.UPDATED_BY, updatedBy);
          }
          upsertAddress(addressReq);
          telemetryGenerationForOrgAddress(
              addressReq, dbOrgDetails, isAddressUpdated, actorMessage.getContext());
        }
      }

      if (StringUtils.isNotBlank(((String) request.get(JsonKey.HASHTAGID)))) {
        request.put(
            JsonKey.HASHTAGID,
            validateHashTagId(
                ((String) request.get(JsonKey.HASHTAGID)),
                JsonKey.UPDATE,
                orgId,
                actorMessage.getRequestContext()));
      }
      if (!(StringUtils.isBlank(updatedBy))) {
        updateOrgDao.put(JsonKey.UPDATED_BY, updatedBy);
      }
      updateOrgDao.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      updateOrgDao.put(JsonKey.ID, dbOrgDetails.get(JsonKey.ID));

      // if channel is available then make slug for channel.
      // remove the slug key if coming from user input
      updateOrgDao.remove(JsonKey.SLUG);
      if (StringUtils.isNotBlank((String) updateOrgDao.get(JsonKey.CHANNEL))) {
        String reqChannel = (String) updateOrgDao.get(JsonKey.CHANNEL);
        String dbChannel = (String) dbOrgDetails.get(JsonKey.CHANNEL);
        if (StringUtils.isNotBlank(dbChannel)
            && StringUtils.isNotBlank(reqChannel)
            && !dbChannel.equalsIgnoreCase(reqChannel)) {
          String slug =
              Slug.makeSlug((String) updateOrgDao.getOrDefault(JsonKey.CHANNEL, ""), true);
          if (dbOrgDetails.containsKey(JsonKey.IS_ROOT_ORG)
              && (boolean) dbOrgDetails.get(JsonKey.IS_ROOT_ORG)) {
            String rootOrgId = getRootOrgIdFromSlug(slug, actorMessage.getRequestContext());
            if (StringUtils.isBlank(rootOrgId)
                || (StringUtils.isNotBlank(rootOrgId)
                    && rootOrgId.equalsIgnoreCase((String) dbOrgDetails.get(JsonKey.ID)))) {
              updateOrgDao.put(JsonKey.SLUG, slug);
            } else {
              sender()
                  .tell(ProjectUtil.createClientException(ResponseCode.slugIsNotUnique), self());
              return;
            }
          } else {
            updateOrgDao.put(JsonKey.SLUG, slug);
            updateOrgDao.put(JsonKey.IS_SSO_ROOTORG_ENABLED, false);
          }
        }
      }

      if (null != dbOrgDetails.get(JsonKey.IS_ROOT_ORG)
          && (boolean) dbOrgDetails.get(JsonKey.IS_ROOT_ORG)) {
        String channel = (String) dbOrgDetails.get(JsonKey.CHANNEL);
        String updateOrgDaoChannel = (String) updateOrgDao.get(JsonKey.CHANNEL);
        String license = (String) request.get(JsonKey.LICENSE);
        if (null != updateOrgDaoChannel && null != channel && !(updateOrgDaoChannel.equals(channel))
            || StringUtils.isNotBlank(license)) {
          Map<String, Object> tempMap = new HashMap<>();
          tempMap.put(JsonKey.CHANNEL, updateOrgDaoChannel);
          tempMap.put(JsonKey.HASHTAGID, dbOrgDetails.get(JsonKey.HASHTAGID));
          tempMap.put(JsonKey.DESCRIPTION, dbOrgDetails.get(JsonKey.DESCRIPTION));
          tempMap.put(JsonKey.LICENSE, license);
          boolean bool = Util.updateChannel(tempMap, actorMessage.getRequestContext());
          if (!bool) {
            sender().tell(ProjectUtil.createServerError(ResponseCode.channelRegFailed), self());
            return;
          }
        }
      }
      ObjectMapper mapper = new ObjectMapper();
      // This will remove all extra unnecessary parameter from request
      Organisation org = mapper.convertValue(updateOrgDao, Organisation.class);
      updateOrgDao = mapper.convertValue(org, Map.class);
      Response response =
          cassandraOperation.updateRecord(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              updateOrgDao,
              actorMessage.getRequestContext());
      response.getResult().put(JsonKey.ORGANISATION_ID, dbOrgDetails.get(JsonKey.ID));

      if (StringUtils.isNotBlank(passedExternalId)) {
        String channel = (String) request.get(JsonKey.CHANNEL);
        if (StringUtils.isBlank(existingExternalId)) {
          createOrgExternalIdRecord(
              channel, passedExternalId, orgId, actorMessage.getRequestContext());
        } else {
          if (!existingExternalId.equalsIgnoreCase(passedExternalId)) {
            deleteOrgExternalIdRecord(
                channel, existingExternalId, actorMessage.getRequestContext());
            createOrgExternalIdRecord(
                channel, passedExternalId, orgId, actorMessage.getRequestContext());
          }
        }
      }

      sender().tell(response, self());

      if (null != addressReq) {
        updateOrgDao.put(JsonKey.ADDRESS, addressReq);
      }

      Request orgRequest = new Request();
      orgRequest.setRequestContext(actorMessage.getRequestContext());
      orgRequest.getRequest().put(JsonKey.ORGANISATION, updateOrgDao);
      orgRequest.setOperation(ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue());
      tellToAnother(orgRequest);
      targetObject =
          TelemetryUtil.generateTargetObject(
              (String) dbOrgDetails.get(JsonKey.ID), JsonKey.ORGANISATION, JsonKey.UPDATE, null);
      TelemetryUtil.telemetryProcessingCall(
          updateOrgDao, targetObject, correlatedObject, actorMessage.getContext());
    } catch (ProjectCommonException e) {
      sender().tell(e, self());
      return;
    }
  }

  private void telemetryGenerationForOrgAddress(
      Map<String, Object> addressReq,
      Map<String, Object> orgDao,
      boolean isAddressUpdated,
      Map<String, Object> context) {

    String addressState = JsonKey.CREATE;
    if (isAddressUpdated) {
      addressState = JsonKey.UPDATE;
    }
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(
            (String) addressReq.get(JsonKey.ID), JsonKey.ADDRESS, addressState, null);
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    TelemetryUtil.generateCorrelatedObject(
        (String) orgDao.get(JsonKey.ID), JsonKey.ORGANISATION, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(addressReq, targetObject, correlatedObject, context);
  }

  /** Method to add member to the organisation */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void addMemberOrganisation(Request actorMessage) {
    // object of telemetry event...
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Response response = null;
    actorMessage.toLower();
    Util.DbInfo userOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    Util.DbInfo organisationDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Map<String, Object> usrOrgData = actorMessage.getRequest();
    if (!(validateOrgRequestForMembers(usrOrgData, actorMessage.getRequestContext()))) {
      logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID");
      return;
    }
    if (!(validateUsrRequest(usrOrgData, actorMessage.getRequestContext()))) {
      logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID");
      return;
    }
    usrOrgData.put(JsonKey.IS_DELETED, false);

    String updatedBy = null;
    String orgId = null;
    String userId = null;
    List<String> roles = new ArrayList<>();
    orgId = (String) usrOrgData.get(JsonKey.ORGANISATION_ID);
    userId = (String) usrOrgData.get(JsonKey.USER_ID);
    if (isNotNull(usrOrgData.get(JsonKey.REQUESTED_BY))) {
      updatedBy = (String) usrOrgData.get(JsonKey.REQUESTED_BY);
      usrOrgData.remove(JsonKey.REQUESTED_BY);
    }
    if (isNotNull(usrOrgData.get(JsonKey.ROLES))) {
      roles.addAll((List<String>) usrOrgData.get(JsonKey.ROLES));
      if (!((List<String>) usrOrgData.get(JsonKey.ROLES)).isEmpty()) {
        String msg = Util.validateRoles((List<String>) usrOrgData.get(JsonKey.ROLES));
        if (!msg.equalsIgnoreCase(JsonKey.SUCCESS)) {
          throw new ProjectCommonException(
              ResponseCode.invalidRole.getErrorCode(),
              ResponseCode.invalidRole.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }

    usrOrgData.remove(JsonKey.ROLE);
    if (isNull(roles) && roles.isEmpty()) {
      // create exception here invalid request data and tell the exception , then
      // return
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.invalidRequestData.getErrorCode(),
              ResponseCode.invalidRequestData.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      sender().tell(exception, self());
      return;
    }
    if (!roles.isEmpty()) usrOrgData.put(JsonKey.ROLES, roles);
    // check user already exist for the org or not
    Map<String, Object> requestData = new HashMap<>();
    requestData.put(JsonKey.USER_ID, userId);
    requestData.put(JsonKey.ORGANISATION_ID, orgId);
    Response result =
        cassandraOperation.getRecordsByCompositeKey(
            userOrgDbInfo.getKeySpace(),
            userOrgDbInfo.getTableName(),
            requestData,
            actorMessage.getRequestContext());

    List list = (List) result.get(JsonKey.RESPONSE);
    Map<String, Object> tempOrgap = null;
    boolean isNewRecord = false;
    if (!list.isEmpty()) {
      tempOrgap = (Map<String, Object>) list.get(0);
      if (null != tempOrgap && !((boolean) tempOrgap.get(JsonKey.IS_DELETED))) {
        // user already enrolled for the organisation
        response = new Response();
        response.getResult().put(JsonKey.RESPONSE, ResponseMessage.Message.EXISTING_ORG_MEMBER);
        sender().tell(response, self());
        return;
      } else if (null != tempOrgap && ((boolean) tempOrgap.get(JsonKey.IS_DELETED))) {
        usrOrgData.put(JsonKey.ID, tempOrgap.get(JsonKey.ID));
      }
    } else {
      usrOrgData.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv()));
      isNewRecord = true;
    }
    if (!(StringUtils.isBlank(updatedBy))) {
      String updatedByName = Util.getUserNamebyUserId(updatedBy, actorMessage.getRequestContext());
      usrOrgData.put(JsonKey.ADDED_BY, updatedBy);
      usrOrgData.put(JsonKey.APPROVED_BY, updatedBy);
      if (!StringUtils.isBlank(updatedByName)) {
        usrOrgData.put(JsonKey.ADDED_BY_NAME, updatedByName);
      }
    }
    usrOrgData.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    usrOrgData.put(JsonKey.APPROOVE_DATE, ProjectUtil.getFormattedDate());
    usrOrgData.put(JsonKey.IS_REJECTED, false);
    usrOrgData.put(JsonKey.IS_APPROVED, true);
    usrOrgData.put(JsonKey.IS_DELETED, false);
    if (isNewRecord) {
      response =
          cassandraOperation.insertRecord(
              userOrgDbInfo.getKeySpace(),
              userOrgDbInfo.getTableName(),
              usrOrgData,
              actorMessage.getRequestContext());
    } else {
      response =
          cassandraOperation.updateRecord(
              userOrgDbInfo.getKeySpace(),
              userOrgDbInfo.getTableName(),
              usrOrgData,
              actorMessage.getRequestContext());
    }
    Response orgResult =
        cassandraOperation.getRecordById(
            organisationDbInfo.getKeySpace(),
            organisationDbInfo.getTableName(),
            orgId,
            actorMessage.getRequestContext());

    List orgList = (List) orgResult.get(JsonKey.RESPONSE);
    Map<String, Object> newOrgMap = new HashMap<>();
    if (!orgList.isEmpty()) {
      Integer count = 0;
      Map<String, Object> orgMap = (Map<String, Object>) orgList.get(0);
      if (isNotNull(orgMap.get(JsonKey.NO_OF_MEMBERS))) {
        count = (Integer) orgMap.get(JsonKey.NO_OF_MEMBERS);
      }
      newOrgMap.put(JsonKey.ID, orgId);
      newOrgMap.put(JsonKey.NO_OF_MEMBERS, count + 1);
      cassandraOperation.updateRecord(
          organisationDbInfo.getKeySpace(),
          organisationDbInfo.getTableName(),
          newOrgMap,
          actorMessage.getRequestContext());
    }

    sender().tell(response, self());

    // update ES with latest data through background job manager
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      logger.info(actorMessage.getRequestContext(), "method call going to satrt for ES--.....");
      Request request = new Request();
      request.setRequestContext(actorMessage.getRequestContext());
      request.setOperation(ActorOperations.UPDATE_USER_ORG_ES.getValue());
      request.getRequest().put(JsonKey.USER, usrOrgData);
      logger.info(actorMessage.getRequestContext(), "making a call to save user data to ES");
      try {
        tellToAnother(request);
      } catch (Exception ex) {
        logger.error(
            actorMessage.getRequestContext(),
            "Exception Occurred during saving user to Es while addMemberOrganisation : ",
            ex);
      }
    } else {
      logger.info(actorMessage.getRequestContext(), "no call for ES to save user");
    }

    targetObject = TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.CREATE, null);
    TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
    TelemetryUtil.generateCorrelatedObject(orgId, JsonKey.ORGANISATION, null, correlatedObject);
    Map<String, Object> telemetryAction = new HashMap<>();
    telemetryAction.put("orgMembershipAdded", "orgMembershipAdded");
    TelemetryUtil.telemetryProcessingCall(
        telemetryAction, targetObject, correlatedObject, actorMessage.getContext());
  }

  /** Method to remove member from the organisation */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private void removeMemberOrganisation(Request actorMessage) {
    Response response = null;
    // object of telemetry event...
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    actorMessage.toLower();
    Util.DbInfo userOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    Util.DbInfo organisationDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Map<String, Object> usrOrgData = actorMessage.getRequest();
    if (!(validateUsrRequest(usrOrgData, actorMessage.getRequestContext()))) {
      logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID");
      return;
    }
    if (!(validateOrgRequestForMembers(usrOrgData, actorMessage.getRequestContext()))) {
      return;
    }

    String updatedBy = null;
    String orgId = null;
    String userId = null;

    orgId = (String) usrOrgData.get(JsonKey.ORGANISATION_ID);
    userId = (String) usrOrgData.get(JsonKey.USER_ID);

    if (StringUtils.isNotBlank((String) usrOrgData.get(JsonKey.REQUESTED_BY))) {
      updatedBy = (String) usrOrgData.get(JsonKey.REQUESTED_BY);
    }
    // check user already exist for the org or not
    Map<String, Object> requestData = new LinkedHashMap<>(2);
    requestData.put(JsonKey.USER_ID, userId);
    requestData.put(JsonKey.ORGANISATION_ID, orgId);
    Response result =
        cassandraOperation.getRecordsByCompositeKey(
            userOrgDbInfo.getKeySpace(),
            userOrgDbInfo.getTableName(),
            requestData,
            actorMessage.getRequestContext());

    List list = (List) result.get(JsonKey.RESPONSE);
    if (list.isEmpty()) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.invalidRequestData.getErrorCode(),
              ResponseCode.invalidRequestData.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      sender().tell(exception, self());
      return;
    } else {
      Map<String, Object> dataMap = (Map<String, Object>) list.get(0);
      if (null != dataMap.get(JsonKey.IS_DELETED) && (boolean) dataMap.get(JsonKey.IS_DELETED)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.userInactiveForThisOrg.getErrorCode(),
                ResponseCode.userInactiveForThisOrg.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }
      if (!(StringUtils.isBlank(updatedBy))) {
        dataMap.put(JsonKey.UPDATED_BY, updatedBy);
      }
      dataMap.put(JsonKey.ORG_LEFT_DATE, ProjectUtil.getFormattedDate());
      dataMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      dataMap.put(JsonKey.IS_DELETED, true);
      response =
          cassandraOperation.updateRecord(
              userOrgDbInfo.getKeySpace(),
              userOrgDbInfo.getTableName(),
              dataMap,
              actorMessage.getRequestContext());
      Map<String, Object> newOrgMap = new HashMap<>();

      Response orgresult =
          cassandraOperation.getRecordById(
              organisationDbInfo.getKeySpace(),
              organisationDbInfo.getTableName(),
              orgId,
              actorMessage.getRequestContext());
      List orgList = (List) orgresult.get(JsonKey.RESPONSE);
      if (!orgList.isEmpty()) {
        Map<String, Object> orgMap = (Map<String, Object>) orgList.get(0);
        if (isNotNull(orgMap.get(JsonKey.NO_OF_MEMBERS))) {
          Integer count = (Integer) orgMap.get(JsonKey.NO_OF_MEMBERS);
          newOrgMap.put(JsonKey.ID, orgId);
          newOrgMap.put(JsonKey.NO_OF_MEMBERS, count == 0 ? 0 : (count - 1));
          cassandraOperation.updateRecord(
              organisationDbInfo.getKeySpace(),
              organisationDbInfo.getTableName(),
              newOrgMap,
              actorMessage.getRequestContext());
        }
      }
      sender().tell(response, self());

      // update ES with latest data through background job manager
      if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
        logger.info(actorMessage.getRequestContext(), "method call going to satrt for ES--.....");
        Request request = new Request();
        request.setRequestContext(actorMessage.getRequestContext());
        request.setOperation(ActorOperations.REMOVE_USER_ORG_ES.getValue());
        request.getRequest().put(JsonKey.USER, dataMap);
        logger.info(actorMessage.getRequestContext(), "making a call to save user data to ES");
        try {
          tellToAnother(request);
        } catch (Exception ex) {
          logger.error(
              actorMessage.getRequestContext(),
              "Exception Occurred during saving user to Es while removing memeber from Organisation : ",
              ex);
        }
      } else {
        logger.info(actorMessage.getRequestContext(), "no call for ES to save user");
      }
      Map<String, Object> targetObject =
          TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.CREATE, null);
      TelemetryUtil.generateCorrelatedObject(userId, JsonKey.USER, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(orgId, JsonKey.ORGANISATION, null, correlatedObject);
      Map<String, Object> telemetryAction = new HashMap<>();
      telemetryAction.put("orgMembershipRemoved", "orgMembershipRemoved");
      TelemetryUtil.telemetryProcessingCall(
          telemetryAction, targetObject, correlatedObject, actorMessage.getContext());
    }
  }

  /** Provides the details of the Organisation */
  private void getOrgDetails(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> request = actorMessage.getRequest();
    if (!(validateOrgRequest(request, actorMessage.getRequestContext()))) {
      logger.info(actorMessage.getRequestContext(), "REQUESTED DATA IS NOT VALID");
      return;
    }
    String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.organisation.getTypeName(), orgId, actorMessage.getRequestContext());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

    if (MapUtils.isEmpty(result)) {
      throw new ProjectCommonException(
          ResponseCode.orgDoesNotExist.getErrorCode(),
          ResponseCode.orgDoesNotExist.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    result.remove(JsonKey.CONTACT_DETAILS);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }

  /** Inserts an address if not present, else updates the existing address */
  private void upsertAddress(Map<String, Object> addressReq) {
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ADDRESS_DB);
    cassandraOperation.upsertRecord(
        orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), addressReq, null);
  }

  // Check whether channel value is present
  public void channelMandatoryValidation(Map<String, Object> request) {
    if (StringUtils.isBlank((String) request.get(JsonKey.CHANNEL))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Validates whether the organisation or source with externalId exists in DB
   *
   * @param req Request from the user
   * @return boolean
   */
  @SuppressWarnings("unchecked")
  private boolean validateOrgRequest(Map<String, Object> req, RequestContext context) {
    String orgId = (String) req.get(JsonKey.ORGANISATION_ID);
    String provider = (String) req.get(JsonKey.PROVIDER);
    String externalId = (String) req.get(JsonKey.EXTERNAL_ID);
    if (StringUtils.isBlank(orgId)) {
      if (StringUtils.isBlank(provider) || StringUtils.isBlank(externalId)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidRequestData.getErrorCode(),
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return false;
      } else {
        // fetch orgid from database on basis of source and external id and put orgid
        // into request .
        OrgExternalService orgExtService = new OrgExternalService();
        String organisationId =
            orgExtService.getOrgIdFromOrgExternalIdAndProvider(
                (String) req.get(JsonKey.EXTERNAL_ID), (String) req.get(JsonKey.PROVIDER), context);
        if (StringUtils.isEmpty(organisationId)) {
          ProjectCommonException exception =
              new ProjectCommonException(
                  ResponseCode.invalidRequestData.getErrorCode(),
                  ResponseCode.invalidRequestData.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
          sender().tell(exception, self());
          return false;
        }
        req.put(JsonKey.ORGANISATION_ID, organisationId);
      }
    }
    return true;
  }

  /**
   * Validates whether the organisation or source with externalId exists in DB
   *
   * @param req
   * @return boolean
   */
  @SuppressWarnings("unchecked")
  private boolean validateOrgRequestForMembers(Map<String, Object> req, RequestContext context) {
    String orgId = (String) req.get(JsonKey.ORGANISATION_ID);
    String externalId = (String) req.get(JsonKey.EXTERNAL_ID);
    String provider = (String) req.get(JsonKey.PROVIDER);
    if (StringUtils.isBlank(orgId)
        && (StringUtils.isBlank(provider) || StringUtils.isBlank(externalId))) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.sourceAndExternalIdValidationError.getErrorCode(),
              ResponseCode.sourceAndExternalIdValidationError.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      sender().tell(exception, self());
      return false;
    }
    // fetch orgid from database on basis of source and external id and put orgid
    // into request .

    Map<String, Object> requestDbMap = new HashMap<>();
    if (StringUtils.isNotBlank((String) req.get(JsonKey.ORGANISATION_ID))) {
      requestDbMap.put(JsonKey.ID, req.get(JsonKey.ORGANISATION_ID));
    } else {
      requestDbMap.put(JsonKey.PROVIDER, StringUtils.lowerCase((String) req.get(JsonKey.PROVIDER)));
      requestDbMap.put(
          JsonKey.EXTERNAL_ID, StringUtils.lowerCase((String) req.get(JsonKey.EXTERNAL_ID)));
    }
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, requestDbMap);
    Future<Map<String, Object>> esResponseF =
        esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> list = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
    if (CollectionUtils.isEmpty(list)) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.invalidOrgData.getErrorCode(),
              ResponseCode.invalidOrgData.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      sender().tell(exception, self());
      return false;
    }
    req.put(JsonKey.ORGANISATION_ID, list.get(0).get(JsonKey.ID));
    req.put(JsonKey.HASHTAGID, list.get(0).get(JsonKey.HASHTAGID));
    return true;
  }

  /**
   * Validates where the userId or provider with userName is in database and is valid
   *
   * @param req
   * @return boolean
   */
  @SuppressWarnings("unchecked")
  private boolean validateUsrRequest(Map<String, Object> req, RequestContext context) {
    Map<String, Object> data = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    data.putAll(req);
    String userId = (String) data.get(JsonKey.USER_ID);
    String userExternalId = (String) data.get(JsonKey.USER_EXTERNAL_ID);
    String userName = (String) data.get(JsonKey.USERNAME);
    if (StringUtils.isBlank(userId)
        && StringUtils.isBlank(userExternalId)
        && StringUtils.isBlank(userName)) {
      ProjectCommonException exception =
          new ProjectCommonException(
              ResponseCode.usrValidationError.getErrorCode(),
              ResponseCode.usrValidationError.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
      sender().tell(exception, self());
      return false;
    }
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Map<String, Object> requestDbMap = new HashMap<>();
    if (StringUtils.isNotBlank((String) data.get(JsonKey.USER_ID))) {
      requestDbMap.put(JsonKey.ID, data.get(JsonKey.USER_ID));
      req.put(JsonKey.USER_ID, userId);
      Response response =
          cassandraOperation.getRecordById(
              usrDbInfo.getKeySpace(),
              usrDbInfo.getTableName(),
              (String) data.get(JsonKey.USER_ID),
              context);
      List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (list.isEmpty()) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidUsrData.getErrorCode(),
                ResponseCode.invalidUsrData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return false;
      }
    } else {
      userId = getUserIdByUserLookUp(JsonKey.USERNAME.toLowerCase(), userName, context);
      if (StringUtils.isBlank(userId)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidUsrData.getErrorCode(),
                ResponseCode.invalidUsrData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return false;
      }
      req.put(JsonKey.USER_ID, userId);
    }
    return true;
  }

  private String getUserIdByUserLookUp(String type, String value, RequestContext context) {
    try {
      value = encryptionService.encryptData(value, context);
    } catch (Exception e) {
      logger.info(context, "Exception occurred while encrypting email/phone " + e);
    }
    Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOKUP);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.TYPE, type);
    reqMap.put(JsonKey.VALUE, value);
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            userLookUp.getKeySpace(), userLookUp.getTableName(), reqMap, context);
    List<Map<String, Object>> userMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(userMapList)) {
      Map<String, Object> userMap = userMapList.get(0);
      return (String) userMap.get(JsonKey.USER_ID);
    }
    return "";
  }

  private String getRootOrgIdFromSlug(String slug, RequestContext context) {
    if (!StringUtils.isBlank(slug)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.SLUG, slug);
      filters.put(JsonKey.IS_ROOT_ORG, true);
      Map<String, Object> esResult =
          elasticSearchComplexSearch(
              filters, EsIndex.sunbird.getIndexName(), EsType.organisation.getTypeName(), context);
      if (isNotNull(esResult)
          && esResult.containsKey(JsonKey.CONTENT)
          && isNotNull(esResult.get(JsonKey.CONTENT))
          && (!((List) esResult.get(JsonKey.CONTENT)).isEmpty())) {
        Map<String, Object> esContent =
            ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
        return (String) esContent.getOrDefault(JsonKey.ID, "");
      }
    }
    return "";
  }

  private boolean isSlugUnique(String slug, RequestContext context) {
    if (!StringUtils.isBlank(slug)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.SLUG, slug);
      filters.put(JsonKey.IS_ROOT_ORG, true);
      Map<String, Object> esResult =
          elasticSearchComplexSearch(
              filters, EsIndex.sunbird.getIndexName(), EsType.organisation.getTypeName(), context);
      if (isNotNull(esResult)
          && esResult.containsKey(JsonKey.CONTENT)
          && isNotNull(esResult.get(JsonKey.CONTENT))) {
        return (((List) esResult.get(JsonKey.CONTENT)).isEmpty());
      }
    }
    return false;
  }

  private Map<String, Object> elasticSearchComplexSearch(
      Map<String, Object> filters, String index, String type, RequestContext context) {

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> resultF = esService.search(searchDTO, type, context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return esResponse;
  }

  /**
   * validates if channel is already present in the organisation while Updating
   *
   * @param channel
   * @return boolean
   */
  @SuppressWarnings("unchecked")
  private boolean validateChannelUniqueness(String channel, String orgId, RequestContext context) {
    if (!StringUtils.isBlank(channel)) {
      return validateFieldUniqueness(JsonKey.CHANNEL, channel, orgId, context);
    }
    return (orgId == null);
  }

  private boolean validateChannelExternalIdUniqueness(
      String channel, String externalId, String orgId, RequestContext context) {
    OrgExternalService orgExternalService = new OrgExternalService();
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      String orgIdFromDb =
          orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
              StringUtils.lowerCase(externalId), StringUtils.lowerCase(channel), context);
      if (StringUtils.isEmpty(orgIdFromDb)) {
        return true;
      } else {
        if (orgId == null) {
          return false;
        }
        if (orgIdFromDb.equalsIgnoreCase(orgId)) {
          return true;
        } else {
          return false;
        }
      }
    }
    return false;
  }

  private boolean validateFieldUniqueness(
      String key, String value, String orgId, RequestContext context) {
    if (value != null) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(key, value);
      SearchDTO searchDto = new SearchDTO();
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filters);
      Future<Map<String, Object>> resultF =
          esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
      Map<String, Object> result =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
      List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
      if ((list.isEmpty())) {
        return true;
      } else {
        if (orgId == null) {
          return false;
        }
        Map<String, Object> data = list.get(0);
        String id = (String) data.get(JsonKey.ID);
        if (id.equalsIgnoreCase(orgId)) {
          return true;
        } else {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * This method will do the channel uniqueness validation
   *
   * @param req
   */
  private void validateChannel(Map<String, Object> req, RequestContext context) {
    // this if will run for suborg creation, it will fetch
    // rootOrgId from passed channel value.
    if (!req.containsKey(JsonKey.IS_ROOT_ORG) || !(Boolean) req.get(JsonKey.IS_ROOT_ORG)) {
      String channel = (String) req.get(JsonKey.CHANNEL);

      Map<String, Object> rootOrg = getRootOrgFromChannel(channel, context);
      if (MapUtils.isEmpty(rootOrg)) {
        logger.info(
            context, "OrganisationManagementActor:validateChannel: Invalid channel = " + channel);
        throw new ProjectCommonException(
            ResponseCode.invalidChannel.getErrorCode(),
            ResponseCode.invalidChannel.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      String rootOrgId = (String) rootOrg.get(JsonKey.ID);
      if (!StringUtils.isBlank(rootOrgId)) {
        req.put(JsonKey.ROOT_ORG_ID, rootOrgId);
      } else {
        logger.info(
            context, "OrganisationManagementActor:validateChannel: Invalid channel = " + channel);
        throw new ProjectCommonException(
            ResponseCode.invalidChannel.getErrorCode(),
            ResponseCode.invalidChannel.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
      Object status = rootOrg.get(JsonKey.STATUS);
      if (null != status && 1 != (Integer) status) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorInactiveOrg,
            ProjectUtil.formatMessage(
                ResponseCode.errorInactiveOrg.getErrorMessage(), JsonKey.CHANNEL, channel));
      }
    } else if (!validateChannelUniqueness((String) req.get(JsonKey.CHANNEL), null, context)) {
      logger.info(
          context, "OrganisationManagementActor:validateChannel: Channel validation failed");
      throw new ProjectCommonException(
          ResponseCode.channelUniquenessInvalid.getErrorCode(),
          ResponseCode.channelUniquenessInvalid.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /*
   * This method will fetch root org details from elastic search based on channel value.
   */
  private Map<String, Object> getRootOrgFromChannel(String channel, RequestContext context) {
    logger.info(context, "OrganisationManagementActor:getRootOrgFromChannel: channel = " + channel);
    if (StringUtils.isNotBlank(channel)) {
      Map<String, Object> filterMap = new HashMap<>();
      filterMap.put(JsonKey.CHANNEL, channel);
      filterMap.put(JsonKey.IS_ROOT_ORG, true);
      SearchDTO searchDTO = new SearchDTO();
      searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filterMap);
      Future<Map<String, Object>> esResponseF =
          esService.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName(), context);
      Map<String, Object> esResponse =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);

      List<Map<String, Object>> list = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(list)) {
        return list.get(0);
      }
    }
    return new HashMap();
  }

  /*
   * This method will validate the locationId and locationCode.
   */
  @SuppressWarnings("unchecked")
  private void validateOrgLocation(Map<String, Object> request, RequestContext context) {
    List<Map<String, String>> orgLocationList =
        (List<Map<String, String>>) request.get(JsonKey.ORG_LOCATION);
    if (CollectionUtils.isEmpty(orgLocationList)) {
      return;
    }
    List<String> locList = new ArrayList<>();
    orgLocationList
        .stream()
        .forEach(
            loc -> {
              locList.add(loc.get(JsonKey.ID));
            });
    List<String> locationIdsList =
        validator.getHierarchyLocationIds(
            getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()), locList);

    List<Map<String, String>> newOrgLocationList = new ArrayList<>();
    List<Location> locationList =
        locationClient.getLocationByIds(
            getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
            locationIdsList,
            context);
    locationList
        .stream()
        .forEach(
            location -> {
              Map<String, String> map = new HashMap<>();
              map.put(JsonKey.ID, location.getId());
              map.put(JsonKey.TYPE, location.getType());
              newOrgLocationList.add(map);
            });
    ObjectMapper mapper = new ObjectMapper();
    try {
      String orgLoc = mapper.writeValueAsString(newOrgLocationList);
      request.put(JsonKey.ORG_LOCATION, orgLoc);
    } catch (JsonProcessingException e) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

  private Map<String, Object> getOrgById(String id, RequestContext context) {
    Map<String, Object> responseMap = new HashMap<>();
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Response response =
        cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), id, context);
    Map<String, Object> record = response.getResult();
    if (null != record && null != record.get(JsonKey.RESPONSE)) {
      if (((List) record.get(JsonKey.RESPONSE)).size() != 0) {
        responseMap = (Map<String, Object>) ((List) record.get(JsonKey.RESPONSE)).get(0);
      }
      logger.info(context, "OrganisationManagementActor:getOrgById found org with Id: " + id);
    }
    return responseMap;
  }

  private boolean isRootOrgIdValid(String id, RequestContext context) {
    Map<String, Object> orgDbMap = getOrgById(id, context);
    return MapUtils.isNotEmpty(orgDbMap) ? (boolean) orgDbMap.get(JsonKey.IS_ROOT_ORG) : false;
  }

  private void throwExceptionForInvalidRootOrg(String id) {
    logger.info(
        "OrganisationManagementActor:throwExceptionForInvalidRootOrg no root org found with Id: "
            + id);
    throw new ProjectCommonException(
        ResponseCode.invalidRequestData.getErrorCode(),
        ResponseCode.invalidOrgId.getErrorMessage(),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private void assignKey(Request request) {
    addKeysToRequestMap(request);
    removeUnusedField(request);
    if (!isRootOrgIdValid((String) request.get(JsonKey.ID), request.getRequestContext())) {
      throwExceptionForInvalidRootOrg((String) request.get(JsonKey.ID));
    }
    Response response = updateCassandraOrgRecord(request.getRequest(), request.getRequestContext());
    sender().tell(response, self());
    logger.info(
        request.getRequestContext(),
        "OrganisationManagementActor:assignKey keys assigned to root org with Id: "
            + request.get(JsonKey.ID));
    updateOrgInfoToES(request.getRequest(), request.getRequestContext());
  }

  private void removeUnusedField(Request request) {
    request.getRequest().remove(JsonKey.ENC_KEYS);
    request.getRequest().remove(JsonKey.SIGN_KEYS);
    request.getRequest().remove(JsonKey.USER_ID);
  }

  private void addKeysToRequestMap(Request request) {
    List<String> encKeys = (List<String>) request.get(JsonKey.ENC_KEYS);
    List<String> signKeys = (List<String>) request.get(JsonKey.SIGN_KEYS);
    Map<String, List<String>> keys = new HashMap<>();
    keys.put(JsonKey.ENC_KEYS, encKeys);
    keys.put(JsonKey.SIGN_KEYS, signKeys);
    request.getRequest().put(JsonKey.KEYS, keys);
  }

  private void updateOrgInfoToES(Map<String, Object> updatedOrgMap, RequestContext context) {
    Request orgRequest = new Request();
    orgRequest.setRequestContext(context);
    orgRequest.getRequest().put(JsonKey.ORGANISATION, updatedOrgMap);
    orgRequest.setOperation(ActorOperations.UPDATE_ORG_INFO_ELASTIC.getValue());
    tellToAnother(orgRequest);
  }

  private Response updateCassandraOrgRecord(Map<String, Object> reqMap, RequestContext context) {
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    return cassandraOperation.updateRecord(
        orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), reqMap, context);
  }
}
