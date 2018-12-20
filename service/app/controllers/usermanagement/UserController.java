package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserGetRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import org.sunbird.models.user.UserType;
import org.sunbird.user.util.UserConstants;
import play.libs.F.Promise;
import play.mvc.Result;

public class UserController extends BaseController {

  public Promise<Result> createUser() {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          request.getRequest().put(UserConstants.USER_TYPE, UserType.OTHER.getTypeName());
          new UserRequestValidator().validateCreateUserV1Request(request);
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> createUserV2() {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          request.getRequest().put(UserConstants.USER_TYPE, UserType.OTHER.getTypeName());
          new UserRequestValidator().validateCreateUserV2Request(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> updateUser() {
    return handleRequest(
        ActorOperations.UPDATE_USER.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.USER_ID, ctx().flash().get(JsonKey.USER_ID));
          new UserRequestValidator().validateUpdateUserRequest(request);
          request.getContext().put(JsonKey.IS_AUTH_REQ, ctx().flash().get(JsonKey.IS_AUTH_REQ));
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> getUserById(String userId) {
    return handleGetUserProfile(ActorOperations.GET_USER_PROFILE.getValue(), userId);
  }

  public Promise<Result> getUserByIdV2(String userId) {
    return handleGetUserProfile(ActorOperations.GET_USER_PROFILE_V2.getValue(), userId);
  }

  public Promise<Result> getUserByLoginId() {
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);

    return handleRequest(
        ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue(),
        request().body().asJson(),
        (req) -> {
          Request request = (Request) req;
          new UserRequestValidator().validateVerifyUser(request);
          request.getContext().put(JsonKey.FIELDS, requestedFields);

          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> getUserByKey() {
    return handleRequest(
        ActorOperations.GET_USER_BY_KEY.getValue(),
        request().body().asJson(),
        (request) -> {
          new UserGetRequestValidator().validateGetUserByKeyRequest((Request) request);
          return null;
        });
  }

  public Promise<Result> searchUser() {
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);
    return handleSearchRequest(
        ActorOperations.COMPOSITE_SEARCH.getValue(),
        request().body().asJson(),
        userSearchRequest -> {
          Request request = (Request) userSearchRequest;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          new BaseRequestValidator().validateSearchRequest(request);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(request()),
        EsType.user.getTypeName());
  }

  private Promise<Result> handleGetUserProfile(String operation, String userId) {
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);

    return handleRequest(
        operation,
        null,
        (req) -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.FIELDS, requestedFields);
          return null;
        },
        userId,
        JsonKey.USER_ID,
        false);
  }
}
