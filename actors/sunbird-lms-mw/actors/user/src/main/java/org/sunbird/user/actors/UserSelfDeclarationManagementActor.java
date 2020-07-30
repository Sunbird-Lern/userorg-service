package org.sunbird.user.actors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.user.util.UserActorOperations;

@ActorConfig(
  tasks = {"upsertUserSelfDeclaredDetails"},
  asyncTasks = {"upsertUserSelfDeclaredDetails"}
)
public class UserSelfDeclarationManagementActor extends BaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (UserActorOperations.UPSERT_USER_SELF_DECLARED_DETAILS
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      upsertUserSelfDeclaredDetails(request);
    } else {
      onReceiveUnsupportedOperation("UserSelfDeclarationManagementActor");
    }
  }

  private void upsertUserSelfDeclaredDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    List<UserDeclareEntity> selfDeclaredFields =
        (List<UserDeclareEntity>) requestMap.get(JsonKey.DECLARATIONS);

    String userId = (String) requestMap.get(JsonKey.USER_ID);
    List<Map<String, Object>> responseDeclareFields = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(selfDeclaredFields)) {
      Response response = new Response();
      List<String> errMsgs = new ArrayList<>();
      try {
        for (UserDeclareEntity userDeclareEntity : selfDeclaredFields) {
          switch (userDeclareEntity.getOperation()) {
            case JsonKey.ADD:
              addUserSelfDeclaredDetails(userDeclareEntity);
              break;
            case JsonKey.EDIT:
              updateUserSelfDeclaredDetails(userDeclareEntity);
              break;
            case JsonKey.REMOVE:
              deleteUserSelfDeclaredDetails(
                  userDeclareEntity.getUserId(),
                  userDeclareEntity.getOrgId(),
                  userDeclareEntity.getPersona());
              break;
          }
        }
      } catch (Exception e) {
        errMsgs.add(e.getMessage());
        ProjectLogger.log(
            "UserSelfDeclarationManagementActor:upsertUserSelfDeclaredDetails: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
      if (CollectionUtils.isNotEmpty(errMsgs)) {
        response.put(JsonKey.ERROR_MSG, errMsgs);
      } else {
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
      }
      sender().tell(response, self());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void insertSelfDeclaredFields(Map<String, Object> extIdMap) {

    cassandraOperation.insertRecord(JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, extIdMap);
  }

  public void updateUserSelfDeclaredDetails(UserDeclareEntity userDeclareEntity) {
    List<Map<String, Object>> dbSelfDeclaredResults =
        getUserSelfDeclaredFields(userDeclareEntity.getUserId());
    // store records to be remove
    List<Map<String, Object>> removeDeclaredFields = new ArrayList<>();
    // Check if only persona changes ,includes only records belong to same org
    isPersonaEditOperation(userDeclareEntity, dbSelfDeclaredResults, removeDeclaredFields);
    if (CollectionUtils.isEmpty(removeDeclaredFields)) {
      // check if only org changes, includes only records belong to same persona
      isOrgEditOperation(userDeclareEntity, dbSelfDeclaredResults, removeDeclaredFields);
    }
    if (CollectionUtils.isEmpty(removeDeclaredFields)) {
      // check if both role and org, includes all records of userId
      isOrgAndPersonaEditOperation(userDeclareEntity, dbSelfDeclaredResults, removeDeclaredFields);
    }
    if (CollectionUtils.isNotEmpty(removeDeclaredFields)) {
      for (Map<String, Object> declareField : removeDeclaredFields) {
        deleteUserSelfDeclaredDetails(
            (String) declareField.get(JsonKey.USER_ID),
            (String) declareField.get(JsonKey.ORG_ID),
            (String) declareField.get(JsonKey.PERSONA));
      }
    }
    upsertUserSelfDeclaredFields(userDeclareEntity);
  }

  private void addUserSelfDeclaredDetails(UserDeclareEntity userDeclareEntity) {

    List<Map<String, Object>> dbSelfDeclaredResults = getUserSelfDeclaredFields(userDeclareEntity);
    Map<String, Object> extIdMap =
        mapper.convertValue(userDeclareEntity, new TypeReference<Map<String, Object>>() {});
    if (CollectionUtils.isEmpty(dbSelfDeclaredResults)) {
      extIdMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    } else {
      deleteUserSelfDeclaredDetails(
          userDeclareEntity.getUserId(),
          userDeclareEntity.getOrgId(),
          userDeclareEntity.getPersona());
    }
    extIdMap.remove(JsonKey.OPERATION);
    insertSelfDeclaredFields(extIdMap);
  }

  private List<Map<String, Object>> getUserSelfDeclaredFields(UserDeclareEntity userDeclareEntity) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    properties.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    properties.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Response response =
        cassandraOperation.getRecordsByProperties(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private UserDeclareEntity upsertUserSelfDeclaredFields(UserDeclareEntity userDeclareEntity) {
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    compositeKey.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    compositeKey.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Map<String, Object> updateFieldsMap = new HashMap<>();
    updateFieldsMap.put(JsonKey.USER_INFO, userDeclareEntity.getUserInfo());
    updateFieldsMap.put(JsonKey.STATUS, userDeclareEntity.getStatus());
    updateFieldsMap.put(JsonKey.ERROR_TYPE, userDeclareEntity.getErrorType());
    updateFieldsMap.put(JsonKey.CREATED_BY, userDeclareEntity.getCreatedBy());
    updateFieldsMap.put(JsonKey.CREATED_ON, userDeclareEntity.getCreatedOn());
    updateFieldsMap.put(JsonKey.UPDATED_BY, userDeclareEntity.getUpdatedBy());
    updateFieldsMap.put(
        JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    cassandraOperation.updateRecord(
        JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, updateFieldsMap, compositeKey);
    return userDeclareEntity;
  }

  private void isPersonaEditOperation(
      UserDeclareEntity userDeclareEntity,
      List<Map<String, Object>> dbSelfDeclaredResults,
      List<Map<String, Object>> removeDeclaredFields) {
    for (Map<String, Object> dbSelfDeclareFields : dbSelfDeclaredResults) {
      String orgId = (String) dbSelfDeclareFields.get(JsonKey.ORG_ID);
      String persona = (String) dbSelfDeclareFields.get(JsonKey.PERSONA);
      if (orgId.equals(userDeclareEntity.getOrgId())
          && !persona.equals(userDeclareEntity.getPersona())) {
        removeDeclaredFields.add(dbSelfDeclareFields);
      }
    }
  }

  private void isOrgEditOperation(
      UserDeclareEntity userDeclareEntity,
      List<Map<String, Object>> dbSelfDeclaredResults,
      List<Map<String, Object>> removeDeclaredFields) {
    for (Map<String, Object> dbSelfDeclareFields : dbSelfDeclaredResults) {
      String orgId = (String) dbSelfDeclareFields.get(JsonKey.ORG_ID);
      String persona = (String) dbSelfDeclareFields.get(JsonKey.PERSONA);
      if (!orgId.equals(userDeclareEntity.getOrgId())
          && persona.equals(userDeclareEntity.getPersona())) {
        removeDeclaredFields.add(dbSelfDeclareFields);
      }
    }
  }

  private void isOrgAndPersonaEditOperation(
      UserDeclareEntity userDeclareEntity,
      List<Map<String, Object>> dbSelfDeclaredResults,
      List<Map<String, Object>> removeDeclaredFields) {
    for (Map<String, Object> dbSelfDeclareFields : dbSelfDeclaredResults) {
      String orgId = (String) dbSelfDeclareFields.get(JsonKey.ORG_ID);
      String persona = (String) dbSelfDeclareFields.get(JsonKey.PERSONA);
      if (!(orgId.equals(userDeclareEntity.getOrgId())
          && persona.equals(userDeclareEntity.getPersona()))) {
        removeDeclaredFields.add(dbSelfDeclareFields);
      }
    }
  }

  private List<Map<String, Object>> getUserSelfDeclaredFields(String userId) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordsByProperties(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private void deleteUserSelfDeclaredDetails(String userId, String orgId, String persona) {
    Map<String, String> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    properties.put(JsonKey.ORG_ID, orgId);
    properties.put(JsonKey.PERSONA, persona);
    cassandraOperation.deleteRecord(JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties);
  }
}
