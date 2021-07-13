package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserGetRequestValidator;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.util.ProjectUtil;
import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.request.Request;
import org.sunbird.validator.user.UserRequestValidator;
import play.mvc.Http;
import play.mvc.Result;
import util.Attrs;
import util.Common;

public class UserController extends BaseController {

  public CompletionStage<Result> createUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getRequest().put("sync", true);
          new UserRequestValidator().validateCreateUserRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createSSOUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_SSO_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getRequest().put("sync", true);
          new UserRequestValidator().validateCreateUserRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createUserV3(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_USER_V3.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateUserCreateV3(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_3);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createSSUUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_SSU_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateUserCreateV3(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createUserV4(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_USER_V4.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateUserCreateV4(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_4);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createManagedUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_MANAGED_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateUserCreateV4(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> updateUser(Http.Request httpRequest) {

    return handleRequest(
        ActorOperations.UPDATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request
              .getContext()
              .put(JsonKey.USER_ID, Common.getFromRequest(httpRequest, Attrs.USER_ID));
          new UserRequestValidator().validateUpdateUserRequest(request);
          request
              .getContext()
              .put(JsonKey.IS_AUTH_REQ, Common.getFromRequest(httpRequest, Attrs.IS_AUTH_REQ));

          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> updateUserV2(Http.Request httpRequest) {

    return handleRequest(
        ActorOperations.UPDATE_USER_V2.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request
              .getContext()
              .put(JsonKey.USER_ID, Common.getFromRequest(httpRequest, Attrs.USER_ID));
          new UserRequestValidator().validateUpdateUserRequest(request);
          request
              .getContext()
              .put(JsonKey.IS_AUTH_REQ, Common.getFromRequest(httpRequest, Attrs.IS_AUTH_REQ));

          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> getUserByIdV3(String userId, Http.Request httpRequest) {
    return handleGetUserProfileV3(
        ActorOperations.GET_USER_PROFILE_V3.getValue(),
        ProjectUtil.getLmsUserId(userId),
        httpRequest);
  }

  // removing deprecating columns
  public CompletionStage<Result> getUserByIdV4(String userId, Http.Request httpRequest) {
    return handleGetUserProfileV3(
        ActorOperations.GET_USER_PROFILE_V4.getValue(),
        ProjectUtil.getLmsUserId(userId),
        httpRequest);
  }

  public CompletionStage<Result> getUserByIdV5(String userId, Http.Request httpRequest) {
    return handleGetUserProfileV3(
        ActorOperations.GET_USER_PROFILE_V5.getValue(),
        ProjectUtil.getLmsUserId(userId),
        httpRequest);
  }

  public CompletionStage<Result> getUserByLoginId(Http.Request httpRequest) {
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);

    return handleRequest(
        ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateVerifyUser(request);
          request.getContext().put(JsonKey.FIELDS, requestedFields);

          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> getUserByKey(String idType, String id, Http.Request httpRequest) {

    HashMap<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, JsonKey.LOGIN_ID.equalsIgnoreCase(idType) ? JsonKey.LOGIN_ID : idType);
    map.put(JsonKey.VALUE, ProjectUtil.getLmsUserId(id));
    return handleRequest(
        ActorOperations.GET_USER_BY_KEY.getValue(),
        null,
        req -> {
          Request request = (Request) req;
          request.setRequest(map);
          new UserGetRequestValidator().validateGetUserByKeyRequest(request);
          return null;
        },
        null,
        null,
        false,
        httpRequest);
  }

  public CompletionStage<Result> searchUser(Http.Request httpRequest) {
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);
    return handleSearchRequest(
        ActorOperations.USER_SEARCH.getValue(),
        httpRequest.body().asJson(),
        userSearchRequest -> {
          Request request = (Request) userSearchRequest;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          new BaseRequestValidator().validateSearchRequest(request);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        ProjectUtil.EsType.user.getTypeName(),
        httpRequest);
  }

  // removing the deprecating columns and disabling search with those columns
  public CompletionStage<Result> searchUserV2(Http.Request httpRequest) {
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);
    return handleSearchRequest(
        ActorOperations.USER_SEARCH_V2.getValue(),
        httpRequest.body().asJson(),
        userSearchRequest -> {
          Request request = (Request) userSearchRequest;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          new BaseRequestValidator().validateSearchRequest(request);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        ProjectUtil.EsType.user.getTypeName(),
        httpRequest);
  }

  public CompletionStage<Result> searchUserV3(Http.Request httpRequest) {
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);
    return handleSearchRequest(
        ActorOperations.USER_SEARCH_V3.getValue(),
        httpRequest.body().asJson(),
        userSearchRequest -> {
          Request request = (Request) userSearchRequest;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          new BaseRequestValidator().validateSearchRequest(request);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        ProjectUtil.EsType.user.getTypeName(),
        httpRequest);
  }

  public CompletionStage<Result> userLookup(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.USER_LOOKUP.getValue(),
        httpRequest.body().asJson(),
        userSearchRequest -> {
          Request request = (Request) userSearchRequest;
          new UserRequestValidator().validateUserLookupRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  private CompletionStage<Result> handleGetUserProfileV3(
      String operation, String userId, Http.Request httpRequest) {
    final boolean isPrivate = httpRequest.path().contains(JsonKey.PRIVATE) ? true : false;
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);
    final String provider = httpRequest.getQueryString(JsonKey.PROVIDER);
    final String idType = httpRequest.getQueryString(JsonKey.ID_TYPE);
    final String withTokens = httpRequest.getQueryString(JsonKey.WITH_TOKENS);
    userId = ProjectUtil.getLmsUserId(userId);
    return handleRequest(
        operation,
        null,
        req -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          request.getContext().put(JsonKey.PRIVATE, isPrivate);
          request.getContext().put(JsonKey.WITH_TOKENS, withTokens);
          request.getContext().put(JsonKey.PROVIDER, provider);
          request.getContext().put(JsonKey.ID_TYPE, idType);
          return null;
        },
        userId,
        JsonKey.USER_ID,
        false,
        httpRequest);
  }

  public CompletionStage<Result> isUserValid(String key, String value, Http.Request httpRequest) {
    HashMap<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, key);
    map.put(JsonKey.VALUE, value);
    return handleRequest(
        "checkUserExistence",
        null,
        req -> {
          Request request = (Request) req;
          request.setRequest(map);
          new UserGetRequestValidator().validateGetUserByKeyRequest(request);
          return null;
        },
        null,
        null,
        false,
        httpRequest);
  }

  public CompletionStage<Result> getManagedUsers(String luaUuid, Http.Request httpRequest) {
    HashMap<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, luaUuid);
    String withTokens = httpRequest.getQueryString(JsonKey.WITH_TOKENS);
    map.put(JsonKey.WITH_TOKENS, withTokens);
    map.put(JsonKey.SORTBY, httpRequest.getQueryString(JsonKey.SORTBY)); // createdDate
    map.put(JsonKey.ORDER, httpRequest.getQueryString(JsonKey.ORDER)); // desc
    return handleRequest(
        ActorOperations.GET_MANAGED_USERS.getValue(),
        null,
        req -> {
          Request request = (Request) req;
          request.setRequest(map);
          new UserRequestValidator().validateUserId(luaUuid);
          return null;
        },
        null,
        null,
        false,
        httpRequest);
  }

  public CompletionStage<Result> userExists(
      String searchKey, String searchValue, Http.Request httpRequest) {
    HashMap<String, Object> map = new HashMap<>();
    map.put(JsonKey.KEY, searchKey);
    map.put(JsonKey.VALUE, searchValue);
    return handleRequest(
        ActorOperations.CHECK_USER_EXISTENCEV2.getValue(),
        null,
        req -> {
          Request request = (Request) req;
          request.setRequest(map);
          new UserGetRequestValidator()
              .validateGetUserByKeyRequestaWithCaptcha(request, httpRequest);
          return null;
        },
        null,
        null,
        false,
        httpRequest);
  }

  public CompletionStage<Result> updateUserDeclarations(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.UPDATE_USER_DECLARATIONS.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getRequest().put("sync", true);
          new UserRequestValidator().validateUserDeclarationRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
