package controllers.usermanagement;

import controllers.BaseController;
import controllers.usermanagement.validator.UserGetRequestValidator;
import java.util.HashMap;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserRequestValidator;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

public class UserController extends BaseController {

  public Promise<Result> createUser() {
    return handleRequest(
        ActorOperations.CREATE_USER.getValue(),
        request().body().asJson(),
        req -> {
          Request request = (Request) req;
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
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateCreateUserV2Request(request);
          request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> updateUser() {
    final boolean isPrivate;
    if (request().path().contains(JsonKey.PRIVATE)) {
      isPrivate = true;
    } else {
      isPrivate = false;
    }
    return handleRequest(
        ActorOperations.UPDATE_USER.getValue(),
        request().body().asJson(),
        req -> {
          Request request = (Request) req;
          request.getContext().put(JsonKey.USER_ID, ctx().flash().get(JsonKey.USER_ID));
          request.getContext().put(JsonKey.PRIVATE, isPrivate);
          new UserRequestValidator().validateUpdateUserRequest(request);
          request.getContext().put(JsonKey.IS_AUTH_REQ, ctx().flash().get(JsonKey.IS_AUTH_REQ));

          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> getUserById(String userId) {
    return handleGetUserProfile(
        ActorOperations.GET_USER_PROFILE.getValue(), ProjectUtil.getLmsUserId(userId));
  }

  public Result getUserByIdMock(String userId) {
    return Results.ok(
        Json.parse(
            "{\"id\":\"\",\"ver\":\"private\",\"ts\":\"2019-01-17 16:53:26:286+0530\",\"params\":{\"resmsgid\":null,\"msgid\":\"8e27cbf5-e299-43b0-bca7-8347f7e5abcf\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"webPages\":[],\"education\":[],\"subject\":[],\"channel\":\"channel_01\",\"language\":[],\"updatedDate\":null,\"skills\":[],\"id\":\"56a194c6-a064-4ba7-9771-7811d8f6d487\",\"identifier\":\"56a194c6-a064-4ba7-9771-7811d8f6d487\",\"profileVisibility\":{\"lastName\":\"public\",\"webPages\":\"public\",\"jobProfile\":\"public\",\"address\":\"public\",\"education\":\"public\",\"gender\":\"public\",\"profileSummary\":\"public\",\"subject\":\"public\",\"language\":\"public\",\"avatar\":\"public\",\"userName\":\"public\",\"skills\":\"public\",\"firstName\":\"public\",\"badgeAssertions\":\"public\",\"phone\":\"private\",\"countryCode\":\"public\",\"dob\":\"public\",\"grade\":\"public\",\"location\":\"public\",\"email\":\"private\"},\"thumbnail\":null,\"updatedBy\":null,\"jobProfile\":[],\"locationIds\":[],\"externalIds\":[{\"idType\":\"111\",\"provider\":\"111\",\"id\":\"111\"}],\"registryId\":null,\"rootOrgId\":\"0125716257142538240\",\"firstName\":\"Amit\",\"tncAcceptedOn\":null,\"phone\":\"9*****4848\",\"dob\":null,\"grade\":[],\"currentLoginTime\":null,\"userType\":\"OTHER\",\"status\":1,\"lastName\":\"Kumar\",\"tncLatestVersion\":\"v2\",\"gender\":\"male\",\"roles\":[\"PUBLIC\"],\"badgeAssertions\":[],\"isDeleted\":false,\"organisations\":[{\"organisationId\":\"0125716257142538240\",\"updatedBy\":null,\"addedByName\":null,\"addedBy\":null,\"roles\":[\"PUBLIC\"],\"approvedBy\":null,\"updatedDate\":null,\"userId\":\"56a194c6-a064-4ba7-9771-7811d8f6d487\",\"approvaldate\":null,\"isDeleted\":false,\"hashTagId\":\"0125716257142538240\",\"isRejected\":null,\"id\":\"0126788301339033600\",\"position\":null,\"isApproved\":null,\"orgjoindate\":\"2019-01-17 12:33:35:186+0530\",\"orgLeftDate\":null}],\"countryCode\":null,\"tncLatestVersionUrl\":\"https://dev-sunbird-temp.azureedge.net/portal/terms-and-conditions-v1.html\",\"tempPassword\":null,\"email\":\"12*@gmail.com\",\"rootOrg\":{\"dateTime\":null,\"preferredLanguage\":null,\"approvedBy\":null,\"channel\":\"channel_01\",\"description\":\"channel1\",\"updatedDate\":null,\"addressId\":null,\"orgType\":null,\"provider\":null,\"locationId\":null,\"orgCode\":null,\"theme\":null,\"id\":\"0125716257142538240\",\"communityId\":null,\"isApproved\":null,\"slug\":\"channel_01\",\"identifier\":\"0125716257142538240\",\"thumbnail\":null,\"orgName\":\"mantra\",\"updatedBy\":null,\"externalId\":null,\"isRootOrg\":true,\"rootOrgId\":\"0125716257142538240\",\"approvedDate\":null,\"imgUrl\":null,\"homeUrl\":null,\"orgTypeId\":null,\"isDefault\":null,\"contactDetail\":[],\"createdDate\":\"2018-08-19 01:19:41:624+0530\",\"createdBy\":\"84635401-2f52-44eb-8fe7-b4949f0af308\",\"parentOrgId\":null,\"hashTagId\":\"0125716257142538240\",\"noOfMembers\":null,\"status\":1},\"address\":[],\"defaultProfileFieldVisibility\":\"public\",\"phoneVerified\":true,\"profileSummary\":null,\"avatar\":null,\"userName\":\"123\",\"userId\":\"56a194c6-a064-4ba7-9771-7811d8f6d487\",\"promptTnC\":true,\"emailVerified\":true,\"lastLoginTime\":null,\"createdDate\":\"2019-01-17 12:33:33:301+0530\",\"createdBy\":null,\"location\":null,\"tncAcceptedVersion\":null,\"frameworkdata\":null}}}"));
  }

  public Promise<Result> getUserByIdV2(String userId) {
    return handleGetUserProfile(
        ActorOperations.GET_USER_PROFILE_V2.getValue(), ProjectUtil.getLmsUserId(userId));
  }

  public Promise<Result> getUserByLoginId() {
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);

    return handleRequest(
        ActorOperations.GET_USER_DETAILS_BY_LOGINID.getValue(),
        request().body().asJson(),
        req -> {
          Request request = (Request) req;
          new UserRequestValidator().validateVerifyUser(request);
          request.getContext().put(JsonKey.FIELDS, requestedFields);

          return null;
        },
        null,
        null,
        true);
  }

  public Promise<Result> getUserByKey(String idType, String id) {

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
        false);
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
    final boolean isPrivate = request().path().contains(JsonKey.PRIVATE) ? true : false;
    final String requestedFields = request().getQueryString(JsonKey.FIELDS);
    final String provider = request().getQueryString(JsonKey.PROVIDER);
    final String idType = request().getQueryString(JsonKey.ID_TYPE);
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
        false);
  }
}
