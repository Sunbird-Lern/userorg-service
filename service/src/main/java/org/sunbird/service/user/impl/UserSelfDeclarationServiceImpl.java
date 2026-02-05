package org.sunbird.service.user.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.sunbird.dao.user.UserSelfDeclarationDao;
import org.sunbird.dao.user.impl.UserSelfDeclarationDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserSelfDeclarationService;
import org.sunbird.util.user.UserUtil;

public class UserSelfDeclarationServiceImpl implements UserSelfDeclarationService {

  private final LoggerUtil logger = new LoggerUtil(UserSelfDeclarationServiceImpl.class);
  private static UserSelfDeclarationService selfDeclarationService = null;
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserSelfDeclarationDao userSelfDeclarationDao =
      UserSelfDeclarationDaoImpl.getInstance();

  public static UserSelfDeclarationService getInstance() {
    if (selfDeclarationService == null) {
      selfDeclarationService = new UserSelfDeclarationServiceImpl();
    }
    return selfDeclarationService;
  }

  public Response saveUserSelfDeclareAttributes(
      Map<String, Object> requestMap, RequestContext context) {
    List<UserDeclareEntity> selfDeclaredFields =
        (List<UserDeclareEntity>) requestMap.get(JsonKey.DECLARATIONS);

    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();
    List<UserDeclareEntity> responseSelfDeclaredLists = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(selfDeclaredFields)) {
      try {
        for (UserDeclareEntity userDeclareEntity : selfDeclaredFields) {
          logger.info(
              context,
              "UserSelfDeclarationServiceImpl:saveUserSelfDeclareAttributes operationType: "
                  + userDeclareEntity.getOperation());
          switch (userDeclareEntity.getOperation()) {
            case JsonKey.ADD:
              addUserSelfDeclaredDetails(userDeclareEntity, context);
              break;
            case JsonKey.EDIT:
              updateUserSelfDeclaredDetails(userDeclareEntity, context);
              break;
            case JsonKey.REMOVE:
              userSelfDeclarationDao.deleteUserSelfDeclaredDetails(
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
    return response;
  }

  public void updateUserSelfDeclaredDetails(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    List<Map<String, Object>> dbSelfDeclaredResults =
        userSelfDeclarationDao.getUserSelfDeclaredFields(userDeclareEntity.getUserId(), context);
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
        userSelfDeclarationDao.deleteUserSelfDeclaredDetails(
            (String) declareField.get(JsonKey.USER_ID),
            (String) declareField.get(JsonKey.ORG_ID),
            (String) declareField.get(JsonKey.PERSONA),
            context);
      }
    }
    userSelfDeclarationDao.upsertUserSelfDeclaredFields(userDeclareEntity, context);
  }

  public void updateSelfDeclaration(UserDeclareEntity userDeclareEntity, RequestContext context) {
    userSelfDeclarationDao.upsertUserSelfDeclaredFields(userDeclareEntity, context);
  }

  public Response updateSelfDeclaration(
      Map<String, Object> updateFieldsMap,
      Map<String, Object> compositeKey,
      RequestContext context) {
    return userSelfDeclarationDao.updateUserSelfDeclaredFields(
        updateFieldsMap, compositeKey, context);
  }

  @Override
  public List<Map<String, Object>> fetchUserDeclarations(String userId, RequestContext context) {
    List<Map<String, Object>> finalRes = new ArrayList<>();
    List<Map<String, Object>> resExternalIds =
        userSelfDeclarationDao.getUserSelfDeclaredFields(userId, context);
    if (CollectionUtils.isNotEmpty(resExternalIds)) {
      resExternalIds.forEach(
          item -> {
            Map<String, Object> declaration = new HashMap<>();
            Map<String, String> declaredFields = (Map<String, String>) item.get(JsonKey.USER_INFO);
            if (MapUtils.isNotEmpty(declaredFields)) {
              decryptDeclarationFields(declaredFields, context);
            }
            declaration.put(JsonKey.STATUS, item.get(JsonKey.STATUS));
            declaration.put(JsonKey.ERROR_TYPE, item.get(JsonKey.ERROR_TYPE));
            declaration.put(JsonKey.ORG_ID, item.get(JsonKey.ORG_ID));
            declaration.put(JsonKey.PERSONA, item.get(JsonKey.PERSONA));
            declaration.put(JsonKey.INFO, declaredFields);
            finalRes.add(declaration);
          });
    }
    return finalRes;
  }

  private Map<String, String> decryptDeclarationFields(
      Map<String, String> declaredFields, RequestContext context) {
    if (declaredFields.containsKey(JsonKey.DECLARED_EMAIL)) {
      declaredFields.put(
          JsonKey.DECLARED_EMAIL,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_EMAIL), context));
    }
    if (declaredFields.containsKey(JsonKey.DECLARED_PHONE)) {
      declaredFields.put(
          JsonKey.DECLARED_PHONE,
          UserUtil.getDecryptedData(declaredFields.get(JsonKey.DECLARED_PHONE), context));
    }
    return declaredFields;
  }

  private void addUserSelfDeclaredDetails(
      UserDeclareEntity userDeclareEntity, RequestContext context) {
    List<Map<String, Object>> dbSelfDeclaredResults =
        userSelfDeclarationDao.getUserSelfDeclaredFields(userDeclareEntity, context);
    Map<String, Object> extIdMap =
        mapper.convertValue(userDeclareEntity, new TypeReference<Map<String, Object>>() {});
    if (CollectionUtils.isNotEmpty(dbSelfDeclaredResults)) {
      logger.info(
          context,
          "UserSelfDeclarationManagementActor:addUserSelfDeclaredDetails: deleting existing while adding ");
      userSelfDeclarationDao.deleteUserSelfDeclaredDetails(
          userDeclareEntity.getUserId(),
          userDeclareEntity.getOrgId(),
          userDeclareEntity.getPersona(),
          context);
    }
    extIdMap.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
    extIdMap.remove(JsonKey.OPERATION);
    logger.info(
        context,
        "UserSelfDeclarationManagementActor:addUserSelfDeclaredDetails: printing map key and values ");
    extIdMap
        .entrySet()
        .forEach(
            entry -> {
              logger.info("print key: " + entry.getKey() + " value: " + entry.getValue());
            });
    userSelfDeclarationDao.insertSelfDeclaredFields(extIdMap, context);
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
}
