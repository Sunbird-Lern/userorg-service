package org.sunbird.service.user.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.client.systemsettings.SystemSettingClient;
import org.sunbird.client.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.UserLookupDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.dao.user.impl.UserLookupDaoImpl;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.util.AdminUtilHandler;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.UserUtility;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.adminutil.AdminUtilRequestData;
import org.sunbird.model.systemsettings.SystemSetting;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Slug;
import org.sunbird.util.user.UserActorOperations;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class UserServiceImpl implements UserService {

  private LoggerUtil logger = new LoggerUtil(UserServiceImpl.class);
  private EncryptionService encryptionService =
    org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(null);
  private static UserDao userDao = UserDaoImpl.getInstance();
  private static UserService userService = null;
  private static UserLookupDao userLookupDao = UserLookupDaoImpl.getInstance();

  private static final int GENERATE_USERNAME_COUNT = 10;
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  public static UserService getInstance() {
    if (userService == null) {
      userService = new UserServiceImpl();
    }
    return userService;
  }

  @Override
  public Response createUser(Map<String, Object> user, RequestContext context) {
    return userDao.createUser(user, context);
  }

  @Override
  public User getUserById(String userId, RequestContext context) {
    User user = userDao.getUserById(userId, context);
    if (null == user) {
      throw new ProjectCommonException(
        ResponseCode.userNotFound.getErrorCode(),
        ResponseCode.userNotFound.getErrorMessage(),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return user;
  }

  // This function is called during createUserV4 and update of users.
  @Override
  public void validateUserId(Request request, String managedById, RequestContext context) {
    String userId = null;
    String ctxtUserId = (String) request.getContext().get(JsonKey.USER_ID);
    String managedForId = (String) request.getContext().get(JsonKey.MANAGED_FOR);
    if (StringUtils.isEmpty(ctxtUserId)) {
      // In case of create, pick the ctxUserId from a different header
      // TODO: Unify and rely on one header for the context user identification
      ctxtUserId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
    } else {
      userId = UserUtil.getUserId(request.getRequest(), context);
    }
    logger.info(
      "validateUserId :: ctxtUserId : "
        + ctxtUserId
        + " userId: "
        + userId
        + " managedById: "
        + managedById
        + " managedForId: "
        + managedForId);
    // LIUA token is validated when LIUA is updating own account details or LIUA token is validated
    // when updating MUA details
    if ((StringUtils.isNotEmpty(managedForId) && !managedForId.equals(userId))
      || (StringUtils.isEmpty(managedById)
      && (!StringUtils.isBlank(userId) && !userId.equals(ctxtUserId))) // UPDATE
      || (StringUtils.isNotEmpty(managedById)
      && !(ctxtUserId.equals(managedById)))) // CREATE NEW USER/ UPDATE MUA {
      throw new ProjectCommonException(
        ResponseCode.unAuthorized.getErrorCode(),
        ResponseCode.unAuthorized.getErrorMessage(),
        ResponseCode.UNAUTHORIZED.getResponseCode());
  }

  @Override
  public Map<String, Object> esGetPublicUserProfileById(String userId, RequestContext context) {
    Future<Map<String, Object>> esResultF =
      esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId, context);
    Map<String, Object> esResult =
      (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (esResult == null || esResult.size() == 0) {
      throw new ProjectCommonException(
        ResponseCode.userNotFound.getErrorCode(),
        ResponseCode.userNotFound.getErrorMessage(),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return esResult;
  }

  @Override
  public String getRootOrgIdFromChannel(String channel, RequestContext context) {

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.IS_TENANT, true);
    filters.put(JsonKey.CHANNEL, channel);

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> esResultF =
      esUtil.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> esResult =
      (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (MapUtils.isNotEmpty(esResult)
      && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      Map<String, Object> esContent =
        ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
      if (null != esContent.get(JsonKey.STATUS)) {
        int status = (int) esContent.get(JsonKey.STATUS);
        if (1 != status) {
          ProjectCommonException.throwClientErrorException(
            ResponseCode.errorInactiveOrg,
            ProjectUtil.formatMessage(
              ResponseCode.errorInactiveOrg.getErrorMessage(), JsonKey.CHANNEL, channel));
        }
      } else {
        ProjectCommonException.throwClientErrorException(
          ResponseCode.errorInactiveOrg,
          ProjectUtil.formatMessage(
            ResponseCode.errorInactiveOrg.getErrorMessage(), JsonKey.CHANNEL, channel));
      }
      return (String) esContent.get(JsonKey.ID);
    } else {
      if (StringUtils.isNotBlank(channel)) {
        throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          ProjectUtil.formatMessage(
            ResponseCode.invalidParameterValue.getErrorMessage(), channel, JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
      } else {
        throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
            ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
          ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }

  @Override
  public String getCustodianChannel(
    Map<String, Object> userMap, ActorRef actorRef, RequestContext context) {
    String channel = (String) userMap.get(JsonKey.CHANNEL);
    if (StringUtils.isBlank(channel)) {
      try {
        Map<String, String> configSettingMap = DataCacheHandler.getConfigSettings();
        channel = configSettingMap.get(JsonKey.CUSTODIAN_ORG_CHANNEL);
        if (StringUtils.isBlank(channel)) {
          SystemSettingClient client = SystemSettingClientImpl.getInstance();
          SystemSetting custodianOrgChannelSetting =
            client.getSystemSettingByField(actorRef, JsonKey.CUSTODIAN_ORG_CHANNEL, context);
          if (custodianOrgChannelSetting != null
            && StringUtils.isNotBlank(custodianOrgChannelSetting.getValue())) {
            configSettingMap.put(
              custodianOrgChannelSetting.getId(), custodianOrgChannelSetting.getValue());
            channel = custodianOrgChannelSetting.getValue();
          }
        }
      } catch (Exception ex) {
        logger.error(
          context,
          "getCustodianChannel: Exception occurred while fetching custodian channel from system setting.",
          ex);
      }
    }
    if (StringUtils.isBlank(channel)) {
      channel = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_CHANNEL);
      userMap.put(JsonKey.CHANNEL, channel);
    }
    if (StringUtils.isBlank(channel)) {
      throw new ProjectCommonException(
        ResponseCode.mandatoryParamsMissing.getErrorCode(),
        ProjectUtil.formatMessage(
          ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
        ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return channel;
  }

  @Override
  public void validateUploader(Request request, RequestContext context) {
    // uploader and user should belong to same root org,
    // then only will allow to update user profile details.
    Map<String, Object> userMap = request.getRequest();
    String userId = (String) userMap.get(JsonKey.USER_ID);
    String uploaderUserId = (String) userMap.get(JsonKey.UPDATED_BY);
    User uploader = getUserById(uploaderUserId, context);
    User user = getUserById(userId, context);
    if (!user.getRootOrgId().equalsIgnoreCase(uploader.getRootOrgId())) {
      ProjectCommonException.throwUnauthorizedErrorException();
    }
  }

  @Override
  public List<String> getEncryptedList(List<String> dataList, RequestContext context) {
    List<String> encryptedDataList = new ArrayList<>();
    for (String data : dataList) {
      String encData = "";
      try {
        encData = encryptionService.encryptData(data, context);
      } catch (Exception e) {
        logger.error(
          context,
          "UserServiceImpl:getEncryptedDataList: Exception occurred with error message ",
          e);
      }
      if (StringUtils.isNotBlank(encData)) {
        encryptedDataList.add(encData);
      }
    }
    return encryptedDataList;
  }

  @Override
  public List<String> generateUsernames(
    String name, List<String> excludedUsernames, RequestContext context) {
    if (name == null || name.isEmpty()) return null;
    name = Slug.makeSlug(name, true);
    int numOfCharsToAppend =
      Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_USERNAME_NUM_DIGITS).trim());
    HashSet<String> userNameSet = new HashSet<>();
    int totalUserNameGenerated = 0;
    String nameLowercase = name.toLowerCase().replaceAll("\\-+", "");
    while (totalUserNameGenerated < GENERATE_USERNAME_COUNT) {
      String userNameSuffix =
        RandomStringUtils.randomAlphanumeric(numOfCharsToAppend).toLowerCase();

      StringBuilder userNameSB = new StringBuilder();
      userNameSB.append(nameLowercase).append("_").append(userNameSuffix);
      String generatedUsername = userNameSB.toString();

      if (!userNameSet.contains(generatedUsername)
        && !excludedUsernames.contains(generatedUsername)) {
        userNameSet.add(generatedUsername);
        totalUserNameGenerated += 1;
      }
    }
    return new ArrayList<>(userNameSet);
  }

  @Override
  public List<Map<String, Object>> searchUserNameInUserLookup(
    List<String> encUserNameList, RequestContext context) {

    Map<String, Object> reqMap = new LinkedHashMap<>();
    reqMap.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_USER_NAME);
    reqMap.put(JsonKey.VALUE, encUserNameList);

    return userLookupDao.getUsersByUserNames(reqMap, context);
  }

  @Override
  public Response userLookUpByKey(
    String key, String value, List<String> fields, RequestContext context) {
    Response response;
    if (JsonKey.ID.equalsIgnoreCase(key)) {
      List<String> ids = new ArrayList<>(2);
      ids.add(value);
      response = userDao.getUserPropertiesById(ids, fields, context);
    } else {
      List<Map<String, Object>> records =
        userLookupDao.getRecordByType(key.toLowerCase(), value.toLowerCase(), true, context);
      List<String> ids = new ArrayList<>();
      records
        .stream()
        .forEach(
          record -> {
            ids.add((String) record.get(JsonKey.USER_ID));
          });
      response = userDao.getUserPropertiesById(ids, fields, context);
    }
    for (Map<String, Object> userMap :
      (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE)) {
      UserUtility.decryptUserDataFrmES(userMap);
    }
    return response;
  }

  @Override
  public String getCustodianOrgId(ActorRef actorRef, RequestContext context) {
    String custodianOrgId = "";
    try {
      SystemSettingClient client = SystemSettingClientImpl.getInstance();
      SystemSetting systemSetting =
        client.getSystemSettingByField(actorRef, JsonKey.CUSTODIAN_ORG_ID, context);
      if (null != systemSetting && StringUtils.isNotBlank(systemSetting.getValue())) {
        custodianOrgId = systemSetting.getValue();
      }
    } catch (Exception ex) {
      logger.error(
        context,
        "getCustodianOrgId: Exception occurred with error message = " + ex.getMessage(),
        ex);
      ProjectCommonException.throwServerErrorException(
        ResponseCode.errorSystemSettingNotFound,
        ProjectUtil.formatMessage(
          ResponseCode.errorSystemSettingNotFound.getErrorMessage(), JsonKey.CUSTODIAN_ORG_ID));
    }
    return custodianOrgId;
  }

  /**
   * Fetch encrypted token list from admin utils
   *
   * @param parentId
   * @param respList
   * @param context
   * @return encryptedTokenList
   */
  public Map<String, Object> fetchEncryptedToken(
    String parentId, List<Map<String, Object>> respList, RequestContext context) {
    Map<String, Object> encryptedTokenList = null;
    try {
      // create AdminUtilRequestData list of managedUserId and parentId
      List<AdminUtilRequestData> managedUsers = createManagedUserList(parentId, respList);
      // Fetch encrypted token list from admin utils
      encryptedTokenList =
        AdminUtilHandler.fetchEncryptedToken(
          AdminUtilHandler.prepareAdminUtilPayload(managedUsers), context);
    } catch (ProjectCommonException pe) {
      throw pe;
    } catch (Exception e) {
      throw new ProjectCommonException(
        ResponseCode.unableToParseData.getErrorCode(),
        ResponseCode.unableToParseData.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return encryptedTokenList;
  }

  /**
   * Append encrypted token to the user list
   *
   * @param encryptedTokenList
   * @param respList
   * @param context
   */
  public void appendEncryptedToken(
    Map<String, Object> encryptedTokenList,
    List<Map<String, Object>> respList,
    RequestContext context) {
    ArrayList<Map<String, Object>> data =
      (ArrayList<Map<String, Object>>) encryptedTokenList.get(JsonKey.DATA);
    for (Object object : data) {
      Map<String, Object> tempMap = (Map<String, Object>) object;
      respList
        .stream()
        .filter(o -> o.get(JsonKey.ID).equals(tempMap.get(JsonKey.SUB)))
        .forEach(
          o -> {
            o.put(JsonKey.MANAGED_TOKEN, tempMap.get(JsonKey.TOKEN));
          });
    }
  }

  /**
   * Create managed user user list with parentId(managedBY) and childId(managedUser) in admin util
   * request format
   *
   * @param parentId
   * @param respList
   * @return reqData List<AdminUtilRequestData>
   */
  private List<AdminUtilRequestData> createManagedUserList(
    String parentId, List<Map<String, Object>> respList) {
    return respList
      .stream()
      .map(p -> new AdminUtilRequestData(parentId, (String) p.get(JsonKey.ID)))
      .collect(Collectors.toList());
  }

  public Response saveUserAttributes(
    Map<String, Object> userMap, ActorRef actorRef, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(UserActorOperations.SAVE_USER_ATTRIBUTES.getValue());
    request.getRequest().putAll(userMap);
    logger.info(context, "saveUserAttributes");
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      return (Response) Await.result(future, t.duration());
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    return null;
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userId
   * @param type value can be email, phone, recoveryEmail, recoveryPhone , prevUsedEmail or prevUsedPhone
   * @return
   */
  public String getDecryptedEmailPhoneByUserId(String userId, String type, RequestContext context) {
    Map<String, Object> user = userDao.getUserDetailsById(userId, context);
    if (MapUtils.isEmpty(user)) {
      throw new ProjectCommonException(
        ResponseCode.userNotFound.getErrorCode(),
        ResponseCode.userNotFound.getErrorMessage(),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    String emailPhone = getDecryptedValue((String) user.get(type), context);
    if (StringUtils.isBlank(emailPhone)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidRequestData);
    }
    return emailPhone;
  }

  private String getDecryptedValue(String key, RequestContext context) {
    if (StringUtils.isNotBlank(key)) {
      DecryptionService decService =
        org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(null);
      return decService.decryptData(key, context);
    }
    return "";
  }

  /**
   * This method will return either email or phone value of user based on the asked type in request
   *
   * @param userIds
   * @param type value can be email, phone
   * @return List<Map<String, Object>> i.e List of Map of userId, email/phone
   */
  @Override
  public List<Map<String, Object>> getDecryptedEmailPhoneByUserIds(List<String> userIds, String type, RequestContext context) {
    List<String> properties = new ArrayList<>();
    properties.add(type);
    properties.add(JsonKey.ID);
    properties.add(JsonKey.FIRST_NAME);
    properties.add(JsonKey.ROOT_ORG_ID);
    Response  response = userDao.getUserPropertiesById(userIds, properties, context);
    List<Map<String, Object>> responseList = (List<Map<String, Object>>)response.get(JsonKey.RESPONSE);
    responseList.stream().forEach(resMap -> {
      resMap.put(type, getDecryptedValue((String)resMap.get(type), context));
    });
    return responseList;
  }

  @Override
  public List<Map<String, Object>> getUserEmailsBySearchQuery(Map<String, Object> searchQuery, RequestContext context) {
    Future<Map<String, Object>> esResultF =
        esUtil.search(
          ElasticSearchHelper.createSearchDTO(searchQuery),
          ProjectUtil.EsType.user.getTypeName(),
          context);
    List<Map<String, Object>> usersList = new ArrayList<>();
    Map<String, Object> esResult = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);
    if (MapUtils.isNotEmpty(esResult)
      && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      usersList =
        (List<Map<String, Object>>) esResult.get(JsonKey.CONTENT);
      usersList.forEach(
        user -> {
          if (org.apache.commons.lang.StringUtils.isNotBlank((String) user.get(JsonKey.EMAIL))) {
            String email = getDecryptedValue((String) user.get(JsonKey.EMAIL), context);
            if (ProjectUtil.isEmailvalid(email)) {
              user.put(JsonKey.EMAIL,email);
            } else {
              logger.info(
                "UserServiceImpl:getUserEmailsBySearchQuery: Invalid Email or its decryption failed for userId = "
                  + user.get(JsonKey.USER_ID));
              user.put(JsonKey.EMAIL,null);
            }
          }
        });
    }
    return usersList;
  }
}
