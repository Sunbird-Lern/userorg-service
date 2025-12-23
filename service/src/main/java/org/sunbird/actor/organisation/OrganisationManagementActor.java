package org.sunbird.actor.organisation;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.actor.organisation.validator.OrganisationRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.CloudStorageUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Slug;
import org.sunbird.util.Util;
import org.sunbird.validator.EmailValidator;

public class OrganisationManagementActor extends BaseActor {
  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final OrganisationRequestValidator orgValidator = new OrganisationRequestValidator();

  @Inject
  @Named("org_background_actor")
  private ActorRef organisationBackgroundActor;

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
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.ADD_ENCRYPTION_KEY.getValue())) {
      addEncryptionKey(request);
    } else {
      onReceiveUnsupportedOperation();
    }
  }

  /** Method to create an Organisation . */
  @SuppressWarnings("unchecked")
  private void createOrg(Request actorMessage) {
    logger.info(
        actorMessage.getRequestContext(), "OrgManagementActor: Create org method call start");
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    actorMessage.toLower();
    Map<String, Object> request = actorMessage.getRequest();
    if (request.containsKey(JsonKey.EMAIL)
        && !EmailValidator.isEmailValid((String) request.get(JsonKey.EMAIL))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.dataFormatError,
          MessageFormat.format(ResponseCode.dataFormatError.getErrorMessage(), JsonKey.EMAIL));
    }
    String orgType = (String) request.get(JsonKey.ORG_TYPE);
    String orgSubType = (String) request.get(JsonKey.ORG_SUB_TYPE);
    orgValidator.validateOrgType(orgType, orgSubType, JsonKey.CREATE);
    request.put(JsonKey.ORG_TYPE, OrgTypeValidator.getInstance().getValueByType(orgType));
    if (StringUtils.isNotBlank(orgSubType)) {
      request.put(JsonKey.ORG_SUB_TYPE, OrgTypeValidator.getInstance().getValueByType(orgSubType));
    }
    // Channel is mandatory for all org
    orgValidator.channelMandatoryValidation(request);
    String channel = (String) request.get(JsonKey.CHANNEL);
    orgValidator.validateChannel(request, actorMessage.getRequestContext());

    Boolean isTenant = (Boolean) request.get(JsonKey.IS_TENANT);
    orgValidator.validateSlug(request, actorMessage.getRequestContext());

    orgValidator.validateOrgLocation(request, actorMessage.getRequestContext());

    String passedExternalId = (String) request.get(JsonKey.EXTERNAL_ID);
    orgValidator.validateExternalId(request, actorMessage.getRequestContext());

    String createdBy = (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY);
    request.put(JsonKey.CREATED_BY, createdBy);
    request.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    String uniqueId = ProjectUtil.getUniqueIdFromTimestamp(actorMessage.getEnv());
    request.put(JsonKey.ID, uniqueId);
    // RootOrgId will always be same as orgId
    request.put(JsonKey.ROOT_ORG_ID, uniqueId);

    if (JsonKey.BULK_ORG_UPLOAD.equalsIgnoreCase(callerId)) {
      request.computeIfAbsent((JsonKey.STATUS), k -> ProjectUtil.OrgStatus.ACTIVE.getValue());
    } else {
      request.put(JsonKey.STATUS, ProjectUtil.OrgStatus.ACTIVE.getValue());
    }

    if (null != isTenant && isTenant) {
      boolean bool =
          orgService.registerChannel(request, JsonKey.CREATE, actorMessage.getRequestContext());
      request.put(
          JsonKey.IS_SSO_ROOTORG_ENABLED,
          request.containsKey(JsonKey.IS_SSO_ROOTORG_ENABLED)
              ? request.get(JsonKey.IS_SSO_ROOTORG_ENABLED)
              : false);
      if (!bool) {
        ProjectCommonException.throwServerErrorException(ResponseCode.channelRegFailed);
        return;
      }
    } else {
      request.put(JsonKey.IS_TENANT, false);
      request.put(JsonKey.IS_SSO_ROOTORG_ENABLED, false);
    }
    // This will remove all extra unnecessary parameter from request
    ObjectMapper mapper = new ObjectMapper();
    Organisation org = mapper.convertValue(request, Organisation.class);
    request = mapper.convertValue(org, Map.class);
    Response result = orgService.createOrganisation(request, actorMessage.getRequestContext());
    if (StringUtils.isNotBlank(passedExternalId)) {
      orgService.createOrgExternalIdRecord(
          channel, passedExternalId, uniqueId, actorMessage.getRequestContext());
    }
    logger.info(
        actorMessage.getRequestContext(),
        "OrgManagementActor : createOrg : Created org id is ----." + uniqueId);
    result.getResult().put(JsonKey.ORGANISATION_ID, uniqueId);
    sender().tell(result, self());
    saveDataToES(request, JsonKey.INSERT, actorMessage.getRequestContext());
    generateTelemetry(
        uniqueId,
        (Map<String, Object>) actorMessage.getRequest().get(JsonKey.ORGANISATION),
        JsonKey.CREATE,
        actorMessage);
  }

  /** Updates the status of the Organisation */
  @SuppressWarnings("unchecked")
  private void updateOrgStatus(Request actorMessage) {
    try {
      actorMessage.toLower();
      Map<String, Object> request = actorMessage.getRequest();
      orgValidator.validateOrgRequest(request, actorMessage.getRequestContext());

      String updatedBy = (String) request.get(JsonKey.REQUESTED_BY);
      String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
      Map<String, Object> orgDao = orgService.getOrgById(orgId, actorMessage.getRequestContext());
      if (MapUtils.isEmpty(orgDao)) {
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.invalidRequestData,
                ResponseCode.invalidRequestData.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
        return;
      }

      Integer currentStatus = (Integer) orgDao.get(JsonKey.STATUS);
      Integer nextStatus = (Integer) request.get(JsonKey.STATUS);
      if (!(orgService.checkOrgStatusTransition(currentStatus, nextStatus))) {
        logger.info(actorMessage.getRequestContext(), "Invalid Org State transation");
        ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
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
          orgService.updateOrganisation(updateOrgDao, actorMessage.getRequestContext());
      response.getResult().put(JsonKey.ORGANISATION_ID, orgDao.get(JsonKey.ID));

      saveDataToES(updateOrgDao, JsonKey.UPDATE, actorMessage.getRequestContext());

      sender().tell(response, self());

      generateTelemetry(orgId, new HashMap<>(), JsonKey.UPDATE_ORG_STATUS, actorMessage);
    } catch (ProjectCommonException e) {
      logger.error(actorMessage.getRequestContext(), e.getMessage(), e);
      sender().tell(e, self());
    }
  }

  private void generateTelemetry(
      String orgId, Map<String, Object> orgMap, String operation, Request actorMessage) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(orgId, JsonKey.ORGANISATION, operation, null);
    if (JsonKey.CREATE.equals(operation)) {
      TelemetryUtil.generateCorrelatedObject(orgId, JsonKey.ORGANISATION, null, correlatedObject);
    } else if (JsonKey.UPDATE_ORG_STATUS.equals(operation)) {
      orgMap.put("updateOrgStatus", "org status updated.");
    }
    TelemetryUtil.telemetryProcessingCall(
        orgMap, targetObject, correlatedObject, actorMessage.getContext());
  }

  /** Update the Organisation data */
  @SuppressWarnings("unchecked")
  private void updateOrgData(Request actorMessage) {
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);
    try {
      actorMessage.toLower();
      Map<String, Object> request = actorMessage.getRequest();

      orgValidator.validateOrgRequest(request, actorMessage.getRequestContext());
      if (request.containsKey(JsonKey.EMAIL)
          && !EmailValidator.isEmailValid((String) request.get(JsonKey.EMAIL))) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.dataFormatError,
            MessageFormat.format(ResponseCode.dataFormatError.getErrorMessage(), JsonKey.EMAIL));
      }
      String orgType = (String) request.get(JsonKey.ORG_TYPE);
      String orgSubType = (String) request.get(JsonKey.ORG_SUB_TYPE);
      orgValidator.validateOrgType(orgType, orgSubType, JsonKey.UPDATE);
      if (StringUtils.isNotBlank(orgType)) {
        request.put(JsonKey.ORG_TYPE, OrgTypeValidator.getInstance().getValueByType(orgType));
      }
      if (StringUtils.isNotBlank(orgSubType)) {
        request.put(
            JsonKey.ORG_SUB_TYPE, OrgTypeValidator.getInstance().getValueByType(orgSubType));
      }
      String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
      Map<String, Object> dbOrgDetails =
          orgService.getOrgById(orgId, actorMessage.getRequestContext());
      if (MapUtils.isEmpty(dbOrgDetails)) {
        logger.info(
            actorMessage.getRequestContext(),
            "OrganisationManagementActor: updateOrgData invalid orgId");
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData,
            ResponseCode.invalidRequestData.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }

      String existingExternalId = (String) dbOrgDetails.get(JsonKey.EXTERNAL_ID);
      orgValidator.validateOrgLocation(request, actorMessage.getRequestContext());
      if (request.containsKey(JsonKey.CHANNEL)
          && !orgValidator.validateChannelUniqueness(
              (String) request.get(JsonKey.CHANNEL),
              (String) request.get(JsonKey.ORGANISATION_ID),
              (Boolean) dbOrgDetails.get(JsonKey.IS_TENANT),
              actorMessage.getRequestContext())) {
        logger.info(actorMessage.getRequestContext(), "Channel validation failed");
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorDuplicateEntry,
            MessageFormat.format(
                ResponseCode.errorDuplicateEntry.getErrorMessage(),
                (String) request.get(JsonKey.CHANNEL),
                JsonKey.CHANNEL));
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
        if (!orgValidator.validateChannelExternalIdUniqueness(
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
            String orgIdBySlug =
                orgService.getOrgIdFromSlug(slug, actorMessage.getRequestContext());
            if (StringUtils.isBlank(orgIdBySlug)
                || (StringUtils.isNotBlank(orgIdBySlug)
                    && orgIdBySlug.equalsIgnoreCase((String) dbOrgDetails.get(JsonKey.ID)))) {
              updateOrgDao.put(JsonKey.SLUG, slug);
            } else {
              sender()
                  .tell(
                      ProjectUtil.createClientException(
                          ResponseCode.errorDuplicateEntry,
                          MessageFormat.format(
                              ResponseCode.errorDuplicateEntry.getErrorMessage(),
                              slug,
                              JsonKey.SLUG)),
                      self());
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
          boolean bool =
              orgService.registerChannel(request, JsonKey.UPDATE, actorMessage.getRequestContext());
          if (!bool) {
            ProjectCommonException.throwClientErrorException(ResponseCode.channelRegFailed);
            return;
          }
        }
      }
      ObjectMapper mapper = new ObjectMapper();
      // This will remove all extra unnecessary parameter from request
      Organisation org = mapper.convertValue(updateOrgDao, Organisation.class);
      updateOrgDao = mapper.convertValue(org, Map.class);
      Response response =
          orgService.updateOrganisation(updateOrgDao, actorMessage.getRequestContext());
      response.getResult().put(JsonKey.ORGANISATION_ID, dbOrgDetails.get(JsonKey.ID));

      if (StringUtils.isNotBlank(passedExternalId)) {
        String channel =
            StringUtils.isNotBlank((String) request.get(JsonKey.CHANNEL))
                ? (String) request.get(JsonKey.CHANNEL)
                : (String) dbOrgDetails.get(JsonKey.CHANNEL);
        if (StringUtils.isBlank(existingExternalId)) {
          orgService.createOrgExternalIdRecord(
              channel, passedExternalId, orgId, actorMessage.getRequestContext());
        } else {
          if (!existingExternalId.equalsIgnoreCase(passedExternalId)) {
            orgService.deleteOrgExternalIdRecord(
                channel, existingExternalId, actorMessage.getRequestContext());
            orgService.createOrgExternalIdRecord(
                channel, passedExternalId, orgId, actorMessage.getRequestContext());
          }
        }
      }
      saveDataToES(updateOrgDao, JsonKey.UPDATE, actorMessage.getRequestContext());

      sender().tell(response, self());
      generateTelemetry(
          (String) dbOrgDetails.get(JsonKey.ID), updateOrgDao, JsonKey.UPDATE, actorMessage);
    } catch (ProjectCommonException e) {
      sender().tell(e, self());
    }
  }

  private void saveDataToES(Map<String, Object> locData, String opType, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(ActorOperations.UPSERT_ORGANISATION_TO_ES.getValue());
    request.getRequest().put(JsonKey.ORGANISATION, locData);
    request.getRequest().put(JsonKey.OPERATION_TYPE, opType);
    try {
      organisationBackgroundActor.tell(request, self());
    } catch (Exception ex) {
      logger.error(context, "LocationActor:saveDataToES: Exception occurred", ex);
    }
  }
  /** Provides the details of the Organisation */
  private void getOrgDetails(Request actorMessage) {
    actorMessage.toLower();
    Map<String, Object> request = actorMessage.getRequest();

    orgValidator.validateOrgRequest(request, actorMessage.getRequestContext());
    String orgId = (String) request.get(JsonKey.ORGANISATION_ID);
    Map<String, Object> result = orgService.getOrgById(orgId, actorMessage.getRequestContext());
    if (MapUtils.isNotEmpty(result)) {
      result.put(JsonKey.HASHTAGID, result.get(JsonKey.ID));
      if (null != result.get(JsonKey.ORGANISATION_TYPE)) {
        int orgType = (int) result.get(JsonKey.ORGANISATION_TYPE);
        boolean isSchool =
            (orgType == OrgTypeValidator.getInstance().getValueByType(JsonKey.ORG_TYPE_SCHOOL))
                ? true
                : false;
        result.put(JsonKey.IS_SCHOOL, isSchool);
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(
              ResponseCode.resourceNotFound.getErrorMessage(), JsonKey.ORGANISATION),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    result.putAll(Util.getOrgDefaultValue());
    result.remove(JsonKey.CONTACT_DETAILS);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }

  private void assignKey(Request request) {
    addKeysToRequestMap(request);
    removeUnusedField(request);
    orgValidator.isTenantIdValid((String) request.get(JsonKey.ID), request.getRequestContext());
    Response response =
        orgService.updateOrganisation(request.getRequest(), request.getRequestContext());
    sender().tell(response, self());
    saveDataToES(request.getRequest(), JsonKey.UPDATE, request.getRequestContext());
  }

  private void removeUnusedField(Request request) {
    request.getRequest().remove(JsonKey.ENC_KEYS);
    request.getRequest().remove(JsonKey.SIGN_KEYS);
    request.getRequest().remove(JsonKey.EXHAUST_ENCRYPTION_KEY);
    request.getRequest().remove(JsonKey.USER_ID);
  }

  private void addKeysToRequestMap(Request request) {
    List<String> encKeys = (List<String>) request.get(JsonKey.ENC_KEYS);
    List<String> signKeys = (List<String>) request.get(JsonKey.SIGN_KEYS);
    List<String> exhaustKeys;

    Map<String, List<String>> keys = new HashMap<>();
    keys.put(JsonKey.ENC_KEYS, encKeys);
    keys.put(JsonKey.SIGN_KEYS, signKeys);

    String orgId = (String) request.get(JsonKey.ID);
    Map<String, List<String>> fetchedKeys = getKeysInDB(request, orgId);

    if (fetchedKeys != null && fetchedKeys.containsKey(JsonKey.EXHAUST_ENCRYPTION_KEY)) {
      exhaustKeys = fetchedKeys.get(JsonKey.EXHAUST_ENCRYPTION_KEY);
      keys.put(JsonKey.EXHAUST_ENCRYPTION_KEY, exhaustKeys);
    }
    request.getRequest().put(JsonKey.KEYS, keys);
  }

  private void addEncryptionKey(Request request) {
    RequestContext context = request.getRequestContext();
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    Map<String, Object> req = (Map<String, Object>) request.getRequest().get(JsonKey.DATA);
    String orgId = (String) req.get(JsonKey.ORGANISATION_ID);

    Map<String, List<String>> fetchedKeys = getKeysInDB(request, orgId);
    String publicKeyUrl = uploadEncryptionFile(req, context, processId, fetchedKeys);
    Response response = updateEncryptionKey(request, orgId, fetchedKeys, publicKeyUrl);
    sender().tell(response, self());
  }

  private String uploadEncryptionFile(
      Map<String, Object> req,
      RequestContext context,
      String processId,
      Map<String, List<String>> fetchedKeys) {
    String fileExtension = "";
    String fileName = (String) req.get(JsonKey.FILE_NAME);
    if (!StringUtils.isBlank(fileName)) {
      String[] split = fileName.split("\\.");
      if (split.length > 1) {
        fileExtension = split[split.length - 1];
      }
    }
    String fName = "File-" + processId;
    if (!StringUtils.isBlank(fileExtension)) {
      fName = fName + "." + fileExtension.toLowerCase();
      logger.info(context, "File - " + fName + " Extension is " + fileExtension);
    }

    File file = new File(fName);
    FileOutputStream fos = null;
    String publicKeyUrl = null;
    try {
      fos = new FileOutputStream(file);
      fos.write((byte[]) req.get(JsonKey.FILE));
      String cspProvider = ProjectUtil.getConfigValue(JsonKey.CLOUD_SERVICE_PROVIDER);
      if (StringUtils.isBlank(cspProvider)) {
        logger.info(context, "OrganisationManagementActor:: The cloud service is not available");
        ProjectCommonException exception =
            new ProjectCommonException(
                ResponseCode.errorUnsupportedCloudStorage,
                ProjectUtil.formatMessage(
                    ResponseCode.errorUnsupportedCloudStorage.getErrorMessage(), cspProvider),
                ResponseCode.CLIENT_ERROR.getResponseCode());
        sender().tell(exception, self());
      }
      String container = ProjectUtil.getConfigValue(JsonKey.CLOUD_SERVICE_CONTAINER);
      if (fetchedKeys != null
              && fetchedKeys.containsKey(JsonKey.EXHAUST_ENCRYPTION_KEY)
              && !fetchedKeys.get(JsonKey.EXHAUST_ENCRYPTION_KEY).isEmpty()) {
        String oldKey =
                fetchedKeys
                        .get(JsonKey.EXHAUST_ENCRYPTION_KEY)
                        .get(0)
                        .substring(
                                fetchedKeys.get(JsonKey.EXHAUST_ENCRYPTION_KEY).get(0).indexOf(container)
                                        + container.length()
                                        + 1);
        CloudStorageUtil.deleteFile(cspProvider, container, oldKey);
      }
      publicKeyUrl =
          CloudStorageUtil.upload(
              cspProvider,
              container,
              JsonKey.ORGANISATION
                  + File.separator
                  + req.get(JsonKey.ORGANISATION_ID)
                  + File.separator
                  + fileName,
              file.getAbsolutePath());
      return publicKeyUrl;
    } catch (IOException e) {
      throw new ProjectCommonException(
          ResponseCode.invalidEncryptionFile,
          ResponseCode.invalidEncryptionFile.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } finally {
      try {
        if (null != (fos)) {
          fos.close();
        }
        Files.delete(file.toPath());
      } catch (IOException e) {
        logger.error(
            context,
            "Exception Occurred while closing fileInputStream in OrganisationManagementActor",
            e);
      }
    }
  }

  private Map<String, List<String>> getKeysInDB(Request request, String orgId) {
    Map<String, List<String>> fetchedKeys = null;
    Map<String, Object> dbOrgDetails = orgService.getOrgById(orgId, request.getRequestContext());

    if (MapUtils.isEmpty(dbOrgDetails)) {
      logger.info(
          request.getRequestContext(),
          "OrganisationManagementActor: addEncryptionKey:: invalid orgId");
      throw new ProjectCommonException(
          ResponseCode.rootOrgAssociationError,
          ProjectUtil.formatMessage(ResponseCode.rootOrgAssociationError.getErrorMessage(), orgId),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (dbOrgDetails.containsKey("keys")) {
      fetchedKeys = (Map<String, List<String>>) dbOrgDetails.get("keys");
    }
    return fetchedKeys;
  }

  private Response updateEncryptionKey(
      Request request, String orgId, Map<String, List<String>> fetchedKeys, String publicKeyUrl) {
    List<String> exhaustKeys = new ArrayList<>();
    exhaustKeys.add(publicKeyUrl);
    Map<String, List<String>> updateKeys = null;

    if (fetchedKeys != null) {
      fetchedKeys.put(JsonKey.EXHAUST_ENCRYPTION_KEY, exhaustKeys);
      updateKeys = fetchedKeys;
    }

    if (updateKeys == null) {
      Map<String, List<String>> keys = new HashMap<>();
      keys.put(JsonKey.ENC_KEYS, new ArrayList<>());
      keys.put(JsonKey.SIGN_KEYS, new ArrayList<>());
      keys.put(JsonKey.EXHAUST_ENCRYPTION_KEY, exhaustKeys);
      updateKeys = keys;
    }

    HashMap<String, Object> updateOrgMap = new HashMap<>();
    updateOrgMap.put(JsonKey.ID, orgId);
    updateOrgMap.put(JsonKey.KEYS, updateKeys);

    Request updateRequest = new Request();
    updateRequest.setRequestContext(request.getRequestContext());
    updateRequest.setRequest(updateOrgMap);
    Response response =
        orgService.updateOrganisation(updateRequest.getRequest(), request.getRequestContext());
    saveDataToES(updateRequest.getRequest(), JsonKey.UPDATE, request.getRequestContext());
    return response;
  }
}
