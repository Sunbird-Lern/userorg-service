package org.sunbird.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.datasecurity.DataMaskingService;
import org.sunbird.datasecurity.DecryptionService;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import scala.concurrent.Future;

/**
 * Utility class for actors
 *
 * @author arvind .
 */
public final class Util {
  private static LoggerUtil logger = new LoggerUtil(Util.class);

  public static final Map<String, DbInfo> dbInfoMap = new HashMap<>();
  public static final int DEFAULT_ELASTIC_DATA_LIMIT = 10000;
  public static final String KEY_SPACE_NAME = "sunbird";
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance();
  private static DecryptionService decService =
      org.sunbird.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance();
  private static DataMaskingService maskingService =
      org.sunbird.datasecurity.impl.ServiceFactory.getMaskingServiceInstance();
  private static ObjectMapper mapper = new ObjectMapper();
  private static ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private static UserOrgService userOrgService = UserOrgServiceImpl.getInstance();

  static {
    initializeDBProperty();
  }

  private Util() {}

  /** This method will initialize the cassandra data base property */
  private static void initializeDBProperty() {
    // setting db info (keyspace , table) into static map
    // this map will be used during cassandra data base interaction.
    // this map will have each DB name and it's corresponding keyspace and table
    // name.
    dbInfoMap.put(JsonKey.USER_DB, getDbInfoObject(KEY_SPACE_NAME, "user"));
    dbInfoMap.put(JsonKey.ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "organisation"));
    dbInfoMap.put(JsonKey.ROLE, getDbInfoObject(KEY_SPACE_NAME, "role"));
    dbInfoMap.put(JsonKey.URL_ACTION, getDbInfoObject(KEY_SPACE_NAME, "url_action"));
    dbInfoMap.put(JsonKey.ACTION_GROUP, getDbInfoObject(KEY_SPACE_NAME, "action_group"));
    dbInfoMap.put(JsonKey.USER_ACTION_ROLE, getDbInfoObject(KEY_SPACE_NAME, "user_action_role"));
    dbInfoMap.put(JsonKey.ROLE_GROUP, getDbInfoObject(KEY_SPACE_NAME, "role_group"));
    dbInfoMap.put(JsonKey.USER_ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "user_organisation"));
    dbInfoMap.put(JsonKey.BULK_OP_DB, getDbInfoObject(KEY_SPACE_NAME, "bulk_upload_process"));
    dbInfoMap.put(JsonKey.USER_NOTES_DB, getDbInfoObject(KEY_SPACE_NAME, "user_notes"));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_DB, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference"));
    dbInfoMap.put(JsonKey.SYSTEM_SETTINGS_DB, getDbInfoObject(KEY_SPACE_NAME, "system_settings"));
    dbInfoMap.put(JsonKey.USER_CERT, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_CERT));
    dbInfoMap.put(JsonKey.USER_FEED_DB, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_FEED_DB));
    dbInfoMap.put(
        JsonKey.USR_DECLARATION_TABLE,
        getDbInfoObject(KEY_SPACE_NAME, JsonKey.USR_DECLARATION_TABLE));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_V2, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference_v2"));

    dbInfoMap.put(JsonKey.USER_LOOKUP, getDbInfoObject(KEY_SPACE_NAME, "user_lookup"));
    dbInfoMap.put(JsonKey.LOCATION, getDbInfoObject(KEY_SPACE_NAME, JsonKey.LOCATION));
    dbInfoMap.put(JsonKey.USER_ROLES, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_ROLES));
  }

  private static DbInfo getDbInfoObject(String keySpace, String table) {
    DbInfo dbInfo = new DbInfo();
    dbInfo.setKeySpace(keySpace);
    dbInfo.setTableName(table);
    return dbInfo;
  }

  /** class to hold cassandra db info. */
  public static class DbInfo {
    private String keySpace;
    private String tableName;

    /** No-arg constructor */
    DbInfo() {}

    public String getKeySpace() {
      return keySpace;
    }

    public void setKeySpace(String keySpace) {
      this.keySpace = keySpace;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }
  }

  /**
   * This method will take searchQuery map and internally it will convert map to SearchDto object.
   *
   * @param searchQueryMap Map<String , Object>
   * @return SearchDTO
   */
  public static SearchDTO createSearchDto(Map<String, Object> searchQueryMap) {
    SearchDTO search = new SearchDTO();
    if (searchQueryMap.containsKey(JsonKey.QUERY)) {
      search.setQuery((String) searchQueryMap.get(JsonKey.QUERY));
    }
    if (searchQueryMap.containsKey(JsonKey.QUERY_FIELDS)) {
      search.setQueryFields((List<String>) searchQueryMap.get(JsonKey.QUERY_FIELDS));
    }
    if (searchQueryMap.containsKey(JsonKey.FACETS)) {
      search.setFacets((List<Map<String, String>>) searchQueryMap.get(JsonKey.FACETS));
    }
    if (searchQueryMap.containsKey(JsonKey.FIELDS)) {
      search.setFields((List<String>) searchQueryMap.get(JsonKey.FIELDS));
    }
    if (searchQueryMap.containsKey(JsonKey.FILTERS)) {
      search.getAdditionalProperties().put(JsonKey.FILTERS, searchQueryMap.get(JsonKey.FILTERS));
    }
    if (searchQueryMap.containsKey(JsonKey.EXISTS)) {
      search.getAdditionalProperties().put(JsonKey.EXISTS, searchQueryMap.get(JsonKey.EXISTS));
    }
    if (searchQueryMap.containsKey(JsonKey.NOT_EXISTS)) {
      search
          .getAdditionalProperties()
          .put(JsonKey.NOT_EXISTS, searchQueryMap.get(JsonKey.NOT_EXISTS));
    }
    if (searchQueryMap.containsKey(JsonKey.SORT_BY)) {
      search
          .getSortBy()
          .putAll((Map<? extends String, ? extends String>) searchQueryMap.get(JsonKey.SORT_BY));
    }
    if (searchQueryMap.containsKey(JsonKey.OFFSET)) {
      if ((searchQueryMap.get(JsonKey.OFFSET)) instanceof Integer) {
        search.setOffset((int) searchQueryMap.get(JsonKey.OFFSET));
      } else {
        search.setOffset(((BigInteger) searchQueryMap.get(JsonKey.OFFSET)).intValue());
      }
    }
    if (searchQueryMap.containsKey(JsonKey.LIMIT)) {
      if ((searchQueryMap.get(JsonKey.LIMIT)) instanceof Integer) {
        search.setLimit((int) searchQueryMap.get(JsonKey.LIMIT));
      } else {
        search.setLimit(((BigInteger) searchQueryMap.get(JsonKey.LIMIT)).intValue());
      }
    }
    if (search.getLimit() > DEFAULT_ELASTIC_DATA_LIMIT) {
      search.setLimit(DEFAULT_ELASTIC_DATA_LIMIT);
    }
    if (search.getLimit() + search.getOffset() > DEFAULT_ELASTIC_DATA_LIMIT) {
      search.setLimit(DEFAULT_ELASTIC_DATA_LIMIT - search.getOffset());
    }
    if (searchQueryMap.containsKey(JsonKey.GROUP_QUERY)) {
      search
          .getGroupQuery()
          .addAll(
              (Collection<? extends Map<String, Object>>) searchQueryMap.get(JsonKey.GROUP_QUERY));
    }
    if (searchQueryMap.containsKey(JsonKey.SOFT_CONSTRAINTS)) {
      search.setSoftConstraints(
          (Map<String, Integer>) searchQueryMap.get(JsonKey.SOFT_CONSTRAINTS));
    }
    return search;
  }

  public static void initializeContext(Request request, String env) {
    Map<String, Object> requestContext = request.getContext();
    env = StringUtils.isNotBlank(env) ? env : "";
    requestContext.put(JsonKey.ENV, env);
    requestContext.put(JsonKey.REQUEST_TYPE, JsonKey.API_CALL);
    if (JsonKey.USER.equalsIgnoreCase((String) request.getContext().get(JsonKey.ACTOR_TYPE))) {
      String requestedByUserId = (String) request.getContext().get(JsonKey.REQUESTED_BY);
      if (StringUtils.isNotBlank(requestedByUserId)) {
        Util.DbInfo usrDbInfo = dbInfoMap.get(JsonKey.USER_DB);
        Response userResponse =
            cassandraOperation.getRecordById(
                usrDbInfo.getKeySpace(),
                usrDbInfo.getTableName(),
                (String) request.getContext().get(JsonKey.REQUESTED_BY),
                request.getRequestContext());
        List<Map<String, Object>> userList =
            (List<Map<String, Object>>) userResponse.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(userList)) {
          Map<String, Object> result = userList.get(0);
          if (result != null) {
            String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);
            if (StringUtils.isNotBlank(rootOrgId)) {
              Map<String, String> rollup = new HashMap<>();

              rollup.put("l1", rootOrgId);
              requestContext.put(JsonKey.ROLLUP, rollup);
            }
          }
        }
      }
    }
  }

  public static Map<String, Object> getOrgDetails(String identifier, RequestContext context) {
    if (StringUtils.isNotBlank(identifier)) {
      DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
      Response response =
          cassandraOperation.getRecordById(
              orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), identifier, context);
      List<Map<String, Object>> res = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (null != res && !res.isEmpty()) {
        Map<String, Object> result = res.get(0);
        result.put(JsonKey.HASHTAGID, result.get(JsonKey.ID));
        return result;
      }
    }
    return Collections.emptyMap();
  }

  public static String encryptData(String value) {
    return encryptionService.encryptData(value, null);
  }

  /**
   * This method will search in ES for user with given search query
   *
   * @param searchQueryMap Query filters as Map.
   * @param context
   * @return List<User> List of User object.
   */
  public static List<Map<String, Object>> searchUser(
      Map<String, Object> searchQueryMap, RequestContext context) {
    List<Map<String, Object>> searchResult = new ArrayList<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    searchRequestMap.put(JsonKey.FILTERS, searchQueryMap);
    SearchDTO searchDto = Util.createSearchDto(searchRequestMap);
    Future<Map<String, Object>> resultf =
        esService.search(searchDto, ProjectUtil.EsType.user.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultf);
    if (MapUtils.isNotEmpty(result)) {
      searchResult = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    }
    return searchResult;
  }

  public static void registerUserToOrg(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    reqMap.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    reqMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    reqMap.put(JsonKey.IS_DELETED, false);
    reqMap.put(JsonKey.ASSOCIATION_TYPE, userMap.get(JsonKey.ASSOCIATION_TYPE));
    if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.HASHTAGID))) {
      reqMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.HASHTAGID));
    }
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    try {
      cassandraOperation.insertRecord(
          usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap, context);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
  }

  public static void upsertUserOrgData(Map<String, Object> userMap, RequestContext context) {
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
    Map<String, Object> map = new LinkedHashMap<>();
    map.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    map.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    Response response =
        cassandraOperation.getRecordsByProperties(
            usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), map, context);
    List<Map<String, Object>> resList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!resList.isEmpty()) {
      Map<String, Object> res = resList.get(0);
      Map<String, Object> reqMap = new WeakHashMap<>();
      reqMap.put(JsonKey.ID, res.get(JsonKey.ID));
      if (null != userMap.get(JsonKey.ROLES)) {
        reqMap.put(JsonKey.ROLES, userMap.get(JsonKey.ROLES));
      }
      reqMap.put(JsonKey.UPDATED_BY, userMap.get(JsonKey.UPDATED_BY));
      reqMap.put(JsonKey.ASSOCIATION_TYPE, userMap.get(JsonKey.ASSOCIATION_TYPE));
      reqMap.put(JsonKey.IS_DELETED, false);
      reqMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.HASHTAGID))) {
        reqMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.HASHTAGID));
      }
      try {
        cassandraOperation.updateRecord(
            usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap, context);
      } catch (Exception e) {
        logger.error(context, "Util:upsertUserOrgData exception : " + e.getMessage(), e);
      }
    } else {
      userOrgService.registerUserToOrg(userMap, context);
    }
  }

  public static List<Map<String, Object>> getUserRoles(String userId, RequestContext context) {
    DbInfo userRoleDbInfo = dbInfoMap.get(JsonKey.USER_ROLES);
    List<String> userIds = new ArrayList<>();
    userIds.add(userId);
    Response result =
        cassandraOperation.getRecordsByPrimaryKeys(
            userRoleDbInfo.getKeySpace(),
            userRoleDbInfo.getTableName(),
            userIds,
            JsonKey.USER_ID,
            context);
    List<Map<String, Object>> userRoleList =
        (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    userRoleList
        .stream()
        .forEach(
            userRole -> {
              try {
                String dbScope = (String) userRole.get(JsonKey.SCOPE);
                if (StringUtils.isNotBlank(dbScope)) {
                  List<Map<String, String>> scope = mapper.readValue(dbScope, ArrayList.class);
                  userRole.put(JsonKey.SCOPE, scope);
                }
              } catch (Exception e) {
                logger.error(
                    context,
                    "Exception because of mapper read value" + userRole.get(JsonKey.SCOPE),
                    e);
              }
            });
    return userRoleList;
  }

  public static Map<String, Object> getUserDetails(
      Map<String, Object> userDetails, Map<String, Object> orgMap, RequestContext context) {
    String userId = (String) userDetails.get(JsonKey.USER_ID);
    logger.info(context, "get user profile method call started user Id : " + userId);
    List<Map<String, Object>> orgList = new ArrayList<Map<String, Object>>();
    orgList.add(orgMap);
    logger.info(context, "Util:getUserDetails: userId = " + userId);
    userDetails.put(JsonKey.ORGANISATIONS, orgList);
    Map<String, Object> rootOrg =
        getOrgDetails((String) userDetails.get(JsonKey.ROOT_ORG_ID), context);
    if (!MapUtils.isEmpty(rootOrg)) {
      userDetails.put(JsonKey.ROOT_ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
    } else {
      userDetails.put(JsonKey.ROOT_ORG_NAME, "");
    }
    // save masked email and phone number
    addMaskEmailAndPhone(userDetails);
    userDetails.remove(JsonKey.PASSWORD);
    addEmailAndPhone(userDetails);
    checkEmailAndPhoneVerified(userDetails);
    List<Map<String, String>> userLocList = new ArrayList<>();
    String profLocation = (String) userDetails.get(JsonKey.PROFILE_LOCATION);
    if (StringUtils.isNotBlank(profLocation)) {
      try {
        userLocList = mapper.readValue(profLocation, List.class);
      } catch (Exception e) {
        logger.info(
            context,
            "Exception occurred while converting profileLocation to List<Map<String,String>>.");
      }
    }
    userDetails.put(JsonKey.PROFILE_LOCATION, userLocList);
    Map<String, Object> userTypeDetail = new HashMap<>();
    String profUserType = (String) userDetails.get(JsonKey.PROFILE_USERTYPE);
    if (StringUtils.isNotBlank(profUserType)) {
      try {
        userTypeDetail = mapper.readValue(profUserType, Map.class);
      } catch (Exception e) {
        logger.info(
            context, "Exception occurred while converting profileUserType to Map<String,String>.");
      }
    }
    userDetails.put(JsonKey.PROFILE_USERTYPE, userTypeDetail);
    return userDetails;
  }

  public static void addEmailAndPhone(Map<String, Object> userDetails) {
    userDetails.put(JsonKey.PHONE, userDetails.remove(JsonKey.ENC_PHONE));
    userDetails.put(JsonKey.EMAIL, userDetails.remove(JsonKey.ENC_EMAIL));
  }

  public static void checkEmailAndPhoneVerified(Map<String, Object> userDetails) {
    if (null != userDetails.get(JsonKey.FLAGS_VALUE)) {
      int flagsValue = Integer.parseInt(userDetails.get(JsonKey.FLAGS_VALUE).toString());
      Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(flagsValue);
      userDetails.putAll(userFlagMap);
    }
  }

  public static void addMaskEmailAndPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    userMap.put(JsonKey.ENC_PHONE, phone);
    userMap.put(JsonKey.ENC_EMAIL, email);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.PHONE, maskingService.maskPhone(decService.decryptData(phone, null)));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.EMAIL, maskingService.maskEmail(decService.decryptData(email, null)));
    }
  }

  public static List<Map<String, Object>> getUserOrgDetails(String userId, RequestContext context) {
    List<Map<String, Object>> userOrgList = new ArrayList<>();
    List<Map<String, Object>> userOrgDataList;
    try {
      List<String> userIds = new ArrayList<>();
      userIds.add(userId);
      DbInfo orgUsrDbInfo = dbInfoMap.get(JsonKey.USER_ORG_DB);
      Response result =
          cassandraOperation.getRecordsByPrimaryKeys(
              orgUsrDbInfo.getKeySpace(),
              orgUsrDbInfo.getTableName(),
              userIds,
              JsonKey.USER_ID,
              context);
      userOrgDataList = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
      userOrgDataList
          .stream()
          .forEach(
              (dataMap) -> {
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
        DbInfo orgDbInfo = dbInfoMap.get(JsonKey.ORG_DB);
        Response orgResult =
            cassandraOperation.getPropertiesValueById(
                orgDbInfo.getKeySpace(),
                orgDbInfo.getTableName(),
                organisationIds,
                fields,
                context);
        List<Map<String, Object>> orgDataList =
            (List<Map<String, Object>>) orgResult.get(JsonKey.RESPONSE);
        Map<String, Map<String, Object>> orgInfoMap = new HashMap<>();
        orgDataList.stream().forEach(org -> orgInfoMap.put((String) org.get(JsonKey.ID), org));
        for (Map<String, Object> userOrg : userOrgList) {
          Map<String, Object> orgMap = orgInfoMap.get(userOrg.get(JsonKey.ORGANISATION_ID));
          userOrg.put(JsonKey.ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
          userOrg.remove(JsonKey.ROLES);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return userOrgList;
  }

  public static Map<String, Object> getUserDefaultValue() {
    Map<String, Object> user = new HashMap<>();
    user.put("avatar", null);
    user.put("gender", null);
    user.put("grade", null);
    user.put("language", null);
    user.put("lastLoginTime", null);
    user.put("location", null);
    user.put("profileSummary", null);
    user.put("profileVisibility", null);
    user.put("tempPassword", null);
    user.put("thumbnail", null);
    user.put("registryId", null);
    user.put("accesscode", null);
    user.put("subject", null);
    user.put("webPages", null);
    user.put("currentLoginTime", null);
    user.put("password", null);
    user.put("loginId", null);
    user.put(JsonKey.EMAIL_VERIFIED, true);
    user.put(JsonKey.PHONE_VERIFIED, true);
    return user;
  }

  public static Map<String, Object> getOrgDefaultValue() {
    Map<String, Object> org = new HashMap<>();
    org.put("dateTime", null);
    org.put("preferredLanguage", null);
    org.put("approvedBy", null);
    org.put("addressId", null);
    org.put("approvedDate", null);
    org.put("communityId", null);
    org.put("homeUrl", null);
    org.put("imgUrl", null);
    org.put("isApproved", null);
    org.put("locationId", null);
    org.put("noOfMembers", null);
    org.put("orgCode", null);
    org.put("theme", null);
    org.put("thumbnail", null);
    org.put("isDefault", null);
    org.put("parentOrgId", null);
    org.put("orgTypeId", null);
    org.put("orgType", null);
    return org;
  }
}
