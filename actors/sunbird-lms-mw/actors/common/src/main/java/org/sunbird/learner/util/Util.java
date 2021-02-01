package org.sunbird.learner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.HttpClientUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.OrgStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.url.URLShortner;
import org.sunbird.common.models.util.url.URLShortnerImpl;
import org.sunbird.common.quartz.scheduler.SchedulerManager;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.KeycloakRequiredActionLinkUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.User;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.SMSFactory;
import scala.concurrent.Future;

/**
 * Utility class for actors
 *
 * @author arvind .
 */
public final class Util {
  private static LoggerUtil logger = new LoggerUtil(Util.class);

  public static final Map<String, DbInfo> dbInfoMap = new HashMap<>();
  private static PropertiesCache propertiesCache = PropertiesCache.getInstance();
  public static final int DEFAULT_ELASTIC_DATA_LIMIT = 10000;
  public static final String KEY_SPACE_NAME = "sunbird";
  private static Properties prop = new Properties();
  private static Map<String, String> headers = new HashMap<>();
  private static Map<Integer, List<Integer>> orgStatusTransition = new HashMap<>();
  private static final String SUNBIRD_WEB_URL = "sunbird_web_url";
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static EncryptionService encryptionService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private static DecryptionService decService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getDecryptionServiceInstance(
          null);
  private static DataMaskingService maskingService =
      org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getMaskingServiceInstance(
          null);
  private static ObjectMapper mapper = new ObjectMapper();
  private static ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);

  static {
    initializeOrgStatusTransition();
    initializeDBProperty(); // EkStep HttpClient headers init
    headers.put("content-type", "application/json");
    headers.put("accept", "application/json");
    new Thread(() -> SchedulerManager.getInstance()).start();
  }

  private Util() {}

  /**
   * This method will a map of organization state transaction. which will help us to move the
   * organization status from one Valid state to another state.
   */
  private static void initializeOrgStatusTransition() {
    orgStatusTransition.put(
        OrgStatus.ACTIVE.getValue(),
        Arrays.asList(
            OrgStatus.ACTIVE.getValue(),
            OrgStatus.INACTIVE.getValue(),
            OrgStatus.BLOCKED.getValue(),
            OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        OrgStatus.INACTIVE.getValue(),
        Arrays.asList(OrgStatus.ACTIVE.getValue(), OrgStatus.INACTIVE.getValue()));
    orgStatusTransition.put(
        OrgStatus.BLOCKED.getValue(),
        Arrays.asList(
            OrgStatus.ACTIVE.getValue(),
            OrgStatus.BLOCKED.getValue(),
            OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        OrgStatus.RETIRED.getValue(), Arrays.asList(OrgStatus.RETIRED.getValue()));
    orgStatusTransition.put(
        null,
        Arrays.asList(
            OrgStatus.ACTIVE.getValue(),
            OrgStatus.INACTIVE.getValue(),
            OrgStatus.BLOCKED.getValue(),
            OrgStatus.RETIRED.getValue()));
  }

  /** This method will initialize the cassandra data base property */
  private static void initializeDBProperty() {
    // setting db info (keyspace , table) into static map
    // this map will be used during cassandra data base interaction.
    // this map will have each DB name and it's corresponding keyspace and table
    // name.
    dbInfoMap.put(JsonKey.USER_DB, getDbInfoObject(KEY_SPACE_NAME, "user"));
    dbInfoMap.put(JsonKey.USER_AUTH_DB, getDbInfoObject(KEY_SPACE_NAME, "user_auth"));
    dbInfoMap.put(JsonKey.ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "organisation"));
    dbInfoMap.put(JsonKey.ADDRESS_DB, getDbInfoObject(KEY_SPACE_NAME, "address"));
    dbInfoMap.put(JsonKey.EDUCATION_DB, getDbInfoObject(KEY_SPACE_NAME, "user_education"));
    dbInfoMap.put(JsonKey.JOB_PROFILE_DB, getDbInfoObject(KEY_SPACE_NAME, "user_job_profile"));
    dbInfoMap.put(JsonKey.USR_EXT_ID_DB, getDbInfoObject(KEY_SPACE_NAME, "user_external_identity"));

    dbInfoMap.put(JsonKey.ORG_MAP_DB, getDbInfoObject(KEY_SPACE_NAME, "org_mapping"));
    dbInfoMap.put(JsonKey.ORG_TYPE_DB, getDbInfoObject(KEY_SPACE_NAME, "org_type"));
    dbInfoMap.put(JsonKey.ROLE, getDbInfoObject(KEY_SPACE_NAME, "role"));
    dbInfoMap.put(JsonKey.MASTER_ACTION, getDbInfoObject(KEY_SPACE_NAME, "master_action"));
    dbInfoMap.put(JsonKey.URL_ACTION, getDbInfoObject(KEY_SPACE_NAME, "url_action"));
    dbInfoMap.put(JsonKey.ACTION_GROUP, getDbInfoObject(KEY_SPACE_NAME, "action_group"));
    dbInfoMap.put(JsonKey.USER_ACTION_ROLE, getDbInfoObject(KEY_SPACE_NAME, "user_action_role"));
    dbInfoMap.put(JsonKey.ROLE_GROUP, getDbInfoObject(KEY_SPACE_NAME, "role_group"));
    dbInfoMap.put(JsonKey.USER_ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "user_organisation"));
    dbInfoMap.put(JsonKey.BULK_OP_DB, getDbInfoObject(KEY_SPACE_NAME, "bulk_upload_process"));
    dbInfoMap.put(JsonKey.REPORT_TRACKING_DB, getDbInfoObject(KEY_SPACE_NAME, "report_tracking"));
    dbInfoMap.put(JsonKey.USER_NOTES_DB, getDbInfoObject(KEY_SPACE_NAME, "user_notes"));
    dbInfoMap.put(JsonKey.MEDIA_TYPE_DB, getDbInfoObject(KEY_SPACE_NAME, "media_type"));
    dbInfoMap.put(JsonKey.USER_SKILL_DB, getDbInfoObject(KEY_SPACE_NAME, "user_skills"));
    dbInfoMap.put(JsonKey.SKILLS_LIST_DB, getDbInfoObject(KEY_SPACE_NAME, "skills"));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_DB, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference"));
    dbInfoMap.put(JsonKey.GEO_LOCATION_DB, getDbInfoObject(KEY_SPACE_NAME, "geo_location"));

    dbInfoMap.put(JsonKey.CLIENT_INFO_DB, getDbInfoObject(KEY_SPACE_NAME, "client_info"));
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
  }

  /**
   * This method will take org current state and next state and check is it possible to move
   * organization from current state to next state if possible to move then return true else false.
   *
   * @param currentState String
   * @param nextState String
   * @return boolean
   */
  @SuppressWarnings("rawtypes")
  public static boolean checkOrgStatusTransition(Integer currentState, Integer nextState) {
    List list = orgStatusTransition.get(currentState);
    if (null == list) {
      return false;
    }
    return list.contains(nextState);
  }

  /**
   * This method will check the cassandra data base connection. first it will try to established the
   * data base connection from provided environment variable , if environment variable values are
   * not set then connection will be established from property file.
   */
  public static void checkCassandraDbConnections() {
    CassandraConnectionManager cassandraConnectionManager =
        CassandraConnectionMngrFactory.getInstance();
    String nodes = System.getenv(JsonKey.SUNBIRD_CASSANDRA_IP);
    String[] hosts = null;
    if (StringUtils.isNotBlank(nodes)) {
      hosts = nodes.split(",");
    } else {
      hosts = new String[] {"localhost"};
    }
    cassandraConnectionManager.createConnection(hosts);
  }

  public static String getProperty(String key) {
    return prop.getProperty(key);
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
  @SuppressWarnings("unchecked")
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

  /**
   * if Object is null then it will return true else false.
   *
   * @param obj Object
   * @return boolean
   */
  public static boolean isNull(Object obj) {
    return null == obj ? true : false;
  }

  /**
   * if Object is not null then it will return true else false.
   *
   * @param obj Object
   * @return boolean
   */
  public static boolean isNotNull(Object obj) {
    return null != obj ? true : false;
  }

  /**
   * This method will provide user name based on user id if user not found then it will return null.
   *
   * @param userId String
   * @return String
   */
  @SuppressWarnings("unchecked")
  public static String getUserNamebyUserId(String userId, RequestContext context) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo userdbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response result =
        cassandraOperation.getRecordById(
            userdbInfo.getKeySpace(), userdbInfo.getTableName(), userId, context);
    List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    if (!(list.isEmpty())) {
      return (String) (list.get(0).get(JsonKey.USERNAME));
    }
    return null;
  }

  /**
   * This method will provide user details map based on user id if user not found then it will
   * return null.
   *
   * @param userId userId of the user
   * @return userDbRecord of the user from cassandra
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> getUserbyUserId(String userId, RequestContext context) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo userdbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response result =
        cassandraOperation.getRecordById(
            userdbInfo.getKeySpace(), userdbInfo.getTableName(), userId, context);
    List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    if (!(list.isEmpty())) {
      return list.get(0);
    }
    return null;
  }

  public static String getHashTagIdFromOrgId(String orgId, RequestContext context) {
    String hashTagId = "";
    Map<String, Object> organisation = getOrgDetails(orgId, context);
    hashTagId =
        StringUtils.isNotEmpty((String) organisation.get(JsonKey.HASHTAGID))
            ? (String) organisation.get(JsonKey.HASHTAGID)
            : (String) organisation.get(JsonKey.ID);
    return hashTagId;
  }

  public static String validateRoles(List<String> roleList) {
    Map<String, Object> roleMap = DataCacheHandler.getRoleMap();
    if (null != roleMap && !roleMap.isEmpty()) {
      for (String role : roleList) {
        if (null == roleMap.get(role.trim())) {
          return role + " is not a valid role.";
        }
      }
    } else {
      logger.info("Roles are not cached.Please Cache it.");
    }
    return JsonKey.SUCCESS;
  }

  /** @param req Map<String,Object> */
  public static boolean registerChannel(Map<String, Object> req, RequestContext context) {
    logger.info(
        context, "channel registration for hashTag Id = " + req.get(JsonKey.HASHTAGID) + "");
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    headerMap.put("user-id", "");
    ProjectUtil.setTraceIdInHeader(headerMap, context);
    String reqString = "";
    String regStatus = "";
    try {
      logger.info(
          context,
          "start call for registering the channel for hashTag id ==" + req.get(JsonKey.HASHTAGID));
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> reqMap = new HashMap<>();
      Map<String, Object> channelMap = new HashMap<>();
      channelMap.put(JsonKey.NAME, req.get(JsonKey.CHANNEL));
      channelMap.put(JsonKey.DESCRIPTION, req.get(JsonKey.DESCRIPTION));
      channelMap.put(JsonKey.CODE, req.get(JsonKey.HASHTAGID));
      if (req.containsKey(JsonKey.LICENSE)
          && StringUtils.isNotBlank((String) req.get(JsonKey.LICENSE))) {
        channelMap.put(JsonKey.DEFAULT_LICENSE, req.get(JsonKey.LICENSE));
      }

      String defaultFramework = (String) req.get(JsonKey.DEFAULT_FRAMEWORK);
      if (StringUtils.isNotBlank(defaultFramework))
        channelMap.put(JsonKey.DEFAULT_FRAMEWORK, defaultFramework);
      reqMap.put(JsonKey.CHANNEL, channelMap);
      map.put(JsonKey.REQUEST, reqMap);

      reqString = mapper.writeValueAsString(map);
      logger.info(
          context, "Util:registerChannel: Channel registration request data = " + reqString);
      regStatus =
          HttpClientUtil.post(
              (ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CHANNEL_REG_API_URL)),
              reqString,
              headerMap);
      logger.info(
          context,
          "end call for channel registration for hashTag id ==" + req.get(JsonKey.HASHTAGID));
    } catch (Exception e) {
      logger.error(
          context, "Exception occurred while registarting channel in ekstep." + e.getMessage(), e);
    }

    return regStatus.contains("OK");
  }

  /** @param req Map<String,Object> */
  public static boolean updateChannel(Map<String, Object> req, RequestContext context) {
    logger.info(context, "channel update for hashTag Id = " + req.get(JsonKey.HASHTAGID) + "");
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    headerMap.put("user-id", "");
    ProjectUtil.setTraceIdInHeader(headers, context);
    String reqString = "";
    String regStatus = "";
    try {
      logger.info(
          context, "start call for updateChannel for hashTag id ==" + req.get(JsonKey.HASHTAGID));
      String ekStepBaseUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(ekStepBaseUrl)) {
        ekStepBaseUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      Map<String, Object> map = new HashMap<>();
      Map<String, Object> reqMap = new HashMap<>();
      Map<String, Object> channelMap = new HashMap<>();
      channelMap.put(JsonKey.NAME, req.get(JsonKey.CHANNEL));
      channelMap.put(JsonKey.DESCRIPTION, req.get(JsonKey.DESCRIPTION));
      channelMap.put(JsonKey.CODE, req.get(JsonKey.HASHTAGID));
      String license = (String) req.get(JsonKey.LICENSE);
      if (StringUtils.isNotBlank(license)) {
        channelMap.put(JsonKey.DEFAULT_LICENSE, license);
      }
      reqMap.put(JsonKey.CHANNEL, channelMap);
      map.put(JsonKey.REQUEST, reqMap);

      reqString = mapper.writeValueAsString(map);

      regStatus =
          HttpClientUtil.patch(
              (ekStepBaseUrl
                      + PropertiesCache.getInstance()
                          .getProperty(JsonKey.EKSTEP_CHANNEL_UPDATE_API_URL))
                  + "/"
                  + req.get(JsonKey.HASHTAGID),
              reqString,
              headerMap);
      logger.info(
          context, "end call for channel update for hashTag id ==" + req.get(JsonKey.HASHTAGID));
    } catch (Exception e) {
      logger.error(
          context, "Exception occurred while updating channel in ekstep. " + e.getMessage(), e);
    }
    return regStatus.contains("SUCCESS");
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

  public static String getSunbirdWebUrlPerTenent(Map<String, Object> userMap) {
    StringBuilder webUrl = new StringBuilder();
    String slug = "";
    if (StringUtils.isBlank(System.getenv(SUNBIRD_WEB_URL))) {
      webUrl.append(propertiesCache.getProperty(SUNBIRD_WEB_URL));
    } else {
      webUrl.append(System.getenv(SUNBIRD_WEB_URL));
    }
    if (!StringUtils.isBlank((String) userMap.get(JsonKey.ROOT_ORG_ID))) {
      Map<String, Object> orgMap = getOrgDetails((String) userMap.get(JsonKey.ROOT_ORG_ID), null);
      slug = (String) orgMap.get(JsonKey.SLUG);
    }
    if (!StringUtils.isBlank(slug)) {
      webUrl.append("/" + slug);
    }
    return webUrl.toString();
  }

  /**
   * As per requirement this page need to be redirect to /resources always.
   *
   * @return url of login page
   */
  public static String getSunbirdLoginUrl() {
    StringBuilder webUrl = new StringBuilder();
    String slug = "/resources";
    if (StringUtils.isBlank(System.getenv(SUNBIRD_WEB_URL))) {
      webUrl.append(propertiesCache.getProperty(SUNBIRD_WEB_URL));
    } else {
      webUrl.append(System.getenv(SUNBIRD_WEB_URL));
    }
    webUrl.append(slug);
    return webUrl.toString();
  }

  public static Map<String, Object> getOrgDetails(String identifier, RequestContext context) {
    DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Response response =
        cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), identifier, context);
    List<Map<String, Object>> res = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != res && !res.isEmpty()) {
      return res.get(0);
    }
    return Collections.emptyMap();
  }

  public static String encryptData(String value) {
    try {
      return encryptionService.encryptData(value, null);
    } catch (Exception e) {
      throw new ProjectCommonException(
          ResponseCode.userDataEncryptionError.getErrorCode(),
          ResponseCode.userDataEncryptionError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }

  /**
   * This method will search in ES for user with given search query
   *
   * @param searchQueryMap Query filters as Map.
   * @param context
   * @return List<User> List of User object.
   */
  public static List<User> searchUser(Map<String, Object> searchQueryMap, RequestContext context) {
    List<User> userList = new ArrayList<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    searchRequestMap.put(JsonKey.FILTERS, searchQueryMap);
    SearchDTO searchDto = Util.createSearchDto(searchRequestMap);
    Future<Map<String, Object>> resultf =
        esService.search(searchDto, ProjectUtil.EsType.user.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultf);
    if (MapUtils.isNotEmpty(result)) {
      List<Map<String, Object>> searchResult =
          (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
      if (CollectionUtils.isNotEmpty(searchResult)) {
        userList =
            searchResult
                .stream()
                .map(s -> mapper.convertValue(s, User.class))
                .collect(Collectors.toList());
      }
    }
    return userList;
  }

  public static String getLoginId(Map<String, Object> userMap) {
    String loginId;
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.CHANNEL))) {
      loginId =
          (String) userMap.get(JsonKey.USERNAME) + "@" + (String) userMap.get(JsonKey.CHANNEL);
    } else {
      loginId = (String) userMap.get(JsonKey.USERNAME);
    }
    return loginId;
  }

  public static void registerUserToOrg(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    reqMap.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    if (null != userMap.get(JsonKey.ROLES)) {
      reqMap.put(JsonKey.ROLES, userMap.get(JsonKey.ROLES));
    }
    reqMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    reqMap.put(JsonKey.IS_DELETED, false);
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

  public static String getChannel(String rootOrgId, RequestContext context) {
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    String channel = null;
    Response resultFrRootOrg =
        cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), rootOrgId, context);
    if (CollectionUtils.isNotEmpty(
        (List<Map<String, Object>>) resultFrRootOrg.get(JsonKey.RESPONSE))) {
      Map<String, Object> rootOrg =
          ((List<Map<String, Object>>) resultFrRootOrg.get(JsonKey.RESPONSE)).get(0);
      channel = (String) rootOrg.get(JsonKey.CHANNEL);
    }
    return channel;
  }

  @SuppressWarnings("unchecked")
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
      registerUserToOrg(userMap, context);
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getUserDetails(String userId, RequestContext context) {
    logger.info(context, "get user profile method call started user Id : " + userId);
    Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response = null;
    List<Map<String, Object>> userList = null;
    Map<String, Object> userDetails = null;
    try {
      response =
          cassandraOperation.getRecordById(
              userDbInfo.getKeySpace(), userDbInfo.getTableName(), userId, context);
      userList = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      logger.info(
          context, "Util:getUserProfile: collecting user data to save for userId : " + userId);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
    String username = "";
    if (!(userList.isEmpty())) {
      userDetails = userList.get(0);
      username = (String) userDetails.get(JsonKey.USERNAME);
      logger.info(context, "Util:getUserDetails: userId = " + userId);
      userDetails.put(JsonKey.ORGANISATIONS, getUserOrgDetails(userId, context));
      Map<String, Object> orgMap =
          getOrgDetails((String) userDetails.get(JsonKey.ROOT_ORG_ID), context);
      if (!MapUtils.isEmpty(orgMap)) {
        userDetails.put(JsonKey.ROOT_ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
      } else {
        userDetails.put(JsonKey.ROOT_ORG_NAME, "");
      }
      // store alltncaccepted as Map Object in ES
      Map<String, Object> allTncAccepted =
          (Map<String, Object>) userDetails.get(JsonKey.ALL_TNC_ACCEPTED);
      if (MapUtils.isNotEmpty(allTncAccepted)) {
        convertTncJsonStringToMapObject(allTncAccepted);
      }
      // save masked email and phone number
      addMaskEmailAndPhone(userDetails);
      userDetails.remove(JsonKey.PASSWORD);
      addEmailAndPhone(userDetails);
      checkEmailAndPhoneVerified(userDetails);

    } else {
      logger.info(
          context,
          "Util:getUserProfile: User data not available to save in ES for userId : " + userId);
    }
    userDetails.put(JsonKey.USERNAME, username);
    return userDetails;
  }

  // Convert Json String tnc format to object to store in Elastic
  private static void convertTncJsonStringToMapObject(Map<String, Object> allTncAccepted) {
    for (Map.Entry<String, Object> tncAccepted : allTncAccepted.entrySet()) {
      String tncType = tncAccepted.getKey();
      Map<String, String> tncAcceptedDetailMap = new HashMap<>();
      try {
        tncAcceptedDetailMap = mapper.readValue((String) tncAccepted.getValue(), Map.class);
        allTncAccepted.put(tncType, tncAcceptedDetailMap);
      } catch (JsonProcessingException e) {
        logger.error("Json Parsing Exception", e);
      }
    }
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
    return userDetails;
  }

  public static void addEmailAndPhone(Map<String, Object> userDetails) {
    userDetails.put(JsonKey.PHONE, userDetails.remove(JsonKey.ENC_PHONE));
    userDetails.put(JsonKey.EMAIL, userDetails.remove(JsonKey.ENC_EMAIL));
  }

  public static void checkEmailAndPhoneVerified(Map<String, Object> userDetails) {
    int flagsValue = Integer.parseInt(userDetails.get(JsonKey.FLAGS_VALUE).toString());
    Map<String, Boolean> userFlagMap = UserFlagUtil.assignUserFlagValues(flagsValue);
    userDetails.putAll(userFlagMap);
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
    List<Map<String, Object>> userOrgDataList = new ArrayList<>();
    List<Map<String, Object>> userOrganisations = new ArrayList<>();
    try {
      List<String> ids = new ArrayList<>();
      ids.add(userId);
      Util.DbInfo orgUsrDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
      Response result =
          cassandraOperation.getRecordsByPrimaryKeys(
              orgUsrDbInfo.getKeySpace(),
              orgUsrDbInfo.getTableName(),
              ids,
              JsonKey.USER_ID,
              context);
      List<Map<String, Object>> userOrgList = new ArrayList<>();
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
        List<String> fields = Arrays.asList(JsonKey.ORG_NAME, JsonKey.PARENT_ORG_ID, JsonKey.ID);

        Future<Map<String, Map<String, Object>>> orgInfoMapF =
            esService.getEsResultByListOfIds(
                organisationIds, fields, EsType.organisation.getTypeName(), context);
        Map<String, Map<String, Object>> orgInfoMap =
            (Map<String, Map<String, Object>>)
                ElasticSearchHelper.getResponseFromFuture(orgInfoMapF);

        for (Map<String, Object> userOrg : userOrgList) {
          Map<String, Object> esOrgMap = orgInfoMap.get(userOrg.get(JsonKey.ORGANISATION_ID));
          esOrgMap.remove(JsonKey.ID);
          userOrg.putAll(esOrgMap);
          userOrganisations.add(userOrg);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return userOrganisations;
  }

  public static Request sendOnboardingMail(Map<String, Object> emailTemplateMap) {
    Request request = null;
    if ((StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      String envName = propertiesCache.getProperty(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      String welcomeSubject = propertiesCache.getProperty(JsonKey.ONBOARDING_MAIL_SUBJECT);
      emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(welcomeSubject, envName));
      List<String> reciptientsMail = new ArrayList<>();
      reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
      emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
      emailTemplateMap.put(
          JsonKey.BODY, propertiesCache.getProperty(JsonKey.ONBOARDING_WELCOME_MAIL_BODY));
      emailTemplateMap.put(JsonKey.NOTE, propertiesCache.getProperty(JsonKey.MAIL_NOTE));
      emailTemplateMap.put(JsonKey.ORG_NAME, envName);
      String welcomeMessage = propertiesCache.getProperty(JsonKey.ONBOARDING_MAIL_MESSAGE);
      emailTemplateMap.put(
          JsonKey.WELCOME_MESSAGE, ProjectUtil.formatMessage(welcomeMessage, envName));

      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "welcome");
      setRequiredActionLink(emailTemplateMap);
      if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK))
          && StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.VERIFY_EMAIL_LINK))) {
        logger.info("Util:sendOnboardingMail: Email not sent as generated link is empty");
        return null;
      }

      request = new Request();
      request.setOperation(BackgroundOperations.emailService.name());
      request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    }
    return request;
  }

  private static void setRequiredActionLink(Map<String, Object> templateMap) {
    String setPasswordLink = (String) templateMap.get(JsonKey.SET_PASSWORD_LINK);
    String verifyEmailLink = (String) templateMap.get(JsonKey.VERIFY_EMAIL_LINK);
    if (StringUtils.isNotBlank(setPasswordLink)) {
      templateMap.put(JsonKey.LINK, setPasswordLink);
      templateMap.put(JsonKey.SET_PW_LINK, "true");
    } else if (StringUtils.isNotBlank(verifyEmailLink)) {
      templateMap.put(JsonKey.LINK, verifyEmailLink);
      templateMap.put(JsonKey.SET_PW_LINK, null);
    }
  }

  public static String getUserRequiredActionLink(
      Map<String, Object> templateMap, boolean isUrlShortRequired, RequestContext context) {
    URLShortner urlShortner = new URLShortnerImpl();
    String redirectUri =
        StringUtils.isNotBlank((String) templateMap.get(JsonKey.REDIRECT_URI))
            ? ((String) templateMap.get(JsonKey.REDIRECT_URI))
            : null;
    logger.info(context, "Util:getUserRequiredActionLink redirectURI = " + redirectUri);
    if (StringUtils.isBlank((String) templateMap.get(JsonKey.PASSWORD))) {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.UPDATE_PASSWORD,
              context);

      templateMap.put(
          JsonKey.SET_PASSWORD_LINK, isUrlShortRequired ? urlShortner.shortUrl(url) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url) : url;

    } else {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.VERIFY_EMAIL,
              context);
      templateMap.put(
          JsonKey.VERIFY_EMAIL_LINK, isUrlShortRequired ? urlShortner.shortUrl(url) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url) : url;
    }
  }

  public static void getUserRequiredActionLink(
      Map<String, Object> templateMap, RequestContext context) {
    getUserRequiredActionLink(templateMap, true, context);
  }

  public static void sendSMS(Map<String, Object> userMap) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PHONE))) {
      String envName = propertiesCache.getProperty(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      setRequiredActionLink(userMap);
      if (StringUtils.isBlank((String) userMap.get(JsonKey.SET_PASSWORD_LINK))
          && StringUtils.isBlank((String) userMap.get(JsonKey.VERIFY_EMAIL_LINK))) {
        logger.info("Util:sendSMS: SMS not sent as generated link is empty");
        return;
      }
      Map<String, String> smsTemplate = new HashMap<>();
      smsTemplate.put("instanceName", envName);
      smsTemplate.put(JsonKey.LINK, (String) userMap.get(JsonKey.LINK));
      smsTemplate.put(JsonKey.SET_PW_LINK, (String) userMap.get(JsonKey.SET_PW_LINK));
      String sms = ProjectUtil.getSMSBody(smsTemplate);
      if (StringUtils.isBlank(sms)) {
        sms = PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_DEFAULT_WELCOME_MSG);
      }
      logger.info("SMS text : " + sms);
      String countryCode = "";
      if (StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
        countryCode =
            PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE);
      } else {
        countryCode = (String) userMap.get(JsonKey.COUNTRY_CODE);
      }
      ISmsProvider smsProvider = SMSFactory.getInstance("91SMS");
      logger.info("SMS text : " + sms + " with phone " + (String) userMap.get(JsonKey.PHONE));
      boolean response = smsProvider.send((String) userMap.get(JsonKey.PHONE), countryCode, sms);
      logger.info("Response from smsProvider : " + response);
      if (response) {
        logger.info("Welcome Message sent successfully to ." + (String) userMap.get(JsonKey.PHONE));
      } else {
        logger.info("Welcome Message failed for ." + (String) userMap.get(JsonKey.PHONE));
      }
    }
  }

  public static Request sendResetPassMail(Map<String, Object> emailTemplateMap) {
    Request request = null;
    if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK))) {
      logger.info("Util:sendResetPassMail: Email not sent as generated link is empty");
      return null;
    } else if ((StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.EMAIL)))) {
      String envName = propertiesCache.getProperty(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      String welcomeSubject = propertiesCache.getProperty(JsonKey.SUNBIRD_RESET_PASS_MAIL_SUBJECT);
      emailTemplateMap.put(JsonKey.SUBJECT, ProjectUtil.formatMessage(welcomeSubject, envName));
      List<String> reciptientsMail = new ArrayList<>();
      reciptientsMail.add((String) emailTemplateMap.get(JsonKey.EMAIL));
      emailTemplateMap.put(JsonKey.RECIPIENT_EMAILS, reciptientsMail);
      emailTemplateMap.put(JsonKey.ORG_NAME, envName);
      emailTemplateMap.put(JsonKey.EMAIL_TEMPLATE_TYPE, "resetPassword");
      setRequiredActionLink(emailTemplateMap);
    } else if (StringUtils.isNotBlank((String) emailTemplateMap.get(JsonKey.PHONE))) {
      emailTemplateMap.put(
          JsonKey.BODY,
          ProjectUtil.formatMessage(
              propertiesCache.getProperty("sunbird_reset_pass_msg"),
              (String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK)));
      emailTemplateMap.put(JsonKey.MODE, "SMS");
      List<String> phoneList = new ArrayList<String>();
      phoneList.add((String) emailTemplateMap.get(JsonKey.PHONE));
      emailTemplateMap.put(JsonKey.RECIPIENT_PHONES, phoneList);
    } else {
      logger.info("Util:sendResetPassMail: requested data is neither having email nor phone ");
      return null;
    }
    request = new Request();
    request.setOperation(BackgroundOperations.emailService.name());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    return request;
  }
}
