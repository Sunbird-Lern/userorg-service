package org.sunbird.actor.user;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserSelfDeclarationService;
import org.sunbird.service.user.impl.UserSelfDeclarationServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.Util;
import org.sunbird.util.user.UserUtil;

public class UserSelfDeclarationManagementActor extends BaseActor {
  private final UserSelfDeclarationService userSelfDeclarationService =
      UserSelfDeclarationServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "updateUserDeclarations": // update self declare
        updateUserDeclarations(request);
        break;
      case "updateUserSelfDeclarationsErrorType":
        updateUserSelfDeclaredErrorStatus(request);
        break;
      case "upsertUserSelfDeclarations":
        upsertUserSelfDeclaredDetails(request);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }
  /**
   * This method will update self declaration for the user to Cassandra
   *
   * @param actorMessage
   */
  private void updateUserDeclarations(Request actorMessage) {
    logger.debug(
        actorMessage.getRequestContext(),
        "UserSelfDeclarationManagementActor:updateUserDeclarations method called.");

    Util.initializeContext(actorMessage, TelemetryEnvKey.USER);
    String callerId = (String) actorMessage.getContext().get(JsonKey.CALLER_ID);

    actorMessage.toLower();
    Map<String, Object> userMap = actorMessage.getRequest();
    Response response = new Response();
    List<String> errMsgs = new ArrayList<>();

    try {
      List<Map<String, Object>> declarations =
          (List<Map<String, Object>>) userMap.get(JsonKey.DECLARATIONS);
      // Get the User ID
      userMap.put(JsonKey.USER_ID, declarations.get(0).get(JsonKey.USER_ID));

      Map<String, Object> userDbRecord =
          UserUtil.validateExternalIdsAndReturnActiveUser(
              userMap, actorMessage.getRequestContext());

      UserUtil.encryptDeclarationFields(
          declarations, userDbRecord, actorMessage.getRequestContext());

      List<UserDeclareEntity> userDeclareEntityList = new ArrayList<>();
      for (Map<String, Object> declareFieldMap : declarations) {
        String custodianOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
        if (((String) declareFieldMap.get(JsonKey.ORG_ID)).equalsIgnoreCase(custodianOrgId)) {
          ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameter,
            MessageFormat.format(
              ResponseCode.invalidParameter.getErrorMessage(),
              JsonKey.ORGANISATION + JsonKey.ID));
        }
        UserDeclareEntity userDeclareEntity =
            UserUtil.createUserDeclaredObject(declareFieldMap, callerId);
        Map userInfo = userDeclareEntity.getUserInfo();
        if (StringUtils.isEmpty((String) userInfo.get(JsonKey.DECLARED_SCHOOL_UDISE_CODE))) {
          updateSchoolInfoInSelfDeclaration(
              (String) userMap.get(JsonKey.USER_ID), actorMessage.getRequestContext(), userInfo);
        }
        logger.debug(
            actorMessage.getRequestContext(),
            "UserSelfDeclarationManagementActor:updateUserDeclarations method userDeclareEntity obj: "
                + userDeclareEntity);
        userDeclareEntityList.add(userDeclareEntity);
      }

      userMap.remove(JsonKey.DECLARATIONS);
      userMap.put(JsonKey.DECLARATIONS, userDeclareEntityList);

      response =
          userSelfDeclarationService.saveUserSelfDeclareAttributes(
              userMap, actorMessage.getRequestContext());

    } catch (Exception ex) {
      errMsgs.add(ex.getMessage());
      logger.error(
          actorMessage.getRequestContext(),
          "UserSelfDeclarationManagementActor:upsertUserSelfDeclarations: Exception occurred with error message = "
              + ex.getMessage(),
          ex);
    }

    if (CollectionUtils.isNotEmpty((List<String>) response.getResult().get(JsonKey.ERROR_MSG))
        || CollectionUtils.isNotEmpty(errMsgs)) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR, errMsgs.get(0));
    }

    sender().tell(response, self());

    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) userMap.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.telemetryProcessingCall(
        userMap, targetObject, correlatedObject, actorMessage.getContext());
  }

  private Map<String, Object> updateSchoolInfoInSelfDeclaration(
      String userId, RequestContext context, Map<String, Object> userInfo) {
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();
    Response res = userOrgDao.getUserOrgDetails(userId, null, context);
    List<Map<String, Object>> userOrgLst = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    String custodianRootOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    for (Map<String, Object> userOrg : userOrgLst) {
      if (!userOrg.get(JsonKey.ORGANISATION_ID).equals(custodianRootOrgId)) {
        String organisationId = (String) userOrg.get(JsonKey.ORGANISATION_ID);
        Map<String, Object> organisation = orgService.getOrgById(organisationId, context);
        if (MapUtils.isNotEmpty(organisation)
            && null != organisation.get(JsonKey.IS_TENANT)
            && BooleanUtils.isFalse((Boolean) organisation.get(JsonKey.IS_TENANT))) {
          userInfo.put(JsonKey.DECLARED_SCHOOL_UDISE_CODE, organisation.get(JsonKey.EXTERNAL_ID));
          userInfo.put(JsonKey.DECLARED_SCHOOL_NAME, organisation.get(JsonKey.ORG_NAME));
        }
      }
    }
    return userInfo;
  }

  private void upsertUserSelfDeclaredDetails(Request request) {
    RequestContext context = request.getRequestContext();
    Map<String, Object> requestMap = request.getRequest();

    Response response =
        userSelfDeclarationService.saveUserSelfDeclareAttributes(requestMap, context);

    sender().tell(response, self());
  }

  public void updateUserSelfDeclaredErrorStatus(Request request) {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);

    Map<String, Object> requestMap = request.getRequest();
    UserDeclareEntity userDeclareEntity = (UserDeclareEntity) requestMap.get(JsonKey.DECLARATIONS);

    if (JsonKey.SELF_DECLARED_ERROR.equals(userDeclareEntity.getStatus())
        && StringUtils.isNotEmpty(userDeclareEntity.getErrorType())) {
      userSelfDeclarationService.updateSelfDeclaration(
          userDeclareEntity, request.getRequestContext());
    } else {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.declaredUserErrorStatusNotUpdated);
    }
    sender().tell(response, self());
  }
}
