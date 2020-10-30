package org.sunbird.user.service.impl;

import akka.actor.ActorRef;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.*;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.AdminUtilHandler;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.models.adminutil.AdminUtilRequestData;
import org.sunbird.models.systemsetting.SystemSetting;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.service.UserService;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

public class UserServiceImpl implements UserService {

  private LoggerUtil logger = new LoggerUtil(UserServiceImpl.class);
  private EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private static UserDao userDao = UserDaoImpl.getInstance();
  private static UserService userService = null;
  private static final int GENERATE_USERNAME_COUNT = 10;
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  private static Random rand = new Random(System.nanoTime());
  private static final String[] alphabet =
      new String[] {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i",
        "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
      };

  private static String stripChars = "0";
  private static BigDecimal largePrimeNumber = new BigDecimal(1679979167);

  public static UserService getInstance() {
    if (userService == null) {
      userService = new UserServiceImpl();
    }
    return userService;
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
  public void syncUserProfile(
      String userId,
      Map<String, Object> userDataMap,
      Map<String, Object> userPrivateDataMap,
      RequestContext context) {
    esUtil.save(
        ProjectUtil.EsType.userprofilevisibility.getTypeName(),
        userId,
        userPrivateDataMap,
        context);
    esUtil.save(ProjectUtil.EsType.user.getTypeName(), userId, userDataMap, context);
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
  public Map<String, Object> esGetPrivateUserProfileById(String userId, RequestContext context) {
    Future<Map<String, Object>> resultF =
        esUtil.getDataByIdentifier(
            ProjectUtil.EsType.userprofilevisibility.getTypeName(), userId, context);
    return (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
  }

  @Override
  public String getRootOrgIdFromChannel(String channel, RequestContext context) {

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.IS_ROOT_ORG, true);
    if (StringUtils.isNotBlank(channel)) {
      filters.put(JsonKey.CHANNEL, channel);
    } else {
      // If channel value is not coming in request then read the default channel value provided from
      // ENV.
      if (StringUtils.isNotBlank(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_CHANNEL))) {
        filters.put(JsonKey.CHANNEL, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_CHANNEL));
      } else {
        throw new ProjectCommonException(
            ResponseCode.mandatoryParamsMissing.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.CHANNEL),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    Future<Map<String, Object>> esResultF =
        esUtil.search(searchDTO, EsType.organisation.getTypeName(), context);
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
    User uploader = userService.getUserById(uploaderUserId, context);
    User user = userService.getUserById(userId, context);
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
      String userNameSuffix = generateUniqueString(numOfCharsToAppend);

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

  private String generateUniqueString(int length) {
    int totalChars = alphabet.length;
    BigDecimal exponent = BigDecimal.valueOf(totalChars);
    exponent = exponent.pow(length);
    String code = "";
    BigDecimal number = new BigDecimal(rand.nextInt(1000000));
    BigDecimal num = number.multiply(largePrimeNumber).remainder(exponent);
    code = baseN(num, totalChars);
    int codeLenght = code.length();
    if (codeLenght < length) {
      for (int i = codeLenght; i < length; i++) {
        code = code + alphabet[rand.nextInt(totalChars - 1)];
      }
    }
    if (NumberUtils.isNumber(code.substring(1, 2)) || NumberUtils.isNumber(code.substring(2, 3))) {
      return code;
    } else {
      code = code.substring(0, 1) + alphabet[rand.nextInt(9)] + code.substring(2);
      return code;
    }
  }

  private String baseN(BigDecimal num, int base) {
    if (num.doubleValue() == 0) {
      return "0";
    }
    double div = Math.floor(num.doubleValue() / base);
    String val = baseN(new BigDecimal(div), base);
    return StringUtils.stripStart(val, stripChars)
        + alphabet[num.remainder(new BigDecimal(base)).intValue()];
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Map<String, Object>> esSearchUserByFilters(
      Map<String, Object> filters, RequestContext context) {
    SearchDTO searchDTO = new SearchDTO();

    List<String> list = new ArrayList<>();
    list.add(JsonKey.ID);
    list.add(JsonKey.USERNAME);

    searchDTO.setFields(list);
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);

    Future<Map<String, Object>> esResultF =
        esUtil.search(searchDTO, EsType.user.getTypeName(), context);
    Map<String, Object> esResult =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResultF);

    return (List<Map<String, Object>>) esResult.get(JsonKey.CONTENT);
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
              AdminUtilHandler.prepareAdminUtilPayload(managedUsers));
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
    List<AdminUtilRequestData> reqData =
        respList
            .stream()
            .map(p -> new AdminUtilRequestData(parentId, (String) p.get(JsonKey.ID)))
            .collect(Collectors.toList());
    reqData.forEach(System.out::println);
    return reqData;
  }
}
