package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserGetRequestValidator;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class UserController extends BaseController {

  public CompletionStage<Result> createUser(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateCreateUserV1Request(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createUserV2(Http.Request httpRequest) {

    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateCreateUserV2Request(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> createUserV3Sync(Http.Request httpRequest) {

    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getRequest().put("sync", true);
          new UserRequestValidator().validateCreateUserV2Request(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_3);
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

  public CompletionStage<Result> updateUser(Http.Request httpRequest) {
    final boolean isPrivate;
    if (httpRequest.path().contains(JsonKey.PRIVATE)) {
      isPrivate = true;
    } else {
      isPrivate = false;
    }
    return handleRequest(
        ActorOperations.UPDATE_USER.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.USER_ID, httpRequest.flash().get(JsonKey.USER_ID));
          request.getContext().put(JsonKey.PRIVATE, isPrivate);
          new UserRequestValidator().validateUpdateUserRequest(request);
          request
              .getContext()
              .put(JsonKey.IS_AUTH_REQ, httpRequest.flash().get(JsonKey.IS_AUTH_REQ));

          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> getUserById(String userId, Http.Request httpRequest) {
    return handleGetUserProfile(
        ActorOperations.GET_USER_PROFILE.getValue(), ProjectUtil.getLmsUserId(userId), httpRequest);
  }

  public CompletionStage<Result> getUserByIdV2(String userId, Http.Request httpRequest) {
    return handleGetUserProfile(
        ActorOperations.GET_USER_PROFILE_V2.getValue(),
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
        ActorOperations.COMPOSITE_SEARCH.getValue(),
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
        EsType.user.getTypeName(),
        httpRequest);
  }

  private CompletionStage<Result> handleGetUserProfile(
      String operation, String userId, Http.Request httpRequest) {
    final boolean isPrivate = httpRequest.path().contains(JsonKey.PRIVATE) ? true : false;
    final String requestedFields = httpRequest.getQueryString(JsonKey.FIELDS);
    final String provider = httpRequest.getQueryString(JsonKey.PROVIDER);
    final String idType = httpRequest.getQueryString(JsonKey.ID_TYPE);
    userId = ProjectUtil.getLmsUserId(userId);
    return handleRequest(
        operation,
        null,
        req -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          request.getContext().put(JsonKey.PROVIDER, provider);
          request.getContext().put(JsonKey.ID_TYPE, idType);
          request.getContext().put(JsonKey.PRIVATE, isPrivate);
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
}
