package org.sunbird.learner.util;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sunbird.actor.background.BackgroundOperations;
import org.sunbird.actorutil.systemsettings.SystemSettingClient;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsIndex;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.models.util.ProjectUtil.OrgStatus;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.models.util.datasecurity.DataMaskingService;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.url.URLShortner;
import org.sunbird.common.models.util.url.URLShortnerImpl;
import org.sunbird.common.quartz.scheduler.SchedulerManager;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.common.services.ProfileCompletenessService;
import org.sunbird.common.services.impl.ProfileCompletenessFactory;
import org.sunbird.common.util.ConfigUtil;
import org.sunbird.common.util.KeycloakRequiredActionLinkUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.systemsetting.SystemSetting;
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
    loadPropertiesFile();
    initializeOrgStatusTransition();
    initializeDBProperty(); // EkStep HttpClient headers init
    headers.put("content-type", "application/json");
    headers.put("accept", "application/json");
    new Thread(
            new Runnable() {

              @Override
              public void run() {
                SchedulerManager.getInstance();
              }
            })
        .start();
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
    dbInfoMap.put(JsonKey.USR_ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "user_org"));
    dbInfoMap.put(JsonKey.USR_EXT_ID_DB, getDbInfoObject(KEY_SPACE_NAME, "user_external_identity"));

    dbInfoMap.put(JsonKey.ORG_MAP_DB, getDbInfoObject(KEY_SPACE_NAME, "org_mapping"));
    dbInfoMap.put(JsonKey.ORG_TYPE_DB, getDbInfoObject(KEY_SPACE_NAME, "org_type"));
    dbInfoMap.put(JsonKey.ROLE, getDbInfoObject(KEY_SPACE_NAME, "role"));
    dbInfoMap.put(JsonKey.MASTER_ACTION, getDbInfoObject(KEY_SPACE_NAME, "master_action"));
    dbInfoMap.put(JsonKey.URL_ACTION, getDbInfoObject(KEY_SPACE_NAME, "url_action"));
    dbInfoMap.put(JsonKey.ACTION_GROUP, getDbInfoObject(KEY_SPACE_NAME, "action_group"));
    dbInfoMap.put(JsonKey.USER_ACTION_ROLE, getDbInfoObject(KEY_SPACE_NAME, "user_action_role"));
    dbInfoMap.put(JsonKey.ROLE_GROUP, getDbInfoObject(KEY_SPACE_NAME, "role_group"));
    dbInfoMap.put(JsonKey.USER_ORG_DB, getDbInfoObject(KEY_SPACE_NAME, "user_org"));
    dbInfoMap.put(JsonKey.BULK_OP_DB, getDbInfoObject(KEY_SPACE_NAME, "bulk_upload_process"));
    dbInfoMap.put(JsonKey.REPORT_TRACKING_DB, getDbInfoObject(KEY_SPACE_NAME, "report_tracking"));
    dbInfoMap.put(JsonKey.BADGES_DB, getDbInfoObject(KEY_SPACE_NAME, "badge"));
    dbInfoMap.put(JsonKey.USER_BADGES_DB, getDbInfoObject(KEY_SPACE_NAME, "user_badge"));
    dbInfoMap.put(JsonKey.USER_NOTES_DB, getDbInfoObject(KEY_SPACE_NAME, "user_notes"));
    dbInfoMap.put(JsonKey.MEDIA_TYPE_DB, getDbInfoObject(KEY_SPACE_NAME, "media_type"));
    dbInfoMap.put(JsonKey.USER_SKILL_DB, getDbInfoObject(KEY_SPACE_NAME, "user_skills"));
    dbInfoMap.put(JsonKey.SKILLS_LIST_DB, getDbInfoObject(KEY_SPACE_NAME, "skills"));
    dbInfoMap.put(
        JsonKey.TENANT_PREFERENCE_DB, getDbInfoObject(KEY_SPACE_NAME, "tenant_preference"));
    dbInfoMap.put(JsonKey.GEO_LOCATION_DB, getDbInfoObject(KEY_SPACE_NAME, "geo_location"));

    dbInfoMap.put(JsonKey.CLIENT_INFO_DB, getDbInfoObject(KEY_SPACE_NAME, "client_info"));
    dbInfoMap.put(JsonKey.SYSTEM_SETTINGS_DB, getDbInfoObject(KEY_SPACE_NAME, "system_settings"));

    dbInfoMap.put(
        BadgingJsonKey.USER_BADGE_ASSERTION_DB,
        getDbInfoObject(KEY_SPACE_NAME, "user_badge_assertion"));
    dbInfoMap.put(JsonKey.USER_CERT, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_CERT));
    dbInfoMap.put(JsonKey.USER_FEED_DB, getDbInfoObject(KEY_SPACE_NAME, JsonKey.USER_FEED_DB));
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
  public static void checkCassandraDbConnections(String keySpace) {

    PropertiesCache propertiesCache = PropertiesCache.getInstance();

    String cassandraMode = propertiesCache.getProperty(JsonKey.SUNBIRD_CASSANDRA_MODE);
    if (StringUtils.isBlank(cassandraMode)
        || cassandraMode.equalsIgnoreCase(JsonKey.EMBEDDED_MODE)) {

      // configure the Embedded mode and return true here ....
      CassandraConnectionManager cassandraConnectionManager =
          CassandraConnectionMngrFactory.getObject(cassandraMode);
      boolean result =
          cassandraConnectionManager.createConnection(null, null, null, null, keySpace);
      if (result) {
        ProjectLogger.log(
            "CONNECTION CREATED SUCCESSFULLY FOR IP:" + " : KEYSPACE :" + keySpace,
            LoggerEnum.INFO.name());
      } else {
        ProjectLogger.log("CONNECTION CREATION FAILED FOR IP: " + " : KEYSPACE :" + keySpace);
      }

    } else if (cassandraMode.equalsIgnoreCase(JsonKey.STANDALONE_MODE)) {
      if (readConfigFromEnv(keySpace)) {
        ProjectLogger.log("db connection is created from System env variable.");
        return;
      }
      CassandraConnectionManager cassandraConnectionManager =
          CassandraConnectionMngrFactory.getObject(JsonKey.STANDALONE_MODE);
      String[] ipList = prop.getProperty(JsonKey.DB_IP).split(",");
      String[] portList = prop.getProperty(JsonKey.DB_PORT).split(",");
      // String[] keyspaceList = prop.getProperty(JsonKey.DB_KEYSPACE).split(",");

      String userName = prop.getProperty(JsonKey.DB_USERNAME);
      String password = prop.getProperty(JsonKey.DB_PASSWORD);
      for (int i = 0; i < ipList.length; i++) {
        String ip = ipList[i];
        String port = portList[i];
        // Reading the same keyspace which is passed in the method
        // String keyspace = keyspaceList[i];

        try {

          boolean result =
              cassandraConnectionManager.createConnection(ip, port, userName, password, keySpace);
          if (result) {
            ProjectLogger.log(
                "CONNECTION CREATED SUCCESSFULLY FOR IP: " + ip + " : KEYSPACE :" + keySpace,
                LoggerEnum.INFO.name());
          } else {
            ProjectLogger.log(
                "CONNECTION CREATION FAILED FOR IP: " + ip + " : KEYSPACE :" + keySpace);
          }

        } catch (ProjectCommonException ex) {
          ProjectLogger.log(ex.getMessage(), ex);
        }
      }
    }
  }

  /**
   * This method will read the configuration from System variable.
   *
   * @return boolean
   */
  public static boolean readConfigFromEnv(String keyspace) {
    boolean response = false;
    String ips = System.getenv(JsonKey.SUNBIRD_CASSANDRA_IP);
    String envPort = System.getenv(JsonKey.SUNBIRD_CASSANDRA_PORT);
    CassandraConnectionManager cassandraConnectionManager =
        CassandraConnectionMngrFactory.getObject(JsonKey.STANDALONE_MODE);

    if (StringUtils.isBlank(ips) || StringUtils.isBlank(envPort)) {
      ProjectLogger.log("Configuration value is not coming form System variable.");
      return response;
    }
    String[] ipList = ips.split(",");
    String[] portList = envPort.split(",");
    String userName = System.getenv(JsonKey.SUNBIRD_CASSANDRA_USER_NAME);
    String password = System.getenv(JsonKey.SUNBIRD_CASSANDRA_PASSWORD);
    for (int i = 0; i < ipList.length; i++) {
      String ip = ipList[i];
      String port = portList[i];
      try {
        boolean result =
            cassandraConnectionManager.createConnection(ip, port, userName, password, keyspace);
        if (result) {
          response = true;
          ProjectLogger.log(
              "CONNECTION CREATED SUCCESSFULLY FOR IP: " + ip + " : KEYSPACE :" + keyspace,
              LoggerEnum.INFO.name());
        } else {
          ProjectLogger.log(
              "CONNECTION CREATION FAILED FOR IP: " + ip + " : KEYSPACE :" + keyspace,
              LoggerEnum.INFO.name());
        }
      } catch (ProjectCommonException ex) {
        ProjectLogger.log(
            "Util:readConfigFromEnv: Exception occurred with message = " + ex.getMessage(),
            LoggerEnum.ERROR);
      }
    }
    if (!response) {
      throw new ProjectCommonException(
          ResponseCode.invaidConfiguration.getErrorCode(),
          ResponseCode.invaidConfiguration.getErrorCode(),
          ResponseCode.SERVER_ERROR.hashCode());
    }
    return response;
  }

  /** This method will load the db config properties file. */
  private static void loadPropertiesFile() {

    InputStream input = null;

    try {
      input = Util.class.getClassLoader().getResourceAsStream("dbconfig.properties");
      // load a properties file
      prop.load(input);
    } catch (IOException ex) {
      ProjectLogger.log(ex.getMessage(), ex);
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          ProjectLogger.log(e.getMessage(), e);
        }
      }
    }
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
    private String userName;
    private String password;
    private String ip;
    private String port;

    /**
     * @param keySpace
     * @param tableName
     * @param userName
     * @param password
     */
    DbInfo(
        String keySpace,
        String tableName,
        String userName,
        String password,
        String ip,
        String port) {
      this.keySpace = keySpace;
      this.tableName = tableName;
      this.userName = userName;
      this.password = password;
      this.ip = ip;
      this.port = port;
    }

    /** No-arg constructor */
    DbInfo() {}

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DbInfo) {
        DbInfo ob = (DbInfo) obj;
        if (this.ip.equals(ob.getIp())
            && this.port.equals(ob.getPort())
            && this.keySpace.equals(ob.getKeySpace())) {
          return true;
        }
      }
      return false;
    }

    @Override
    public int hashCode() {
      return 1;
    }

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

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getIp() {
      return ip;
    }

    public void setIp(String ip) {
      this.ip = ip;
    }

    public String getPort() {
      return port;
    }

    public void setPort(String port) {
      this.port = port;
    }
  }

  /**
   * This method will take the map<String,Object> and list<Stirng> of keys. it will remove all the
   * keys from the source map.
   *
   * @param map Map<String, Object>
   * @param keys List<String>
   */
  public static void removeAttributes(Map<String, Object> map, List<String> keys) {

    if (null != map && null != keys) {
      for (String key : keys) {
        map.remove(key);
      }
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
   * This method will make a call to EKStep content search api and final response will be appended
   * with same requested map, with key "contents". Requester can read this key to collect the
   * response.
   *
   * @param section String, Object>
   */
  public static void getContentData(Map<String, Object> section) {
    String response = "";
    JSONObject data;
    JSONObject jObject;
    try {
      String baseSearchUrl = ProjectUtil.getConfigValue(JsonKey.SEARCH_SERVICE_API_BASE_URL);
      headers.put(
          JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.EKSTEP_AUTHORIZATION));
      if (StringUtils.isBlank(headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
      }
      response =
          HttpUtil.sendPostRequest(
              baseSearchUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CONTENT_SEARCH_URL),
              (String) section.get(JsonKey.SEARCH_QUERY),
              headers);
      jObject = new JSONObject(response);
      data = jObject.getJSONObject(JsonKey.RESULT);
      JSONArray contentArray = data.getJSONArray(JsonKey.CONTENT);
      section.put(JsonKey.CONTENTS, mapper.readValue(contentArray.toString(), Object[].class));
    } catch (IOException | JSONException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
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
  public static String getUserNamebyUserId(String userId) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo userdbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response result =
        cassandraOperation.getRecordById(
            userdbInfo.getKeySpace(), userdbInfo.getTableName(), userId);
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
  public static Map<String, Object> getUserbyUserId(String userId) {
    CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    Util.DbInfo userdbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response result =
        cassandraOperation.getRecordById(
            userdbInfo.getKeySpace(), userdbInfo.getTableName(), userId);
    List<Map<String, Object>> list = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    if (!(list.isEmpty())) {
      return list.get(0);
    }
    return null;
  }

  public static String getHashTagIdFromOrgId(String orgId) {
    String hashTagId = "";
    Map<String, Object> organisation = getOrgDetails(orgId);
    hashTagId =
        StringUtils.isNotEmpty((String) organisation.get(JsonKey.HASHTAGID))
            ? (String) organisation.get(JsonKey.HASHTAGID)
            : (String) organisation.get(JsonKey.ID);
    return hashTagId;
  }

  /**
   * This method will validate channel and return the id of organization associated with this
   * channel.
   *
   * @param channel value of channel of an organization
   * @return Id of Root organization.
   */
  public static String getRootOrgIdFromChannel(String channel) {
    return getRootOrgIdOrNameFromChannel(channel, false);
  }

  public static String getRootOrgIdOrNameFromChannel(String channel, boolean isName) {
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
    Map<String, Object> esResult =
        elasticSearchComplexSearch(
            filters, EsIndex.sunbird.getIndexName(), EsType.organisation.getTypeName());
    if (MapUtils.isNotEmpty(esResult)
        && CollectionUtils.isNotEmpty((List) esResult.get(JsonKey.CONTENT))) {
      Map<String, Object> esContent =
          ((List<Map<String, Object>>) esResult.get(JsonKey.CONTENT)).get(0);
      if (!isName) return (String) esContent.get(JsonKey.ID);
      else return (String) esContent.get(JsonKey.ORG_NAME);
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

  private static Map<String, Object> elasticSearchComplexSearch(
      Map<String, Object> filters, String index, String type) {

    SearchDTO searchDTO = new SearchDTO();
    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);

    Future<Map<String, Object>> mapF = esService.search(searchDTO, type);
    return (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(mapF);
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
      ProjectLogger.log("Roles are not cached.Please Cache it.");
    }
    return JsonKey.SUCCESS;
  }

  /** @param req Map<String,Object> */
  public static boolean registerChannel(Map<String, Object> req) {
    ProjectLogger.log(
        "channel registration for hashTag Id = " + req.get(JsonKey.HASHTAGID) + "",
        LoggerEnum.INFO.name());
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
    String reqString = "";
    String regStatus = "";
    try {
      ProjectLogger.log(
          "start call for registering the channel for hashTag id ==" + req.get(JsonKey.HASHTAGID),
          LoggerEnum.INFO.name());
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
      ProjectLogger.log(
          "Util:registerChannel: Channel registration request data = " + reqString,
          LoggerEnum.DEBUG.name());
      regStatus =
          HttpUtil.sendPostRequest(
              (ekStepBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_CHANNEL_REG_API_URL)),
              reqString,
              headerMap);
      ProjectLogger.log(
          "end call for channel registration for hashTag id ==" + req.get(JsonKey.HASHTAGID),
          LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(
          "Exception occurred while registarting channel in ekstep." + e.getMessage(),
          LoggerEnum.ERROR.name());
    }

    return regStatus.contains("OK");
  }

  /** @param req Map<String,Object> */
  public static boolean updateChannel(Map<String, Object> req) {
    ProjectLogger.log(
        "channel update for hashTag Id = " + req.get(JsonKey.HASHTAGID) + "",
        LoggerEnum.INFO.name());
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
    String reqString = "";
    String regStatus = "";
    try {
      ProjectLogger.log(
          "start call for updateChannel for hashTag id ==" + req.get(JsonKey.HASHTAGID),
          LoggerEnum.INFO.name());
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
          HttpUtil.sendPatchRequest(
              (ekStepBaseUrl
                      + PropertiesCache.getInstance()
                          .getProperty(JsonKey.EKSTEP_CHANNEL_UPDATE_API_URL))
                  + "/"
                  + req.get(JsonKey.HASHTAGID),
              reqString,
              headerMap);
      ProjectLogger.log(
          "end call for channel update for hashTag id ==" + req.get(JsonKey.HASHTAGID),
          LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(
          "Exception occurred while updating channel in ekstep. " + e.getMessage(),
          LoggerEnum.ERROR.name());
    }

    return regStatus.contains("SUCCESS");
  }

  public static void initializeContext(Request actorMessage, String env) {

    ExecutionContext context = ExecutionContext.getCurrent();
    Map<String, Object> requestContext = null;
    if (actorMessage.getContext().get(JsonKey.TELEMETRY_CONTEXT) != null) {
      // means request context is already set by some other actor ...
      requestContext =
          (Map<String, Object>) actorMessage.getContext().get(JsonKey.TELEMETRY_CONTEXT);
    } else {
      requestContext = new HashMap<>();
      // request level info ...
      Map<String, Object> req = actorMessage.getRequest();
      String requestedBy = (String) req.get(JsonKey.REQUESTED_BY);
      String actorId = getKeyFromContext(JsonKey.ACTOR_ID, actorMessage);
      String actorType = getKeyFromContext(JsonKey.ACTOR_TYPE, actorMessage);
      String appId = getKeyFromContext(JsonKey.APP_ID, actorMessage);
      env = StringUtils.isNotBlank(env) ? env : "";
      String deviceId = getKeyFromContext(JsonKey.DEVICE_ID, actorMessage);
      String channel = getKeyFromContext(JsonKey.CHANNEL, actorMessage);
      requestContext.put(JsonKey.CHANNEL, channel);
      requestContext.put(JsonKey.ACTOR_ID, actorId);
      requestContext.put(JsonKey.ACTOR_TYPE, actorType);
      requestContext.put(JsonKey.APP_ID, appId);
      requestContext.put(JsonKey.ENV, env);
      requestContext.put(JsonKey.REQUEST_TYPE, JsonKey.API_CALL);
      requestContext.put(JsonKey.REQUEST_ID, actorMessage.getRequestId());
      requestContext.put(JsonKey.DEVICE_ID, deviceId);

      if (JsonKey.USER.equalsIgnoreCase(
          (String) actorMessage.getContext().get(JsonKey.ACTOR_TYPE))) {
        // assign rollup of user ...
        Future<Map<String, Object>> resultF =
            esService.getDataByIdentifier(
                EsType.user.getTypeName(),
                (String) actorMessage.getContext().get(JsonKey.REQUESTED_BY));
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

        if (result != null) {
          String rootOrgId = (String) result.get(JsonKey.ROOT_ORG_ID);

          if (StringUtils.isNotBlank(rootOrgId)) {
            Map<String, String> rollup = new HashMap<>();

            rollup.put("l1", rootOrgId);
            requestContext.put(JsonKey.ROLLUP, rollup);
          }
        }
      }
      context.setRequestContext(requestContext);
      // and global context will be set at the time of creation of thread local
      // automatically ...
    }
  }

  public static String getKeyFromContext(String key, Request actorMessage) {
    return actorMessage.getContext() != null && actorMessage.getContext().containsKey(key)
        ? (String) actorMessage.getContext().get(key)
        : "";
  }

  public static void initializeContextForSchedulerJob(
      String actorType, String actorId, String environment) {

    ExecutionContext context = ExecutionContext.getCurrent();
    Map<String, Object> requestContext = new HashMap<>();
    requestContext.put(JsonKey.CHANNEL, JsonKey.DEFAULT_ROOT_ORG_ID);
    requestContext.put(JsonKey.ACTOR_ID, actorId);
    requestContext.put(JsonKey.ACTOR_TYPE, actorType);
    requestContext.put(JsonKey.ENV, environment);
    context.setRequestContext(requestContext);
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
      Map<String, Object> orgMap = getOrgDetails((String) userMap.get(JsonKey.ROOT_ORG_ID));
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

  public static Map<String, Object> getOrgDetails(String identifier) {
    DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    Response response =
        cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), identifier);
    List<Map<String, Object>> res = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (null != res && !res.isEmpty()) {
      return res.get(0);
    }
    return Collections.emptyMap();
  }

  // --------------------------------------------------------
  // user utility methods

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

  /**
   * This method will check for user in our application with userName@channel i.e loginId value.
   *
   * @param user User object.
   */
  public static void checkUserExistOrNot(User user) {
    Map<String, Object> searchQueryMap = new HashMap<>();
    // loginId is encrypted in our application
    searchQueryMap.put(JsonKey.LOGIN_ID, getEncryptedData(user.getLoginId()));
    if (CollectionUtils.isNotEmpty(searchUser(searchQueryMap))) {
      throw new ProjectCommonException(
          ResponseCode.userAlreadyExists.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.userAlreadyExists.getErrorMessage(), JsonKey.USERNAME),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * This method will check the uniqueness for externalId and provider combination.
   *
   * @param user
   */
  public static void checkExternalIdUniqueness(User user, String operation) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      for (Map<String, String> externalId : user.getExternalIds()) {
        if (StringUtils.isNotBlank(externalId.get(JsonKey.ID))
            && StringUtils.isNotBlank(externalId.get(JsonKey.PROVIDER))
            && StringUtils.isNotBlank(externalId.get(JsonKey.ID_TYPE))) {
          Map<String, Object> externalIdReq = new HashMap<>();
          externalIdReq.put(JsonKey.PROVIDER, externalId.get(JsonKey.PROVIDER));
          externalIdReq.put(JsonKey.ID_TYPE, externalId.get(JsonKey.ID_TYPE));
          externalIdReq.put(JsonKey.EXTERNAL_ID, encryptData(externalId.get(JsonKey.ID)));
          Response response =
              cassandraOperation.getRecordsByCompositeKey(
                  KEY_SPACE_NAME, JsonKey.USR_EXT_IDNT_TABLE, externalIdReq);
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

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.externalIdNotFound.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.externalIdNotFound.getErrorMessage(), externalId, idType, provider),
        ResponseCode.CLIENT_ERROR.getResponseCode());
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

  /**
   * This method will search in ES for user with given search query
   *
   * @param searchQueryMap Query filters as Map.
   * @return List<User> List of User object.
   */
  private static List<User> searchUser(Map<String, Object> searchQueryMap) {
    List<User> userList = new ArrayList<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    searchRequestMap.put(JsonKey.FILTERS, searchQueryMap);
    SearchDTO searchDto = Util.createSearchDto(searchRequestMap);
    Future<Map<String, Object>> resultf =
        esService.search(searchDto, ProjectUtil.EsType.user.getTypeName());
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

  public static void addUserExtIds(Map<String, Object> requestMap) {
    List<Map<String, String>> externalIds =
        (List<Map<String, String>>) requestMap.get(JsonKey.EXTERNAL_IDS);
    if (CollectionUtils.isNotEmpty(externalIds)) {
      for (Map<String, String> extIdsMap : externalIds) {
        if (StringUtils.isBlank(extIdsMap.get(JsonKey.OPERATION))
            || JsonKey.ADD.equalsIgnoreCase(extIdsMap.get(JsonKey.OPERATION))) {
          upsertUserExternalIdentityData(extIdsMap, requestMap, JsonKey.CREATE);
        }
      }
    }
  }

  private static void upsertUserExternalIdentityData(
      Map<String, String> extIdsMap, Map<String, Object> requestMap, String operation) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.EXTERNAL_ID, encryptData(extIdsMap.get(JsonKey.ID)));
      map.put(
          JsonKey.ORIGINAL_EXTERNAL_ID, encryptData(extIdsMap.get(JsonKey.ORIGINAL_EXTERNAL_ID)));
      map.put(JsonKey.PROVIDER, extIdsMap.get(JsonKey.PROVIDER));
      map.put(JsonKey.ORIGINAL_PROVIDER, extIdsMap.get(JsonKey.ORIGINAL_PROVIDER));
      map.put(JsonKey.ID_TYPE, extIdsMap.get(JsonKey.ID_TYPE));
      map.put(JsonKey.ORIGINAL_ID_TYPE, extIdsMap.get(JsonKey.ORIGINAL_ID_TYPE));
      map.put(JsonKey.USER_ID, requestMap.get(JsonKey.USER_ID));
      if (JsonKey.CREATE.equalsIgnoreCase(operation)) {
        map.put(JsonKey.CREATED_BY, requestMap.get(JsonKey.CREATED_BY));
        map.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      } else {
        map.put(JsonKey.LAST_UPDATED_BY, requestMap.get(JsonKey.UPDATED_BY));
        map.put(JsonKey.LAST_UPDATED_ON, new Timestamp(Calendar.getInstance().getTime().getTime()));
      }
      cassandraOperation.upsertRecord(KEY_SPACE_NAME, JsonKey.USR_EXT_IDNT_TABLE, map);
    } catch (Exception ex) {
      ProjectLogger.log("Util:upsertUserExternalIdentityData : Exception occurred", ex);
    }
  }

  private static List<Map<String, String>> getUserExternalIds(Map<String, Object> requestMap) {
    List<Map<String, String>> dbResExternalIds = new ArrayList<>();
    Response response =
        cassandraOperation.getRecordsByIndexedProperty(
            KEY_SPACE_NAME,
            JsonKey.USR_EXT_IDNT_TABLE,
            JsonKey.USER_ID,
            requestMap.get(JsonKey.USER_ID));
    if (null != response && null != response.getResult()) {
      dbResExternalIds = (List<Map<String, String>>) response.getResult().get(JsonKey.RESPONSE);
    }
    return dbResExternalIds;
  }

  public static void updateUserExtId(Map<String, Object> requestMap) {
    List<Map<String, String>> dbResExternalIds = getUserExternalIds(requestMap);
    List<Map<String, String>> externalIds =
        (List<Map<String, String>>) requestMap.get(JsonKey.EXTERNAL_IDS);
    if (CollectionUtils.isNotEmpty(externalIds)) {
      // will not allow user to update idType value, if user will try to update idType will
      // ignore
      // user will have only one entry for a idType for given provider so get extId based on idType
      // List of idType values for a user will distinct and unique
      for (Map<String, String> extIdMap : externalIds) {
        Optional<Map<String, String>> extMap = checkExternalID(dbResExternalIds, extIdMap);
        Map<String, String> map = extMap.orElse(null);
        // Allowed operation type for externalIds ("add", "remove", "edit")
        if (JsonKey.ADD.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))
            || StringUtils.isBlank(extIdMap.get(JsonKey.OPERATION))) {
          if (MapUtils.isEmpty(map)) {
            upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.CREATE);
          } else {
            // if external Id with same provider and idType exist then delete first then update
            // to update user externalId first we need to delete the record as externalId is the
            // part of composite key
            deleteUserExternalId(requestMap, map);
            upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE);
          }
        } else {
          // operation is either edit or remove
          if (MapUtils.isNotEmpty(map)) {
            if (JsonKey.REMOVE.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              if (StringUtils.isNotBlank(map.get(JsonKey.ID_TYPE))
                  && StringUtils.isNotBlank((String) requestMap.get(JsonKey.USER_ID))
                  && StringUtils.isNotBlank(map.get(JsonKey.PROVIDER))) {
                deleteUserExternalId(requestMap, map);
              }
            } else if (JsonKey.EDIT.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))) {
              // to update user externalId first we need to delete the record as externalId is the
              // part of composite key
              deleteUserExternalId(requestMap, map);
              upsertUserExternalIdentityData(extIdMap, requestMap, JsonKey.UPDATE);
            }
          } else {
            throwExternalIDNotFoundException(
                extIdMap.get(JsonKey.ID),
                extIdMap.get(JsonKey.ID_TYPE),
                extIdMap.get(JsonKey.PROVIDER));
          }
        }
      }
    }
  }

  private static void deleteUserExternalId(
      Map<String, Object> requestMap, Map<String, String> map) {
    map.remove(JsonKey.LAST_UPDATED_BY);
    map.remove(JsonKey.CREATED_BY);
    map.remove(JsonKey.LAST_UPDATED_ON);
    map.remove(JsonKey.CREATED_ON);
    map.remove(JsonKey.USER_ID);
    map.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
    map.remove(JsonKey.ORIGINAL_ID_TYPE);
    map.remove(JsonKey.ORIGINAL_PROVIDER);
    cassandraOperation.deleteRecord(KEY_SPACE_NAME, JsonKey.USR_EXT_IDNT_TABLE, map);
  }

  public static void registerUserToOrg(Map<String, Object> userMap) {
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
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
    try {
      cassandraOperation.insertRecord(usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
  }

  public static Map<String, Object> getUserFromExternalId(Map<String, Object> userMap) {
    Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Map<String, Object> user = null;
    Map<String, Object> externalIdReq = new WeakHashMap<>();
    externalIdReq.put(
        JsonKey.PROVIDER, ((String) userMap.get(JsonKey.EXTERNAL_ID_PROVIDER)).toLowerCase());
    externalIdReq.put(
        JsonKey.ID_TYPE, ((String) userMap.get(JsonKey.EXTERNAL_ID_TYPE)).toLowerCase());
    externalIdReq.put(
        JsonKey.EXTERNAL_ID,
        encryptData(((String) userMap.get(JsonKey.EXTERNAL_ID)).toLowerCase()));
    Response response =
        cassandraOperation.getRecordsByCompositeKey(
            KEY_SPACE_NAME, JsonKey.USR_EXT_IDNT_TABLE, externalIdReq);
    List<Map<String, Object>> userRecordList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    if (CollectionUtils.isNotEmpty(userRecordList)) {
      Map<String, Object> userExtIdRecord = userRecordList.get(0);
      Response res =
          cassandraOperation.getRecordById(
              usrDbInfo.getKeySpace(),
              usrDbInfo.getTableName(),
              (String) userExtIdRecord.get(JsonKey.USER_ID));
      if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) res.get(JsonKey.RESPONSE))) {
        // user exist
        user = ((List<Map<String, Object>>) res.get(JsonKey.RESPONSE)).get(0);
      }
    }
    return user;
  }

  public static String getChannel(String rootOrgId) {
    Util.DbInfo orgDbInfo = Util.dbInfoMap.get(JsonKey.ORG_DB);
    String channel = null;
    Response resultFrRootOrg =
        cassandraOperation.getRecordById(
            orgDbInfo.getKeySpace(), orgDbInfo.getTableName(), rootOrgId);
    if (CollectionUtils.isNotEmpty(
        (List<Map<String, Object>>) resultFrRootOrg.get(JsonKey.RESPONSE))) {
      Map<String, Object> rootOrg =
          ((List<Map<String, Object>>) resultFrRootOrg.get(JsonKey.RESPONSE)).get(0);
      channel = (String) rootOrg.get(JsonKey.CHANNEL);
    }
    return channel;
  }

  @SuppressWarnings("unchecked")
  public static void upsertUserOrgData(Map<String, Object> userMap) {
    Util.DbInfo usrOrgDb = Util.dbInfoMap.get(JsonKey.USR_ORG_DB);
    Map<String, Object> map = new WeakHashMap<>();
    map.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    map.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    Response response =
        cassandraOperation.getRecordsByProperties(
            usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), map);
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
        cassandraOperation.updateRecord(usrOrgDb.getKeySpace(), usrOrgDb.getTableName(), reqMap);
      } catch (Exception e) {
        ProjectLogger.log("Util:upsertUserOrgData exception : " + e.getMessage(), e);
      }
    } else {
      Util.registerUserToOrg(userMap);
    }
  }

  public static void validateUserExternalIds(Map<String, Object> requestMap) {
    List<Map<String, String>> dbResExternalIds = getUserExternalIds(requestMap);
    List<Map<String, String>> externalIds =
        (List<Map<String, String>>) requestMap.get(JsonKey.EXTERNAL_IDS);
    if (CollectionUtils.isNotEmpty(externalIds)) {
      for (Map<String, String> extIdMap : externalIds) {
        Optional<Map<String, String>> extMap = checkExternalID(dbResExternalIds, extIdMap);
        Map<String, String> map = extMap.orElse(null);
        // Allowed operation type for externalIds ("add", "remove", "edit")
        if (!(JsonKey.ADD.equalsIgnoreCase(extIdMap.get(JsonKey.OPERATION))
            || StringUtils.isBlank(extIdMap.get(JsonKey.OPERATION)))) {
          // operation is either edit or remove
          if (MapUtils.isEmpty(map)) {
            throwExternalIDNotFoundException(
                extIdMap.get(JsonKey.ID),
                extIdMap.get(JsonKey.ID_TYPE),
                extIdMap.get(JsonKey.PROVIDER));
          }
        }
      }
    }
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

  public static void storeOriginalExternalIdsValue(List<Map<String, String>> externalIds) {
    externalIds.forEach(
        externalIdMap -> {
          externalIdMap.put(JsonKey.ORIGINAL_EXTERNAL_ID, externalIdMap.get(JsonKey.ID));
          externalIdMap.put(JsonKey.ORIGINAL_PROVIDER, externalIdMap.get(JsonKey.PROVIDER));
          externalIdMap.put(JsonKey.ORIGINAL_ID_TYPE, externalIdMap.get(JsonKey.ID_TYPE));
        });
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getUserDetails(String userId, ActorRef actorRef) {
    ProjectLogger.log("get user profile method call started user Id : " + userId);
    Util.DbInfo userDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Response response = null;
    List<Map<String, Object>> userList = null;
    Map<String, Object> userDetails = null;
    try {
      response =
          cassandraOperation.getRecordById(
              userDbInfo.getKeySpace(), userDbInfo.getTableName(), userId);
      userList = (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      ProjectLogger.log(
          "Util:getUserProfile: collecting user data to save for userId : " + userId,
          LoggerEnum.INFO.name());
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    String username = "";
    if (!(userList.isEmpty())) {
      userDetails = userList.get(0);
      username = (String) userDetails.get(JsonKey.USERNAME);
      ProjectLogger.log("Util:getUserDetails: userId = " + userId, LoggerEnum.INFO.name());
      userDetails.put(JsonKey.ADDRESS, getAddressDetails(userId, null));
      userDetails.put(JsonKey.EDUCATION, getUserEducationDetails(userId));
      userDetails.put(JsonKey.JOB_PROFILE, getJobProfileDetails(userId));
      userDetails.put(JsonKey.ORGANISATIONS, getUserOrgDetails(userId));
      userDetails.put(JsonKey.BADGE_ASSERTIONS, getUserBadge(userId));
      userDetails.put(JsonKey.SKILLS, getUserSkills(userId));
      Map<String, Object> orgMap = getOrgDetails((String) userDetails.get(JsonKey.ROOT_ORG_ID));
      if (!MapUtils.isEmpty(orgMap)) {
        userDetails.put(JsonKey.ROOT_ORG_NAME, orgMap.get(JsonKey.ORG_NAME));
      } else {
        userDetails.put(JsonKey.ROOT_ORG_NAME, "");
      }
      // save masked email and phone number
      addMaskEmailAndPhone(userDetails);
      checkProfileCompleteness(userDetails);
      if (actorRef != null) {
        checkUserProfileVisibility(userDetails, actorRef);
      }
      userDetails.remove(JsonKey.PASSWORD);
      addEmailAndPhone(userDetails);
      checkEmailAndPhoneVerified(userDetails);
    } else {
      ProjectLogger.log(
          "Util:getUserProfile: User data not available to save in ES for userId : " + userId,
          LoggerEnum.INFO.name());
    }
    userDetails.put(JsonKey.USERNAME, username);
    return userDetails;
  }

  public static Map<String, Object> getUserDetails(
      Map<String, Object> userDetails, Map<String, Object> orgMap) {
    String userId = (String) userDetails.get(JsonKey.USER_ID);
    ProjectLogger.log("get user profile method call started user Id : " + userId);
    List<Map<String, Object>> orgList = new ArrayList<Map<String, Object>>();
    orgList.add(orgMap);
    ProjectLogger.log("Util:getUserDetails: userId = " + userId, LoggerEnum.INFO.name());
    userDetails.put(JsonKey.ORGANISATIONS, orgList);
    Map<String, Object> rootOrg = getOrgDetails((String) userDetails.get(JsonKey.ROOT_ORG_ID));
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

  public static void checkProfileCompleteness(Map<String, Object> userMap) {
    ProfileCompletenessService profileService = ProfileCompletenessFactory.getInstance();
    Map<String, Object> profileResponse = profileService.computeProfile(userMap);
    userMap.putAll(profileResponse);
  }

  public static void checkUserProfileVisibility(Map<String, Object> userMap, ActorRef actorRef) {
    ProjectLogger.log(
        "Util:checkUserProfileVisibility: userId = " + userMap.get(JsonKey.USER_ID),
        LoggerEnum.INFO.name());
    Map<String, String> userProfileVisibilityMap =
        (Map<String, String>) userMap.get(JsonKey.PROFILE_VISIBILITY);
    Map<String, String> completeProfileVisibilityMap =
        getCompleteProfileVisibilityMap(userProfileVisibilityMap, actorRef);
    ProjectLogger.log(
        "Util:checkUserProfileVisibility: completeProfileVisibilityMap is "
            + completeProfileVisibilityMap,
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "Util:checkUserProfileVisibility: userMap contains username and the encrypted value before removing"
            + userMap.get(JsonKey.USER_NAME),
        LoggerEnum.INFO.name());
    if (MapUtils.isNotEmpty(completeProfileVisibilityMap)) {
      Map<String, Object> privateFieldsMap = new HashMap<>();
      for (String field : completeProfileVisibilityMap.keySet()) {
        if (JsonKey.PRIVATE.equalsIgnoreCase(completeProfileVisibilityMap.get(field))) {
          privateFieldsMap.put(field, userMap.remove(field));
        }
      }
      ProjectLogger.log(
          "Util:checkUserProfileVisibility: private fields key are " + privateFieldsMap.keySet(),
          LoggerEnum.INFO.name());
      ProjectLogger.log(
          "Util:checkUserProfileVisibility: userMap contains username and the encrypted value after removing"
              + userMap.get(JsonKey.USER_NAME),
          LoggerEnum.INFO.name());
      esService.upsert(
          ProjectUtil.EsType.userprofilevisibility.getTypeName(),
          (String) userMap.get(JsonKey.USER_ID),
          privateFieldsMap);
    } else {
      userMap.put(JsonKey.PROFILE_VISIBILITY, new HashMap<String, String>());
    }
  }

  public static Map<String, String> getCompleteProfileVisibilityPrivateMap(
      Map<String, String> userProfileVisibilityMap, ActorRef actorRef) {
    Map<String, String> completeProfileVisibilityMap =
        getCompleteProfileVisibilityMap(userProfileVisibilityMap, actorRef);
    Map<String, String> completeProfileVisibilityPrivateMap = new HashMap<String, String>();
    for (String key : completeProfileVisibilityMap.keySet()) {
      if (JsonKey.PRIVATE.equalsIgnoreCase(completeProfileVisibilityMap.get(key))) {
        completeProfileVisibilityPrivateMap.put(key, JsonKey.PRIVATE);
      }
    }
    return completeProfileVisibilityPrivateMap;
  }

  public static Map<String, String> getCompleteProfileVisibilityMap(
      Map<String, String> userProfileVisibilityMap, ActorRef actorRef) {
    String defaultProfileVisibility =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_USER_PROFILE_FIELD_DEFAULT_VISIBILITY);
    if (!(JsonKey.PUBLIC.equalsIgnoreCase(defaultProfileVisibility)
        || JsonKey.PRIVATE.equalsIgnoreCase(defaultProfileVisibility))) {
      ProjectLogger.log(
          "Util:getCompleteProfileVisibilityMap: Invalid configuration - "
              + defaultProfileVisibility
              + " - for default profile visibility (public / private)",
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(ResponseCode.invaidConfiguration, "");
    }

    Config userProfileConfig = getUserProfileConfig(actorRef);
    List<String> userDataFields = userProfileConfig.getStringList(JsonKey.FIELDS);
    List<String> publicFields = userProfileConfig.getStringList(JsonKey.PUBLIC_FIELDS);
    List<String> privateFields = userProfileConfig.getStringList(JsonKey.PRIVATE_FIELDS);

    // Order of preference - public/private fields settings, user settings, global settings
    Map<String, String> completeProfileVisibilityMap = new HashMap<String, String>();
    for (String field : userDataFields) {
      completeProfileVisibilityMap.put(field, defaultProfileVisibility);
    }
    completeProfileVisibilityMap.putAll(userProfileVisibilityMap);
    for (String field : publicFields) {
      completeProfileVisibilityMap.put(field, JsonKey.PUBLIC);
    }
    for (String field : privateFields) {
      completeProfileVisibilityMap.put(field, JsonKey.PRIVATE);
    }

    return completeProfileVisibilityMap;
  }

  public static void addMaskEmailAndPhone(Map<String, Object> userMap) {
    String phone = (String) userMap.get(JsonKey.PHONE);
    String email = (String) userMap.get(JsonKey.EMAIL);
    userMap.put(JsonKey.ENC_PHONE, phone);
    userMap.put(JsonKey.ENC_EMAIL, email);
    if (!StringUtils.isBlank(phone)) {
      userMap.put(JsonKey.PHONE, maskingService.maskPhone(decService.decryptData(phone)));
    }
    if (!StringUtils.isBlank(email)) {
      userMap.put(JsonKey.EMAIL, maskingService.maskEmail(decService.decryptData(email)));
    }
  }

  public static List<Map<String, Object>> getUserSkills(String userId) {
    Util.DbInfo userSkillDbInfo = Util.dbInfoMap.get(JsonKey.USER_SKILL_DB);
    Response skillresponse =
        cassandraOperation.getRecordsByIndexedProperty(
            userSkillDbInfo.getKeySpace(), userSkillDbInfo.getTableName(), JsonKey.USER_ID, userId);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) skillresponse.get(JsonKey.RESPONSE);
    return responseList;
  }

  public static List<Map<String, Object>> getUserBadge(String userId) {
    DbInfo badgeDbInfo = Util.dbInfoMap.get(JsonKey.USER_BADGE_ASSERTION_DB);
    List<Map<String, Object>> badges = new ArrayList<>();
    try {
      Response result =
          cassandraOperation.getRecordsByIndexedProperty(
              badgeDbInfo.getKeySpace(), badgeDbInfo.getTableName(), JsonKey.USER_ID, userId);
      badges = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return badges;
  }

  public static List<Map<String, Object>> getUserOrgDetails(String userId) {
    List<Map<String, Object>> userOrgList = null;
    List<Map<String, Object>> userOrganisations = new ArrayList<>();
    try {
      Map<String, Object> reqMap = new WeakHashMap<>();
      reqMap.put(JsonKey.USER_ID, userId);
      reqMap.put(JsonKey.IS_DELETED, false);
      Util.DbInfo orgUsrDbInfo = Util.dbInfoMap.get(JsonKey.USER_ORG_DB);
      Response result =
          cassandraOperation.getRecordsByProperties(
              orgUsrDbInfo.getKeySpace(), orgUsrDbInfo.getTableName(), reqMap);
      userOrgList = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
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
                organisationIds, fields, EsType.organisation.getTypeName());
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
      ProjectLogger.log(e.getMessage(), e);
    }
    return userOrganisations;
  }

  public static List<Map<String, Object>> getJobProfileDetails(String userId) {
    Util.DbInfo jobProDbInfo = Util.dbInfoMap.get(JsonKey.JOB_PROFILE_DB);
    List<Map<String, Object>> userJobProfileList = new ArrayList<>();
    Response jobProfileResponse;
    try {
      ProjectLogger.log("collecting user jobprofile user Id : " + userId);
      jobProfileResponse =
          cassandraOperation.getRecordsByIndexedProperty(
              jobProDbInfo.getKeySpace(), jobProDbInfo.getTableName(), JsonKey.USER_ID, userId);
      userJobProfileList =
          (List<Map<String, Object>>) jobProfileResponse.getResult().get(JsonKey.RESPONSE);
      ProjectLogger.log("collecting user jobprofile collection completed userId : " + userId);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    for (Map<String, Object> jobProfile : userJobProfileList) {
      String addressId = (String) jobProfile.get(JsonKey.ADDRESS_ID);
      if (!StringUtils.isBlank(addressId)) {
        List<Map<String, Object>> addrList = getAddressDetails(null, addressId);
        if (CollectionUtils.isNotEmpty(addrList)) jobProfile.put(JsonKey.ADDRESS, addrList.get(0));
      }
    }
    return userJobProfileList;
  }

  public static List<Map<String, Object>> getUserEducationDetails(String userId) {
    Util.DbInfo eduDbInfo = Util.dbInfoMap.get(JsonKey.EDUCATION_DB);
    List<Map<String, Object>> userEducationList = new ArrayList<>();
    Response eduResponse = null;
    try {
      eduResponse =
          cassandraOperation.getRecordsByIndexedProperty(
              eduDbInfo.getKeySpace(), eduDbInfo.getTableName(), JsonKey.USER_ID, userId);
      userEducationList = (List<Map<String, Object>>) eduResponse.getResult().get(JsonKey.RESPONSE);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    for (Map<String, Object> eduMap : userEducationList) {
      String addressId = (String) eduMap.get(JsonKey.ADDRESS_ID);
      if (!StringUtils.isBlank(addressId)) {
        List<Map<String, Object>> addrList = getAddressDetails(null, addressId);
        if (CollectionUtils.isNotEmpty(addrList)) eduMap.put(JsonKey.ADDRESS, addrList.get(0));
      }
    }
    return userEducationList;
  }

  public static List<Map<String, Object>> getAddressDetails(String userId, String addressId) {
    Util.DbInfo addrDbInfo = Util.dbInfoMap.get(JsonKey.ADDRESS_DB);
    List<Map<String, Object>> userAddressList = new ArrayList<>();
    Response addrResponse = null;
    try {
      if (StringUtils.isNotBlank(userId)) {
        ProjectLogger.log("collecting user address operation user Id : " + userId);
        String encUserId = encryptData(userId);
        addrResponse =
            cassandraOperation.getRecordsByIndexedProperty(
                addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), JsonKey.USER_ID, encUserId);
      } else {
        addrResponse =
            cassandraOperation.getRecordById(
                addrDbInfo.getKeySpace(), addrDbInfo.getTableName(), addressId);
      }
      userAddressList = (List<Map<String, Object>>) addrResponse.getResult().get(JsonKey.RESPONSE);
      ProjectLogger.log("collecting user address operation completed user Id : " + userId);
    } catch (Exception e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return userAddressList;
  }

  public static void saveUserDataToES(Map<String, Object> userData) {
    boolean flag =
        insertDataToElastic(
            ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.user.getTypeName(),
            (String) userData.get(JsonKey.USER_ID),
            userData);
    if (flag) {
      ProjectLogger.log(
          "Util:saveUserDataToES : ES save operation is successful for userId : "
              + (String) userData.get(JsonKey.USER_ID));
    } else {
      ProjectLogger.log(
          "Util:saveUserDataToES : ES save operation is unsuccessful for userId : "
              + (String) userData.get(JsonKey.USER_ID));
    }
  }

  private static boolean insertDataToElastic(
      String index, String type, String identifier, Map<String, Object> data) {
    Future<String> responseF = esService.save(type, identifier, data);
    String response = (String) ElasticSearchHelper.getResponseFromFuture(responseF);
    if (!StringUtils.isBlank(response)) {
      ProjectLogger.log("User Data is saved successfully ES ." + type + "  " + identifier);
      return true;
    }
    ProjectLogger.log(
        "unbale to save the data inside ES with identifier " + identifier, LoggerEnum.INFO.name());
    return false;
  }

  public static void checkPhoneUniqueness(Map<String, Object> userMap, String opType) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phoneSetting = DataCacheHandler.getConfigSettings().get(JsonKey.PHONE_UNIQUE);
    if (StringUtils.isNotBlank(phoneSetting) && Boolean.parseBoolean(phoneSetting)) {
      String phone = (String) userMap.get(JsonKey.PHONE);
      if (StringUtils.isNotBlank(phone)) {
        try {
          phone = encryptionService.encryptData(phone);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting phone number ", e);
        }
        DbInfo userDb = dbInfoMap.get(JsonKey.USER_DB);
        Response result =
            cassandraOperation.getRecordsByIndexedProperty(
                userDb.getKeySpace(), userDb.getTableName(), (JsonKey.PHONE), phone);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
        if (!userMapList.isEmpty()) {
          if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
            throw new ProjectCommonException(
                ResponseCode.PhoneNumberInUse.getErrorCode(),
                ResponseCode.PhoneNumberInUse.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          } else {
            Map<String, Object> user = userMapList.get(0);
            if (!(((String) user.get(JsonKey.ID))
                .equalsIgnoreCase((String) userMap.get(JsonKey.ID)))) {
              throw new ProjectCommonException(
                  ResponseCode.PhoneNumberInUse.getErrorCode(),
                  ResponseCode.PhoneNumberInUse.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
          }
        }
      }
    }
  }

  public static void checkEmailUniqueness(Map<String, Object> userMap, String opType) {
    // Get Email configuration if not found , by default Email can be duplicate
    // across the
    // application
    String emailSetting = DataCacheHandler.getConfigSettings().get(JsonKey.EMAIL_UNIQUE);
    if (StringUtils.isNotBlank(emailSetting) && Boolean.parseBoolean(emailSetting)) {
      String email = (String) userMap.get(JsonKey.EMAIL);
      if (StringUtils.isNotBlank(email)) {
        try {
          email = encryptionService.encryptData(email);
        } catch (Exception e) {
          ProjectLogger.log("Exception occurred while encrypting Email ", e);
        }
        Map<String, Object> filters = new HashMap<>();
        filters.put(JsonKey.ENC_EMAIL, email);
        Map<String, Object> map = new HashMap<>();
        map.put(JsonKey.FILTERS, filters);
        SearchDTO searchDto = Util.createSearchDto(map);
        Future<Map<String, Object>> resultF =
            esService.search(searchDto, ProjectUtil.EsType.user.getTypeName());
        Map<String, Object> result =
            (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
        List<Map<String, Object>> userMapList =
            (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
        if (!userMapList.isEmpty()) {
          if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
            throw new ProjectCommonException(
                ResponseCode.emailInUse.getErrorCode(),
                ResponseCode.emailInUse.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          } else {
            Map<String, Object> user = userMapList.get(0);
            if (!(((String) user.get(JsonKey.ID))
                .equalsIgnoreCase((String) userMap.get(JsonKey.ID)))) {
              throw new ProjectCommonException(
                  ResponseCode.emailInUse.getErrorCode(),
                  ResponseCode.emailInUse.getErrorMessage(),
                  ResponseCode.CLIENT_ERROR.getResponseCode());
            }
          }
        }
      }
    }
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
        ProjectLogger.log(
            "Util:sendOnboardingMail: Email not sent as generated link is empty", LoggerEnum.ERROR);
        return null;
      }

      request = new Request();
      request.setOperation(BackgroundOperations.emailService.name());
      request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
    }
    return request;
  }

  public static Request sendResetPassMail(Map<String, Object> emailTemplateMap) {
    Request request = null;
    if (StringUtils.isBlank((String) emailTemplateMap.get(JsonKey.SET_PASSWORD_LINK))) {
      ProjectLogger.log(
          "Util:sendResetPassMail: Email not sent as generated link is empty",
          LoggerEnum.ERROR.name());
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
      ProjectLogger.log(
          "Util:sendResetPassMail: requested data is neither having email nor phone ",
          LoggerEnum.ERROR.name());
      return null;
    }
    request = new Request();
    request.setOperation(BackgroundOperations.emailService.name());
    request.put(JsonKey.EMAIL_REQUEST, emailTemplateMap);
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
      Map<String, Object> templateMap, boolean isUrlShortRequired) {
    URLShortner urlShortner = new URLShortnerImpl();
    String redirectUri =
        StringUtils.isNotBlank((String) templateMap.get(JsonKey.REDIRECT_URI))
            ? ((String) templateMap.get(JsonKey.REDIRECT_URI))
            : null;
    ProjectLogger.log(
        "Util:getUserRequiredActionLink redirectURI = " + redirectUri, LoggerEnum.INFO.name());
    if (StringUtils.isBlank((String) templateMap.get(JsonKey.PASSWORD))) {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.UPDATE_PASSWORD);

      templateMap.put(
          JsonKey.SET_PASSWORD_LINK, isUrlShortRequired ? urlShortner.shortUrl(url) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url) : url;

    } else {
      String url =
          KeycloakRequiredActionLinkUtil.getLink(
              (String) templateMap.get(JsonKey.USERNAME),
              redirectUri,
              KeycloakRequiredActionLinkUtil.VERIFY_EMAIL);
      templateMap.put(
          JsonKey.VERIFY_EMAIL_LINK, isUrlShortRequired ? urlShortner.shortUrl(url) : url);
      return isUrlShortRequired ? urlShortner.shortUrl(url) : url;
    }
  }

  public static void getUserRequiredActionLink(Map<String, Object> templateMap) {
    getUserRequiredActionLink(templateMap, true);
  }

  public static void sendSMS(Map<String, Object> userMap) {
    if (StringUtils.isNotBlank((String) userMap.get(JsonKey.PHONE))) {
      String envName = propertiesCache.getProperty(JsonKey.SUNBIRD_INSTALLATION_DISPLAY_NAME);
      setRequiredActionLink(userMap);
      if (StringUtils.isBlank((String) userMap.get(JsonKey.SET_PASSWORD_LINK))
          && StringUtils.isBlank((String) userMap.get(JsonKey.VERIFY_EMAIL_LINK))) {
        ProjectLogger.log(
            "Util:sendSMS: SMS not sent as generated link is empty", LoggerEnum.ERROR);
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
      ProjectLogger.log("SMS text : " + sms, LoggerEnum.INFO);
      String countryCode = "";
      if (StringUtils.isBlank((String) userMap.get(JsonKey.COUNTRY_CODE))) {
        countryCode =
            PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_DEFAULT_COUNTRY_CODE);
      } else {
        countryCode = (String) userMap.get(JsonKey.COUNTRY_CODE);
      }
      ISmsProvider smsProvider = SMSFactory.getInstance("91SMS");
      ProjectLogger.log(
          "SMS text : " + sms + " with phone " + (String) userMap.get(JsonKey.PHONE),
          LoggerEnum.INFO.name());
      boolean response = smsProvider.send((String) userMap.get(JsonKey.PHONE), countryCode, sms);
      ProjectLogger.log("Response from smsProvider : " + response, LoggerEnum.INFO);
      if (response) {
        ProjectLogger.log(
            "Welcome Message sent successfully to ." + (String) userMap.get(JsonKey.PHONE),
            LoggerEnum.INFO.name());
      } else {
        ProjectLogger.log(
            "Welcome Message failed for ." + (String) userMap.get(JsonKey.PHONE),
            LoggerEnum.INFO.name());
      }
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

  public static String getCustodianChannel(Map<String, Object> userMap, ActorRef actorRef) {
    String channel = (String) userMap.get(JsonKey.CHANNEL);
    if (StringUtils.isBlank(channel)) {
      try {
        SystemSettingClient client = SystemSettingClientImpl.getInstance();
        SystemSetting systemSetting =
            client.getSystemSettingByField(actorRef, JsonKey.CUSTODIAN_ORG_CHANNEL);
        if (null != systemSetting && StringUtils.isNotBlank(systemSetting.getValue())) {
          channel = systemSetting.getValue();
        }
      } catch (Exception ex) {
        ProjectLogger.log(
            "Util:getCustodianChannel: Exception occurred while fetching custodian channel from system setting.",
            ex);
      }
    }
    if (StringUtils.isBlank(channel)) {
      channel = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DEFAULT_CHANNEL);
      userMap.put(JsonKey.CHANNEL, channel);
    }
    return channel;
  }

  /*
   * Get user profile configuration from system settings
   *
   * @param getSystemSetting actor reference
   * @return user profile configuration
   */
  public static Config getUserProfileConfig(ActorRef actorRef) {
    SystemSetting userProfileConfigSetting =
        getSystemSettingByField(JsonKey.USER_PROFILE_CONFIG, actorRef);
    String userProfileConfigString = userProfileConfigSetting.getValue();
    Config userProfileConfig =
        ConfigUtil.getConfigFromJsonString(userProfileConfigString, JsonKey.USER_PROFILE_CONFIG);
    validateUserProfileConfig(userProfileConfig);
    return userProfileConfig;
  }

  private static void validateUserProfileConfig(Config userProfileConfig) {
    if (CollectionUtils.isEmpty(userProfileConfig.getStringList(JsonKey.FIELDS))) {
      ProjectLogger.log(
          "Util:validateUserProfileConfig: User profile fields is not configured.",
          LoggerEnum.ERROR.name());
      ProjectCommonException.throwServerErrorException(ResponseCode.invaidConfiguration, "");
    }
    List<String> publicFields = null;
    List<String> privateFields = null;
    try {
      publicFields = userProfileConfig.getStringList(JsonKey.PUBLIC_FIELDS);
      privateFields = userProfileConfig.getStringList(JsonKey.PRIVATE_FIELDS);
    } catch (Exception e) {
      ProjectLogger.log(
          "Util:validateUserProfileConfig: Invalid configuration for public / private fields.",
          LoggerEnum.ERROR.name());
    }

    if (CollectionUtils.isNotEmpty(privateFields) && CollectionUtils.isNotEmpty(publicFields)) {
      for (String field : publicFields) {
        if (privateFields.contains(field)) {
          ProjectLogger.log(
              "Field "
                  + field
                  + " in user configuration is conflicting in publicFields and privateFields.",
              LoggerEnum.ERROR.name());
          ProjectCommonException.throwServerErrorException(
              ResponseCode.errorConflictingFieldConfiguration,
              ProjectUtil.formatMessage(
                  ResponseCode.errorConflictingFieldConfiguration.getErrorMessage(),
                  field,
                  JsonKey.USER,
                  JsonKey.PUBLIC_FIELDS,
                  JsonKey.PRIVATE_FIELDS));
        }
      }
    }
  }

  /*
   * Method to fetch a system setting based on given system setting field
   *
   * @param system setting field
   * @param getSystemSetting actor reference
   * @return system setting
   */
  public static SystemSetting getSystemSettingByField(
      String systemSettingField, ActorRef actorRef) {
    SystemSetting systemSetting = null;
    try {
      SystemSettingClient client = SystemSettingClientImpl.getInstance();
      systemSetting = client.getSystemSettingByField(actorRef, systemSettingField);
      if (null == systemSetting || null == systemSetting.getValue()) {
        throw new Exception();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "Util:getSystemSettingByField: System setting not found for field - "
              + systemSettingField,
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.errorSystemSettingNotFound,
          ProjectUtil.formatMessage(
              ResponseCode.errorSystemSettingNotFound.getErrorMessage(), systemSettingField));
    }
    return systemSetting;
  }
}

@FunctionalInterface
interface ConvertValuesToLowerCase {
  Map<String, String> convertToLowerCase(Map<String, String> map);
}
