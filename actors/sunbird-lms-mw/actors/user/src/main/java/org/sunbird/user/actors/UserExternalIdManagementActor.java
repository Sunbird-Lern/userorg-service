package org.sunbird.user.actors;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.util.UserActorOperations;

@ActorConfig(
  tasks = {"upsertUserExternalIdentityDetails"},
  asyncTasks = {"upsertUserExternalIdentityDetails"}
)
public class UserExternalIdManagementActor extends BaseActor {

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static DecryptionService service =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);

  @Override
  public void onReceive(Request request) throws Throwable {
    if (UserActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      upsertUserExternalIdentityDetails(request);
    } else {
      onReceiveUnsupportedOperation("UserExternalIdManagementActor");
    }
  }

  @SuppressWarnings("unchecked")
  private void upsertUserExternalIdentityDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String operationtype = (String) requestMap.get(JsonKey.OPERATION_TYPE);
    requestMap.remove(JsonKey.OPERATION_TYPE);
    List<Map<String, String>> externalIds =
        (List<Map<String, String>>) requestMap.get(JsonKey.EXTERNAL_IDS);
    List<Map<String, Object>> responseExternalIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      Response response = new Response();
      List<String> errMsgs = new ArrayList<>();
      for (Map<String, String> extIdsMap : externalIds) {
        try {
          if (JsonKey.CREATE.equalsIgnoreCase(operationtype)) {
            if (StringUtils.isBlank(extIdsMap.get(JsonKey.OPERATION))
                || JsonKey.ADD.equalsIgnoreCase(extIdsMap.get(JsonKey.OPERATION))) {
              responseExternalIdList.add(
                  upsertUserExternalIdentityData(extIdsMap, requestMap, JsonKey.CREATE));
            }
          } else {
            updateUserExtId(requestMap, responseExternalIdList);
          }
        } catch (Exception e) {
          errMsgs.add(e.getMessage());
          ProjectLogger.log(
              "UserExternalIdManagementActor:upsertUserExternalIdentityDetails: Exception occurred with error message = "
                  + e.getMessage(),
              e);
        }
        response.put(JsonKey.EXTERNAL_IDS, responseExternalIdList);
        response.put(JsonKey.KEY, JsonKey.EXTERNAL_IDS);
        if (CollectionUtils.isNotEmpty(errMsgs)) {
          response.put(JsonKey.ERROR_MSG, errMsgs);
        } else {
          response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        }
        sender().tell(response, self());
      }
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  public static void updateUserExtId(
      Map<String, Object> requestMap, List<Map<String, Object>> responseExternalIdList) {
    List<Map<String, String>> dbResExternalIds = getUserExternalIds(requestMap);
    List<Map<String, String>> externalIds =
        (List<Map<String, String>>) requestMap.get(JsonKey.EXTERNAL_IDS);
    if (CollectionUtils.isNotEmpty(externalIds)) {
      // will not allow user to update idType value, if user will try to update idType will
      // ignore
      // user will have only one entry for a idType for given provider so get extId based on idType
      // List of idType values for a user will distinct and unique
      for (Map<String, String> extIdMap : externalIds) {
        Optional<Map<String, String>> extMap = checkExternalID(dbResExternalIds, extIdMap);
        Map<String, String> map = extMap.orElse(null);
        // Allowed operation type for externalIds ("add", "remove", "edit")
        if (JsonKey.ADD.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))
            || StringUtils.isBlank(extIdMap.get(JsonKey.OPERATION))) {
          if (MapUtils.isEmpty(map)) {
            responseExternalIdList.add(
                upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.CREATE));
          } else {
            // if external Id with same provider and idType exist then delete first then update
            // to update user externalId first we need to delete the record as externalId is the
            // part of composite key
            deleteUserExternalId(map);
            responseExternalIdList.add(
                upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE));
          }
        } else {
          // operation is either edit or remove
          if (MapUtils.isNotEmpty(map)) {
            if (JsonKey.REMOVE.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              if (StringUtils.isNotBlank(map.get(JsonKey.ID_TYPE))
                  && StringUtils.isNotBlank((String) requestMap.get(JsonKey.USER_ID))
                  && StringUtils.isNotBlank(map.get(JsonKey.PROVIDER))) {
                deleteUserExternalId(map);
              }
            } else if (JsonKey.EDIT.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              // to update user externalId first we need to delete the record as externalId is the
              // part of composite key
              deleteUserExternalId(map);
              responseExternalIdList.add(
                  upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE));
            }
          } else {
            throwExternalIDNotFoundException(
                extIdMap.get(JsonKey.ID),
                extIdMap.get(JsonKey.ID_TYPE),
                extIdMap.get(JsonKey.PROVIDER));
          }
        }
      }
    }
  }

  private static Optional<Map<String, String>> checkExternalID(
      List<Map<String, String>> dbResExternalIds, Map<String, String> extIdMap) {
    Optional<Map<String, String>> extMap =
        dbResExternalIds
            .stream()
            .filter(
                s -> {
                  if (((s.get(JsonKey.ID_TYPE)).equalsIgnoreCase(extIdMap.get(JsonKey.ID_TYPE)))
                      && ((s.get(JsonKey.PROVIDER))
                          .equalsIgnoreCase(extIdMap.get(JsonKey.PROVIDER)))) {
                    return true;
                  } else {
                    return false;
                  }
                })
            .findFirst();
    return extMap;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, String>> getUserExternalIds(Map<String, Object> requestMap) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD,
            JsonKey.USR_EXT_IDNT_TABLE,
            JsonKey.USER_ID,
            requestMap.get(JsonKey.USER_ID));
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private static void deleteUserExternalId(Map<String, String> map) {
    map.remove(JsonKey.LAST_UPDATED_BY);
    map.remove(JsonKey.CREATED_BY);
    map.remove(JsonKey.LAST_UPDATED_ON);
    map.remove(JsonKey.CREATED_ON);
    map.remove(JsonKey.USER_ID);
    map.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
    map.remove(JsonKey.ORIGINAL_ID_TYPE);
    map.remove(JsonKey.ORIGINAL_PROVIDER);
    cassandraOperation.deleteRecord(JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, map);
  }

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.externalIdNotFound.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.externalIdNotFound.getErrorMessage(), externalId, idType, provider),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private static Map<String, Object> upsertUserExternalIdentityData(
      Map<String, String> extIdsMap, Map<String, Object> requestMap, String operation) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.EXTERNAL_ID, extIdsMap.get(JsonKey.ID));
    map.put(JsonKey.ORIGINAL_EXTERNAL_ID, extIdsMap.get(JsonKey.ORIGINAL_EXTERNAL_ID));
    map.put(JsonKey.PROVIDER, extIdsMap.get(JsonKey.PROVIDER));
    map.put(JsonKey.ORIGINAL_PROVIDER, extIdsMap.get(JsonKey.ORIGINAL_PROVIDER));
    map.put(JsonKey.ID_TYPE, extIdsMap.get(JsonKey.ID_TYPE));
    map.put(JsonKey.ORIGINAL_ID_TYPE, extIdsMap.get(JsonKey.ORIGINAL_ID_TYPE));
    map.put(JsonKey.USER_ID, requestMap.get(JsonKey.USER_ID));
    if (JsonKey.CREATE.equalsIgnoreCase(operation)) {
      map.put(JsonKey.CREATED_BY, requestMap.get(JsonKey.CREATED_BY));
      map.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    } else {
      map.put(JsonKey.LAST_UPDATED_BY, requestMap.get(JsonKey.UPDATED_BY));
      map.put(JsonKey.LAST_UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    }
    cassandraOperation.upsertRecord(JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, map);
    return map;
  }
}
