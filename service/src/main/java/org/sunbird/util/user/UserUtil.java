package org.sunbird.util.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.sf.junidecode.Junidecode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.user.validator.UserRequestValidator;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserExternalIdentityService;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserExternalIdentityServiceImpl;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.sso.SSOManager;
import org.sunbird.sso.SSOServiceFactory;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.UserUtility;
import org.sunbird.util.contentstore.ContentStoreUtil;
import scala.concurrent.Future;

public class UserUtil {
  private static final LoggerUtil logger = new LoggerUtil(UserUtil.class);
  private static final EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final SSOManager ssoManager = SSOServiceFactory.getInstance();
  private static final PropertiesCache propertiesCache = PropertiesCache.getInstance();
  private static final DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance();
  private static final DecryptionService decService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
  private static final UserService userService = UserServiceImpl.getInstance();
  private static final UserExternalIdentityService userExternalIdentityService =
      new UserExternalIdentityServiceImpl();
  private static final UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  private static final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
  private static final OrgService orgService = OrgServiceImpl.getInstance();

  private UserUtil() {}

  // Update channel info with orgId info
  public static void updateExternalIdsProviderWithOrgId(
      Map<String, Object> userMap, RequestContext context) {
    if (MapUtils.isNotEmpty(userMap)) {
      Set<String> providerSet = new HashSet<>();
      List<Map<String, String>> extList =
          (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS);
      if (CollectionUtils.isNotEmpty(extList)) {
        for (Map<String, String> extId : extList) {
          providerSet.add(extId.get(JsonKey.PROVIDER));
        }
      }
      Map<String, String> orgProviderMap;
      if (CollectionUtils.isNotEmpty(providerSet) && providerSet.size() == 1) {
        String channel = providerSet.stream().findFirst().orElse("");
        orgProviderMap = new HashMap<>();
        if (channel.equalsIgnoreCase((String) userMap.get(JsonKey.CHANNEL))) {
          orgProviderMap.put(channel, (String) userMap.get(JsonKey.ROOT_ORG_ID));
        } else {
          orgProviderMap = fetchOrgIdByProvider(new ArrayList<>(providerSet), context);
        }
      } else {
        orgProviderMap = fetchOrgIdByProvider(new ArrayList<>(providerSet), context);
      }
      if (CollectionUtils.isNotEmpty(
          (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS))) {
        for (Map<String, String> externalId :
            (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS)) {

          String orgId =
              getCaseInsensitiveOrgFromProvider(externalId.get(JsonKey.PROVIDER), orgProviderMap);
          if (StringUtils.isBlank(orgId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.invalidParameterValue,
                MessageFormat.format(
                    ResponseCode.invalidParameterValue.getErrorMessage(),
                    JsonKey.PROVIDER,
                    externalId.get(JsonKey.PROVIDER)));
          }
          if (null != externalId.get(JsonKey.PROVIDER)
              && null != externalId.get(JsonKey.ID_TYPE)
              && externalId.get(JsonKey.PROVIDER).equals(externalId.get(JsonKey.ID_TYPE))) {
            externalId.put(JsonKey.ID_TYPE, orgId);
          }
          externalId.put(JsonKey.PROVIDER, orgId);
        }
      }
    }
  }

  public static String getCaseInsensitiveOrgFromProvider(
      String provider, Map<String, String> providerOrgMap) {
    // In some cases channel is provided in smaller case
    String orgId = providerOrgMap.get(provider);
    if (null == orgId && StringUtils.isNotBlank(provider)) {
      Map<String, String> providerOrgCaseInsensitiveMap =
          new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      providerOrgCaseInsensitiveMap.putAll(providerOrgMap);
      logger.info(String.format("Checking channel: %s as with any case", provider));
      orgId = providerOrgCaseInsensitiveMap.get(provider);
    }
    return orgId;
  }

