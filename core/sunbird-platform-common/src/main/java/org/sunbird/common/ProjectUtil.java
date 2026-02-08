package org.sunbird.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.UrlValidator;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.http.HttpUtil;
import org.sunbird.utils.EsConfigUtil;
import org.sunbird.request.Request;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;

/**
 * Utility class containing common methods and constants used across the project.
 * Handles date formatting, email validation, ID generation, and configuration management.
 *
 * @author Manzarul
 * @author Amit Kumar
 */
public class ProjectUtil {

  /** format the date in YYYY-MM-DD hh:mm:ss:SSZ */
  private static AtomicInteger atomicInteger = new AtomicInteger();

  public static Integer DEFAULT_BATCH_SIZE = 10;
  public static final long BACKGROUND_ACTOR_WAIT_TIME = 30;
  public static final String ELASTIC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  public static final String YEAR_MONTH_DATE_FORMAT = "yyyy-MM-dd";
  private static final int randomPasswordLength = 9;
  private static LoggerUtil logger = new LoggerUtil(ProjectUtil.class);

  protected static final String FILE_NAME[] = {
    "cassandratablecolumn.properties",
    "elasticsearch.config.properties",
    "cassandra.config.properties",
    "dbconfig.properties",
    "externalresource.properties",
    "sso.properties",
    "userencryption.properties",
    "profilecompleteness.properties",
    "mailTemplates.properties"
  };
  public static PropertiesCache propertiesCache;
  private static Pattern pattern;
  public static final String EMAIL_PATTERN =
      "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
          + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
  public static final String[] excludes =
      new String[] {
        JsonKey.COMPLETENESS,
        JsonKey.MISSING_FIELDS,
        JsonKey.PROFILE_VISIBILITY,
        JsonKey.LOGIN_ID,
        JsonKey.USER_ID
      };

  public static final String[] defaultPrivateFields = new String[] {JsonKey.EMAIL, JsonKey.PHONE};
  private static final String INDEX_NAME = "telemetry.raw";
  private static String YYYY_MM_DD_FORMATTER = "yyyy-MM-dd";
  private static final String STARTDATE = "startDate";
  private static final String ENDDATE = "endDate";
  private static ObjectMapper mapper = new ObjectMapper();

  static {
    pattern = Pattern.compile(EMAIL_PATTERN);
    propertiesCache = PropertiesCache.getInstance();
  }

  /**
   * Enumeration for Environment types.
   */
  public enum Environment {
    dev(1),
    qa(2),
    prod(3);
    int value;

