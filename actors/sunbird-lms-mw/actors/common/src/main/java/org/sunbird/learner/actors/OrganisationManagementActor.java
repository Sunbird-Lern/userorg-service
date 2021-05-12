package org.sunbird.learner.actors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.Slug;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
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
  tasks = {"createOrg", "updateOrg", "updateOrgStatus", "getOrgDetails", "assignKeys"},
  asyncTasks = {}
)
public class OrganisationManagementActor extends BaseActor {
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final LocationRequestValidator validator = new LocationRequestValidator();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private LocationClient locationClient = LocationClientImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.ORGANISATION);
    if (request.getOperation().equalsIgnoreCase(ActorOperations.CREATE_ORG.getValue())) {
      createOrg(request);
    } else if (request.getOperation().equalsIgnoreCase(ActorOperations.UPDATE_ORG.getValue())) {
      updateOrgData(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_ORG_STATUS.getValue())) {
      updateOrgStatus(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_ORG_DETAILS.getValue())) {
      getOrgDetails(request);
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
      String orgType = (String) request.get(JsonKey.ORG_TYPE);
      validateOrgType(orgType, JsonKey.CREATE);
      request.put(JsonKey.ORG_TYPE, OrgTypeEnum.getValueByType(orgType));
      // Channel is mandatory for all org
      channelMandatoryValidation(request);
      String channel = (String) request.get(JsonKey.CHANNEL);
      validateChannel(request, actorMessage.getRequestContext());

      Boolean isTenant = (Boolean) request.get(JsonKey.IS_TENANT);
      String slug = Slug.makeSlug((String) request.getOrDefault(JsonKey.CHANNEL, ""), true);
      if (null != isTenant && isTenant) {
        String orgId = getOrgIdFromSlug(slug, actorMessage.getRequestContext());
        if (StringUtils.isBlank(orgId)) {
          request.put(JsonKey.SLUG, slug);
        } else {
          ProjectCommonException.throwClientErrorException(ResponseCode.slugIsNotUnique);
        }
      } else {
        request.put(JsonKey.SLUG, slug);
      }

      validateOrgLocation(request, actorMessage.getRequestContext());

      String passedExternalId = (String) request.get(JsonKey.EXTERNAL_ID);
      if (StringUtils.isNotBlank(passedExternalId)) {
        passedExternalId = passedExternalId.toLowerCase();
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

      String createdBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
      request.put(JsonKey.CREATED_BY, createdBy);
      request.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
      String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
      request.put(JsonKey.ID, uniqueId);
      // RootOrgId will always be same as orgId
      request.put(JsonKey.ROOT_ORG_ID, uniqueId);

      if (JsonKey.BULK_ORG_UPLOAD.equalsIgnoreCase(callerId)) {
        if (null == request.get(JsonKey.STATUS)) {
          request.put(JsonKey.STATUS, ProjectUtil.OrgStatus.ACTIVE.getValue());
        }
      } else {
        request.put(JsonKey.STATUS, ProjectUtil.OrgStatus.ACTIVE.getValue());
      }

      if (null != isTenant && isTenant) {
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
        request.put(JsonKey.IS_TENANT, false);
        request.put(JsonKey.IS_SSO_ROOTORG_ENABLED, false);
      }
      Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      // This will remove all extra unnecessary parameter from request
      ObjectMapper mapper = new ObjectMapper();
      Organisation org = mapper.convertValue(request, Organisation.class);
      request = mapper.convertValue(org, Map.class);
      try {
        String orgLoc = mapper.writeValueAsString(org.getOrgLocation());
        request.put(JsonKey.ORG_LOCATION, orgLoc);
      } catch (JsonProcessingException e) {
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
      Response result =
          cassandraOperation.insertRecord(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              request,
              actorMessage.getRequestContext());

      if (StringUtils.isNotBlank(passedExternalId)) {
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
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      Map<String, Object> orgExtIdRequest = new WeakHashMap<>(3);
      orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
      orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
      orgExtIdRequest.put(JsonKey.ORG_ID, orgId);
      cassandraOperation.insertRecord(
          JsonKey.SUNBIRD, JsonKey.ORG_EXT_ID_DB, orgExtIdRequest, context);
    }
  }

  private void deleteOrgExternalIdRecord(
      String channel, String externalId, RequestContext context) {
    if (StringUtils.isNotBlank(channel) && StringUtils.isNotBlank(externalId)) {
      Map<String, String> orgExtIdRequest = new WeakHashMap<>(3);
      orgExtIdRequest.put(JsonKey.PROVIDER, StringUtils.lowerCase(channel));
      orgExtIdRequest.put(JsonKey.EXTERNAL_ID, StringUtils.lowerCase(externalId));
      cassandraOperation.deleteRecord(
          JsonKey.SUNBIRD, JsonKey.ORG_EXT_ID_DB, orgExtIdRequest, context);
    }
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

    if (StringUtils.isNotBlank(orgType) && !orgTypeList.contains(orgType)) {
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
      validateOrgRequest(request, actorMessage.getRequestContext());
      Map<String, Object> orgDao;
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
      Map<String, Object> updateOrgDao = new HashMap<>();
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
      validateOrgRequest(request, actorMessage.getRequestContext());
      if (request.containsKey(JsonKey.EMAIL)
          && !EmailValidator.isEmailValid((String) request.get(JsonKey.EMAIL))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.emailFormatError);
      }
      String orgType = (String) request.get(JsonKey.ORG_TYPE);
      validateOrgType(orgType, JsonKey.UPDATE);
      if (StringUtils.isNotBlank(orgType)) {
        request.put(JsonKey.ORG_TYPE, OrgTypeEnum.getValueByType(orgType));
      }
      String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
      OrgService orgService = OrgServiceImpl.getInstance();
      Map<String, Object> dbOrgDetails =
          orgService.getOrgById(orgId, actorMessage.getRequestContext());
      if (MapUtils.isEmpty(dbOrgDetails)) {
        logger.info(
            actorMessage.getRequestContext(),
            "OrganisationManagementActor: updateOrgData invalid orgId");
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      String existingExternalId = (String) dbOrgDetails.get(JsonKey.EXTERNAL_ID);
      validateOrgLocation(request, actorMessage.getRequestContext());
      if (request.containsKey(JsonKey.CHANNEL)
          && !validateChannelUniqueness(
              (String) request.get(JsonKey.CHANNEL),
              (String) request.get(JsonKey.ORGANISATION_ID),
              (Boolean) dbOrgDetails.get(JsonKey.IS_TENANT),
              actorMessage.getRequestContext())) {
        logger.info(actorMessage.getRequestContext(), "Channel validation failed");
        ProjectCommonException.throwClientErrorException(ResponseCode.channelUniquenessInvalid);
      }
      // allow lower case values for source and externalId to the database
      if (request.get(JsonKey.PROVIDER) != null) {
        request.put(JsonKey.PROVIDER, ((String) request.get(JsonKey.PROVIDER)).toLowerCase());
      } else {
        String reqChannel = (String) request.get(JsonKey.CHANNEL);
        String dbChannel = (String) dbOrgDetails.get(JsonKey.CHANNEL);
        if (StringUtils.isNotBlank(reqChannel) && !reqChannel.equalsIgnoreCase(dbChannel)) {
          request.put(JsonKey.PROVIDER, reqChannel.toLowerCase());
        }
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
      updateOrgDao.remove(JsonKey.ROOT_ORG_ID);
      updateOrgDao.remove(JsonKey.CONTACT_DETAILS);
      if (JsonKey.BULK_ORG_UPLOAD.equalsIgnoreCase(callerId)) {
        if (null == request.get(JsonKey.STATUS)) {
          updateOrgDao.remove(JsonKey.STATUS);
        }
      } else {
        updateOrgDao.remove(JsonKey.STATUS);
      }

      String updatedBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
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
          if (dbOrgDetails.containsKey(JsonKey.IS_TENANT)
              && (boolean) dbOrgDetails.get(JsonKey.IS_TENANT)) {
            String orgIdBySlug = getOrgIdFromSlug(slug, actorMessage.getRequestContext());
            if (StringUtils.isBlank(orgIdBySlug)
                || (StringUtils.isNotBlank(orgIdBySlug)
                    && orgIdBySlug.equalsIgnoreCase((String) dbOrgDetails.get(JsonKey.ID)))) {
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

      if (null != dbOrgDetails.get(JsonKey.IS_TENANT)
          && (boolean) dbOrgDetails.get(JsonKey.IS_TENANT)) {
        String channel = (String) dbOrgDetails.get(JsonKey.CHANNEL);
        String updateOrgDaoChannel = (String) updateOrgDao.get(JsonKey.CHANNEL);
        String license = (String) request.get(JsonKey.LICENSE);
        if (null != updateOrgDaoChannel && null != channel && !(updateOrgDaoChannel.equals(channel))
            || StringUtils.isNotBlank(license)) {
          Map<String, Object> tempMap = new HashMap<>();
          tempMap.put(JsonKey.CHANNEL, updateOrgDaoChannel);
          tempMap.put(JsonKey.HASHTAGID, dbOrgDetails.get(JsonKey.ID));
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
      try {
        if (CollectionUtils.isNotEmpty(org.getOrgLocation())) {
          String orgLoc = mapper.writeValueAsString(org.getOrgLocation());
          updateOrgDao.put(JsonKey.ORG_LOCATION, orgLoc);
        }
      } catch (JsonProcessingException e) {
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
      Response response =
          cassandraOperation.updateRecord(
              orgDbInfo.getKeySpace(),
              orgDbInfo.getTableName(),
              updateOrgDao,
              actorMessage.getRequestContext());
      response.getResult().put(JsonKey.ORGANISATION_ID, dbOrgDetails.get(JsonKey.ID));

      if (StringUtils.isNotBlank(passedExternalId)) {
        String channel =
            StringUtils.isNotBlank((String) request.get(JsonKey.CHANNEL))
                ? (String) request.get(JsonKey.CHANNEL)
                : (String) dbOrgDetails.get(JsonKey.CHANNEL);
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

      String orgLocation = (String) updateOrgDao.get(JsonKey.ORG_LOCATION);
      List orgLocationList = new ArrayList<>();
      if (StringUtils.isNotBlank(orgLocation)) {
        try {
          orgLocationList = mapper.readValue(orgLocation, List.class);
        } catch (Exception e) {
          logger.info(
              actorMessage.getRequestContext(),
              "Exception occurred while converting orgLocation to List<Map<String,String>>.");
        }
      }
      updateOrgDao.put(JsonKey.ORG_LOCATION, orgLocationList);

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

  /** Provides the details of the Organisation */
  private void getOrgDetails(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> request = actorMessage.getRequest();
    validateOrgRequest(request, actorMessage.getRequestContext());
    String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
    Future<Map<String, Object>> resultF =
        esService.getDataByIdentifier(
            ProjectUtil.EsType.organisation.getTypeName(), orgId, actorMessage.getRequestContext());
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    if (MapUtils.isNotEmpty(result)) {
      result.put(JsonKey.HASHTAGID, result.get(JsonKey.ID));
      if (null != result.get(JsonKey.ORGANISATION_TYPE)) {
        int orgType = (int) result.get(JsonKey.ORGANISATION_TYPE);
        boolean isSchool =
            (orgType == OrgTypeEnum.getValueByType(OrgTypeEnum.SCHOOL.getType())) ? true : false;
        result.put(JsonKey.IS_SCHOOL, isSchool);
      }
    }
    if (MapUtils.isEmpty(result)) {
      throw new ProjectCommonException(
          ResponseCode.orgDoesNotExist.getErrorCode(),
          ResponseCode.orgDoesNotExist.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    result.putAll(Util.getOrgDefaultValue());
    result.remove(JsonKey.CONTACT_DETAILS);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }

  public void channelMandatoryValidation(Map<String, Object> request) {
    if (StringUtils.isBlank((String) request.get(JsonKey.CHANNEL))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateOrgRequest(Map<String, Object> req, RequestContext context) {
    String orgId = (String) req.get(JsonKey.ORGANISATION_ID);
    String provider = (String) req.get(JsonKey.PROVIDER);
    String externalId = (String) req.get(JsonKey.EXTERNAL_ID);
    if (StringUtils.isBlank(orgId)) {
      if (StringUtils.isBlank(provider) || StringUtils.isBlank(externalId)) {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData.getErrorCode(),
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      } else {
        // fetch orgid from database on basis of source and external id and put orgid
        // into request .
        OrgExternalService orgExtService = new OrgExternalService();
        String organisationId =
            orgExtService.getOrgIdFromOrgExternalIdAndProvider(
                (String) req.get(JsonKey.EXTERNAL_ID), (String) req.get(JsonKey.PROVIDER), context);
        if (StringUtils.isEmpty(organisationId)) {
          throw new ProjectCommonException(
              ResponseCode.invalidRequestData.getErrorCode(),
              ResponseCode.invalidRequestData.getErrorMessage(),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        req.put(JsonKey.ORGANISATION_ID, organisationId);
      }
    }
  }

  private String getOrgIdFromSlug(String slug, RequestContext context) {
    if (!StringUtils.isBlank(slug)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.SLUG, slug);
      filters.put(JsonKey.IS_TENANT, true);
      Map<String, Object> esResult =
          elasticSearchComplexSearch(filters, EsType.organisation.getTypeName(), context);
      if (MapUtils.isNotEmpty(esResult)
          && esResult.containsKey(JsonKey.CONTENT)
          && (CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT)))) {
        Map<String, Object> esContent =
            ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
        return (String) esContent.getOrDefault(JsonKey.ID, "");
      }
    }
    return "";
  }

  private Map<String, Object> elasticSearchComplexSearch(
      Map<String, Object> filters, String type, RequestContext context) {
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> resultF = esService.search(searchDTO, type, context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return esResponse;
  }

  private boolean validateChannelUniqueness(
      String channel, String orgId, Boolean isTenant, RequestContext context) {
    if (StringUtils.isNotBlank(channel)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(JsonKey.CHANNEL, channel);
      filters.put(JsonKey.IS_TENANT, true);
      return validateChannelUniqueness(filters, orgId, isTenant, context);
    }
    return (orgId == null);
  }

  private boolean validateChannelUniqueness(
      Map<String, Object> filters, String orgId, Boolean isTenant, RequestContext context) {
    if (MapUtils.isNotEmpty(filters)) {
      SearchDTO searchDto = new SearchDTO();
      searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filters);
      Future<Map<String, Object>> resultF =
          esService.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
      Map<String, Object> result =
          (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
      List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
      if (CollectionUtils.isEmpty(list)) {
        if (StringUtils.isBlank(orgId)) {
          return true;
        } else {
          if (isTenant) {
            return true;
          } else {
            return false;
          }
        }
      } else {
        if (StringUtils.isBlank(orgId)) {
          return false;
        } else {
          Map<String, Object> data = list.get(0);
          String id = (String) data.get(JsonKey.ID);
          if (isTenant) {
            return id.equalsIgnoreCase(orgId);
          } else {
            // for suborg channel should be valid
            return true;
          }
        }
      }
    }
    return true;
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
        return orgIdFromDb.equalsIgnoreCase(orgId);
      }
    }
    return false;
  }

  private void validateChannel(Map<String, Object> req, RequestContext context) {
    String channel = (String) req.get(JsonKey.CHANNEL);
    if (!req.containsKey(JsonKey.IS_TENANT) || !(Boolean) req.get(JsonKey.IS_TENANT)) {
      Map<String, Object> rootOrg = getRootOrgFromChannel(channel, context);
      if (MapUtils.isEmpty(rootOrg)) {
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
    } else if (!validateChannelUniqueness((String) req.get(JsonKey.CHANNEL), null, null, context)) {
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
      filterMap.put(JsonKey.IS_TENANT, true);
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

  private void validateOrgLocation(Map<String, Object> request, RequestContext context) {
    List<String> locList = new ArrayList<>();
    List<Map<String, String>> orgLocationList =
        (List<Map<String, String>>) request.get(JsonKey.ORG_LOCATION);
    if (CollectionUtils.isEmpty(orgLocationList)) {
      // Request is from org upload
      if (CollectionUtils.isNotEmpty((List<String>) request.get(JsonKey.LOCATION_CODE))) {
        locList =
            validator.getValidatedLocationIds(
                getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()),
                (List<String>) request.get(JsonKey.LOCATION_CODE));
        request.remove(JsonKey.LOCATION_CODE);
      } else {
        return;
      }
    } else {
      List<String> finalLocList = locList;
      // If request orglocation is a list of map , which has location id, not location code
      orgLocationList
          .stream()
          .forEach(
              loc -> {
                if (loc.containsKey(JsonKey.ID)) {
                  finalLocList.add(loc.get(JsonKey.ID));
                }
              });
      // If request orglocation is a list of map , which doesn't have location id, but has location
      // code
      if (CollectionUtils.isEmpty(finalLocList)) {
        orgLocationList
            .stream()
            .forEach(
                loc -> {
                  if (loc.containsKey(JsonKey.CODE)) {
                    finalLocList.add(loc.get(JsonKey.CODE));
                  }
                });
        if (CollectionUtils.isNotEmpty(finalLocList)) {
          locList =
              validator.getValidatedLocationIds(
                  getActorRef(LocationActorOperation.SEARCH_LOCATION.getValue()), finalLocList);
        }
      }
    }
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
    request.put(JsonKey.ORG_LOCATION, newOrgLocationList);
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

  private boolean isTenantIdValid(String id, RequestContext context) {
    Map<String, Object> orgDbMap = getOrgById(id, context);
    return MapUtils.isNotEmpty(orgDbMap) ? (boolean) orgDbMap.get(JsonKey.IS_TENANT) : false;
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
    if (!isTenantIdValid((String) request.get(JsonKey.ID), request.getRequestContext())) {
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
