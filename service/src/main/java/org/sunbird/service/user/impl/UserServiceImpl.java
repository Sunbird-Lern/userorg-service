package org.sunbird.service.user.impl;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.typesafe.config.ConfigFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.ElasticSearchHelper;
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
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.adminutil.AdminUtilRequestData;
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.organisation.OrgService;
import org.sunbird.service.organisation.impl.OrgServiceImpl;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.util.*;
import org.sunbird.util.user.ProfileUtil;
import org.sunbird.util.user.UserTncUtil;
import org.sunbird.util.user.UserUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class UserServiceImpl implements UserService {

  private final LoggerUtil logger = new LoggerUtil(UserServiceImpl.class);
  private final EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
  private final UserDao userDao = UserDaoImpl.getInstance();
  private static UserService userService = null;
  private final UserLookupDao userLookupDao = UserLookupDaoImpl.getInstance();
  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();
  private final OrgService orgService = OrgServiceImpl.getInstance();
  private final UserRoleService userRoleService = UserRoleServiceImpl.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();

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
  public Response updateUser(Map<String, Object> user, RequestContext context) {
    return userDao.updateUser(user, context);
  }

  @Override
  public User getUserById(String userId, RequestContext context) {
    User user = userDao.getUserById(userId, context);
    if (null == user) {
      ProjectCommonException.throwResourceNotFoundException(
          ResponseCode.resourceNotFound, JsonKey.USER);
    }
    return user;
  }

  @Override
  public Map<String, Object> getUserDetailsById(String userId, RequestContext context) {
    Map<String, Object> user = userDao.getUserDetailsById(userId, context);
    if (MapUtils.isEmpty(user)) {
      ProjectCommonException.throwResourceNotFoundException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(ResponseCode.resourceNotFound.getErrorMessage(), JsonKey.USER));
    }
    if (user.get(JsonKey.PROFILE_DETAILS) != null) {
      logger.debug(context, "getUserDetailsById :: read Profile details String is :: " + user.get(JsonKey.PROFILE_DETAILS).toString());
      user.put(JsonKey.PROFILE_DETAILS, ProfileUtil.toMap(user.get(JsonKey.PROFILE_DETAILS).toString()));
    }
    user.putAll(Util.getUserDefaultValue());
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
    if(ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED)) {
      if ((StringUtils.isNotEmpty(managedForId) && !managedForId.equals(userId))
              || (StringUtils.isEmpty(managedById)
              && (!StringUtils.isBlank(userId) && !userId.equals(ctxtUserId))) // UPDATE
              || (StringUtils.isNotEmpty(managedById)
              && !(ctxtUserId.equals(managedById)))) // CREATE NEW USER/ UPDATE MUA {
        ProjectCommonException.throwUnauthorizedErrorException();
    }
  }

  @Override
  public Map<String, Object> esGetPublicUserProfileById(String userId, RequestContext context) {
    return userDao.getEsUserById(userId, context);
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
    if(ConfigFactory.load().getBoolean(JsonKey.AUTH_ENABLED)) {
      if (!user.getRootOrgId().equalsIgnoreCase(uploader.getRootOrgId())) {
        ProjectCommonException.throwUnauthorizedErrorException();
      }
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
    int GENERATE_USERNAME_COUNT = 10;
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
  public String getUserIdByUserLookUp(String key, String value, RequestContext context) {
    List<Map<String, Object>> records =
        userLookupDao.getRecordByType(key.toLowerCase(), value.toLowerCase(), true, context);
    if (CollectionUtils.isNotEmpty(records)) {
      return (String) records.get(0).get(JsonKey.USER_ID);
    }
    return "";
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
      logger.error(context, "unable to parse data :", e);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
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
    request.setOperation(ActorOperations.SAVE_USER_ATTRIBUTES.getValue());
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
   * @param type value can be email, phone, recoveryEmail, recoveryPhone , prevUsedEmail or
   *     prevUsedPhone
   * @return
   */
  public String getDecryptedEmailPhoneByUserId(String userId, String type, RequestContext context) {
    Map<String, Object> user = userDao.getUserDetailsById(userId, context);
    if (MapUtils.isEmpty(user)) {
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound,
          MessageFormat.format(ResponseCode.resourceNotFound.getErrorMessage(), JsonKey.USER),
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
          org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
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
  public List<Map<String, Object>> getDecryptedEmailPhoneByUserIds(
      List<String> userIds, String type, RequestContext context) {
    List<String> properties = new ArrayList<>();
    properties.add(type);
    properties.add(JsonKey.ID);
    properties.add(JsonKey.FIRST_NAME);
    properties.add(JsonKey.ROOT_ORG_ID);
    Response response = userDao.getUserPropertiesById(userIds, properties, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    responseList
        .stream()
        .forEach(resMap -> resMap.put(type, getDecryptedValue((String) resMap.get(type), context)));
    return responseList;
  }

  @Override
  public List<Map<String, Object>> getUserEmailsBySearchQuery(
      Map<String, Object> searchQuery, RequestContext context) {
    List<Map<String, Object>> usersList = new ArrayList<>();
    Map<String, Object> esResult =
        searchUser(ElasticSearchHelper.createSearchDTO(searchQuery), context);
    if (MapUtils.isNotEmpty(esResult)
        && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      usersList = (List<Map<String, Object>>) esResult.get(JsonKey.CONTENT);
      usersList.forEach(
          user -> {
            if (org.apache.commons.lang.StringUtils.isNotBlank((String) user.get(JsonKey.EMAIL))) {
              String email = getDecryptedValue((String) user.get(JsonKey.EMAIL), context);
              if (ProjectUtil.isEmailvalid(email)) {
                user.put(JsonKey.EMAIL, email);
              } else {
                logger.info(
                    "UserServiceImpl:getUserEmailsBySearchQuery: Invalid Email or its decryption failed for userId = "
                        + user.get(JsonKey.USER_ID));
                user.put(JsonKey.EMAIL, null);
              }
            }
          });
    }
    return usersList;
  }

  @Override
  public Map<String, Object> searchUser(SearchDTO searchDTO, RequestContext context) {
    return userDao.search(searchDTO, context);
  }

  @Override
  public boolean updateUserDataToES(
      String identifier, Map<String, Object> data, RequestContext context) {
    return userDao.updateUserDataToES(identifier, data, context);
  }

  @Override
  public String saveUserToES(String identifier, Map<String, Object> data, RequestContext context) {
    return userDao.saveUserToES(identifier, data, context);
  }

  @Override
  public Map<String, Object> getUserDetailsForES(String userId, RequestContext context) {
    logger.info(context, "get user profile method call started user Id : " + userId);
    Map<String, Object> userDetails = getUserDetailsById(userId, context);
    if (MapUtils.isNotEmpty(userDetails)) {
      logger.debug(context, "getUserDetails: userId = " + userId);
      userDetails.put(JsonKey.ORGANISATIONS, getUserOrgDetails(userId, context));
      Map<String, Object> orgMap =
          orgService.getOrgById((String) userDetails.get(JsonKey.ROOT_ORG_ID), context);
      if (MapUtils.isNotEmpty(orgMap)) {
        userDetails.put(JsonKey.ROOT_ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
      }
      // store alltncaccepted as Map Object in ES
      Map<String, String> allTncAccepted =
          (Map<String, String>) userDetails.get(JsonKey.ALL_TNC_ACCEPTED);
      if (MapUtils.isNotEmpty(allTncAccepted)) {
        userDetails.put(
            JsonKey.ALL_TNC_ACCEPTED, UserTncUtil.convertTncStringToJsonMap(allTncAccepted));
      }
      userDetails.remove(JsonKey.PASSWORD);
      checkEmailAndPhoneVerified(userDetails);
      List<Map<String, String>> userLocList = new ArrayList<>();
      String profLocation = (String) userDetails.get(JsonKey.PROFILE_LOCATION);
      if (StringUtils.isNotBlank(profLocation)) {
        try {
          userLocList = mapper.readValue(profLocation, List.class);
        } catch (Exception e) {
          logger.error(
              context,
              "Exception while converting profileLocation to List<Map<String,String>>.",
              e);
        }
      }
      userDetails.put(JsonKey.PROFILE_LOCATION, userLocList);
      Map<String, Object> userTypeDetail = new HashMap<>();
      String profUserType = (String) userDetails.get(JsonKey.PROFILE_USERTYPE);
      if (StringUtils.isNotBlank(profUserType)) {
        try {
          userTypeDetail = mapper.readValue(profUserType, Map.class);
        } catch (Exception e) {
          logger.error(
              context, "Exception while converting profileUserType to Map<String,String>.", e);
        }
      }
      userDetails.put(JsonKey.PROFILE_USERTYPE, userTypeDetail);
      List<Map<String, Object>> userTypeDetails = new ArrayList<>();
      String profUserTypes = (String) userDetails.get(JsonKey.PROFILE_USERTYPES);
      if (StringUtils.isNotBlank(profUserTypes)) {
        try {
          userTypeDetails = mapper.readValue(profUserTypes, List.class);
        } catch (Exception e) {
          logger.error(
              context,
              "Exception while converting profileUserTypes to List<Map<String, Object>>.",
              e);
        }
      }
      userDetails.put(JsonKey.PROFILE_USERTYPES, userTypeDetails);
      List<Map<String, Object>> userRoleList = userRoleService.getUserRoles(userId, context);
      userDetails.put(JsonKey.ROLES, userRoleList);
    } else {
      logger.info(
          context, "getUserProfile: User data not available to save in ES for userId : " + userId);
    }
    return userDetails;
  }

  private List<Map<String, Object>> getUserOrgDetails(String userId, RequestContext context) {
    List<Map<String, Object>> userOrgList = new ArrayList<>();
    List<Map<String, Object>> userOrgDataList;
    try {
      userOrgDataList = userOrgService.getUserOrgListByUserId(userId, context);
      userOrgDataList
          .stream()
          .forEach(
              dataMap -> {
                if (null != dataMap.get(JsonKey.IS_DELETED)
                    && !((boolean) dataMap.get(JsonKey.IS_DELETED))) {
                  userOrgList.add(dataMap);
                }
              });
      if (CollectionUtils.isNotEmpty(userOrgList)) {
        List<String> organisationIds =
            userOrgList
                .stream()
                .map(m -> (String) m.get(JsonKey.ORGANISATION_ID))
                .distinct()
                .collect(Collectors.toList());
        List<String> fields = Arrays.asList(JsonKey.ORG_NAME, JsonKey.ID);
        List<Map<String, Object>> orgDataList =
            orgService.getOrgByIds(organisationIds, fields, context);
        Map<String, Map<String, Object>> orgInfoMap = new HashMap<>();
        orgDataList.stream().forEach(org -> orgInfoMap.put((String) org.get(JsonKey.ID), org));
        for (Map<String, Object> userOrg : userOrgList) {
          Map<String, Object> orgMap = orgInfoMap.get(userOrg.get(JsonKey.ORGANISATION_ID));
          userOrg.put(JsonKey.ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
          userOrg.remove(JsonKey.ROLES);
        }
      }
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    return userOrgList;
  }

  private void checkEmailAndPhoneVerified(Map<String, Object> userDetails) {
    if (null != userDetails.get(JsonKey.FLAGS_VALUE)) {
      int flagsValue = Integer.parseInt(userDetails.get(JsonKey.FLAGS_VALUE).toString());
      Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(flagsValue);
      userDetails.putAll(userFlagMap);
    }
  }
}