    private Environment(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public enum UserLookupType {
    USERNAME(JsonKey.USER_LOOKUP_FILED_USER_NAME),
    EMAIL(JsonKey.EMAIL),
    PHONE(JsonKey.PHONE);

    private String type;

    UserLookupType(String type) {
      this.type = type;
    }

    public String getType() {
      return this.type;
    }
  }

  /**
   * Enumeration for Status.
   */
  public enum Status {
    ACTIVE(1),
    INACTIVE(0),
    DELETED(2);

    private int value;

    Status(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Bulk Process Status.
   */
  public enum BulkProcessStatus {
    NEW(0),
    IN_PROGRESS(1),
    INTERRUPT(2),
    COMPLETED(3),
    FAILED(9);

    private int value;

    BulkProcessStatus(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Org Status.
   */
  public enum OrgStatus {
    INACTIVE(0),
    ACTIVE(1),
    BLOCKED(2),
    RETIRED(3);

    private Integer value;

    OrgStatus(Integer value) {
      this.value = value;
    }

    public Integer getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Progress Status.
   */
  public enum ProgressStatus {
    NOT_STARTED(0),
    STARTED(1),
    COMPLETED(2);

    private int value;

    ProgressStatus(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Active Status.
   */
  public enum ActiveStatus {
    ACTIVE(true),
    INACTIVE(false);

    private boolean value;

    ActiveStatus(boolean value) {
      this.value = value;
    }

    public boolean getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Action.
   */
  public enum Action {
    YES(1),
    NO(0);

    private int value;

    Action(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  /**
   * Enumeration for Course Management Status.
   */
  public enum CourseMgmtStatus {
    DRAFT("draft"),
    LIVE("live"),
    RETIRED("retired");

    private String value;

    CourseMgmtStatus(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for Source.
   */
  public enum Source {
    WEB("web"),
    ANDROID("android"),
    IOS("ios"),
    APP("app");

    private String value;

    Source(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  /**
   * Enumeration for User Role.
   */
  public enum UserRole {
    PUBLIC("PUBLIC"),
    CONTENT_CREATOR("CONTENT_CREATOR"),
    CONTENT_REVIEWER("CONTENT_REVIEWER"),
    ORG_ADMIN("ORG_ADMIN"),
    ORG_MEMBER("ORG_MEMBER");

    private String value;

    UserRole(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }
  }

  /**
   * This method will check incoming value is null or empty it will do empty check by doing trim
   * method. in case of null or empty it will return true else false.
   *
   * @param value String value to check
   * @return boolean true if null or empty
   */
  public static boolean isStringNullOREmpty(String value) {
    return (value == null || "".equals(value.trim()));
  }

  /**
   * This method will provide formatted date.
   *
   * @return String formatted date
   */
  public static String getFormattedDate() {
    return getDateFormatter().format(new Date());
  }

  /**
   * This method will provide timestamp.
   *
   * @return Date current timestamp
   */
  public static Date getTimeStamp() {
    return new Timestamp(System.currentTimeMillis());
  }

  /**
   * This method will provide formatted date.
   *
   * @param date Date object
   * @return String formatted date
   */
  public static String formatDate(Date date) {
    if (null != date) return getDateFormatter().format(date);
    else return null;
  }

  /**
   * Validate email with regular expression.
   *
   * @param email String email
   * @return true valid email, false invalid email
   */
  public static boolean isEmailvalid(final String email) {
    if (StringUtils.isBlank(email)) {
      return false;
    }
    Matcher matcher = pattern.matcher(email);
    return matcher.matches();
  }

  /**
   * This method will generate auth token based on name , source and timestamp.
   *
   * @param name String name
   * @param source String source
   * @return String auth token
   */
  public static String createAuthToken(String name, String source) {
    String data = name + source + System.currentTimeMillis();
    UUID authId = UUID.nameUUIDFromBytes(data.getBytes(StandardCharsets.UTF_8));
    return authId.toString();
  }

  /**
   * This method will generate unique id based on current time stamp and some random value mixed up.
   *
   * @param environmentId int environment id
   * @return String unique id
   */
  public static String getUniqueIdFromTimestamp(int environmentId) {
    Random random = new Random();
    long env = (environmentId + random.nextInt(99999)) / 10000000;
    long uid = System.currentTimeMillis() + random.nextInt(999999);
    uid = uid << 13;
    return env + "" + uid + "" + atomicInteger.getAndIncrement();
  }

  /**
   * This method will generate the unique id.
   *
   * @return String unique id
   */
  public static synchronized String generateUniqueId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Enumeration for HTTP Methods.
   */
  public enum Method {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
  }

  /**
   * Enum to hold the index name for Elastic search.
   */
  public enum EsIndex {
    sunbird("searchindex"),
    sunbirdPlugin("sunbirdplugin"),
    courseBatchStats("cbatchstats");
    private String indexName;

    private EsIndex(String name) {
      this.indexName = name;
    }

    public String getIndexName() {
      return indexName;
    }
  }

  /**
   * This enum will hold all the ES type name.
   */
  public enum EsType {
    course(EsConfigUtil.getConfigValue(JsonKey.ES_COURSE_INDEX)),
    courseBatch(EsConfigUtil.getConfigValue(JsonKey.ES_COURSE_BATCH_INDEX)),
    user(EsConfigUtil.getConfigValue(JsonKey.ES_USER_INDEX)),
    organisation(EsConfigUtil.getConfigValue(JsonKey.ES_ORGANISATION_INDEX)),
    usercourses(EsConfigUtil.getConfigValue(JsonKey.ES_USER_COURSES_INDEX)),
    location(EsConfigUtil.getConfigValue(JsonKey.ES_LOCATION_INDEX)),
    usernotes(EsConfigUtil.getConfigValue(JsonKey.ES_USER_NOTES_INDEX)),
    userfeed(EsConfigUtil.getConfigValue(JsonKey.ES_USER_FEED_INDEX));

    private String typeName;

    private EsType(String name) {
      this.typeName = name;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  /**
   * Enumeration for Section Data Type.
   */
  public enum SectionDataType {
    course("course"),
    content("content");
    private String typeName;

    private SectionDataType(String name) {
      this.typeName = name;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  /**
   * Enumeration for Address Type.
   */
  public enum AddressType {
    permanent("permanent"),
    current("current"),
    office("office"),
    home("home");
    private String typeName;

    private AddressType(String name) {
      this.typeName = name;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  /**
   * Enumeration for Assessment Result.
   */
  public enum AssessmentResult {
    gradeA("A", "Pass"),
    gradeB("B", "Pass"),
    gradeC("C", "Pass"),
    gradeD("D", "Pass"),
    gradeE("E", "Pass"),
    gradeF("F", "Fail");
    private String grade;
    private String result;

    private AssessmentResult(String grade, String result) {
      this.grade = grade;
      this.result = result;
    }

    public String getGrade() {
      return grade;
    }

    public String getResult() {
      return result;
    }
  }

  /**
   * This method will calculate the percentage.
   *
   * @param score double score
   * @param maxScore double max score
   * @return double percentage
   */
  public static double calculatePercentage(double score, double maxScore) {
    double percentage = (score * 100) / (maxScore * 1.0);
    return Math.round(percentage);
  }

  /**
   * This method will calculate grade based on percentage marks.
   *
   * @param percentage double percentage
   * @return AssessmentResult
   */
  public static AssessmentResult calcualteAssessmentResult(double percentage) {
    switch (Math.round(Float.valueOf(String.valueOf(percentage))) / 10) {
      case 10:
        return AssessmentResult.gradeA;
      case 9:
        return AssessmentResult.gradeA;
      case 8:
        return AssessmentResult.gradeB;
      case 7:
        return AssessmentResult.gradeC;
      case 6:
        return AssessmentResult.gradeD;
      case 5:
        return AssessmentResult.gradeE;
      default:
        return AssessmentResult.gradeF;
    }
  }

  /**
   * Checks if object is null.
   *
   * @param obj Object
   * @return boolean true if null
   */
  public static boolean isNull(Object obj) {
    return null == obj ? true : false;
  }

  /**
   * Checks if object is not null.
   *
   * @param obj Object
   * @return boolean true if not null
   */
  public static boolean isNotNull(Object obj) {
    return null != obj ? true : false;
  }

  /**
   * Formats message with values.
   *
   * @param exceptionMsg String message pattern
   * @param fieldValue Object... values
   * @return String formatted message
   */
  public static String formatMessage(String exceptionMsg, Object... fieldValue) {
    return MessageFormat.format(exceptionMsg, fieldValue);
  }

  /**
   * Gets default date formatter.
   *
   * @return SimpleDateFormat
   */
  public static SimpleDateFormat getDateFormatter() {
    return getDateFormatter("yyyy-MM-dd HH:mm:ss:SSSZ");
  }

  /**
   * Gets date formatter for pattern.
   *
   * @param pattern String pattern
   * @return SimpleDateFormat
   */
  public static SimpleDateFormat getDateFormatter(String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    simpleDateFormat.setLenient(false);
    return simpleDateFormat;
  }

  /**
   * Enumeration for Enrolment Type.
   */
  public enum EnrolmentType {
    open("open"),
    inviteOnly("invite-only");
    private String val;

    EnrolmentType(String val) {
      this.val = val;
    }

    public String getVal() {
      return val;
    }
  }

  /**
   * Gets Velocity Context from map.
   *
   * @param map Map<String, Object> data
   * @return VelocityContext
   */
  public static VelocityContext getContext(Map<String, Object> map) {
    propertiesCache = PropertiesCache.getInstance();
    VelocityContext context = new VelocityContext();
    if (StringUtils.isNotBlank((String) map.get(JsonKey.ACTION_URL))) {
      context.put(JsonKey.ACTION_URL, getValue(map, JsonKey.ACTION_URL));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.NAME))) {
      context.put(JsonKey.NAME, getValue(map, JsonKey.NAME));
    }
    context.put(JsonKey.BODY, getValue(map, JsonKey.BODY));
    String fromEmail = getFromEmail(map);
    if (StringUtils.isNotBlank(fromEmail)) {
      context.put(JsonKey.FROM_EMAIL, fromEmail);
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.ORG_NAME))) {
      context.put(JsonKey.ORG_NAME, getValue(map, JsonKey.ORG_NAME));
    }
    String logoUrl = getSunbirdLogoUrl(map);
    if (StringUtils.isNotBlank(logoUrl)) {
      context.put(JsonKey.ORG_IMAGE_URL, logoUrl);
    }
    context.put(JsonKey.ACTION_NAME, getValue(map, JsonKey.ACTION_NAME));
    context.put(JsonKey.USERNAME, getValue(map, JsonKey.USERNAME));
    context.put(JsonKey.TEMPORARY_PASSWORD, getValue(map, JsonKey.TEMPORARY_PASSWORD));

    if (StringUtils.isNotBlank((String) map.get(JsonKey.COURSE_NAME))) {
      context.put(JsonKey.COURSE_NAME, map.remove(JsonKey.COURSE_NAME));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.START_DATE))) {
      context.put(JsonKey.BATCH_START_DATE, map.remove(JsonKey.START_DATE));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.END_DATE))) {
      context.put(JsonKey.BATCH_END_DATE, map.remove(JsonKey.END_DATE));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.BATCH_NAME))) {
      context.put(JsonKey.BATCH_NAME, map.remove(JsonKey.BATCH_NAME));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.FIRST_NAME))) {
      context.put(JsonKey.NAME, map.remove(JsonKey.FIRST_NAME));
    } else {
      context.put(JsonKey.NAME, "");
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.SIGNATURE))) {
      context.put(JsonKey.SIGNATURE, map.remove(JsonKey.SIGNATURE));
    }
    if (StringUtils.isNotBlank((String) map.get(JsonKey.COURSE_BATCH_URL))) {
      context.put(JsonKey.COURSE_BATCH_URL, map.remove(JsonKey.COURSE_BATCH_URL));
    }
    context.put(JsonKey.ALLOWED_LOGIN, propertiesCache.getProperty(JsonKey.SUNBIRD_ALLOWED_LOGIN));
    map = addCertStaticResource(map);
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
    return context;
  }

  private static String getSunbirdLogoUrl(Map<String, Object> map) {
    String logoUrl = (String) getValue(map, JsonKey.ORG_IMAGE_URL);
    if (StringUtils.isBlank(logoUrl)) {
      logoUrl = getConfigValue(JsonKey.SUNBIRD_ENV_LOGO_URL);
    }
    logger.info("ProjectUtil:getSunbirdLogoUrl: url = " + logoUrl);
    return logoUrl;
  }

  private static Map<String, Object> addCertStaticResource(Map<String, Object> map) {
    map.putIfAbsent(
        JsonKey.certificateImgUrl,
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_CERT_COMPLETION_IMG_URL));
    map.putIfAbsent(
        JsonKey.dikshaImgUrl, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_DIKSHA_IMG_URL));
    map.putIfAbsent(JsonKey.stateImgUrl, ProjectUtil.getConfigValue(JsonKey.SUNBIRD_STATE_IMG_URL));
    return map;
  }

  private static String getFromEmail(Map<String, Object> map) {
    String fromEmail = (String) getValue(map, JsonKey.EMAIL_SERVER_FROM);
    if (StringUtils.isBlank(fromEmail)) {
      fromEmail = getConfigValue(JsonKey.EMAIL_SERVER_FROM);
    }
    logger.info("ProjectUtil:getFromEmail: fromEmail = " + fromEmail);
    return fromEmail;
  }

  private static Object getValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    map.remove(key);
    return value;
  }

  /**
   * Enumeration for Report Tracking Status.
   */
  public enum ReportTrackingStatus {
    NEW(0),
    GENERATING_DATA(1),
    UPLOADING_FILE(2),
    UPLOADING_FILE_SUCCESS(3),
    SENDING_MAIL(4),
    SENDING_MAIL_SUCCESS(5),
    FAILED(9);

    private int value;

    ReportTrackingStatus(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }
  }

  /**
   * Creates health check response.
   *
   * @param serviceName String service name
   * @param isError boolean is error
   * @param e Exception
   * @return Map<String, Object> response
   */
  public static Map<String, Object> createCheckResponse(
      String serviceName, boolean isError, Exception e) {
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(JsonKey.NAME, serviceName);
    if (!isError) {
      responseMap.put(JsonKey.Healthy, true);
      responseMap.put(JsonKey.ERROR, "");
      responseMap.put(JsonKey.ERRORMSG, "");
    } else {
      responseMap.put(JsonKey.Healthy, false);
      if (e != null && e instanceof ProjectCommonException) {
        ProjectCommonException commonException = (ProjectCommonException) e;
        responseMap.put(JsonKey.ERROR, commonException.getResponseCode());
        responseMap.put(JsonKey.ERRORMSG, commonException.getMessage());
      } else {
        responseMap.put(JsonKey.ERROR, e != null ? e.getMessage() : "CONNECTION_ERROR");
        responseMap.put(JsonKey.ERRORMSG, e != null ? e.getMessage() : "Connection error");
      }
    }
    return responseMap;
  }

  /**
   * This method will make EkStep api call register the tag.
   *
   * @param tagId String unique tag id.
   * @param body String requested body
   * @param header Map<String,String>
   * @return String tag status
   * @throws Exception if error occurs
   */
  public static String registertag(String tagId, String body, Map<String, String> header)
      throws Exception {
    String tagStatus = "";
    try {
      logger.info("start call for registering the tag ==" + tagId);
      String analyticsBaseUrl = getConfigValue(JsonKey.ANALYTICS_API_BASE_URL);
      tagStatus =
          HttpUtil.sendPostRequest(
              analyticsBaseUrl
                  + PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_TAG_API_URL)
                  + "/"
                  + tagId,
              body,
              header);
      logger.info(
          "end call for tag registration id and status  ==" + tagId + " " + tagStatus);
    } catch (Exception e) {
      throw e;
    }
    return tagStatus;
  }

  /**
   * Enumeration for Object Types.
   */
  public enum ObjectTypes {
    user("user"),
    organisation("organisation"),
    batch("batch");

    private String value;

    private ObjectTypes(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * Generates random password.
   *
   * @return String random password
   */
  public static String generateRandomPassword() {
    String SALTCHARS = "abcdef12345ghijklACDEFGHmnopqrs67IJKLMNOP890tuvQRSTUwxyzVWXYZ";
    StringBuilder salt = new StringBuilder();
    Random rnd = new Random();
    while (salt.length() < randomPasswordLength) { // length of the random string.
      int index = (int) (rnd.nextFloat() * SALTCHARS.length());
      salt.append(SALTCHARS.charAt(index));
    }
    String saltStr = salt.toString();
    return saltStr;
  }

  /**
   * This method will do the phone number validation check.
   *
   * @param phone String phone number
   * @return boolean true if valid
   */
  public static boolean validatePhoneNumber(String phone) {
    String phoneNo = "";
    phoneNo = phone.replace("+", "");
    if (phoneNo.matches("\\d{10}")) return true;
    else if (phoneNo.matches("\\d{3}[-\\.\\s]\\d{3}[-\\.\\s]\\d{4}")) return true;
    else if (phoneNo.matches("\\d{3}-\\d{3}-\\d{4}\\s(x|(ext))\\d{3,5}")) return true;
    else return (phoneNo.matches("\\(\\d{3}\\)-\\d{3}-\\d{4}"));
  }

  /**
   * Gets Ekstep header map.
   *
   * @return Map<String, String> headers
   */
  public static Map<String, String> getEkstepHeader() {
    Map<String, String> headerMap = new HashMap<>();
    String header = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(header)) {
      header = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    } else {
      header = JsonKey.BEARER + header;
    }
    headerMap.put(JsonKey.AUTHORIZATION, header);
    headerMap.put("Content-Type", "application/json");
    return headerMap;
  }

  /**
   * Validates phone number with country code.
   *
   * @param phNumber String phone number
   * @param countryCode String country code
   * @return boolean true if valid
   */
  public static boolean validatePhone(String phNumber, String countryCode) {
    PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    String contryCode = countryCode;
    if (!StringUtils.isBlank(countryCode) && (countryCode.charAt(0) != '+')) {
      contryCode = "+" + countryCode;
    }
    Phonenumber.PhoneNumber phoneNumber = null;
    try {
      if (StringUtils.isBlank(countryCode)) {
        contryCode = PropertiesCache.getInstance().getProperty("sunbird_default_country_code");
      }
      String isoCode = phoneNumberUtil.getRegionCodeForCountryCode(Integer.parseInt(contryCode));
      phoneNumber = phoneNumberUtil.parse(phNumber, isoCode);
      return phoneNumberUtil.isValidNumber(phoneNumber);
    } catch (NumberParseException e) {
      logger.error("Exception occurred while validating phone number : ", e);
      logger.info(phNumber + "this phone no. is not a valid one.");
    }
    return false;
  }

  /**
   * Validates country code.
   *
   * @param countryCode String country code
   * @return boolean true if valid
   */
  public static boolean validateCountryCode(String countryCode) {
    String pattern = "^(?:[+] ?){0,1}(?:[0-9] ?){1,3}";
    try {
      Pattern patt = Pattern.compile(pattern);
      Matcher matcher = patt.matcher(countryCode);
      return matcher.matches();
    } catch (RuntimeException e) {
      return false;
    }
  }

  public static boolean validateUUID(String uuidStr) {
    try {
      UUID.fromString(uuidStr);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  /**
   * Generates SMS body from template.
   *
   * @param smsTemplate Map<String, String> template data
   * @return String SMS body
   */
  public static String getSMSBody(Map<String, String> smsTemplate) {
    try {
      Properties props = new Properties();
      props.put("resource.loader", "class");
      props.put(
          "class.resource.loader.class",
          "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

      VelocityEngine ve = new VelocityEngine();
      ve.init(props);
      smsTemplate.put("newline", "\n");
      smsTemplate.put(
          "instanceName",
          StringUtils.isBlank(smsTemplate.get("instanceName"))
              ? ""
              : smsTemplate.get("instanceName"));
      Template t = ve.getTemplate("/welcomeSmsTemplate.vm");
      VelocityContext context = new VelocityContext(smsTemplate);
      StringWriter writer = new StringWriter();
      t.merge(context, writer);
      return writer.toString();
    } catch (Exception ex) {
      logger.error("Exception occurred while formating and sending SMS ", ex);
    }
    return "";
  }

  /**
   * Checks if date is valid format.
   *
   * @param format String date format
   * @param value String date value
   * @return boolean true if valid
   */
  public static boolean isDateValidFormat(String format, String value) {
    Date date = null;
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(format);
      date = sdf.parse(value);
      if (!value.equals(sdf.format(date))) {
        date = null;
      }
    } catch (ParseException ex) {
      logger.error(ex.getMessage(), ex);
    }
    return date != null;
  }

  /**
   * This method will create a new ProjectCommonException of type server Error and throws it.
   */
  public static void createAndThrowServerError() {
    throw new ProjectCommonException(
        ResponseCode.SERVER_ERROR.getErrorCode(),
        ResponseCode.SERVER_ERROR.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  /**
   * This method will create and return server exception to caller.
   *
   * @param responseCode ResponseCode
   * @return ProjectCommonException
   */
  public static ProjectCommonException createServerError(ResponseCode responseCode) {
    return new ProjectCommonException(
        responseCode.getErrorCode(),
        responseCode.getErrorMessage(),
        ResponseCode.SERVER_ERROR.getResponseCode());
  }

  /**
   * This method will create ProjectCommonException of type invalidUserDate exception and throws it.
   */
  public static void createAndThrowInvalidUserDataException() {
    throw new ProjectCommonException(
        ResponseCode.invalidUsrData.getErrorCode(),
        ResponseCode.invalidUsrData.getErrorMessage(),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  /**
   * Method to verify url is valid or not.
   *
   * @param url String
   * @return boolean
   */
  public static boolean isUrlvalid(String url) {
    String[] schemes = {"http", "https"};
    UrlValidator urlValidator = new UrlValidator(schemes);
    return urlValidator.isValid(url);
  }

  /**
   * Gets config value from env or properties.
   *
   * @param key String key
   * @return String value
   */
  public static String getConfigValue(String key) {
    if (StringUtils.isNotBlank(System.getenv(key))) {
      return System.getenv(key);
    }
    return propertiesCache.readProperty(key);
  }

  /**
   * This method will create index for Elastic search as follow "telemetry.raw.yyyy.mm".
   *
   * @return String index name
   */
  public static String createIndex() {
    Calendar cal = Calendar.getInstance();
    return new StringBuffer()
        .append(INDEX_NAME)
        .append("." + cal.get(Calendar.YEAR))
        .append(
            "."
                + ((cal.get(Calendar.MONTH) + 1) > 9
                    ? (cal.get(Calendar.MONTH) + 1)
                    : "0" + (cal.get(Calendar.MONTH) + 1)))
        .toString();
  }

  /**
   * This method will check whether Array contains only empty string or not.
   *
   * @param strArray String[]
   * @return boolean
   */
  public static boolean isNotEmptyStringArray(String[] strArray) {
    for (String str : strArray) {
      if (StringUtils.isNotEmpty(str)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Method to convert List of map to Json String.
   *
   * @param mapList List of map.
   * @return String List of map converted as Json string.
   */
  public static String convertMapToJsonString(List<Map<String, Object>> mapList) {
    try {
      return mapper.writeValueAsString(mapList);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Method to remove attributes from map.
   *
   * @param map contains data as key value.
   * @param keys list of string that has to be remove from map if presents.
   */
  public static void removeUnwantedFields(Map<String, Object> map, String... keys) {
    Arrays.stream(keys)
        .forEach(
            x -> {
              map.remove(x);
            });
  }

  /**
   * Method to convert Json string to Map.
   *
   * @param jsonString represents json string.
   * @return map corresponding to json string.
   * @throws IOException
   */
  public static Map convertJsonStringToMap(String jsonString) throws IOException {
    return mapper.readValue(jsonString, Map.class);
  }

  /**
   * Method to convert Request object to module specific POJO request.
   *
   * @param request Represents the incoming request object.
   * @param clazz Target POJO class.
   * @param <T> Target request object type.
   * @return request object of target type.
   */
  public static <T> T convertToRequestPojo(Request request, Class<T> clazz) {
    return mapper.convertValue(request.getRequest(), clazz);
  }

  /**
   * This method will take number of days in request and provide date range. Date range is
   * calculated as STARTDATE and ENDDATE, start date will be current date minus provided number of
   * days and ENDDATE will be current date minus one day. If date is less than equal to zero then it
   * will return empty map.
   *
   * @param numDays Number of days.
   * @return Map with STARTDATE and ENDDATE key in YYYY_MM_DD_FORMATTER format.
   */
  public static Map<String, String> getDateRange(int numDays) {
    Map<String, String> map = new HashMap<>();
    if (numDays <= 0) {
      return map;
    }
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -numDays);
    map.put(STARTDATE, new SimpleDateFormat(YYYY_MM_DD_FORMATTER).format(cal.getTime()));
    cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.add(Calendar.DATE, -1);
    map.put(ENDDATE, new SimpleDateFormat(YYYY_MM_DD_FORMATTER).format(cal.getTime()));
    return map;
  }

  /**
   * This method will be used to create ProjectCommonException for all kind of client error for the
   * given response code(enum).
   *
   * @param responseCode An enum of all the api responses.
   * @return ProjectCommonException
   */
  public static ProjectCommonException createClientException(ResponseCode responseCode) {
    return new ProjectCommonException(
        responseCode.getErrorCode(),
        responseCode.getErrorMessage(),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public static ProjectCommonException createClientException(
      ResponseCode responseCode, String message) {
    return new ProjectCommonException(
        responseCode.getErrorCode(), message, ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  /**
   * Gets LMS User ID from federated ID.
   *
   * @param fedUserId String federated user id
   * @return String user id
   */
  public static String getLmsUserId(String fedUserId) {
    String userId = fedUserId;
    String prefix =
        "f:" + getConfigValue(JsonKey.SUNBIRD_KEYCLOAK_USER_FEDERATION_PROVIDER_ID) + ":";
    if (StringUtils.isNotBlank(fedUserId) && fedUserId.startsWith(prefix)) {
      userId = fedUserId.replace(prefix, "");
    }
    return userId;
  }

  /**
   * Gets first N characters of string.
   *
   * @param originalText String original text
   * @param noOfChar int number of characters
   * @return String first N characters
   */
  public static String getFirstNCharacterString(String originalText, int noOfChar) {
    String firstNChars = "";
    if (StringUtils.isBlank(originalText)) {
      return "";
    }
    if (originalText.length() > noOfChar) {
      firstNChars = originalText.substring(0, noOfChar);
    } else {
      firstNChars = originalText;
    }
    return firstNChars;
  }

  /**
   * Enumeration for Migrate Action.
   */
  public enum MigrateAction {
    ACCEPT("accept"),
    REJECT("reject");
    private String value;

    MigrateAction(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  public static void setTraceIdInHeader(
      Map<String, String> header, org.sunbird.request.RequestContext context) {
    if (null != context) {
      header.put(JsonKey.X_TRACE_ENABLED, context.getDebugEnabled());
      header.put(JsonKey.X_REQUEST_ID, context.getReqId());
    }
  }
}
