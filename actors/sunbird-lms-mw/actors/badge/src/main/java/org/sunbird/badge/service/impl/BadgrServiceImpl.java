package org.sunbird.badge.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.badge.service.BadgeClassExtensionService;
import org.sunbird.badge.service.BadgingService;
import org.sunbird.badge.util.BadgingUtil;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.HttpUtilResponse;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.CourseBatchSchedulerUtil;
import org.sunbird.learner.util.Util;
import org.sunbird.telemetry.util.TelemetryUtil;

/** @author Manzarul */
public class BadgrServiceImpl implements BadgingService {
  private BadgeClassExtensionService badgeClassExtensionService;
  private ObjectMapper mapper = new ObjectMapper();
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  public static Map<String, String> headerMap = new HashMap<>();

  static {
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().readProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
  }

  public BadgrServiceImpl() {
    this.badgeClassExtensionService = new BadgeClassExtensionServiceImpl();
  }

  public BadgrServiceImpl(BadgeClassExtensionService badgeClassExtensionService) {
    this.badgeClassExtensionService = badgeClassExtensionService;
  }

  @Override
  public Response createIssuer(Request request) throws IOException {

    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Map<String, Object> req = request.getRequest();
    byte[] image = null;
    Map<String, byte[]> fileData = new HashMap<>();
    if (req.containsKey(JsonKey.IMAGE) && null != req.get(JsonKey.IMAGE)) {
      image = (byte[]) req.get(JsonKey.IMAGE);
    }
    Map<String, String> requestData = new HashMap<>();
    requestData.put(JsonKey.NAME, (String) req.get(JsonKey.NAME));
    requestData.put(JsonKey.DESCRIPTION, (String) req.get(JsonKey.DESCRIPTION));
    requestData.put(JsonKey.URL, (String) req.get(JsonKey.URL));
    requestData.put(JsonKey.EMAIL, (String) req.get(JsonKey.EMAIL));
    requestData.put(JsonKey.SLUG, BadgingJsonKey.ISSUER_SLUG);

    if (null != image) {
      fileData.put(JsonKey.IMAGE, image);
    }
    HttpUtilResponse httpResponse =
        HttpUtil.postFormData(
            requestData,
            fileData,
            BadgingUtil.getBadgrHeaders(false),
            BadgingUtil.getBadgeIssuerUrl());

    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.ISSUER);
    Response response = new Response();
    BadgingUtil.prepareBadgeIssuerResponse(httpResponse.getBody(), response.getResult());

    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) response.getResult().get(BadgingJsonKey.ISSUER_ID),
            BadgingJsonKey.BADGE_ISSUER,
            JsonKey.CREATE,
            null);
    TelemetryUtil.telemetryProcessingCall(req, targetObject, correlatedObject);

    return response;
  }

  @Override
  public Response getIssuerDetails(Request request) throws IOException {
    Map<String, Object> req = request.getRequest();
    String slug = (String) req.get(JsonKey.SLUG);
    HttpUtilResponse httpResponse =
        HttpUtil.doGetRequest(BadgingUtil.getBadgeIssuerUrl(slug), BadgingUtil.getBadgrHeaders());
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.ISSUER);
    Response response = new Response();
    BadgingUtil.prepareBadgeIssuerResponse(httpResponse.getBody(), response.getResult());
    return response;
  }

  @Override
  public Response getIssuerList(Request request) throws IOException {
    HttpUtilResponse httpResponse =
        HttpUtil.doGetRequest(BadgingUtil.getBadgeIssuerUrl(), BadgingUtil.getBadgrHeaders());
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.ISSUER);
    Response response = new Response();

    List<Map<String, Object>> issuers = new ArrayList<>();
    List<Map<String, Object>> data =
        mapper.readValue(httpResponse.getBody(), new TypeReference<List<Map<String, Object>>>() {});
    for (Object badge : data) {
      Map<String, Object> mappedBadge = new HashMap<>();
      BadgingUtil.prepareBadgeIssuerResponse((Map<String, Object>) badge, mappedBadge);
      issuers.add(mappedBadge);
    }
    Map<String, Object> res = new HashMap<>();
    res.put(BadgingJsonKey.ISSUERS, issuers);
    response.getResult().putAll(res);
    return response;
  }

  @Override
  public Response createBadgeClass(Request request) throws ProjectCommonException {
    Response response = new Response();

    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    try {
      Map<String, Object> requestData = request.getRequest();

      Map<String, String> formParams = new HashMap<>();
      formParams.put(
          BadgingJsonKey.ISSUER_SLUG, (String) requestData.get(BadgingJsonKey.ISSUER_ID));
      formParams.put(JsonKey.NAME, (String) requestData.get(JsonKey.NAME));
      formParams.put(JsonKey.DESCRIPTION, (String) requestData.get(JsonKey.DESCRIPTION));
      formParams.put(JsonKey.CRITERIA, (String) requestData.get(JsonKey.CRITERIA));
      formParams.put(JsonKey.SLUG, BadgingJsonKey.BADGE_SLUG);

      Map<String, byte[]> fileParams = new HashMap<>();
      fileParams.put(JsonKey.IMAGE, (byte[]) requestData.get(JsonKey.IMAGE));

      String issuerId = (String) requestData.get(BadgingJsonKey.ISSUER_ID);
      String rootOrgId = (String) requestData.get(JsonKey.ROOT_ORG_ID);
      String type = (String) requestData.get(JsonKey.TYPE);
      String subtype = (String) requestData.get(JsonKey.SUBTYPE);
      String roles = (String) requestData.get(JsonKey.ROLES);

      if (type != null) {
        type = type.toLowerCase();
      }

      if (subtype != null) {
        subtype = subtype.toLowerCase();
      }

      List<String> rolesList;
      if (roles != null) {
        String trimmedRoles = roles.trim();
        if (trimmedRoles.startsWith("[") && trimmedRoles.endsWith("]")) {
          rolesList = mapper.readValue(trimmedRoles, ArrayList.class);
        } else {
          rolesList = new ArrayList<>();
          rolesList.add(trimmedRoles);
        }
      } else {
        rolesList = new ArrayList<>();
      }
      // validating incoming roles.
      validateRoles(rolesList);

      Map<String, String> headers = BadgingUtil.getBadgrHeaders(false);

      HttpUtilResponse httpUtilResponse =
          HttpUtil.postFormData(
              formParams, fileParams, headers, BadgingUtil.getBadgeClassUrl(issuerId));
      String badgrResponseStr = httpUtilResponse.getBody();

      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          httpUtilResponse.getStatusCode(), badgrResponseStr, BadgingJsonKey.BADGE_CLASS);

      Map<String, Object> badgrResponseMap = mapper.readValue(badgrResponseStr, HashMap.class);
      String badgeId = (String) badgrResponseMap.get(BadgingJsonKey.SLUG);

      BadgeClassExtension badgeClassExt =
          new BadgeClassExtension(badgeId, issuerId, rootOrgId, type, subtype, rolesList);
      badgeClassExtensionService.save(badgeClassExt);

      BadgingUtil.prepareBadgeClassResponse(badgrResponseStr, badgeClassExt, response.getResult());
      targetObject =
          TelemetryUtil.generateTargetObject(
              (String) response.getResult().get(BadgingJsonKey.BADGE_ID),
              BadgingJsonKey.BADGE_CLASS,
              JsonKey.CREATE,
              null);
      TelemetryUtil.generateCorrelatedObject(
          issuerId, BadgingJsonKey.BADGE_ISSUER, null, correlatedObject);
      TelemetryUtil.generateCorrelatedObject(
          rootOrgId, JsonKey.ORGANISATION, null, correlatedObject);
      TelemetryUtil.telemetryProcessingCall(requestData, targetObject, correlatedObject);

    } catch (IOException e) {
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.SERVER_ERROR.getResponseCode(), e.getMessage(), BadgingJsonKey.BADGE_CLASS);
    }

    return response;
  }

  @Override
  public Response getBadgeClassDetails(String badgeId) throws ProjectCommonException {
    Response response = new Response();

    try {

      Map<String, String> headers = BadgingUtil.getBadgrHeaders();
      String badgrUrl = BadgingUtil.getBadgeClassUrl(BadgingJsonKey.ISSUER_ID, badgeId);

      HttpUtilResponse httpUtilResponse = HttpUtil.doGetRequest(badgrUrl, headers);
      String badgrResponseStr = httpUtilResponse.getBody();

      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          httpUtilResponse.getStatusCode(), badgrResponseStr, BadgingJsonKey.BADGE_CLASS);

      BadgeClassExtension badgeClassExtension = badgeClassExtensionService.get(badgeId);

      BadgingUtil.prepareBadgeClassResponse(
          badgrResponseStr, badgeClassExtension, response.getResult());
    } catch (IOException e) {
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.SERVER_ERROR.getResponseCode(), e.getMessage(), BadgingJsonKey.BADGE_CLASS);
    }

    return response;
  }

  @Override
  public Response searchBadgeClass(Request request) throws ProjectCommonException {
    Response response = new Response();

    Map<String, Object> filtersMap =
        (Map<String, Object>) request.getRequest().get(JsonKey.FILTERS);

    List<String> issuerList = (List<String>) filtersMap.get(BadgingJsonKey.ISSUER_LIST);
    List<String> badgeList = (List<String>) filtersMap.get(BadgingJsonKey.BADGE_LIST);
    String rootOrgId = (String) filtersMap.get(JsonKey.ROOT_ORG_ID);
    String type = (String) filtersMap.get(JsonKey.TYPE);
    String subtype = (String) filtersMap.get(JsonKey.SUBTYPE);
    List<String> allowedRoles = (List<String>) filtersMap.get(JsonKey.ROLES);

    if (type != null) {
      type = type.toLowerCase();
    }

    if (subtype != null) {
      subtype = subtype.toLowerCase();
    }

    List<BadgeClassExtension> badgeClassExtList =
        badgeClassExtensionService.search(
            issuerList, badgeList, rootOrgId, type, subtype, allowedRoles);
    List<String> filteredIssuerList =
        badgeClassExtList
            .stream()
            .map(badge -> badge.getIssuerId())
            .distinct()
            .collect(Collectors.toList());

    List<Object> badges = new ArrayList<>();

    for (String issuerSlug : filteredIssuerList) {
      badges.addAll(listBadgeClassForIssuer(issuerSlug, badgeClassExtList));
    }

    response.put(BadgingJsonKey.BADGES, badges);

    return response;
  }

  private List<Object> listBadgeClassForIssuer(
      String issuerSlug, List<BadgeClassExtension> badgeClassExtensionList)
      throws ProjectCommonException {
    List<Object> filteredBadges = new ArrayList<>();

    try {
      Map<String, String> headers = BadgingUtil.getBadgrHeaders();
      String badgrUrl = BadgingUtil.getBadgeClassUrl(issuerSlug);

      HttpUtilResponse httpUtilResponse = HttpUtil.doGetRequest(badgrUrl, headers);
      String badgrResponseStr = httpUtilResponse.getBody();

      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          httpUtilResponse.getStatusCode(), badgrResponseStr, BadgingJsonKey.BADGE_CLASS);

      List<Map<String, Object>> badges = mapper.readValue(badgrResponseStr, ArrayList.class);

      for (Map<String, Object> badge : badges) {
        BadgeClassExtension matchedBadgeClassExt =
            badgeClassExtensionList
                .stream()
                .filter(x -> x.getBadgeId().equals(badge.get(BadgingJsonKey.SLUG)))
                .findFirst()
                .orElse(null);

        if (matchedBadgeClassExt != null) {
          Map<String, Object> mappedBadge = new HashMap<>();
          BadgingUtil.prepareBadgeClassResponse(badge, matchedBadgeClassExt, mappedBadge);
          filteredBadges.add(mappedBadge);
        }
      }
    } catch (IOException e) {
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.SERVER_ERROR.getResponseCode(), e.getMessage(), BadgingJsonKey.BADGE_CLASS);
    }

    return filteredBadges;
  }

  @Override
  public Response removeBadgeClass(Request requestMsg) throws ProjectCommonException {
    Response response = new Response();
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    try {
      Map<String, Object> requestData = requestMsg.getRequest();

      String badgeId = (String) requestData.get(BadgingJsonKey.BADGE_ID);

      Map<String, String> headers = BadgingUtil.getBadgrHeaders();
      // Using dummy issuer ID (rather than getting it from DB) as badgr ignores it anyway.
      String badgrUrl = BadgingUtil.getBadgeClassUrl(BadgingJsonKey.ISSUER_ID, badgeId);

      HttpUtilResponse httpUtilResponse = HttpUtil.sendDeleteRequest(headers, badgrUrl);
      String badgrResponseStr = httpUtilResponse.getBody();

      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          httpUtilResponse.getStatusCode(), badgrResponseStr, BadgingJsonKey.BADGE_CLASS);

      badgeClassExtensionService.delete(badgeId);
      response.put(JsonKey.MESSAGE, badgrResponseStr.replaceAll("^\"|\"$", ""));
      targetObject =
          TelemetryUtil.generateTargetObject(
              badgeId, BadgingJsonKey.BADGE_CLASS, JsonKey.DELETE, null);
      TelemetryUtil.telemetryProcessingCall(requestData, targetObject, correlatedObject);

    } catch (IOException e) {
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.SERVER_ERROR.getResponseCode(), e.getMessage(), BadgingJsonKey.BADGE_CLASS);
    }

    return response;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Response badgeAssertion(Request request) throws IOException {
    // Based on incoming recipientType and recipientId collect the email.
    Map<String, Object> requestedData = request.getRequest();
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    String email =
        getEmail(
            (String) requestedData.get(BadgingJsonKey.RECIPIENT_ID),
            (String) requestedData.get(BadgingJsonKey.RECIPIENT_TYPE));
    if (StringUtils.isBlank(email) && !ProjectUtil.isEmailvalid(email)) {
      ProjectLogger.log("Recipient email is not valid ==" + email);
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), null, BadgingJsonKey.BADGE_ASSERTION);
    }
    requestedData.put(BadgingJsonKey.RECIPIENT_EMAIL, email);
    String requestBody = BadgingUtil.createAssertionReqData(requestedData);
    String url =
        BadgingUtil.createBadgerUrl(
            requestedData, BadgingUtil.SUNBIRD_BADGER_CREATE_ASSERTION_URL, 2);
    ProjectLogger.log(
        "AssertionData==" + requestBody + "  " + url + "  " + BadgingUtil.getBadgrHeaders(),
        LoggerEnum.INFO.name());
    HttpUtilResponse httpResponse =
        HttpUtil.doPostRequest(url, requestBody, BadgingUtil.getBadgrHeaders());

    ProjectLogger.log(
        "AssertionDataResponse==" + httpResponse.getStatusCode(), LoggerEnum.INFO.name());
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.BADGE_ASSERTION);
    Map<String, Object> res = mapper.readValue(httpResponse.getBody(), HashMap.class);
    // calling to create response as per sunbird
    res = BadgingUtil.prepareAssertionResponse(res, new HashMap<String, Object>());
    Response response = new Response();
    response.getResult().putAll(res);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) response.getResult().get(BadgingJsonKey.BADGE_ID),
            BadgingJsonKey.BADGE_CLASS,
            JsonKey.CREATE,
            null);
    TelemetryUtil.generateCorrelatedObject(
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_ID),
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_TYPE),
        null,
        correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) requestedData.get(BadgingJsonKey.ISSUER_ID),
        BadgingJsonKey.BADGE_ISSUER,
        null,
        correlatedObject);
    TelemetryUtil.generateCorrelatedObject(
        (String) requestedData.get(BadgingJsonKey.BADGE_ID),
        BadgingJsonKey.BADGE_CLASS,
        null,
        correlatedObject);
    TelemetryUtil.telemetryProcessingCall(requestedData, targetObject, correlatedObject);
    return response;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Response getAssertionDetails(Request request) throws IOException {
    String url =
        BadgingUtil.createBadgerUrl(
            request.getRequest(), BadgingUtil.SUNBIRD_BADGER_GETASSERTION_URL, 3);
    HttpUtilResponse httpResponse = HttpUtil.doGetRequest(url, BadgingUtil.getBadgrHeaders());
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.BADGE_ASSERTION);
    Map<String, Object> res = mapper.readValue(httpResponse.getBody(), HashMap.class);
    // calling to create response as per sunbird
    res = BadgingUtil.prepareAssertionResponse(res, new HashMap<String, Object>());
    Response response = new Response();
    response.getResult().putAll(res);
    return response;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Response getAssertionList(Request request) throws IOException {
    Map<String, Object> filterMap = (Map<String, Object>) request.getRequest().get(JsonKey.FILTERS);
    List<String> requestData = (List) filterMap.get(BadgingJsonKey.ASSERTIONS);
    List<Map<String, Object>> responseList = new ArrayList<>();
    for (String assertionId : requestData) {
      WeakHashMap<String, Object> map = new WeakHashMap<>();
      map.put(BadgingJsonKey.ASSERTION_ID, assertionId);
      String url = BadgingUtil.createBadgerUrl(map, BadgingUtil.SUNBIRD_BADGER_GETASSERTION_URL, 3);
      HttpUtilResponse httpResponse = HttpUtil.doGetRequest(url, BadgingUtil.getBadgrHeaders());
      if (httpResponse.getStatusCode() == 200) {
        Map<String, Object> res = mapper.readValue(httpResponse.getBody(), HashMap.class);
        // calling to create response as per sunbird
        res = BadgingUtil.prepareAssertionResponse(res, new HashMap<String, Object>());
        responseList.add(res);
      }
    }
    Response response = new Response();
    response.getResult().put(BadgingJsonKey.ASSERTIONS, responseList);
    return response;
  }

  @Override
  public Response revokeAssertion(Request request) throws IOException {
    // do the recipient id validation
    Map<String, Object> requestedData = request.getRequest();
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    // This is doing the validation of incoming userId or contnetId
    // only this value is not passed to badgr.io
    getEmail(
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_ID),
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_TYPE));
    String url =
        BadgingUtil.createBadgerUrl(
            request.getRequest(), BadgingUtil.SUNBIRD_BADGER_GETASSERTION_URL, 3);
    String requestBody = BadgingUtil.createAssertionRevokeData(request.getRequest());
    HttpUtilResponse httpResponse =
        HttpUtil.sendDeleteRequest(requestBody, BadgingUtil.getBadgrHeaders(), url);
    String badgrResponseStr = httpResponse.getBody();
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), badgrResponseStr, BadgingJsonKey.BADGE_ASSERTION);
    Response response = new Response();
    response.getResult().put(JsonKey.STATUS, JsonKey.SUCCESS);
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) requestedData.get(BadgingJsonKey.ASSERTION_ID),
            BadgingJsonKey.BADGE_ISSUER,
            JsonKey.DELETE,
            null);
    TelemetryUtil.generateCorrelatedObject(
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_ID),
        (String) requestedData.get(BadgingJsonKey.RECIPIENT_TYPE),
        null,
        correlatedObject);
    TelemetryUtil.telemetryProcessingCall(requestedData, targetObject, correlatedObject);
    return response;
  }

  @Override
  public Response deleteIssuer(Request request) throws IOException {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();

    Map<String, Object> req = request.getRequest();
    String slug = (String) req.get(JsonKey.SLUG);
    HttpUtilResponse httpResponse =
        HttpUtil.sendDeleteRequest(
            BadgingUtil.getBadgrHeaders(), BadgingUtil.getBadgeIssuerUrl(slug));
    BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
        httpResponse.getStatusCode(), null, BadgingJsonKey.ISSUER);
    Response response = new Response();
    // since the response from badger service contains " at beging and end so remove that from
    // response string
    response.put(JsonKey.MESSAGE, StringUtils.strip(httpResponse.getBody(), "\""));

    targetObject =
        TelemetryUtil.generateTargetObject(slug, BadgingJsonKey.BADGE_ISSUER, JsonKey.DELETE, null);
    TelemetryUtil.telemetryProcessingCall(req, targetObject, correlatedObject);
    return response;
  }

  /**
   * This method wu
   *
   * @param recipientId
   * @param recipientType
   * @return
   */
  public static String getEmail(String recipientId, String recipientType) {
    if (BadgingJsonKey.BADGE_TYPE_USER.equalsIgnoreCase(recipientType)) {
      return getUserEmailFromDB(recipientId);
    } else {
      return verifyContent(recipientId);
    }
  }

  /**
   * This method will provide user email id.
   *
   * @param userId String
   * @return String
   */
  private static String getUserEmailFromDB(String userId) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response =
        cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), userId);
    List<Map<String, Object>> user = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if ((user == null) || user.isEmpty()) {
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), null, BadgingJsonKey.USER);
    }
    String email =
        org.sunbird.common.models.util.datasecurity.impl.ServiceFactory
            .getDecryptionServiceInstance(null)
            .decryptData((String) user.get(0).get(JsonKey.EMAIL));
    // verify the email format.
    if (ProjectUtil.isEmailvalid(email)) {
      return email;
    } else {
      return getDefaultEmail();
    }
  }

  /**
   * This method will take contentId and make EKstep api call to do the content validation, if
   * content found inside Ekstep then it will take createdBy attribute and make sunbird api call to
   * get the user email. if content not found then user will get 404 error.
   *
   * @param contentId String
   * @return String
   */
  private static String verifyContent(String contentId) {
    String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
    String url =
        ekStepBaseUrl
            + PropertiesCache.getInstance().getProperty("sunbird_content_read")
            + "/"
            + contentId;
    try {
      HttpUtilResponse response = HttpUtil.doGetRequest(url, CourseBatchSchedulerUtil.headerMap);
      if (response == null || response.getStatusCode() >= 300) {
        BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), null, BadgingJsonKey.CONTENT);
      }
      String userId = getUserIdFromContent(response.getBody());
      // some of the content won't have createdBy , so in that case
      // take do the content validation only and used default
      // set admin email
      if (StringUtils.isBlank(userId)) {
        return getDefaultEmail();
      }
      try {
        return getUserEmailFromDB(userId);
      } catch (Exception e) {
        // no need to throw the exception if content id is valid.
      }
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
      BadgingUtil.throwBadgeClassExceptionOnErrorStatus(
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode(), null, BadgingJsonKey.CONTENT);
    }
    return getDefaultEmail();
  }

  /**
   * This method will provide default email set inside env.
   *
   * @return String
   */
  private static String getDefaultEmail() {
    String adminEmail = ProjectUtil.getConfigValue(BadgingJsonKey.SUNBIRD_INSTALLATION_EMAIL);
    ProjectLogger.log("Default email is set to ==" + adminEmail);
    return adminEmail;
  }

  /**
   * This method will read ekstep content body and get createdBy from that, which will internally
   * refer to our user id.
   *
   * @param body Stirng
   * @return String
   */
  private static String getUserIdFromContent(String body) {
    String userId = null;
    try {
      JSONObject object = new JSONObject(body);
      JSONObject data = object.getJSONObject(JsonKey.RESULT);
      JSONObject contentData = data.getJSONObject(JsonKey.CONTENT);
      userId = contentData.getString(JsonKey.CREATED_BY);
    } catch (JSONException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return userId;
  }

  /**
   * This method will validate the roles.
   *
   * @param roleList
   */
  private static void validateRoles(List<String> roleList) {
    if (null == roleList || roleList.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.badgeRolesRequired.getErrorCode(),
          ResponseCode.badgeRolesRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    String result = Util.validateRoles(roleList);
    if (!(JsonKey.SUCCESS.equalsIgnoreCase(result))) {
      throw new ProjectCommonException(
          ResponseCode.badgeRolesRequired.getErrorCode(),
          ResponseCode.badgeRolesRequired.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }
}
