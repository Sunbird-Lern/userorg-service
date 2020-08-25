package org.sunbird.user.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import net.sf.junidecode.Junidecode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.common.services.ProfileCompletenessService;
import org.sunbird.common.services.impl.ProfileCompletenessFactory;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.SocialMediaType;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.learner.util.Util.DbInfo;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.user.dao.UserExternalIdentityDao;
import org.sunbird.user.dao.impl.UserExternalIdentityDaoImpl;
import org.sunbird.user.service.UserExternalIdentityService;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserExternalIdentityServiceImpl;
import org.sunbird.user.service.impl.UserServiceImpl;
import scala.concurrent.Future;

public class UserUtil {

  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private static DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);
  private static ObjectMapper mapper = new ObjectMapper();
  private static SSOManager ssoManager = SSOServiceFactory.getInstance();
  private static PropertiesCache propertiesCache = PropertiesCache.getInstance();
  private static DataMaskingService maskingService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
          null);
  private static DecryptionService decService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private static UserService userService = UserServiceImpl.getInstance();
  private static UserExternalIdentityDao userExternalIdentityDao =
      new UserExternalIdentityDaoImpl();
  private static ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private static UserExternalIdentityService userExternalIdentityService =
      new UserExternalIdentityServiceImpl();
  static Random rand = new Random(System.nanoTime());
  private static final String[] alphabet =
      new String[] {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "g", "h", "i",
        "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
      };

  private static String stripChars = "0";
  private static BigDecimal largePrimeNumber = new BigDecimal(1679979167);

  private UserUtil() {}

  @SuppressWarnings("unchecked")
  public static void checkPhoneUniqueness(User user, String opType) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
    if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
      String phone = user.getPhone();
      if (StringUtils.isNotBlank(phone)) {
        try {
          phone = encryptionService.encryptData(phone);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting phone number ", e);
        }
        Response result =
            cassandraOperation.getRecordsByIndexedProperty(
                userDb.getKeySpace(), userDb.getTableName(), (JsonKey.PHONE), phone);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
            ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
          } else {
            Map<String, Object> userMap = userMapList.get(0);
            if (!(((String) userMap.get(JsonKey.ID)).equalsIgnoreCase(user.getId()))) {
              ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void checkPhoneUniqueness(String phone) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
    if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
      if (StringUtils.isNotBlank(phone)) {
        try {
          phone = encryptionService.encryptData(phone);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting phone number ", e);
        }
        Response result =
            cassandraOperation.getRecordsByIndexedProperty(
                userDb.getKeySpace(), userDb.getTableName(), (JsonKey.PHONE), phone);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
        }
      }
    }
  }

  public static boolean identifierExists(String type, String value) {

    if (StringUtils.isNotBlank(value)) {
      try {
        value = encryptionService.encryptData(value);
      } catch (Exception e) {
        ProjectLogger.log("Exception occurred while encrypting email/phone", e);
      }
      Response result =
          cassandraOperation.getRecordsByIndexedProperty(
              userDb.getKeySpace(), userDb.getTableName(), type, value);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> userMapList =
          (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      return !userMapList.isEmpty();
    } else {
      return false;
    }
  }

  // Update channel info with orgId info
  public static void updateExternalIdsProviderWithOrgId(Map<String, Object> userMap) {
    if (MapUtils.isNotEmpty(userMap)) {
      Set<String> providerSet = new HashSet<>();
      List<Map<String, String>> extList =
          (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS);
      if (CollectionUtils.isNotEmpty(extList)) {
        for (Map<String, String> extId : extList) {
          providerSet.add(extId.get(JsonKey.PROVIDER));
        }
      }
      Map<String, String> orgProviderMap =
          UserUtil.fetchOrgIdByProvider(new ArrayList<String>(providerSet));

      if (CollectionUtils.isNotEmpty(
          (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS))) {
        for (Map<String, String> externalId :
            (List<Map<String, String>>) userMap.get(JsonKey.EXTERNAL_IDS)) {

          String orgId = orgProviderMap.get(externalId.get(JsonKey.PROVIDER));
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

  public static void checkEmailUniqueness(String email) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
    if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
      if (StringUtils.isNotBlank(email)) {
        try {
          email = encryptionService.encryptData(email);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting phone number ", e);
        }
        Response result =
            cassandraOperation.getRecordsByIndexedProperty(
                userDb.getKeySpace(), userDb.getTableName(), (JsonKey.EMAIL), email);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.emailAlreadyExistError, null);
        }
      }
    }
  }

  public static Map<String, Object> validateExternalIdsAndReturnActiveUser(
      Map<String, Object> userMap) {
    String extId = (String) userMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
    String idType = (String) userMap.get(JsonKey.EXTERNAL_ID_TYPE);
    Map<String, Object> user = null;
    if ((StringUtils.isBlank((String) userMap.get(JsonKey.USER_ID))
            && StringUtils.isBlank((String) userMap.get(JsonKey.ID)))
        && StringUtils.isNotEmpty(extId)
        && StringUtils.isNotEmpty(provider)
        && StringUtils.isNotEmpty(idType)) {
      user = getUserFromExternalId(userMap);
      if (MapUtils.isEmpty(user)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.externalIdNotFound,
            ProjectUtil.formatMessage(
                ResponseCode.externalIdNotFound.getErrorMessage(), extId, idType, provider));
      }
    } else if (StringUtils.isNotBlank((String) userMap.get(JsonKey.USER_ID))
        || StringUtils.isNotBlank((String) userMap.get(JsonKey.ID))) {
      String userId =
          (StringUtils.isNotBlank((String) userMap.get(JsonKey.USER_ID)))
              ? ((String) userMap.get(JsonKey.USER_ID))
              : ((String) userMap.get(JsonKey.ID));
      Future<Map<String, Object>> userF =
          esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
      user = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(userF);
      if (MapUtils.isEmpty(user)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.userNotFound, null);
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
  public static Map<String, Object> getUserFromExternalId(Map<String, Object> userMap) {
    Map<String, Object> user = null;
    String userId = getUserIdFromExternalId(userMap);
    if (!StringUtils.isEmpty(userId)) {
      Future<Map<String, Object>> userF =
          esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId);
      user = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(userF);
    }
    return user;
  }

  public static String getUserIdFromExternalId(Map<String, Object> userMap) {

    String extId = (String) userMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER);
    String idType = (String) userMap.get(JsonKey.EXTERNAL_ID_TYPE);
    return userExternalIdentityService.getUserV2(extId, provider, idType);
  }

  public static String getUserId(Map<String, Object> userMap) {
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
        userId = userExternalIdentityService.getUserV1(extId, provider, idType);
      }
    }
    return userId;
  }

  @SuppressWarnings("unchecked")
  public static void checkEmailUniqueness(User user, String opType) {

    String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
    if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
      String email = user.getEmail();
      if (StringUtils.isNotBlank(email)) {
        try {
          email = encryptionService.encryptData(email);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting email:", e);
        }
        Response result =
            cassandraOperation.getRecordsByIndexedProperty(
                userDb.getKeySpace(), userDb.getTableName(), (JsonKey.EMAIL), email);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
            ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
          } else {
            Map<String, Object> userMap = userMapList.get(0);
            if (!(((String) userMap.get(JsonKey.ID)).equalsIgnoreCase(user.getId()))) {
              ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
            }
          }
        }
      }
    }
  }

  public static void validateUserPhoneEmailAndWebPages(User user, String operationType) {
    checkPhoneUniqueness(user, operationType);
    checkEmailUniqueness(user, operationType);
    if (CollectionUtils.isNotEmpty(user.getWebPages())) {
      SocialMediaType.validateSocialMedia(user.getWebPages());
    }
  }

  public static String getEncryptedData(String value) {
    try {
      return encryptionService.encryptData(value);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  public static String getDecryptedData(String value) {
    try {
      return decService.decryptData(value);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
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
  public static void updateExternalIdsWithProvider(List<Map<String, String>> dbResExternalIds) {
    if (CollectionUtils.isNotEmpty(dbResExternalIds)) {
      String orgId = dbResExternalIds.get(0).get(JsonKey.PROVIDER);
      String provider = fetchProviderByOrgId(orgId);
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

  @SuppressWarnings("unchecked")
  public static void checkExternalIdUniqueness(User user, String operation) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      for (Map<String, String> externalId : user.getExternalIds()) {
        if (StringUtils.isNotBlank(externalId.get(JsonKey.ID))
            && StringUtils.isNotBlank(externalId.get(JsonKey.PROVIDER))
            && StringUtils.isNotBlank(externalId.get(JsonKey.ID_TYPE))) {
          Map<String, Object> externalIdReq = new HashMap<>();
          externalIdReq.put(JsonKey.PROVIDER, externalId.get(JsonKey.PROVIDER));
          externalIdReq.put(JsonKey.ID_TYPE, externalId.get(JsonKey.ID_TYPE));
          externalIdReq.put(JsonKey.EXTERNAL_ID, externalId.get(JsonKey.ID));
          Response response =
              cassandraOperation.getRecordsByProperties(
                  JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, externalIdReq);
          List<Map<String, Object>> externalIdsRecord =
              (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
          if (CollectionUtils.isNotEmpty(externalIdsRecord)) {
            if (JsonKey.CREATE.equalsIgnoreCase(operation)) {
              throwUserAlreadyExistsException(
                  externalId.get(JsonKey.ID),
                  externalId.get(JsonKey.ID_TYPE),
                  externalId.get(JsonKey.PROVIDER));
            } else if (JsonKey.UPDATE.equalsIgnoreCase(operation)) {
              // If end user will try to add,edit or remove other user extIds throw exception
              String userId = (String) externalIdsRecord.get(0).get(JsonKey.USER_ID);
              if (!(user.getUserId().equalsIgnoreCase(userId))) {
                if (JsonKey.ADD.equalsIgnoreCase(externalId.get(JsonKey.OPERATION))
                    || StringUtils.isBlank(externalId.get(JsonKey.OPERATION))) {
                  throw new ProjectCommonException(
                      ResponseCode.externalIdAssignedToOtherUser.getErrorCode(),
                      ProjectUtil.formatMessage(
                          ResponseCode.externalIdAssignedToOtherUser.getErrorMessage(),
                          externalId.get(JsonKey.ID),
                          externalId.get(JsonKey.ID_TYPE),
                          externalId.get(JsonKey.PROVIDER)),
                      ResponseCode.CLIENT_ERROR.getResponseCode());
                } else {
                  throwExternalIDNotFoundException(
                      externalId.get(JsonKey.ID),
                      externalId.get(JsonKey.ID_TYPE),
                      externalId.get(JsonKey.PROVIDER));
                }
              }
            }
          } else {
            // if user will try to delete non existing extIds
            if (JsonKey.UPDATE.equalsIgnoreCase(operation)
                && JsonKey.REMOVE.equalsIgnoreCase(externalId.get(JsonKey.OPERATION))) {
              throwExternalIDNotFoundException(
                  externalId.get(JsonKey.ID),
                  externalId.get(JsonKey.ID_TYPE),
                  externalId.get(JsonKey.PROVIDER));
            }
          }
        }
      }
    }
  }

  private static void throwUserAlreadyExistsException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.userAlreadyExists.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.userAlreadyExists.getErrorMessage(),
            ProjectUtil.formatMessage(
                ResponseMessage.Message.EXTERNAL_ID_FORMAT, externalId, idType, provider)),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.externalIdNotFound.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.externalIdNotFound.getErrorMessage(), externalId, idType, provider),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public static String encryptData(String value) {
    try {
      return encryptionService.encryptData(value);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  public static boolean updatePassword(Map<String, Object> userMap) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PASSWORD))) {
      return ssoManager.updatePassword(
          (String) userMap.get(JsonKey.ID), (String) userMap.get(JsonKey.PASSWORD));
    }
    return true;
  }

  public static void addMaskEmailAndPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.ENC_PHONE, phone);
      userMap.put(JsonKey.PHONE, maskingService.maskPhone(decService.decryptData(phone)));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.ENC_EMAIL, email);
      userMap.put(JsonKey.EMAIL, maskingService.maskEmail(decService.decryptData(email)));
    }
  }

  public static void addMaskEmailAndMaskPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.MASKED_PHONE, maskingService.maskPhone(decService.decryptData(phone)));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.MASKED_EMAIL, maskingService.maskEmail(decService.decryptData(email)));
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> encryptUserData(Map<String, Object> userMap) {
    try {
      UserUtility.encryptUserData(userMap);
    } catch (Exception e1) {
      ProjectCommonException.throwServerErrorException(ResponseCode.userDataEncryptionError, null);
    }
    Map<String, Object> requestMap = new HashMap<>();
    User user = mapper.convertValue(userMap, User.class);
    requestMap.putAll(mapper.convertValue(user, Map.class));
    return requestMap;
  }

  public static Map<String, Object> checkProfileCompleteness(Map<String, Object> userMap) {
    ProfileCompletenessService profileService = ProfileCompletenessFactory.getInstance();
    return profileService.computeProfile(userMap);
  }

  public static void setUserDefaultValueForV3(Map<String, Object> userMap) {
    List<String> roles = new ArrayList<>();
    roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
    userMap.put(JsonKey.ROLES, roles);
    userMap.put(
        JsonKey.COUNTRY_CODE, propertiesCache.getProperty(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE));
    // Since global settings are introduced, profile visibility map should be empty during user
    // creation
    userMap.put(JsonKey.PROFILE_VISIBILITY, new HashMap<String, String>());
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    userMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());

    if (StringUtils.isBlank((String) userMap.get(JsonKey.USERNAME))) {
      String firstName = (String) userMap.get(JsonKey.FIRST_NAME);
      firstName = firstName.split(" ")[0];
      String translatedFirstName = transliterateUserName(firstName);
      userMap.put(JsonKey.USERNAME, translatedFirstName + "_" + generateUniqueString(4));
    } else {
      String userName = transliterateUserName((String) userMap.get(JsonKey.USERNAME));
      userMap.put(JsonKey.USERNAME, userName);
      if (!userService.checkUsernameUniqueness(userName, false)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.userNameAlreadyExistError);
      }
    }
  }

  public static String transliterateUserName(String userName) {
    String translatedUserName = Junidecode.unidecode(userName);
    return translatedUserName;
  }

  public static String generateUniqueString(int length) {
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

  private static String baseN(BigDecimal num, int base) {
    if (num.doubleValue() == 0) {
      return "0";
    }
    double div = Math.floor(num.doubleValue() / base);
    String val = baseN(new BigDecimal(div), base);
    return StringUtils.stripStart(val, stripChars)
        + alphabet[num.remainder(new BigDecimal(base)).intValue()];
  }

  public static void setUserDefaultValue(Map<String, Object> userMap, String callerId) {
    if (StringUtils.isBlank(callerId)) {
      List<String> roles = new ArrayList<>();
      roles.add(ProjectUtil.UserRole.PUBLIC.getValue());
      userMap.put(JsonKey.ROLES, roles);
    }
    if (null == userMap.get(JsonKey.EMAIL_VERIFIED)) {
      userMap.put(JsonKey.EMAIL_VERIFIED, false);
    }
    if (null == userMap.get(JsonKey.PHONE_VERIFIED)) {
      userMap.put(JsonKey.PHONE_VERIFIED, false);
    }
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
      userMap.put(
          JsonKey.COUNTRY_CODE, propertiesCache.getProperty(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE));
    }
    // Since global settings are introduced, profile visibility map should be empty during user
    // creation
    userMap.put(JsonKey.PROFILE_VISIBILITY, new HashMap<String, String>());
    userMap.put(JsonKey.IS_DELETED, false);
    userMap.put(JsonKey.CREATED_DATE, ProjectUtil.getFormattedDate());
    userMap.put(JsonKey.STATUS, ProjectUtil.Status.ACTIVE.getValue());

    if (StringUtils.isBlank((String) userMap.get(JsonKey.USERNAME))) {
      String firstName = (String) userMap.get(JsonKey.FIRST_NAME);
      String lastName = (String) userMap.get(JsonKey.LAST_NAME);

      String name = String.join(" ", firstName, StringUtils.isNotBlank(lastName) ? lastName : "");

      String userName = null;
      while (StringUtils.isBlank(userName)) {
        userName = getUsername(name);
        if (StringUtils.isNotBlank(userName)) {
          userMap.put(JsonKey.USERNAME, transliterateUserName(userName));
        }
      }
    } else {
      userMap.put(JsonKey.USERNAME, transliterateUserName((String) userMap.get(JsonKey.USERNAME)));
      if (!userService.checkUsernameUniqueness((String) userMap.get(JsonKey.USERNAME), false)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.userNameAlreadyExistError);
      }
    }
    // create loginId to ensure uniqueness for combination of userName and channel
    String loginId = Util.getLoginId(userMap);
    userMap.put(JsonKey.LOGIN_ID, loginId);
  }

  private static String getUsername(String name) {
    List<Map<String, Object>> users = null;
    List<String> esUserNameList = new ArrayList<>();
    List<String> encryptedUserNameList = new ArrayList<>();
    List<String> excludedUsernames = new ArrayList<>();
    List<String> userNameList = new ArrayList<>();

    String userName = "";
    do {
      do {
        encryptedUserNameList.clear();
        excludedUsernames.addAll(userNameList);

        // Generate usernames
        userNameList = userService.generateUsernames(name, excludedUsernames);

        // Encrypt each user name
        userService
            .getEncryptedList(userNameList)
            .stream()
            .forEach(value -> encryptedUserNameList.add(value));

        // Throw an error in case of encryption failures
        if (encryptedUserNameList.isEmpty()) {
          ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        // Search if any user names are taking using ES
        List<String> filtersEncryptedUserNameList = new ArrayList<>(encryptedUserNameList);
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.USERNAME, filtersEncryptedUserNameList);
        users = userService.esSearchUserByFilters(filters);
      } while (CollectionUtils.isNotEmpty(users) && users.size() >= encryptedUserNameList.size());

      esUserNameList.clear();

      // Map list of user results (from ES) into list of usernames
      users
          .stream()
          .forEach(
              user -> {
                esUserNameList.add((String) user.get(JsonKey.USERNAME));
              });
      // Query cassandra to find first username that is not yet assigned
      Optional<String> result =
          encryptedUserNameList
              .stream()
              .filter(
                  value -> {
                    if (!esUserNameList.contains(value)) {
                      return userService.checkUsernameUniqueness(value, true);
                    }
                    return false;
                  })
              .findFirst();

      if (result.isPresent()) {
        userName = result.get();
      }

    } while (StringUtils.isBlank(userName));
    return decService.decryptData(userName);
  }
  // validateExternalIds For CREATE USER and MIGRATE USER
  public static void validateExternalIds(User user, String operationType) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      List<Map<String, String>> list = copyAndConvertExternalIdsToLower(user.getExternalIds());
      user.setExternalIds(list);
    }
    checkExternalIdUniqueness(user, operationType);
  }
  // validateExternalIds For UPDATE USER
  public static void validateExternalIdsForUpdateUser(User user, boolean isCustodianOrg) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      List<Map<String, String>> list = copyAndConvertExternalIdsToLower(user.getExternalIds());
      user.setExternalIds(list);
    }
    // If operation is update and user is custodian org, ignore uniqueness check
    if (!isCustodianOrg) {
      checkExternalIdUniqueness(user, JsonKey.UPDATE);
    }
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      validateUserExternalIds(user);
    }
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      updateExternalIdsStatus(user.getExternalIds());
    }
  }

  public static void checkEmailSameOrDiff(
      Map<String, Object> userRequestMap, Map<String, Object> userDbRecord) {
    if (StringUtils.isNotBlank((String) userRequestMap.get(JsonKey.EMAIL))) {
      String email = (String) userDbRecord.get(JsonKey.EMAIL);
      String encEmail = (String) userRequestMap.get(JsonKey.EMAIL);
      if (StringUtils.isNotBlank(email)) {
        try {
          encEmail = encryptionService.encryptData((String) userRequestMap.get(JsonKey.EMAIL));
        } catch (Exception ex) {
          ProjectLogger.log("Exception occurred while encrypting user email.");
        }
        if ((encEmail).equalsIgnoreCase(email)) {
          userRequestMap.remove(JsonKey.EMAIL);
        }
      }
    }
  }

  private static void updateExternalIdsStatus(List<Map<String, String>> externalIds) {
    externalIds.forEach(
        externalIdMap -> {
          // Needed in 3.2
          // externalIdMap.put(JsonKey.STATUS, JsonKey.PENDING);
        });
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

  public static void validateUserExternalIds(User user) {
    List<Map<String, String>> dbResExternalIds = getExternalIds(user.getUserId());
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

  public static List<Map<String, String>> getExternalIds(String userId) {
    List<Map<String, String>> dbResExternalIds =
        userExternalIdentityService.getUserExternalIds(userId);
    List<Map<String, String>> dbSelfDeclaredExternalIds =
        userExternalIdentityService.getSelfDeclaredDetails(userId);

    if (CollectionUtils.isNotEmpty(dbSelfDeclaredExternalIds)) {
      dbResExternalIds.addAll(dbSelfDeclaredExternalIds);
    }
    return dbResExternalIds;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, String>> getUserExternalIds(String userId) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            JsonKey.SUNBIRD, JsonKey.USR_EXT_IDNT_TABLE, JsonKey.USER_ID, userId);
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  public static List<Map<String, Object>> getActiveUserOrgDetails(String userId) {
    return getUserOrgDetails(false, userId);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getUserOrgDetails(boolean isdeleted, String userId) {
    List<Map<String, Object>> userOrgList = new ArrayList<>();
    List<Map<String, Object>> organisations = new ArrayList<>();
    try {
      Util.DbInfo userOrgDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
      List<String> ids = new ArrayList<>();
      ids.add(userId);
      Response result =
          cassandraOperation.getRecordsByPrimaryKeys(
              userOrgDbInfo.getKeySpace(), userOrgDbInfo.getTableName(), ids, JsonKey.USER_ID);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        if (!isdeleted) {
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
      ProjectLogger.log(e.getMessage(), e);
    }
    return organisations;
  }

  public static List<Map<String, Object>> getAllUserOrgDetails(String userId) {
    return getUserOrgDetails(true, userId);
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

  /**
   * Get Tenant Private Fields mapped to OrgId and role
   *
   * @param orgId
   * @param key
   * @return
   */
  public static Set<String> getTenantPrivateFields(String orgId, String key) {
    Set<String> privateFieldsSet = new HashSet<>();
    Map<String, Map<String, Object>> tenantConfigMap = DataCacheHandler.getTenantConfigMap();
    Map<String, Object> roleConfigMap = tenantConfigMap.get(orgId);
    if (MapUtils.isNotEmpty(roleConfigMap)) {
      String data = (String) roleConfigMap.get(key);
      Map<String, Object> dataConfigMap = new HashMap<>();
      try {
        dataConfigMap = mapper.readValue(data, Map.class);
      } catch (JsonProcessingException e) {
        ProjectLogger.log("Error getting Private fields");
      }
      List<String> privateFields = (List<String>) dataConfigMap.get(JsonKey.PRIVATE);
      if (CollectionUtils.isNotEmpty(privateFields)) {
        privateFieldsSet.addAll(privateFields);
      }
    }
    return privateFieldsSet;
  }

  /**
   * Fetch all the tenant config map for orgId
   *
   * @param orgId
   * @return
   */
  public static Set<String> getTenantPrivateFields(String orgId) {
    Set<String> privateFieldsSet = new HashSet<>();
    Map<String, Map<String, Object>> tenantConfigMap = DataCacheHandler.getTenantConfigMap();
    Map<String, Object> roleConfigMap = tenantConfigMap.get(orgId);
    if (MapUtils.isNotEmpty(roleConfigMap)) {
      for (Map.Entry<String, Object> entryItr : roleConfigMap.entrySet()) {
        String key = entryItr.getKey();
        String data = (String) entryItr.getValue();
        Map<String, Object> dataConfigMap = new HashMap<>();
        try {
          dataConfigMap = mapper.readValue(data, Map.class);
        } catch (JsonProcessingException e) {
          ProjectLogger.log("Error getting Private fields");
        }
        List<String> privateFields = (List<String>) dataConfigMap.get(JsonKey.PRIVATE);
        if (CollectionUtils.isNotEmpty(privateFields)) {
          privateFieldsSet.addAll(privateFields);
        }
      }
    }
    return privateFieldsSet;
  }

  public static Map<String, Object> validateManagedByUser(String managedBy) {
    Future<Map<String, Object>> managedByInfoF =
        esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), managedBy);
    Map<String, Object> managedByInfo =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(managedByInfoF);
    if (ProjectUtil.isNull(managedByInfo)
        || StringUtils.isBlank((String) managedByInfo.get(JsonKey.FIRST_NAME))
        || StringUtils.isNotBlank((String) managedByInfo.get(JsonKey.MANAGED_BY))
        || (null != managedByInfo.get(JsonKey.IS_DELETED)
            && (boolean) (managedByInfo.get(JsonKey.IS_DELETED)))) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    UserUtility.decryptUserDataFrmES(managedByInfo);
    return managedByInfo;
  }

  public static void validateManagedUserLimit(String managedBy) {
    if (Boolean.valueOf(ProjectUtil.getConfigValue(JsonKey.LIMIT_MANAGED_USER_CREATION))) {
      Map<String, Object> searchQueryMap = new HashMap<>();
      searchQueryMap.put(JsonKey.MANAGED_BY, managedBy);
      List<User> managedUserList = Util.searchUser(searchQueryMap);
      if (CollectionUtils.isNotEmpty(managedUserList)
          && managedUserList.size()
              >= Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.MANAGED_USER_LIMIT))) {
        throw new ProjectCommonException(
            ResponseCode.managedUserLimitExceeded.getErrorCode(),
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
              JsonKey.TEACHER_PERSONA,
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
              JsonKey.TEACHER_PERSONA,
              userInfo);
      userDeclareEntity.setCreatedBy((String) requestMap.get(JsonKey.CREATED_BY));
      userDeclareEntity.setUpdatedBy((String) requestMap.get(JsonKey.UPDATED_BY));
      userDeclareEntity.setOperation(JsonKey.ADD);
      userDeclareEntities.add(userDeclareEntity);
    }
    return currOrgId;
  }

  public static String fetchProviderByOrgId(String orgId) {
    try {
      if (StringUtils.isNotBlank(orgId)) {
        Future<Map<String, Object>> esOrgResF =
            esUtil.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), orgId);
        Map<String, Object> org =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esOrgResF);

        if (null != org && !org.isEmpty()) {
          return (String) org.get(JsonKey.CHANNEL);
        }
      }
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    }
    return "";
  }

  public static Map<String, String> fetchOrgIdByProvider(List<String> providers) {
    Map<String, String> providerOrgMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(providers)) {
      try {
        Map<String, Object> searchQueryMap = new HashMap<>();
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.IS_ROOT_ORG, true);
        filters.put(JsonKey.CHANNEL, providers);
        searchQueryMap.put(JsonKey.FILTERS, filters);
        SearchDTO searchDTO = Util.createSearchDto(searchQueryMap);
        Future<Map<String, Object>> esOrgResF =
            esUtil.search(searchDTO, ProjectUtil.EsType.organisation.getTypeName());
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
        ProjectLogger.log(ex.getMessage(), ex);
      }
    }
    return providerOrgMap;
  }

  public static void encryptDeclarationFields(List<Map<String, Object>> declarations)
      throws Exception {
    Set<String> privateFieldsSet = getTenantPrivateFields(JsonKey.ALL, JsonKey.SELF_DECLARATIONS);
    for (Map<String, Object> declareFields : declarations) {
      Map<String, Object> userInfoMap = (Map<String, Object>) declareFields.get(JsonKey.INFO);
      for (Map.Entry<String, Object> userInfo : userInfoMap.entrySet()) {
        String key = userInfo.getKey();
        String value = (String) userInfo.getValue();
        if (privateFieldsSet.contains(key)) {
          userInfoMap.put(key, encryptionService.encryptData(value));
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
      userDeclareEntity.setCreatedBy((String) declareFieldMap.get(JsonKey.CREATED_BY));
      userDeclareEntity.setStatus(JsonKey.PENDING);
    } else {
      userDeclareEntity.setUpdatedBy((String) declareFieldMap.get(JsonKey.UPDATED_BY));
      userDeclareEntity.setStatus((String) declareFieldMap.get(JsonKey.STATUS));
    }
    userDeclareEntity.setErrorType((String) declareFieldMap.get(JsonKey.ERR_TYPE));

    return userDeclareEntity;
  }
}

@FunctionalInterface
interface ConvertValuesToLowerCase {
  Map<String, String> convertToLowerCase(Map<String, String> map);
}
