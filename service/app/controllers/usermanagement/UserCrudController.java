package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

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
          UserRequestValidator.validateCreateUserV1Request(request);
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
          UserRequestValidator.validateCreateUserV2Request(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
          return null;
        },
        null,
        null,
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
          request.getContext().put(JsonKey.USER_ID, ctx().flash().get(JsonKey.USER_ID));
          UserRequestValidator.validateUpdateUserRequest(request);
          request.getContext().put(JsonKey.IS_AUTH_REQ, ctx().flash().get(JsonKey.IS_AUTH_REQ));
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
          request.getContext().put(JsonKey.FIELDS, requestedFields);
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
          request.getContext().put(JsonKey.FIELDS, requestedFields);

          return null;
        },
        null,
        null,
        true);
  }

  /**
   * This method will do the user search for Elastic search. this will internally call composite
   * search api.
   *
   * @return Promise<Result>
   */
  public Promise<Result> search() {
    return handleSearchRequest(
        ActorOperations.COMPOSITE_SEARCH.getValue(),
        request().body().asJson(),
        userSearchRequest -> {
          new BaseRequestValidator().validateSearchRequest((Request) userSearchRequest);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(request()),
        EsType.user.getTypeName());
  }

  // ctx().flash().containsKey(JsonKey.AUTH_WITH_MASTER_KEY)
  // ctx().flash().get(JsonKey.USER_ID)

}
