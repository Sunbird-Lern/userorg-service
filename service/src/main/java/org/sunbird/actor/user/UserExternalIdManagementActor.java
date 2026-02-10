package org.sunbird.actor.user;

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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class UserExternalIdManagementActor extends BaseActor {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (ActorOperations.UPSERT_USER_EXTERNAL_IDENTITY_DETAILS
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      upsertUserExternalIdentityDetails(request);
    } else {
      onReceiveUnsupportedOperation();
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
                  upsertUserExternalIdentityData(
                      extIdsMap, requestMap, JsonKey.CREATE, request.getRequestContext()));
            }
          } else {
            updateUserExtId(requestMap, responseExternalIdList, request.getRequestContext());
          }
        } catch (Exception e) {
          logger.error(request.getRequestContext(), "Exception occurred with error message", e);
          errMsgs.add(e.getMessage());
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

  public void updateUserExtId(
      Map<String, Object> requestMap,
      List<Map<String, Object>> responseExternalIdList,
      RequestContext context) {
    List<Map<String, String>> dbResExternalIds = getUserExternalIds(requestMap, context);
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
                upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.CREATE, context));
          } else {
            // if external Id with same provider and idType exist then delete first then update
            // to update user externalId first we need to delete the record as externalId is the
            // part of composite key
            deleteUserExternalId(map, context);
            responseExternalIdList.add(
                upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE, context));
          }
        } else {
          // operation is either edit or remove
          if (MapUtils.isNotEmpty(map)) {
            if (JsonKey.REMOVE.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              if (StringUtils.isNotBlank(map.get(JsonKey.ID_TYPE))
                  && StringUtils.isNotBlank((String) requestMap.get(JsonKey.USER_ID))
                  && StringUtils.isNotBlank(map.get(JsonKey.PROVIDER))) {
                deleteUserExternalId(map, context);
              }
            } else if (JsonKey.EDIT.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              // to update user externalId first we need to delete the record as externalId is the
              // part of composite key
              deleteUserExternalId(map, context);
              responseExternalIdList.add(
                  upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE, context));
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

  private Optional<Map<String, String>> checkExternalID(
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

  private List<Map<String, String>> getUserExternalIds(
      Map<String, Object> requestMap, RequestContext context) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> req = new HashMap<>();
    req.put(JsonKey.USER_ID, requestMap.get(JsonKey.USER_ID));
    Response response =
        cassandraOperation.getRecordById(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USR_EXT_IDNT_TABLE, req, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private void deleteUserExternalId(Map<String, String> map, RequestContext context) {
    map.remove(JsonKey.LAST_UPDATED_BY);
    map.remove(JsonKey.CREATED_BY);
    map.remove(JsonKey.LAST_UPDATED_ON);
    map.remove(JsonKey.CREATED_ON);
    map.remove(JsonKey.EXTERNAL_ID);
    map.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
    map.remove(JsonKey.ORIGINAL_ID_TYPE);
    map.remove(JsonKey.ORIGINAL_PROVIDER);
    // map.remove(JsonKey.STATUS);
    cassandraOperation.deleteRecord(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USR_EXT_IDNT_TABLE, map, context);
  }

  private void throwExternalIDNotFoundException(String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.resourceNotFound,
        ProjectUtil.formatMessage(
            ResponseMessage.Message.EXTERNALID_NOT_FOUND, externalId, idType, provider),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  private Map<String, Object> upsertUserExternalIdentityData(
      Map<String, String> extIdsMap,
      Map<String, Object> requestMap,
      String operation,
      RequestContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.EXTERNAL_ID, extIdsMap.get(JsonKey.ID));
    map.put(JsonKey.ORIGINAL_EXTERNAL_ID, extIdsMap.get(JsonKey.ORIGINAL_EXTERNAL_ID));
    map.put(JsonKey.PROVIDER, extIdsMap.get(JsonKey.PROVIDER));
    map.put(JsonKey.ORIGINAL_PROVIDER, extIdsMap.get(JsonKey.ORIGINAL_PROVIDER));
    map.put(JsonKey.ID_TYPE, extIdsMap.get(JsonKey.ID_TYPE));
    map.put(JsonKey.ORIGINAL_ID_TYPE, extIdsMap.get(JsonKey.ORIGINAL_ID_TYPE));
    map.put(JsonKey.USER_ID, requestMap.get(JsonKey.USER_ID));
    // map.put(JsonKey.STATUS,extIdsMap.get(JsonKey.STATUS));
    if (JsonKey.CREATE.equalsIgnoreCase(operation)) {
      map.put(JsonKey.CREATED_BY, requestMap.get(JsonKey.CREATED_BY));
      map.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    } else {
      map.put(JsonKey.LAST_UPDATED_BY, requestMap.get(JsonKey.UPDATED_BY));
      map.put(JsonKey.LAST_UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    }
    cassandraOperation.upsertRecord(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.USR_EXT_IDNT_TABLE, map, context);
    return map;
  }
}
