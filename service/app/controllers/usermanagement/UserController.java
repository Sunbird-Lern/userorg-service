/** */
package controllers.usermanagement;

import static org.sunbird.learner.util.Util.isNotNull;
import static org.sunbird.learner.util.Util.isNull;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Result;
import util.AuthenticationHelper;

/**
 * This controller will handle all the request and responses for user management.
 *
 * @author Manzarul
 */
public class UserController extends BaseController {

  /**
   * This method will do the registration process. registered user data will be store inside
   * cassandra db.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createUser() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          " get user registration request data = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateCreateUser(reqObj);

      if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.PROVIDER))) {
        reqObj.getRequest().put(JsonKey.EMAIL_VERIFIED, false);
        reqObj.getRequest().put(JsonKey.PHONE_VERIFIED, false);
      }
      reqObj.setOperation(ActorOperations.CREATE_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update user profile data. user can update all the data except email.
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateUserProfile() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get user update profile data = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
          && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
        validateAuthenticity(reqObj);
      }
      UserRequestValidator.validateUpdateUser(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());

      if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.PROVIDER))) {
        reqObj.getRequest().put(JsonKey.EMAIL_VERIFIED, false);
        reqObj.getRequest().put(JsonKey.PHONE_VERIFIED, false);
      }

      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  private void validateAuthenticity(Request reqObj) {

    if (ctx().flash().containsKey(JsonKey.AUTH_WITH_MASTER_KEY)) {
      validateWithClient(reqObj);
    } else {
      validateWithUserId(reqObj);
    }
  }

  private void validateWithClient(Request reqObj) {
    String clientId = ctx().flash().get(JsonKey.USER_ID);
    String userId;
    if (null != reqObj.getRequest().get(JsonKey.USER_ID)) {
      userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
    } else {
      userId = (String) reqObj.getRequest().get(JsonKey.ID);
    }

    Map<String, Object> clientDetail = AuthenticationHelper.getClientAccessTokenDetail(clientId);
    // get user detail from cassandra
    Map<String, Object> userDetail = AuthenticationHelper.getUserDetail(userId);
    // check whether both exist or not ...
    if (clientDetail == null || userDetail == null) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorised.getErrorCode(),
          ResponseCode.unAuthorised.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }

    String userRootOrgId = (String) userDetail.get(JsonKey.ROOT_ORG_ID);
    if (StringUtils.isBlank(userRootOrgId)) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorised.getErrorCode(),
          ResponseCode.unAuthorised.getErrorMessage(),
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
          ResponseCode.unAuthorised.getErrorCode(),
          ResponseCode.unAuthorised.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  private void validateWithUserId(Request reqObj) {
    String userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
    if ((!StringUtils.isBlank(userId)) && (!userId.equals(ctx().flash().get(JsonKey.USER_ID)))) {
      throw new ProjectCommonException(
          ResponseCode.unAuthorised.getErrorCode(),
          ResponseCode.unAuthorised.getErrorMessage(),
          ResponseCode.UNAUTHORIZED.getResponseCode());
    }
  }

  /**
   * This method will do the user authentication based on login type key. login can be done with
   * following ways (simple login , Google plus login , Facebook login , Aadhaar login)
   *
   * @return Promise<Result>
   */
  public Promise<Result> login() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get user login data=" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateUserLogin(reqObj);
      reqObj.setOperation(ActorOperations.LOGIN.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will invalidate user auth token .
   *
   * @return Promise<Result>
   */
  public Promise<Result> logout() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get user logout data = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.LOGOUT.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.AUTH_TOKEN, request().getHeader(HeaderParam.X_Access_TokenId.getName()));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will allow user to change their password.
   *
   * @return Promise<Result>
   */
  public Promise<Result> changePassword() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get user change password data = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateChangePassword(reqObj);
      reqObj.setOperation(ActorOperations.CHANGE_PASSWORD.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      reqObj.getRequest().put(JsonKey.USER_ID, ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserProfile(String userId) {

    try {
      JsonNode requestData = request().body().asJson();
      String requestedFields = request().getQueryString(JsonKey.FIELDS);
      ProjectLogger.log(" get user profile data by id =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_PROFILE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      reqObj.getRequest().put(JsonKey.USER_ID, userId);
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.FIELDS, requestedFields);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide complete role details list.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getRoles() {

    try {
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ROLES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to verify user existence in our DB.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getUserDetailsByLoginId() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          " verify user details by loginId data =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateVerifyUser(reqObj);
      reqObj.setOperation(ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.FIELDS, reqObj.getRequest().get(JsonKey.FIELDS));
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will download user details for particular org or all organizations
   *
   * @return Promise<Result>
   */
  public Promise<Result> downloadUsers() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" Downlaod user data request =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.DOWNLOAD_USERS.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> blockUser() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" blockuser =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.BLOCK_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will assign either user role directly or user org role.
   *
   * @return Promise<Result>
   */
  public Promise<Result> assignRoles() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" Assign roles api request body =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateAssignRole(reqObj);
      reqObj.setOperation(ActorOperations.ASSIGN_ROLES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.getRequest().put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will changes user status from block to unblock
   *
   * @return Promise<Result>
   */
  public Promise<Result> unBlockUser() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" unblockuser =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.UNBLOCK_USER.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will do the user search for Elastic search. this will internally call composite
   * search api.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("User search api call =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));

      List<String> esObjectType = new ArrayList<>();
      esObjectType.add(EsType.user.getTypeName());
      if (reqObj.getRequest().containsKey(JsonKey.FILTERS)
          && reqObj.getRequest().get(JsonKey.FILTERS) != null
          && reqObj.getRequest().get(JsonKey.FILTERS) instanceof Map) {
        ((Map) (reqObj.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, esObjectType);
      } else {
        Map<String, Object> filtermap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(JsonKey.OBJECT_TYPE, esObjectType);
        filtermap.put(JsonKey.FILTERS, dataMap);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update user current login time to keyCloack.
   *
   * @return promise<Result>
   */
  public Promise<Result> updateLoginTime() {
    ProjectLogger.log("Update user login time api call");
    try {
      String userId = ctx().flash().get(JsonKey.USER_ID);
      JsonNode requestData = request().body().asJson();
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      if (reqObj == null) {
        reqObj = new Request();
      }
      reqObj.setOperation(ActorOperations.USER_CURRENT_LOGIN.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      if (!StringUtils.isBlank(userId)) {
        reqObj.getRequest().put(JsonKey.USER_ID, userId);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Get all the social media types supported
   *
   * @return
   */
  public Promise<Result> getMediaTypes() {
    try {
      ProjectLogger.log(" get media Types ", LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_MEDIA_TYPES.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(
          JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will send user temporary password on his/her registered email.in Request it will
   * take either user loginid or email.
   *
   * @return Promise<Result>
   */
  public Promise<Result> forgotpassword() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" get user forgot password call = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateForgotpassword(reqObj);
      reqObj.setOperation(ActorOperations.FORGOT_PASSWORD.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      reqObj.getRequest().put(JsonKey.USER_ID, ctx().flash().get(JsonKey.USER_ID));
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will add or update user profile visibility control. User can make all field as
   * private except name. any private filed of user is not search-able.
   *
   * @return Promise<Result>
   */
  public Promise<Result> profileVisibility() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          " Profile visibility control request= " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      UserRequestValidator.validateProfileVisibility(reqObj);
      if (null != ctx().flash().get(JsonKey.IS_AUTH_REQ)
          && Boolean.parseBoolean(ctx().flash().get(JsonKey.IS_AUTH_REQ))) {
        String userId = (String) reqObj.getRequest().get(JsonKey.USER_ID);
        if (!userId.equals(ctx().flash().get(JsonKey.USER_ID))) {
          throw new ProjectCommonException(
              ResponseCode.unAuthorised.getErrorCode(),
              ResponseCode.unAuthorised.getErrorMessage(),
              ResponseCode.UNAUTHORIZED.getResponseCode());
        }
      }
      reqObj.setOperation(ActorOperations.PROFILE_VISIBILITY.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
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
