package controllers.usermanagement;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

import controllers.BaseController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;
import util.AuthenticationHelper;

public class UserCrudController extends BaseController {

  /**
   * This method will do the registration process. registered user data will be store inside
   * cassandra db.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createUser() {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          UserRequestValidator.validateCreateUser((Request) request);
          UserRequestValidator.fieldsNotAllowed(
              Arrays.asList(JsonKey.ORGANISATION_ID), (Request) request);
          HashMap<String, Object> innerMap = new HashMap<>();
          innerMap.put(JsonKey.USER, request.getRequest());
          request.setRequest(innerMap);
          return null;
        },
        null,
        null,
        true);
  }

  /**
   * This method will do the registration process. registered user data will be store inside
   * cassandra db.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createUserV2() {

    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          UserRequestValidator.validateCreateUserV2(request);
          HashMap<String, Object> innerMap = new HashMap<>();
          innerMap.put(JsonKey.USER, request.getRequest());
          request.setRequest(innerMap);
          return null;
        },
        JsonKey.VERSION_2,
        JsonKey.VERSION,
        true);
  }

  /**
   * This method will update user profile data. user can update all the data except email.
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateUserProfile() {

    return handleRequest(
        ActorOperations.UPDATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          String accessToken =
              request().getHeader(HeaderParam.X_Authenticated_User_Token.getName());
          request.getRequest().put(HeaderParam.X_Authenticated_User_Token.getName(), accessToken);
          UserRequestValidator.validateUpdateUser(request);
          if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
              && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
            validateAuthenticity(request);
            HashMap<String, Object> innerMap = new HashMap<>();
            innerMap.put(JsonKey.USER, request.getRequest());
            request.setRequest(innerMap);
          }
          return null;
        },
        null,
        null,
        true);
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserProfile(String userId) {
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);

    return handleRequest(
        ActorOperations.GET_PROFILE.getValue(),
        null,
        (req) -> {
          Request request = (Request) req;
          HashMap<String, Object> innerMap = new HashMap<>();
          innerMap.put(JsonKey.USER, request.getRequest());
          innerMap.put(JsonKey.FIELDS, requestedFields);
          request.setRequest(innerMap);
          return null;
        },
        userId,
        JsonKey.USER_ID,
        false);
  }

  /**
   * Method to verify user existence in our DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserDetailsByLoginId() {

    final String requestedFields = request().getQueryString(JsonKey.FIELDS);
    return handleRequest(
        ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          UserRequestValidator.validateVerifyUser(request);
          HashMap<String, Object> innerMap = new HashMap<>();
          innerMap.put(JsonKey.USER, request.getRequest());
          innerMap.put(JsonKey.FIELDS, requestedFields);
          request.setRequest(innerMap);
          return null;
        },
        null,
        null,
        true);
  }

  private void validateAuthenticity(Request reqObj) {

    if (ctx().flash().containsKey(JsonKey.AUTH_WITH_MASTER_KEY)) {
      validateWithClient(reqObj);
    } else {
      ProjectLogger.log("Auth token is not master token.");
      validateWithUserId(reqObj);
    }
  }

  private void validateWithClient(Request reqObj) {
    String clientId = ctx().flash().get(JsonKey.USER_ID);
    String userId = getUserIdFromExtIdAndProvider(reqObj);

    Map<String, Object> clientDetail = AuthenticationHelper.getClientAccessTokenDetail(clientId);
    // get user detail from cassandra
    Map<String, Object> userDetail = AuthenticationHelper.getUserDetail(userId);
    // check whether both exist or not ...
    if (clientDetail == null || userDetail == null) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }

    String userRootOrgId = (String) userDetail.get(JsonKey.ROOT_ORG_ID);
    if (StringUtils.isBlank(userRootOrgId)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
    // get the org info from org table
    Map<String, Object> orgDetail = AuthenticationHelper.getOrgDetail(userRootOrgId);
    String userChannel = (String) orgDetail.get(JsonKey.CHANNEL);
    String clientChannel = (String) clientDetail.get(JsonKey.CHANNEL);
    ProjectLogger.log("User channel : " + userChannel);
    ProjectLogger.log("Client channel : " + clientChannel);

    // check whether both belongs to the same channel or not ...
    if (!compareStrings(userChannel, clientChannel)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  private String getUserIdFromExtIdAndProvider(Request reqObj) {
    String userId = "";
    if (null != reqObj.getRequest().get(JsonKey.USER_ID)) {
      userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
    } else {
      userId = (String) reqObj.getRequest().get(JsonKey.ID);
    }
    if (StringUtils.isBlank(userId)) {
      String extId = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID);
      String provider = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_PROVIDER);
      String idType = (String) reqObj.getRequest().get(JsonKey.EXTERNAL_ID_TYPE);
      Map<String, Object> user =
          AuthenticationHelper.getUserFromExternalId(extId, provider, idType);
      if (MapUtils.isNotEmpty(user)) {
        userId = (String) user.get(JsonKey.ID);
      } else {
        throw new ProjectCommonException(
            ResponseCode.invalidParameter.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.invalidParameter.getErrorMessage(),
                StringFormatter.joinByAnd(
                    StringFormatter.joinByComma(JsonKey.EXTERNAL_ID, JsonKey.EXTERNAL_ID_TYPE),
                    JsonKey.EXTERNAL_ID_PROVIDER)),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return userId;
  }

  private void validateWithUserId(Request reqObj) {
    String userId = getUserIdFromExtIdAndProvider(reqObj);
    if ((!StringUtils.isBlank(userId)) && (!userId.equals(ctx().flash().get(JsonKey.USER_ID)))) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorized.getErrorCode(),
          ResponseCode.unAuthorized.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  // method will compare two strings and return true id both are same otherwise false ...
  private boolean compareStrings(String first, String second) {
    if (isNull(first) && isNull(second)) {
      return true;
    }
    if ((isNull(first) && isNotNull(second)) || (isNull(second) && isNotNull(first))) {
      return false;
    }
    return first.equalsIgnoreCase(second);
  }
}
