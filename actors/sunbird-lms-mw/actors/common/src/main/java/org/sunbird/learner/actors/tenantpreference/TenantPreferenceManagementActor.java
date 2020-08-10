package org.sunbird.learner.actors.tenantpreference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

/** Class for Tenant preferences . Created by arvind on 27/10/17. */
@ActorConfig(
  tasks = {
    "createTanentPreference",
    "updateTenantPreference",
    "getTenantPreference",
  },
  asyncTasks = {}
)
public class TenantPreferenceManagementActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private Util.DbInfo tenantPreferenceDbInfo = Util.dbInfoMap.get(JsonKey.TENANT_PREFERENCE_V2);
  private static final String DEFAULT_WILDCARD_ORG_ID = "*";
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.CREATE_TENANT_PREFERENCE.getValue())) {
      createTenantPreference(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.UPDATE_TENANT_PREFERENCE.getValue())) {
      updateTenantPreference(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(ActorOperations.GET_TENANT_PREFERENCE.getValue())) {
      getTenantPreference(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  /**
   * Method to get Tenant preference of the given root org id .
   *
   * @param actorMessage
   */
  private void getTenantPreference(Request actorMessage) {
    String orgId = (String) actorMessage.getRequest().get(JsonKey.ORG_ID);
    ProjectLogger.log(
        "TenantPreferenceManagementActor-getTenantPreference called for org: " + orgId);
    String key = (String) actorMessage.getRequest().get(JsonKey.KEY);
    List<Map<String, Object>> orgPrefMap = getTenantPreferencesFromDB(orgId, key);
    Response finalResponse = new Response();

    if (CollectionUtils.isNotEmpty(orgPrefMap)) {
      String data = (String) orgPrefMap.get(0).get(JsonKey.DATA);
      try {
        Map<String, Object> map = deserialize(data, new TypeReference<Map<String, Object>>() {});
        orgPrefMap.get(0).put(JsonKey.DATA, map);
        finalResponse.getResult().put(JsonKey.RESPONSE, orgPrefMap.get(0));
      } catch (Exception e) {
        ProjectLogger.log(
            "exception while reading preferences " + e.getMessage(), LoggerEnum.DEBUG);
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.preferenceNotFound.getErrorCode(),
          MessageFormat.format(ResponseCode.preferenceNotFound.getErrorMessage(), key, orgId),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    sender().tell(finalResponse, self());
  }

  private List<Map<String, Object>> getTenantPreferencesFromDB(String orgId, String key) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(JsonKey.KEY, key);
    properties.put(JsonKey.ORG_ID, orgId);
    Response tenantPreferences =
        cassandraOperation.getRecordsByProperties(
            tenantPreferenceDbInfo.getKeySpace(),
            tenantPreferenceDbInfo.getTableName(),
            properties,
            null);
    List<Map<String, Object>> preferencesList =
        (List<Map<String, Object>>) tenantPreferences.get(JsonKey.RESPONSE);
    return preferencesList;
  }
  /**
   * Method to update the Tenant preference on basis of id or (role and org id).
   *
   * @param actorMessage
   */
  private void updateTenantPreference(Request actorMessage) {

    Map<String, Object> req = actorMessage.getRequest();
    String orgId = (String) req.get(JsonKey.ORG_ID);
    String key = (String) req.get(JsonKey.KEY);
    ProjectLogger.log(
        "TenantPreferenceManagementActor-updateTenantPreference called for org: "
            + orgId
            + "key "
            + key);
    Response finalResponse = new Response();
    List<Map<String, Object>> preferencesList = getTenantPreferencesFromDB(orgId, key);
    if (CollectionUtils.isEmpty(preferencesList)) {
      ProjectLogger.log(
          "TenantPreferenceManagementActor-updateTenantPreference key "
              + key
              + "already exists in the org "
              + orgId);
      throw new ProjectCommonException(
          ResponseCode.preferenceNotFound.getErrorCode(),
          MessageFormat.format(ResponseCode.preferenceNotFound.getErrorMessage(), key, orgId),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    } else {
      try {
        Map<String, Object> preferenceObj = new HashMap<>();
        Map<String, Object> clusteringKeys = new HashMap<>();
        clusteringKeys.put(JsonKey.KEY, key);
        clusteringKeys.put(JsonKey.ORG_ID, orgId);
        preferenceObj.put(JsonKey.DATA, serialize(req.get(JsonKey.DATA)));
        preferenceObj.put(JsonKey.UPDATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
        preferenceObj.put(
            JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
        cassandraOperation.updateRecord(
            tenantPreferenceDbInfo.getKeySpace(),
            tenantPreferenceDbInfo.getTableName(),
            preferenceObj,
            clusteringKeys,
            null);
      } catch (Exception e) {
        ProjectLogger.log(
            "exception while updating preferences " + e.getMessage(), LoggerEnum.DEBUG);
      }
    }
    finalResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(finalResponse, self());
  }

  /**
   * Method to create tenant preference for an org , if already exists it will not create new one.
   *
   * @param actorMessage
   */
  private void createTenantPreference(Request actorMessage) {
    Map<String, Object> req = actorMessage.getRequest();
    String orgId = (String) req.get(JsonKey.ORG_ID);
    String key = (String) req.get(JsonKey.KEY);
    ProjectLogger.log(
        "TenantPreferenceManagementActor-createTenantPreference called for org: " + orgId);
    List<Map<String, Object>> preferencesList = getTenantPreferencesFromDB(orgId, key);
    Response finalResponse = new Response();
    // check whether already tenant preference exists for the given org id
    if (CollectionUtils.isNotEmpty(preferencesList)) {
      ProjectLogger.log(
          "TenantPreferenceManagementActor-createTenantPreference key "
              + key
              + "already exists in the org "
              + orgId);
      throw new ProjectCommonException(
          ResponseCode.preferenceAlreadyExists.getErrorCode(),
          MessageFormat.format(ResponseCode.preferenceAlreadyExists.getErrorMessage(), key, orgId),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    Map<String, Object> dbMap = new HashMap<>();
    try {
      dbMap.put(JsonKey.ORG_ID, orgId);
      dbMap.put(JsonKey.KEY, key);
      dbMap.put(JsonKey.DATA, serialize(req.get(JsonKey.DATA)));
      dbMap.put(JsonKey.CREATED_BY, actorMessage.getContext().get(JsonKey.REQUESTED_BY));
      dbMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    } catch (Exception e) {
      ProjectLogger.log("exception while adding preferences " + e.getMessage(), LoggerEnum.DEBUG);
    }
    Response response =
        cassandraOperation.insertRecord(
            tenantPreferenceDbInfo.getKeySpace(),
            tenantPreferenceDbInfo.getTableName(),
            dbMap,
            null);
    finalResponse.getResult().put(JsonKey.ORG_ID, orgId);
    finalResponse.getResult().put(JsonKey.KEY, key);
    finalResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(finalResponse, self());
  }

  private String serialize(Object obj) throws Exception {
    return mapper.writeValueAsString(obj);
  }

  private <T> T deserialize(String value, TypeReference<T> valueTypeRef) throws Exception {
    return mapper.readValue(value, valueTypeRef);
  }
}
