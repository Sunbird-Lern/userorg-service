package org.sunbird.metrics.actors;

import static org.sunbird.common.models.util.ProjectUtil.isNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;

public abstract class BaseMetricsActor extends BaseActor {

  private static ObjectMapper mapper = new ObjectMapper();
  public static final String STARTDATE = "startDate";
  public static final String ENDDATE = "endDate";
  public static final String START_TIME_MILLIS = "startTimeMillis";
  public static final String END_TIME_MILLIS = "endTimeMillis";
  public static final String LTE = "<=";
  public static final String LT = "<";
  public static final String GTE = ">=";
  public static final String GT = ">";
  public static final String KEY = "key";
  public static final String KEYNAME = "key_name";
  public static final String GROUP_ID = "group_id";
  public static final String VALUE = "value";
  public static final String INTERVAL = "interval";
  public static final String FORMAT = "format";
  protected static final String USER_ID = "user_id";
  protected static final String FOLDERPATH = "/data/";
  protected static final String FILENAMESEPARATOR = "_";
  private static final String CHARSETS_UTF_8 = "UTF-8";

  protected Map<String, Object> addSnapshot(
      String keyName, String name, Object value, String timeUnit) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put(JsonKey.NAME, name);
    snapshot.put(VALUE, value);
    if (!StringUtils.isBlank(timeUnit)) {
      snapshot.put(JsonKey.TIME_UNIT, timeUnit);
    }
    return snapshot;
  }

  /**
   * This method will provide date day range period. It will take parameter as "xd" where x is an
   * int value. Based on passed parameter it will provide startDate and endDate range. EndDate will
   * be calculated excluding current date.
   *
   * @param period Date range in format of "xd" EX: 7d
   * @return Map having keys ENDDATE,END_TIME_MILLIS,INTERVAL,FORMAT,STARTDATE,START_TIME_MILLIS
   */
  protected static Map<String, Object> getStartAndEndDateForDay(String period) {
    Map<String, Object> dateMap = new HashMap<>();
    int days = getDaysByPeriod(period);
    Date endDateValue = null;
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    calendar.set(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
        23,
        59,
        59);
    calendar.add(Calendar.DATE, -1);
    calendar.add(Calendar.HOUR, -5);
    calendar.add(Calendar.MINUTE, -30);
    endDateValue = calendar.getTime();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(endDateValue.getTime());
    cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -(days));
    cal.add(Calendar.SECOND, +1);
    String startDateStr = sdf.format(cal.getTimeInMillis());
    String endDateStr = sdf.format(endDateValue.getTime());
    dateMap.put(INTERVAL, "1d");
    dateMap.put(FORMAT, "yyyy-MM-dd");
    dateMap.put(STARTDATE, startDateStr);
    dateMap.put(ENDDATE, endDateStr);
    dateMap.put(START_TIME_MILLIS, cal.getTimeInMillis());
    dateMap.put(END_TIME_MILLIS, endDateValue.getTime());
    return dateMap;
  }

  protected static Map<String, Object> getStartAndEndDate(String period) {
    if ("5w".equalsIgnoreCase(period)) {
      return getStartAndEndDateForWeek(period);
    } else {
      return getStartAndEndDateForDay(period);
    }
  }

  /**
   * This method will provide date week range. it will take request param as "xw" where x is a int.
   * Example if user pass "5w" ,it means this method will calculate 5 calendar week from now and
   * provide start date of first week and end data of 5th week.
   *
   * @param period number of week in "xw" format , where x is an int value.
   * @return Map having keys ENDDATE,END_TIME_MILLIS,INTERVAL,FORMAT,STARTDATE,START_TIME_MILLIS
   */
  protected static Map<String, Object> getStartAndEndDateForWeek(String period) {
    Map<String, Object> dateMap = new HashMap<>();
    Map<String, Integer> periodMap = getDaysByPeriodStr(period);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    int firstDayOfWeek = calendar.getFirstDayOfWeek();
    calendar.add(Calendar.DATE, -(calendar.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek));
    calendar.add(Calendar.WEEK_OF_YEAR, 1);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    String endDateStr = sdf.format(calendar.getTime());
    dateMap.put(ENDDATE, endDateStr);
    dateMap.put(END_TIME_MILLIS, calendar.getTimeInMillis());
    calendar.add(periodMap.get(KEY), -(periodMap.get(VALUE)));
    calendar.add(Calendar.DATE, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    dateMap.put(INTERVAL, "1w");
    dateMap.put(FORMAT, "yyyy-ww");
    String startDateStr = sdf.format(calendar.getTime());
    dateMap.put(STARTDATE, startDateStr);
    dateMap.put(START_TIME_MILLIS, calendar.getTimeInMillis());
    return dateMap;
  }

  protected static int getDaysByPeriod(String period) {
    int days = 0;
    switch (period) {
      case "7d":
        days = 7;
        break;

      case "14d":
        days = 14;
        break;

      case "5w":
        days = 35;
        break;

      default:
        days = 0;
        break;
    }
    if (days == 0) {
      throw new ProjectCommonException(
          ResponseCode.invalidPeriod.getErrorCode(),
          ResponseCode.invalidPeriod.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return days;
  }

  protected static Map<String, Integer> getDaysByPeriodStr(String period) {
    Map<String, Integer> dayPeriod = new HashMap<>();
    switch (period) {
      case "7d":
        dayPeriod.put(KEY, Calendar.DATE);
        dayPeriod.put(VALUE, 7);
        break;

      case "14d":
        dayPeriod.put(KEY, Calendar.DATE);
        dayPeriod.put(VALUE, 14);
        break;

      case "5w":
        dayPeriod.put(KEY, Calendar.WEEK_OF_YEAR);
        dayPeriod.put(VALUE, 5);
        break;
    }
    if (dayPeriod.isEmpty()) {
      throw new ProjectCommonException(
          ResponseCode.invalidPeriod.getErrorCode(),
          ResponseCode.invalidPeriod.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return dayPeriod;
  }

  protected static String getEkstepPeriod(String period) {
    String days = "";
    switch (period) {
      case "7d":
        days = "LAST_7_DAYS";
        break;

      case "14d":
        days = "LAST_14_DAYS";
        break;

      case "5w":
        days = "LAST_5_WEEKS";
        break;

      default:
        throw new ProjectCommonException(
            ResponseCode.invalidPeriod.getErrorCode(),
            ResponseCode.invalidPeriod.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return days;
  }

  protected List<Map<String, Object>> createBucketStrForWeek(String periodStr) {
    Map<String, Object> periodMap = getStartAndEndDateForWeek(periodStr);
    String date = (String) periodMap.get(STARTDATE);
    List<Map<String, Object>> bucket = new ArrayList<>();
    Calendar cal = Calendar.getInstance();
    for (int day = 0; day < 5; day++) {
      Map<String, Object> bucketData = new LinkedHashMap<>();
      String keyName = "";
      String key = "";
      Date dateValue = null;
      try {
        keyName = formatKeyNameString(date);
        dateValue = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        cal.setTime(dateValue);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        key = cal.get(Calendar.YEAR) + Integer.toString(week);
        date = keyName.toLowerCase().split("to")[1];
        date = date.trim();
        dateValue = new SimpleDateFormat("yyyy-MM-dd").parse(date);
        cal.setTime(dateValue);
        cal.add(Calendar.DATE, +1);
        date = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
      } catch (ParseException e) {
        ProjectLogger.log("Error occurred", e);
      }
      bucketData.put(KEY, key);
      bucketData.put(KEYNAME, keyName);
      bucketData.put(VALUE, 0);
      bucket.add(bucketData);
    }
    return bucket;
  }

  protected List<Map<String, Object>> createBucketStructure(String periodStr) {
    if ("5w".equalsIgnoreCase(periodStr)) {
      return createBucketStrForWeek(periodStr);
    } else {
      return createBucketStructureDays(periodStr);
    }
  }

  protected List<Map<String, Object>> createBucketStructureDays(String periodStr) {
    int days = getDaysByPeriod(periodStr);
    Date date = null;
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -1);
    date = calendar.getTime();
    List<Map<String, Object>> bucket = new ArrayList<>();
    for (int day = days - 1; day >= 0; day--) {
      Map<String, Object> bucketData = new LinkedHashMap<>();
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(date.getTime());
      cal.add(Calendar.DATE, -(day));
      bucketData.put(KEY, cal.getTimeInMillis());
      bucketData.put(KEYNAME, new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
      bucketData.put(VALUE, 0);
      bucket.add(bucketData);
    }
    return bucket;
  }

  protected static String postDataEkstep(String apiUrl, String request) {
    Map<String, String> headers = new HashMap<>();
    String response = null;
    try {
      String baseSearchUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(baseSearchUrl)) {
        baseSearchUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      headers.put(
          JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.EKSTEP_AUTHORIZATION));
      if (StringUtils.isBlank(headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
        headers.put("Content_Type", "application/json; charset=utf-8");
      }
      response =
          HttpUtil.sendPostRequest(
              baseSearchUrl + PropertiesCache.getInstance().getProperty(apiUrl), request, headers);

    } catch (Exception e) {
      ProjectLogger.log("Error occurred", e);
      throw new ProjectCommonException(
          ResponseCode.unableToConnect.getErrorCode(),
          ResponseCode.unableToConnect.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return response;
  }

  protected static String getDataFromEkstep(String apiUrl) {
    Map<String, String> headers = new HashMap<>();
    String response = null;
    try {
      String baseSearchUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
      if (StringUtils.isBlank(baseSearchUrl)) {
        baseSearchUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
      }
      headers.put(
          JsonKey.AUTHORIZATION, JsonKey.BEARER + System.getenv(JsonKey.EKSTEP_AUTHORIZATION));
      if (StringUtils.isBlank(headers.get(JsonKey.AUTHORIZATION))) {
        headers.put(
            JsonKey.AUTHORIZATION,
            PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION));
        headers.put("Content_Type", "application/json; charset=utf-8");
      }
      response = HttpUtil.sendGetRequest(baseSearchUrl + apiUrl, headers);

    } catch (Exception e) {
      ProjectLogger.log("Error occurred", e);
      throw new ProjectCommonException(
          ResponseCode.unableToConnect.getErrorCode(),
          ResponseCode.unableToConnect.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return response;
  }

  public static String makePostRequest(String baseURL, String apiURL, String body)
      throws IOException {
    ProjectLogger.log("Request to Ekstep for Metrics" + body);
    String authKey = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(authKey)) {
      authKey = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      authKey = JsonKey.BEARER + authKey;
    }
    HttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(baseURL + PropertiesCache.getInstance().getProperty(apiURL));
    post.addHeader("Content-Type", "application/json; charset=utf-8");
    post.addHeader(JsonKey.AUTHORIZATION, authKey);
    post.setEntity(new StringEntity(body, CHARSETS_UTF_8));
    ProjectLogger.log(
        "BaseMetricsActor:makePostRequest completed requested data : " + body,
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "BaseMetricsActor:makePostRequest completed Url : "
            + baseURL
            + PropertiesCache.getInstance().getProperty(apiURL),
        LoggerEnum.INFO.name());
    HttpResponse response = client.execute(post);
    if (response.getStatusLine().getStatusCode() != 200) {
      ProjectLogger.log(
          "BaseMetricsActor:makePostRequest: Status code from analytics is not 200 ",
          LoggerEnum.INFO.name());
      throw new ProjectCommonException(
          ResponseCode.unableToConnect.getErrorCode(),
          ResponseCode.unableToConnect.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    BufferedReader rd =
        new BufferedReader(
            new InputStreamReader(response.getEntity().getContent(), CHARSETS_UTF_8));

    StringBuilder result = new StringBuilder();
    String line = "";
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    ProjectLogger.log(
        "BaseMetricsActor:makePostRequest: Response from analytics store for metrics = "
            + response.toString(),
        LoggerEnum.INFO.name());
    return result.toString();
  }

  public static String makePostRequest(String apiURL, String body) throws IOException {
    String baseSearchUrl = System.getenv(JsonKey.EKSTEP_BASE_URL);
    if (StringUtils.isBlank(baseSearchUrl)) {
      baseSearchUrl = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
    }
    return makePostRequest(baseSearchUrl, apiURL, body);
  }

  @SuppressWarnings({"rawtypes"})
  protected List<Map<String, Object>> getBucketData(Map aggKeyMap, String period) {
    if (null == aggKeyMap || aggKeyMap.isEmpty()) {
      return new ArrayList<>();
    }
    if ("5w".equalsIgnoreCase(period)) {
      return getBucketDataForWeeks(aggKeyMap);
    } else {
      return getBucketDataForDays(aggKeyMap);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected List<Map<String, Object>> getBucketDataForDays(Map aggKeyMap) {
    List<Map<String, Object>> parentGroupList = new ArrayList<>();
    List<Map<String, Double>> aggKeyList = (List<Map<String, Double>>) aggKeyMap.get("buckets");
    for (Map aggKeyListMap : aggKeyList) {
      Map<String, Object> parentCountObject = new LinkedHashMap<>();
      parentCountObject.put(KEY, aggKeyListMap.get(KEY));
      parentCountObject.put(KEYNAME, aggKeyListMap.get("key_as_string"));
      parentCountObject.put(VALUE, aggKeyListMap.get("doc_count"));
      parentGroupList.add(parentCountObject);
    }
    return parentGroupList;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected List<Map<String, Object>> getBucketDataForWeeks(Map aggKeyMap) {
    List<Map<String, Object>> parentGroupList = new ArrayList<>();
    List<Map<String, Double>> aggKeyList = (List<Map<String, Double>>) aggKeyMap.get("buckets");
    for (Map aggKeyListMap : aggKeyList) {
      Map<String, Object> parentCountObject = new LinkedHashMap<>();
      parentCountObject.put(KEY, formatKeyString((String) aggKeyListMap.get("key_as_string")));
      parentCountObject.put(KEYNAME, formatKeyNameString(aggKeyListMap.get(KEY)));
      parentCountObject.put(VALUE, aggKeyListMap.get("doc_count"));
      parentGroupList.add(parentCountObject);
    }
    return parentGroupList;
  }

  protected String formatKeyString(String key) {
    return StringUtils.remove(key, "-");
  }

  protected String formatKeyNameString(Object keyName) {
    StringBuilder buffer = new StringBuilder();
    Date date = new Date();
    if (keyName instanceof Long) {
      date = new Date((Long) keyName);
    } else if (keyName instanceof String) {
      try {
        date = new SimpleDateFormat("yyyy-MM-dd").parse((String) keyName);
      } catch (Exception e) {
        ProjectLogger.log("Error occurred", e);
      }
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.get(Calendar.DAY_OF_WEEK);
    buffer.append(sdf.format(cal.getTime())).append(" To ");
    cal.add(Calendar.DATE, +6);
    buffer.append(sdf.format(cal.getTime()));
    return buffer.toString();
  }

  @SuppressWarnings("unchecked")
  protected Response metricsResponseGenerator(
      String esResponse, String periodStr, Map<String, Object> viewData) {
    Response response = new Response();
    Map<String, Object> responseData = new LinkedHashMap<>();
    try {
      Map<String, Object> esData = mapper.readValue(esResponse, Map.class);
      responseData.putAll(viewData);
      responseData.put(JsonKey.PERIOD, periodStr);
      responseData.put(JsonKey.SNAPSHOT, esData.get(JsonKey.SNAPSHOT));
      responseData.put(JsonKey.SERIES, esData.get(JsonKey.SERIES));
    } catch (IOException e) {
      ProjectLogger.log("Error occurred", e);
    }
    response.putAll(responseData);
    return response;
  }

  protected SearchDTO createESRequest(
      Map<String, Object> filters, Map<String, String> aggs, List<String> fields) {
    SearchDTO searchDTO = new SearchDTO();

    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);
    if (isNotNull(aggs)) {
      searchDTO.getFacets().add(aggs);
    }
    if (isNotNull(fields)) {
      searchDTO.setFields(fields);
    }
    return searchDTO;
  }

  protected void calculateCourseProgressPercentage(List<Map<String, Object>> esContent) {
    for (Map<String, Object> map : esContent) {
      Integer leafNodeCount = (Integer) map.get(JsonKey.LEAF_NODE_COUNT);
      if (leafNodeCount == null) leafNodeCount = Integer.valueOf("0");
      calculateCourseProgressPercentage(map, leafNodeCount);
      map.remove(JsonKey.LEAF_NODE_COUNT);
    }
  }

  protected void calculateCourseProgressPercentage(
      Map<String, Object> batchMap, int leafNodeCount) {
    Integer progress = (Integer) batchMap.get(JsonKey.PROGRESS);
    Integer progressPercentage = Integer.valueOf("0");
    if (leafNodeCount == 0) {
      progressPercentage = Integer.valueOf("100");
    } else if (null != progress && progress > 0) {
      // making percentage as round of value.
      progressPercentage = (int) Math.round((progress * 100.0) / leafNodeCount);
    }
    batchMap.put(JsonKey.PROGRESS, progressPercentage);
  }

  /**
   * This method will convert incoming time period to start and end date. Possible values for period
   * is {"7d","14d","5w"} ,This method will return a map with key as STARTDATE and ENDDATE.
   * STARTDATE will always be currentDate-1, Date format will be YYY_MM_DD_FORMATTER
   *
   * @param period Date range in format of {"7d","14d","5w"} EX: 7d
   * @return map having key as STARTDATE and ENDDATE.
   */
  protected Map<String, String> getDateRange(String period) {
    if (StringUtils.isBlank(period)) {
      throw new ProjectCommonException(
          ResponseCode.invalidPeriod.getErrorCode(),
          ResponseCode.invalidPeriod.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    period = period.toLowerCase();
    int noOfDays = getDaysByPeriod(period);
    ProjectLogger.log(
        "BaseMetricsActor:getDateRange Number of days = " + noOfDays, LoggerEnum.INFO.name());
    return ProjectUtil.getDateRange(noOfDays);
  }
}
