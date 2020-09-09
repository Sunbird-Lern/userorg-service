package org.sunbird.user.actors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.user.util.UserActorOperations;

@ActorConfig(
  tasks = {"upsertUserSelfDeclarations"},
  asyncTasks = {"upsertUserSelfDeclarations", "updateUserSelfDeclarationsErrorType"}
)
public class UserSelfDeclarationManagementActor extends BaseActor {
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    if (UserActorOperations.UPSERT_USER_SELF_DECLARATIONS
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      upsertUserSelfDeclaredDetails(request);
    }
    if (UserActorOperations.UPDATE_USER_SELF_DECLARATIONS_ERROR_TYPE
        .getValue()
        .equalsIgnoreCase(request.getOperation())) {
      updateUserSelfDeclaredErrorStatus(request);
    } else {
      onReceiveUnsupportedOperation("upsertUserSelfDeclarations");
    }
  }

  private void upsertUserSelfDeclaredDetails(Request request) {
    RequestContext context = request.getRequestContext();
    Map<String, Object> requestMap = request.getRequest();
    List<UserDeclareEntity> selfDeclaredFields =
        (List<UserDeclareEntity>) requestMap.get(JsonKey.DECLARATIONS);

    String userId = (String) requestMap.get(JsonKey.USER_ID);
    List<Map<String, Object>> responseDeclareFields = new ArrayList<>();
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<UserDeclareEntity> responseSelfDeclaredLists = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(selfDeclaredFields)) {
      try {
        for (UserDeclareEntity userDeclareEntity : selfDeclaredFields) {
          switch (userDeclareEntity.getOperation()) {
            case JsonKey.ADD:
              addUserSelfDeclaredDetails(userDeclareEntity, context);
              break;
            case JsonKey.EDIT:
              updateUserSelfDeclaredDetails(userDeclareEntity, context);
              break;
            case JsonKey.REMOVE:
              deleteUserSelfDeclaredDetails(
                  userDeclareEntity.getUserId(),
                  userDeclareEntity.getOrgId(),
                  userDeclareEntity.getPersona(),
                  context);
              break;
            default:
              ProjectCommonException.throwClientErrorException(ResponseCode.invalidOperationName);
          }
          responseSelfDeclaredLists.add(userDeclareEntity);
        }
      } catch (Exception e) {
        errMsgs.add(e.getMessage());
        logger.error(
            context,
            "UserSelfDeclarationManagementActor:upsertUserSelfDeclarations: Exception occurred with error message = "
                + e.getMessage(),
            e);
      }
    }
    if (CollectionUtils.isNotEmpty(errMsgs)) {
      response.put(JsonKey.ERROR_MSG, errMsgs);
    } else {
      response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    }
    sender().tell(response, self());
  }

  private void insertSelfDeclaredFields(Map<String, Object> extIdMap, RequestContext context) {

    cassandraOperation.insertRecord(
        JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, extIdMap, context);
  }

  public void updateUserSelfDeclaredDetails(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    List<Map<String, Object>> dbSelfDeclaredResults =
        getUserSelfDeclaredFields(userDeclareEntity.getUserId(), context);
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
            (String) declareField.get(JsonKey.PERSONA),
            context);
      }
    }
    upsertUserSelfDeclaredFields(userDeclareEntity, context);
  }

  private void addUserSelfDeclaredDetails(
      UserDeclareEntity userDeclareEntity, RequestContext context) {

    List<Map<String, Object>> dbSelfDeclaredResults =
        getUserSelfDeclaredFields(userDeclareEntity, context);
    Map<String, Object> extIdMap =
        mapper.convertValue(userDeclareEntity, new TypeReference<Map<String, Object>>() {});
    if (CollectionUtils.isNotEmpty(dbSelfDeclaredResults)) {
      deleteUserSelfDeclaredDetails(
          userDeclareEntity.getUserId(),
          userDeclareEntity.getOrgId(),
          userDeclareEntity.getPersona(),
          context);
    }
    extIdMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    extIdMap.remove(JsonKey.OPERATION);
    insertSelfDeclaredFields(extIdMap, context);
  }

  private List<Map<String, Object>> getUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    properties.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    properties.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Response response =
        cassandraOperation.getRecordsByProperties(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private UserDeclareEntity upsertUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
    compositeKey.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
    compositeKey.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
    Map<String, Object> updateFieldsMap = new HashMap<>();
    updateFieldsMap.put(JsonKey.USER_INFO, userDeclareEntity.getUserInfo());
    updateFieldsMap.put(JsonKey.STATUS, userDeclareEntity.getStatus());
    updateFieldsMap.put(JsonKey.ERROR_TYPE, userDeclareEntity.getErrorType());
    updateFieldsMap.put(JsonKey.UPDATED_BY, userDeclareEntity.getUpdatedBy());
    updateFieldsMap.put(
        JsonKey.UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    cassandraOperation.updateRecord(
        JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, updateFieldsMap, compositeKey, context);
    return userDeclareEntity;
  }

  /**
   * Operation is a persona change : if existing orgId is equal to orgId in the request && existing
   * persona is different from persona in request.
   *
   * @param userDeclareEntity
   * @param dbSelfDeclaredResults
   * @param removeDeclaredFields
   */
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

  /**
   * Operation is a Org change : if existing orgId is different to orgId in the request && existing
   * persona is same as persona in request.
   *
   * @param userDeclareEntity
   * @param dbSelfDeclaredResults
   * @param removeDeclaredFields
   */
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

  /**
   * Operation is to update both Org and Persona change : if existing orgId is different to orgId in
   * the request && existing persona is also different from the persona in request.
   *
   * @param userDeclareEntity
   * @param dbSelfDeclaredResults
   * @param removeDeclaredFields
   */
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

  private List<Map<String, Object>> getUserSelfDeclaredFields(
      String userId, RequestContext context) {
    List<Map<String, Object>> dbResExternalIds = new ArrayList<>();
    Map<String, Object> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    Response response =
        cassandraOperation.getRecordById(
            JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties, context);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  private void deleteUserSelfDeclaredDetails(
      String userId, String orgId, String persona, RequestContext context) {
    Map<String, String> properties = new HashMap<>();
    properties.put(JsonKey.USER_ID, userId);
    properties.put(JsonKey.ORG_ID, orgId);
    properties.put(JsonKey.PERSONA, persona);
    cassandraOperation.deleteRecord(
        JsonKey.SUNBIRD, JsonKey.USER_DECLARATION_DB, properties, context);
  }

  public void updateUserSelfDeclaredErrorStatus(Request request) {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    Map<String, Object> requestMap = request.getRequest();
    UserDeclareEntity userDeclareEntity = (UserDeclareEntity) requestMap.get(JsonKey.DECLARATIONS);
    if (JsonKey.SELF_DECLARED_ERROR.equals(userDeclareEntity.getStatus())
        && StringUtils.isNotEmpty(userDeclareEntity.getErrorType())) {
      Map<String, Object> compositePropertiesMap = new HashMap<>();
      Map<String, Object> propertieMap = new HashMap<>();
      compositePropertiesMap.put(JsonKey.USER_ID, userDeclareEntity.getUserId());
      compositePropertiesMap.put(JsonKey.ORG_ID, userDeclareEntity.getOrgId());
      compositePropertiesMap.put(JsonKey.PERSONA, userDeclareEntity.getPersona());
      propertieMap.put(JsonKey.ERROR_TYPE, userDeclareEntity.getErrorType());
      propertieMap.put(JsonKey.STATUS, userDeclareEntity.getStatus());
      cassandraOperation.updateRecord(
          JsonKey.SUNBIRD,
          JsonKey.USER_DECLARATION_DB,
          propertieMap,
          compositePropertiesMap,
          request.getRequestContext());
    } else {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.declaredUserErrorStatusNotUpdated);
    }
    sender().tell(response, self());
  }
}
