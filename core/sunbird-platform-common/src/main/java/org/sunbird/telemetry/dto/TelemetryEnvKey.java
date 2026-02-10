package org.sunbird.telemetry.dto;

/**
 * Constants for Telemetry Environment Keys.
 * Defines the environment names used in various telemetry events.
 */
public class TelemetryEnvKey {

  /** Constant for User environment */
  public static final String USER = "User";

  /** Constant for Organisation environment */
  public static final String ORGANISATION = "Organisation";

  /** Constant for GeoLocation environment */
  public static final String GEO_LOCATION = "GeoLocation";

  /** Constant for MasterKey environment */
  public static final String MASTER_KEY = "MasterKey";

  /** Constant for ObjectStore environment */
  public static final String OBJECT_STORE = "ObjectStore";

  /** Constant for Location environment */
  public static final String LOCATION = "Location";

  /** Constant for Request environment */
  public static final String REQUEST_UPPER_CAMEL = "Request";

  /** Constant for UserConsent environment */
  public static final String USER_CONSENT = "UserConsent";

  /** Constant for Edata Type User Consent */
  public static final String EDATA_TYPE_USER_CONSENT = "user-consent";

  /** Constant for CourseBatch environment */
  public static final String BATCH = "CourseBatch";

  /** Constant for Page environment */
  public static final String PAGE = "Page";

  /** Constant for PageSection environment */
  public static final String PAGE_SECTION = "PageSection";

  /** Constant for QRCodeDownload environment */
  public static final String QR_CODE_DOWNLOAD = "QRCodeDownload";

  /** Constant for COURSE_CREATE environment */
  public static final String COURSE_CREATE = "COURSE_CREATE";

  /** Constant for Notification Created environment */
  public static final String NOTIFICATION_CREATED = "create-notification";

  /** Private constructor to prevent instantiation. */
  private TelemetryEnvKey() {}
}