  public static Map<String, Object> validateExternalIdsAndReturnActiveUser(
      Map<String, Object> userMap, RequestContext context) {
    String extId = (String) userMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
    String idType = (String) userMap.get(JsonKey.EXTERNAL_ID_TYPE);
    Map<String, Object> user = null;
    if ((StringUtils.isBlank((String) userMap.get(JsonKey.USER_ID))
            && StringUtils.isBlank((String) userMap.get(JsonKey.ID)))
        && StringUtils.isNotEmpty(extId)
        && StringUtils.isNotEmpty(provider)
        && StringUtils.isNotEmpty(idType)) {
      user = getUserFromExternalId(userMap, context);
      if (MapUtils.isEmpty(user)) {
        throw new ProjectCommonException(
            ResponseCode.resourceNotFound,
            ProjectUtil.formatMessage(
                ResponseMessage.Message.EXTERNALID_NOT_FOUND, extId, idType, provider),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
      }
    } else if (StringUtils.isNotBlank((String) userMap.get(JsonKey.USER_ID))
        || StringUtils.isNotBlank((String) userMap.get(JsonKey.ID))) {
      String userId =
          (StringUtils.isNotBlank((String) userMap.get(JsonKey.USER_ID)))
              ? ((String) userMap.get(JsonKey.USER_ID))
              : ((String) userMap.get(JsonKey.ID));
      user = userService.getUserDetailsForES(userId, context);
      if (MapUtils.isEmpty(user)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.resourceNotFound, null);
      }
    }
    if (MapUtils.isNotEmpty(user)) {
      if (null != user.get(JsonKey.IS_DELETED) && (boolean) (user.get(JsonKey.IS_DELETED))) {
        ProjectCommonException.throwClientErrorException(ResponseCode.inactiveUser, null);
      }
      // In request if userId or id not present (when you are sending only externalIds to verify the
      // user)
      if (StringUtils.isBlank((String) userMap.get(JsonKey.USER_ID))) {
        userMap.put(JsonKey.USER_ID, user.get(JsonKey.ID));
      }
      if (StringUtils.isBlank((String) userMap.get(JsonKey.ID))) {
        userMap.put(JsonKey.ID, user.get(JsonKey.ID));
      }
    }
    return user;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getUserFromExternalId(
      Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> user = null;
    String userId = getUserIdFromExternalId(userMap, context);
    if (!StringUtils.isEmpty(userId)) {
      user = userService.getUserDetailsForES(userId, context);
    }
    return user;
  }

  public static String getUserIdFromExternalId(
      Map<String, Object> userMap, RequestContext context) {

    String extId = (String) userMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
    String idType = (String) userMap.get(JsonKey.EXTERNAL_ID_TYPE);
    return userExternalIdentityService.getUserV2(extId, provider, idType, context);
  }

  public static String getUserId(Map<String, Object> userMap, RequestContext context) {
    String userId;
    if (null != userMap.get(JsonKey.USER_ID)) {
      userId = (String) userMap.get(JsonKey.USER_ID);
    } else {
      userId = (String) userMap.get(JsonKey.ID);
    }
    if (StringUtils.isBlank(userId)) {
      String extId = (String) userMap.get(JsonKey.EXTERNAL_ID);
      String provider = (String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
      String idType = (String) userMap.get(JsonKey.EXTERNAL_ID_TYPE);
      if (StringUtils.isNotBlank(provider)
          && StringUtils.isNotBlank(extId)
          && StringUtils.isNotBlank(idType)) {
        userId = userExternalIdentityService.getUserV1(extId, provider, idType, context);
      }
    }
    return userId;
  }

  public static void validateUserPhoneAndEmailUniqueness(
      User user, String operationType, RequestContext context) {
    userLookupService.checkPhoneUniqueness(user, operationType, context);
    userLookupService.checkEmailUniqueness(user, operationType, context);
  }

  public static String getDecryptedData(String value, RequestContext context) {
    try {
      return decService.decryptData(value, context);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.serverError,
          ResponseCode.serverError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  public static List<Map<String, String>> copyAndConvertExternalIdsToLower(
      List<Map<String, String>> externalIds) {
    List<Map<String, String>> list = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      storeOriginalExternalIdsValue(externalIds);
      list = convertExternalIdsValueToLowerCase(externalIds);
    }
    return list;
  }

  public static void storeOriginalExternalIdsValue(List<Map<String, String>> externalIds) {
    externalIds.forEach(
        externalIdMap -> {
          externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, externalIdMap.get(JsonKey.ID));
          externalIdMap.put(JsonKey.ORIGINAL_PROVIDER, externalIdMap.get(JsonKey.PROVIDER));
          externalIdMap.put(JsonKey.ORIGINAL_ID_TYPE, externalIdMap.get(JsonKey.ID_TYPE));
        });
  }

  /**
   * Till 3.1.0 there will be only 1 provider will be active for a user in self declaration and user
   * can migrate only to 1 state. Note: Need fix .If a user can be migrated to multiple states in
   * future
   *
   * @param dbResExternalIds
   */
  public static void updateExternalIdsWithProvider(
      List<Map<String, String>> dbResExternalIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(dbResExternalIds)) {
      String orgId = dbResExternalIds.get(0).get(JsonKey.PROVIDER);
      String provider = fetchProviderByOrgId(orgId, context);
      dbResExternalIds
          .stream()
          .forEach(
              s -> {
                if (s.get(JsonKey.PROVIDER) != null
                    && s.get(JsonKey.PROVIDER).equals(s.get(JsonKey.ID_TYPE))) {
                  s.put(JsonKey.ID_TYPE, provider);
                }
                s.put(JsonKey.PROVIDER, provider);
              });
    }
  }

  public static List<Map<String, String>> convertExternalIdsValueToLowerCase(
      List<Map<String, String>> externalIds) {
    ConvertValuesToLowerCase mapper =
        s -> {
          s.put(JsonKey.ID, s.get(JsonKey.ID).toLowerCase());
          s.put(JsonKey.PROVIDER, s.get(JsonKey.PROVIDER).toLowerCase());
          s.put(JsonKey.ID_TYPE, s.get(JsonKey.ID_TYPE).toLowerCase());
          return s;
        };
    return externalIds.stream().map(s -> mapper.convertToLowerCase(s)).collect(Collectors.toList());
  }

  public static boolean updatePassword(Map<String, Object> userMap, RequestContext context) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PASSWORD))) {
      return ssoManager.updatePassword(
          (String) userMap.get(JsonKey.ID), (String) userMap.get(JsonKey.PASSWORD), context);
    }
    return true;
  }

  public static void addMaskEmailAndMaskPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.MASKED_PHONE, maskingService.maskPhone(phone));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.MASKED_EMAIL, maskingService.maskEmail(email));
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> encryptUserData(Map<String, Object> userMap) {
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception e1) {
      logger.error(e1.getMessage(), e1);
      throw new ProjectCommonException(
          ResponseCode.serverError,
          ResponseCode.serverError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    Map<String, Object> requestMap = new HashMap<>();
    User user = mapper.convertValue(userMap, User.class);
    requestMap.putAll(mapper.convertValue(user, Map.class));
    return requestMap;
  }

  public static String transliterateUserName(String userName) {
    String translatedUserName = Junidecode.unidecode(userName);
    return translatedUserName;
  }

  public static void setUserDefaultValue(Map<String, Object> userMap, RequestContext context) {
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
      userMap.put(
          JsonKey.COUNTRY_CODE, propertiesCache.getProperty(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE));
    }
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    userMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());

    if (StringUtils.isBlank((String) userMap.get(JsonKey.USERNAME))) {
      String firstName = (String) userMap.get(JsonKey.FIRST_NAME);
      String lastName = (String) userMap.get(JsonKey.LAST_NAME);

      String name = String.join(" ", firstName, StringUtils.isNotBlank(lastName) ? lastName : "");

      String userName = null;
      while (StringUtils.isBlank(userName)) {
        userName = getUsername(name, context);
        if (StringUtils.isNotBlank(userName)) {
          userMap.put(JsonKey.USERNAME, transliterateUserName(userName));
        }
      }
    } else {
      userMap.put(JsonKey.USERNAME, transliterateUserName((String) userMap.get(JsonKey.USERNAME)));
      if (!userLookupService.checkUsernameUniqueness(
          (String) userMap.get(JsonKey.USERNAME), false, context)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorParamExists,
            MessageFormat.format(
                ResponseCode.errorParamExists.getErrorMessage(), JsonKey.USERNAME));
      }
    }
  }

  private static String getUsername(String name, RequestContext context) {
    List<Map<String, Object>> users = null;
    List<String> encryptedUserNameList = new ArrayList<>();
    List<String> excludedUsernames = new ArrayList<>();
    List<String> userNameList = new ArrayList<>();

    String userName = "";
    for (int j = 1; j <= 10; j++) {
      logger.info(context, "Generating list of 10 userNames in loop for iteration " + j);
      encryptedUserNameList.clear();
      excludedUsernames.addAll(userNameList);

      // Generate usernames
      userNameList = userService.generateUsernames(name, excludedUsernames, context);

      // Encrypt each user name
      userService
          .getEncryptedList(userNameList, context)
          .stream()
          .forEach(value -> encryptedUserNameList.add(value));

      // Throw an error in case of encryption failures
      if (encryptedUserNameList.isEmpty()) {
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }

      // Search if any user names
      List<String> filtersEncryptedUserNameList = new ArrayList<>(encryptedUserNameList);
      users = userService.searchUserNameInUserLookup(filtersEncryptedUserNameList, context);

      if (!(CollectionUtils.isNotEmpty(users) && users.size() >= encryptedUserNameList.size())) {
        break;
      }
    }

    List<String> esUserNameList = new ArrayList<>();

    // Map list of results (from User lookup) into list of usernames
    users
        .stream()
        .forEach(
            user -> {
              esUserNameList.add((String) user.get(JsonKey.VALUE));
            });

    // Query cassandra to find first username that is not yet assigned
    Optional<String> result =
        encryptedUserNameList
            .stream()
            .filter(
                value -> {
                  if (!esUserNameList.contains(value)) {
                    return userLookupService.checkUsernameUniqueness(value, true, context);
                  }
                  return false;
                })
            .findFirst();

    if (result.isPresent()) {
      userName = result.get();
    }

    if (StringUtils.isNotBlank(userName)) {
      return decService.decryptData(userName, context);
    }

    return "";
  }
  // validateExternalIds For CREATE USER and MIGRATE USER
  public static void validateExternalIds(User user, String operationType, RequestContext context) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      List<Map<String, String>> list = copyAndConvertExternalIdsToLower(user.getExternalIds());
      user.setExternalIds(list);
    }
    userLookupService.checkExternalIdUniqueness(user, operationType, context);
  }
  // validateExternalIds For UPDATE USER
  public static void validateExternalIdsForUpdateUser(
      User user, boolean isCustodianOrg, RequestContext context) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      List<Map<String, String>> list = copyAndConvertExternalIdsToLower(user.getExternalIds());
      user.setExternalIds(list);
    }
    // If operation is update and user is custodian org, ignore uniqueness check
    if (!isCustodianOrg) {
      userLookupService.checkExternalIdUniqueness(user, JsonKey.UPDATE, context);
    }
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      validateUserExternalIds(user, context);
    }
  }

  /**
   * This method will check the diff b/w uer request and user db record email & phone are same or
   * not if its not same then will return true to delete entry from userLookup table
   *
   * @param userRequestMap
   * @param userDbRecord
   * @param emailOrPhone
   * @return
   */
  public static boolean isEmailOrPhoneDiff(
      Map<String, Object> userRequestMap, Map<String, Object> userDbRecord, String emailOrPhone) {
    String encEmailOrPhone = (String) userRequestMap.get(emailOrPhone);
    String dbEmailOrPhone = (String) userDbRecord.get(emailOrPhone);
    if (StringUtils.isNotBlank(encEmailOrPhone) && StringUtils.isNotBlank(dbEmailOrPhone)) {
      return (!((encEmailOrPhone).equalsIgnoreCase(dbEmailOrPhone)));
    }
    return false;
  }

  private static Optional<Map<String, String>> checkExternalID(
      List<Map<String, String>> dbResExternalIds, Map<String, String> extIdMap) {
    Optional<Map<String, String>> extMap =
        dbResExternalIds
            .stream()
            .filter(
                s -> {
                  if (((s.get(JsonKey.ID_TYPE)).equalsIgnoreCase(extIdMap.get(JsonKey.ID_TYPE)))
                      && ((s.get(JsonKey.PROVIDER))
                          .equalsIgnoreCase(extIdMap.get(JsonKey.PROVIDER)))) {
                    return true;
                  } else {
                    return false;
                  }
                })
            .findFirst();
    return extMap;
  }

  public static void validateUserExternalIds(User user, RequestContext context) {
    List<Map<String, String>> dbResExternalIds = getExternalIds(user.getUserId(), true, context);
    List<Map<String, String>> externalIds = user.getExternalIds();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      for (Map<String, String> extIdMap : externalIds) {
        Optional<Map<String, String>> extMap = checkExternalID(dbResExternalIds, extIdMap);
        Map<String, String> map = extMap.orElse(null);
        // Allowed operation type for externalIds ("add", "remove", "edit")
        if (!(JsonKey.ADD.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))
                || StringUtils.isBlank(extIdMap.get(JsonKey.OPERATION)))
            && MapUtils.isEmpty(map)) {
          // operation is either edit or remove
          throwExternalIDNotFoundException(
              extIdMap.get(JsonKey.ID),
              extIdMap.get(JsonKey.ID_TYPE),
              extIdMap.get(JsonKey.PROVIDER));
        }
      }
    }
  }

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.resourceNotFound,
        ProjectUtil.formatMessage(
            ResponseMessage.Message.EXTERNALID_NOT_FOUND, externalId, idType, provider),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }

  public static List<Map<String, String>> getExternalIds(
      String userId, boolean mergeDeclaration, RequestContext context) {
    List<Map<String, String>> dbResExternalIds =
        userExternalIdentityService.getUserExternalIds(userId, context);
    if (mergeDeclaration) {
      List<Map<String, String>> dbSelfDeclaredExternalIds =
          userExternalIdentityService.getSelfDeclaredDetails(userId, context);
      if (CollectionUtils.isNotEmpty(dbSelfDeclaredExternalIds)) {
        dbResExternalIds.addAll(dbSelfDeclaredExternalIds);
      }
    }
    return dbResExternalIds;
  }

  public static List<Map<String, Object>> getActiveUserOrgDetails(
      String userId, RequestContext context) {
    return getUserOrgDetails(false, userId, context);
  }

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> getUserOrgDetails(
      boolean isDeleted, String userId, RequestContext context) {
    List<Map<String, Object>> userOrgList = new ArrayList<>();
    List<Map<String, Object>> organisations = new ArrayList<>();
    try {
      List<Map<String, Object>> responseList =
          userOrgService.getUserOrgListByUserId(userId, context);
      if (CollectionUtils.isNotEmpty(responseList)) {
        if (!isDeleted) {
          responseList
              .stream()
              .forEach(
                  (dataMap) -> {
                    if (null != dataMap.get(JsonKey.IS_DELETED)
                        && !((boolean) dataMap.get(JsonKey.IS_DELETED))) {
                      userOrgList.add(dataMap);
                    }
                  });
        } else {
          userOrgList.addAll(responseList);
        }
        for (Map<String, Object> tempMap : userOrgList) {
          organisations.add(tempMap);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return organisations;
  }

  public static void toLower(Map<String, Object> userMap) {
    Arrays.asList(
            ProjectUtil.getConfigValue(JsonKey.SUNBIRD_API_REQUEST_LOWER_CASE_FIELDS).split(","))
        .stream()
        .forEach(
            field -> {
              if (StringUtils.isNotBlank((String) userMap.get(field))) {
                userMap.put(field, ((String) userMap.get(field)).toLowerCase());
              }
            });
  }

  public static Map<String, Object> validateManagedByUser(
      String managedBy, RequestContext context) {
    Map<String, Object> managedByInfo = userService.getUserDetailsById(managedBy, context);
    if (MapUtils.isEmpty(managedByInfo)
        || StringUtils.isBlank((String) managedByInfo.get(JsonKey.FIRST_NAME))
        || StringUtils.isNotBlank((String) managedByInfo.get(JsonKey.MANAGED_BY))
        || (null != managedByInfo.get(JsonKey.IS_DELETED)
            && (boolean) (managedByInfo.get(JsonKey.IS_DELETED)))) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(ResponseCode.resourceNotFound.getErrorMessage(), JsonKey.USER),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    return managedByInfo;
  }

  public static void validateManagedUserLimit(String managedBy, RequestContext context) {
    if (Boolean.valueOf(ProjectUtil.getConfigValue(JsonKey.LIMIT_MANAGED_USER_CREATION))) {
      Map<String, Object> searchQueryMap = new HashMap<>();
      searchQueryMap.put(JsonKey.MANAGED_BY, managedBy);
      Map<String, Object> searchRequestMap = new HashMap<>();
      searchRequestMap.put(JsonKey.FILTERS, searchQueryMap);
      SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(searchRequestMap);
      Map<String, Object> searchResult = userService.searchUser(searchDTO, context);
      List<Map<String, Object>> managedUserList =
          (List<Map<String, Object>>) searchResult.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(managedUserList)
          && managedUserList.size()
              >= Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.MANAGED_USER_LIMIT))) {
        throw new ProjectCommonException(
            ResponseCode.managedUserLimitExceeded,
            ResponseCode.managedUserLimitExceeded.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
  }

  public static List<UserDeclareEntity> transformExternalIdsToSelfDeclaredRequest(
      List<Map<String, String>> externalIds, Map<String, Object> requestMap) {
    List<UserDeclareEntity> selfDeclaredFieldsList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(externalIds)) {
      String currOrgId =
          updateAddOrEditDeclaredFieldsAndGetOrg(externalIds, selfDeclaredFieldsList, requestMap);
      UserDeclareEntity removeDeclareFields =
          getRemoveDeclaredFields(externalIds, currOrgId, requestMap);

      if (null != removeDeclareFields) {
        selfDeclaredFieldsList.add(removeDeclareFields);
      }
    }
    return selfDeclaredFieldsList;
  }

  private static UserDeclareEntity getRemoveDeclaredFields(
      List<Map<String, String>> externalIds, String currOrgId, Map<String, Object> requestMap) {
    UserDeclareEntity userDeclareEntity = null;
    String prevOrgId = "";
    Map<String, Object> userInfo = new HashMap<>();
    for (Map<String, String> extIdMap : externalIds) {
      if (JsonKey.REMOVE.equals(extIdMap.get(JsonKey.OPERATION)) && !prevOrgId.equals(currOrgId)) {
        prevOrgId = extIdMap.get(JsonKey.ORIGINAL_PROVIDER);
        String idType = extIdMap.get(JsonKey.ORIGINAL_ID_TYPE);
        String value = extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
        userInfo.put(idType, value);
      }
    }
    if (MapUtils.isNotEmpty(userInfo)) {
      userDeclareEntity =
          new UserDeclareEntity(
              (String) requestMap.get(JsonKey.USER_ID),
              prevOrgId,
              JsonKey.DEFAULT_PERSONA,
              userInfo);
      userDeclareEntity.setUpdatedBy((String) requestMap.get(JsonKey.UPDATED_BY));
      userDeclareEntity.setOperation(JsonKey.REMOVE);
    }
    return userDeclareEntity;
  }

  private static String updateAddOrEditDeclaredFieldsAndGetOrg(
      List<Map<String, String>> externalIds,
      List<UserDeclareEntity> userDeclareEntities,
      Map<String, Object> requestMap) {
    String currOrgId = "";
    Map<String, Object> userInfo = new HashMap<>();
    for (Map<String, String> extIdMap : externalIds) {
      if (JsonKey.ADD.equals(extIdMap.get(JsonKey.OPERATION))
          || JsonKey.EDIT.equals(extIdMap.get(JsonKey.OPERATION))) {
        String idType = extIdMap.get(JsonKey.ORIGINAL_ID_TYPE);
        String value = extIdMap.get(JsonKey.ORIGINAL_EXTERNAL_ID);
        currOrgId = extIdMap.get(JsonKey.ORIGINAL_PROVIDER);
        userInfo.put(idType, value);
      }
    }
    if (MapUtils.isNotEmpty(userInfo)) {
      UserDeclareEntity userDeclareEntity =
          new UserDeclareEntity(
              (String) requestMap.get(JsonKey.USER_ID),
              currOrgId,
              JsonKey.DEFAULT_PERSONA,
              userInfo);
      userDeclareEntity.setCreatedBy((String) requestMap.get(JsonKey.CREATED_BY));
      userDeclareEntity.setUpdatedBy((String) requestMap.get(JsonKey.UPDATED_BY));
      userDeclareEntity.setOperation(JsonKey.ADD);
      userDeclareEntity.setStatus(JsonKey.SUBMITTED);
      userDeclareEntities.add(userDeclareEntity);
    }
    return currOrgId;
  }

  public static String fetchProviderByOrgId(String orgId, RequestContext context) {
    try {
      if (StringUtils.isNotBlank(orgId)) {
        Map<String, Object> org = orgService.getOrgById(orgId, context);
        if (null != org && !org.isEmpty()) {
          return (String) org.get(JsonKey.CHANNEL);
        }
      }
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }
    return "";
  }

  public static Map<String, String> fetchOrgIdByProvider(
      List<String> providers, RequestContext context) {
    Map<String, String> providerOrgMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(providers)) {
      try {
        Map<String, Object> searchQueryMap = new HashMap<>();
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.IS_TENANT, true);
        filters.put(JsonKey.CHANNEL, providers);
        searchQueryMap.put(JsonKey.FILTERS, filters);
        SearchDTO searchDTO = ElasticSearchHelper.createSearchDTO(searchQueryMap);
        Future<Map<String, Object>> esOrgResF = orgService.searchOrg(searchDTO, context);
        Map<String, Object> esResOrg =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esOrgResF);
        if (MapUtils.isNotEmpty(esResOrg)) {
          List<Map<String, Object>> orgList =
              (List<Map<String, Object>>) esResOrg.get(JsonKey.CONTENT);
          if (CollectionUtils.isNotEmpty(orgList)) {
            for (Map<String, Object> org : orgList) {
              providerOrgMap.put((String) org.get(JsonKey.CHANNEL), (String) org.get(JsonKey.ID));
            }
          }
        }
      } catch (Exception ex) {
        logger.error(context, ex.getMessage(), ex);
      }
    }
    return providerOrgMap;
  }

  public static void encryptDeclarationFields(
      List<Map<String, Object>> declarations,
      Map<String, Object> userDbRecords,
      RequestContext context) {
    for (Map<String, Object> declareFields : declarations) {
      Map<String, Object> userInfoMap = (Map<String, Object>) declareFields.get(JsonKey.INFO);
      if (MapUtils.isNotEmpty(userInfoMap)) {
        for (Map.Entry<String, Object> userInfo : userInfoMap.entrySet()) {
          String key = userInfo.getKey();
          String value = (String) userInfo.getValue();
          if (JsonKey.DECLARED_EMAIL.equals(key) || JsonKey.DECLARED_PHONE.equals(key)) {
            if (UserUtility.isMasked(value)) {
              if (JsonKey.DECLARED_EMAIL.equals(key)) {
                userInfoMap.put(key, userDbRecords.get(JsonKey.EMAIL));
              } else {
                userInfoMap.put(key, userDbRecords.get(JsonKey.PHONE));
              }
            } else {
              userInfoMap.put(key, encryptionService.encryptData(value, context));
            }
          }
        }
      }
    }
  }

  public static UserDeclareEntity createUserDeclaredObject(
      Map<String, Object> declareFieldMap, String callerId) {
    UserDeclareEntity userDeclareEntity =
        new UserDeclareEntity(
            (String) declareFieldMap.get(JsonKey.USER_ID),
            (String) declareFieldMap.get(JsonKey.ORG_ID),
            (String) declareFieldMap.get(JsonKey.PERSONA),
            (Map<String, Object>) declareFieldMap.get(JsonKey.INFO));

    if (StringUtils.isBlank((String) declareFieldMap.get(JsonKey.OPERATION))) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidOperationName);
    }
    userDeclareEntity.setOperation((String) declareFieldMap.get(JsonKey.OPERATION));
    if (JsonKey.ADD.equals(userDeclareEntity.getOperation())) {
      userDeclareEntity.setCreatedBy(callerId);
    } else {
      userDeclareEntity.setUpdatedBy(callerId);
      userDeclareEntity.setStatus((String) declareFieldMap.get(JsonKey.STATUS));
    }
    if (StringUtils.isBlank((String) declareFieldMap.get(JsonKey.STATUS))) {
      userDeclareEntity.setStatus(JsonKey.SUBMITTED);
    }
    userDeclareEntity.setErrorType((String) declareFieldMap.get(JsonKey.ERR_TYPE));

    return userDeclareEntity;
  }

  public static void removeEntryFromUserLookUp(
      Map<String, Object> userDbMap, List<String> identifiers, RequestContext context) {
    logger.info(
        context,
        "UserUtil:removeEntryFromUserLookUp remove following identifiers from lookUp table "
            + identifiers);
    List<Map<String, String>> reqMap = new ArrayList<>();
    Map<String, String> deleteLookUp = new HashMap<>();
    if (identifiers.contains(JsonKey.EMAIL)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.EMAIL))) {
      deleteLookUp.put(JsonKey.TYPE, JsonKey.EMAIL);
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.EMAIL));
      reqMap.add(deleteLookUp);
    }
    if (identifiers.contains(JsonKey.PHONE)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.PHONE))) {
      deleteLookUp = new HashMap<>();
      deleteLookUp.put(JsonKey.TYPE, JsonKey.PHONE);
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.PHONE));
      reqMap.add(deleteLookUp);
    }
    if (identifiers.contains(JsonKey.USERNAME)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.USERNAME))) {
      deleteLookUp = new HashMap<>();
      deleteLookUp.put(JsonKey.TYPE, JsonKey.USERNAME.toLowerCase());
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.USERNAME));
      logger.info(
          context,
          "UserUtil:removeEntryFromUserLookUp before transliterating username: "
              + (String) userDbMap.get(JsonKey.USERNAME));
      reqMap.add(deleteLookUp);
    }

    if (identifiers.contains(JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID)
        && StringUtils.isNotBlank((String) userDbMap.get(JsonKey.EXTERNAL_ID))) {
      deleteLookUp = new HashMap<>();
      deleteLookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
      deleteLookUp.put(JsonKey.VALUE, (String) userDbMap.get(JsonKey.EXTERNAL_ID));
      reqMap.add(deleteLookUp);
    }

    if (CollectionUtils.isNotEmpty(reqMap)) {
      userLookupService.deleteRecords(reqMap, context);
    }
  }

  public static void validateUserFrameworkData(
      Map<String, Object> userRequestMap,
      Map<String, Object> userDbRecord,
      RequestContext context) {
    UserRequestValidator userRequestValidator = new UserRequestValidator();
    if (userRequestMap.containsKey(JsonKey.FRAMEWORK)) {
      Map<String, Object> framework = (Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK);
      List<String> frameworkIdList;
      if (framework.get(JsonKey.ID) instanceof String) {
        String frameworkIdString = (String) framework.remove(JsonKey.ID);
        frameworkIdList = new ArrayList<>();
        frameworkIdList.add(frameworkIdString);
        framework.put(JsonKey.ID, frameworkIdList);
      } else {
        frameworkIdList = (List<String>) framework.get(JsonKey.ID);
      }

      for(Map.Entry<String, Object> entry: framework.entrySet()){
        String key = entry.getKey();
        Object value = entry.getValue();
        if(!JsonKey.ID.equals(key) && value instanceof String) {
          framework.put(key, Arrays.asList((String) value));
        }
      }
      
      userRequestMap.put(JsonKey.FRAMEWORK, framework);
      Map<String, List<Map<String, String>>> frameworkCachedValue = getFrameworkDetails(frameworkIdList.get(0), context);

      List<String> frameworkFields = new ArrayList<>(frameworkCachedValue.keySet());
      frameworkFields.add(JsonKey.ID);
      List<String> frameworkMandatoryFields = new ArrayList<>();
      userRequestValidator.validateMandatoryFrameworkFields(userRequestMap, frameworkFields, frameworkMandatoryFields);
      Map<String, Object> rootOrgMap =
          orgService.getOrgById((String) userDbRecord.get(JsonKey.ROOT_ORG_ID), context);
      String hashTagId = (String) rootOrgMap.get(JsonKey.HASHTAGID);
      verifyFrameworkId(hashTagId, frameworkIdList, context);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK)).remove(JsonKey.ID);
      userRequestValidator.validateFrameworkCategoryValues(userRequestMap, frameworkCachedValue);
      ((Map<String, Object>) userRequestMap.get(JsonKey.FRAMEWORK))
          .put(JsonKey.ID, frameworkIdList);
    }
  }

  private static void verifyFrameworkId(
      String hashTagId, List<String> frameworkIdList, RequestContext context) {
    List<String> frameworks = DataCacheHandler.getHashtagIdFrameworkIdMap().get(hashTagId);
    String frameworkId = frameworkIdList.get(0);
    if (frameworks != null && frameworks.contains(frameworkId)) {
      return;
    } else {
      Map<String, List<Map<String, String>>> frameworkDetails =
          getFrameworkDetails(frameworkId, context);
      if (frameworkDetails == null)
        throw new ProjectCommonException(
            ResponseCode.errorNoFrameworkFound,
            ResponseCode.errorNoFrameworkFound.getErrorMessage(),
            ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
  }

  private static Map<String, List<Map<String, String>>> getFrameworkDetails(
      String frameworkId, RequestContext context) {
    if (DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId) == null) {
      handleGetFrameworkDetails(frameworkId, context);
    }
    return DataCacheHandler.getFrameworkCategoriesMap().get(frameworkId);
  }

  private static void handleGetFrameworkDetails(String frameworkId, RequestContext context) {
    Map<String, Object> response = ContentStoreUtil.readFramework(frameworkId, context);
    Map<String, List<Map<String, String>>> frameworkCacheMap = new HashMap<>();
    //List<String> supportedfFields = DataCacheHandler.getFrameworkFieldsConfig().get(JsonKey.FIELDS);
    Map<String, Object> result = (Map<String, Object>) response.get(JsonKey.RESULT);
    if (MapUtils.isNotEmpty(result)) {
      Map<String, Object> frameworkDetails = (Map<String, Object>) result.get(JsonKey.FRAMEWORK);
      if (MapUtils.isNotEmpty(frameworkDetails)) {
        List<Map<String, Object>> frameworkCategories =
            (List<Map<String, Object>>) frameworkDetails.get(JsonKey.CATEGORIES);
        if (CollectionUtils.isNotEmpty(frameworkCategories)) {
          for (Map<String, Object> frameworkCategoriesValue : frameworkCategories) {
            String frameworkField = (String) frameworkCategoriesValue.get(JsonKey.CODE);
            List<Map<String, String>> listOfFields = new ArrayList<>();
            List<Map<String, Object>> frameworkTermList =
                  (List<Map<String, Object>>) frameworkCategoriesValue.get(JsonKey.TERMS);
            if (CollectionUtils.isNotEmpty(frameworkTermList)) {
              for (Map<String, Object> frameworkTerm : frameworkTermList) {
                String id = (String) frameworkTerm.get(JsonKey.IDENTIFIER);
                String name = (String) frameworkTerm.get(JsonKey.NAME);
                Map<String, String> writtenValue = new HashMap<>();
                writtenValue.put(JsonKey.ID, id);
                writtenValue.put(JsonKey.NAME, name);
                listOfFields.add(writtenValue);
              }
            }
            if (StringUtils.isNotBlank(frameworkField)
                && CollectionUtils.isNotEmpty(listOfFields))
              frameworkCacheMap.put(frameworkField, listOfFields);
            if (MapUtils.isNotEmpty(frameworkCacheMap))
              DataCacheHandler.updateFrameworkCategoriesMap(frameworkId, frameworkCacheMap);
          }
        }
      }
    }
  }
}

@FunctionalInterface
interface ConvertValuesToLowerCase {
  Map<String, String> convertToLowerCase(Map<String, String> map);
}
