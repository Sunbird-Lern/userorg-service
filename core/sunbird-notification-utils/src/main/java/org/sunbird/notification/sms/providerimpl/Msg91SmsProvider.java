package org.sunbird.notification.sms.providerimpl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.notification.sms.Sms;
import org.sunbird.notification.sms.provider.ISmsProvider;
import org.sunbird.notification.utils.JsonUtil;
import org.sunbird.notification.utils.PropertiesCache;
import org.sunbird.request.RequestContext;

/**
 * Implementation of {@link ISmsProvider} for the Msg91 service. This class handles the
 * configuration and execution of SMS requests via both GET and POST methods as configured.
 */
public class Msg91SmsProvider implements ISmsProvider {
  private static final LoggerUtil logger = new LoggerUtil(Msg91SmsProvider.class);

  private static String baseUrl = null;
  private static String getUrl = null;
  private static String postUrl = null;
  private static String sender = null;
  private static String smsRoute = null;
  private static String smsMethodType = null;
  private static String authKey = null;
  private static String country = null;

  static {
    boolean response = init();
    logger.info("SMS configuration values are set ==" + response);
  }

  /**
   * Initializes the Msg91 configuration properties from system environment variables or property
   * files.
   *
   * @return true if mandatory settings are correctly configured.
   */
  public static boolean init() {
    baseUrl = PropertiesCache.getInstance().getProperty("sunbird.msg.91.baseurl");
    getUrl = PropertiesCache.getInstance().getProperty("sunbird.msg.91.get.url");
    postUrl = PropertiesCache.getInstance().getProperty("sunbird.msg.91.post.url");
    sender = System.getenv("sunbird_msg_sender");
    if (JsonUtil.isStringNullOREmpty(sender)) {
      sender = PropertiesCache.getInstance().getProperty("sunbird.msg.91.sender");
    }
    smsRoute = PropertiesCache.getInstance().getProperty("sunbird.msg.91.route");
    smsMethodType = PropertiesCache.getInstance().getProperty("sunbird.msg.91.method");
    country = PropertiesCache.getInstance().getProperty("sunbird.msg.91.country");
    // first read authkey from ENV variable if not found then read it from property file.
    authKey = System.getenv("sunbird_msg_91_auth");
    if (JsonUtil.isStringNullOREmpty(authKey)) {
      authKey = PropertiesCache.getInstance().getProperty("sunbird.msg.91.auth");
    }
    return validateSettings();
  }

  @Override
  public boolean send(String phoneNumber, String smsText, RequestContext context) {
    if ("POST".equalsIgnoreCase(smsMethodType)) {
      return sendSmsUsingPost(phoneNumber, smsText, context);
    }
    return sendSmsGetMethod(phoneNumber, smsText, context);
  }

  @Override
  public boolean send(
      String phoneNumber, String countryCode, String smsText, RequestContext context) {
    if ("POST".equalsIgnoreCase(smsMethodType)) {
      return sendSmsUsingPost(phoneNumber, smsText, context);
    }
    return sendSmsGetMethod(phoneNumber, smsText, context);
  }

  /**
   * Sends an SMS using the HTTP POST method.
   *
   * @param mobileNumber The recipient's mobile number.
   * @param smsText The content of the SMS.
   * @param context Request context for logging.
   * @return true if the request was successful (HTTP 200).
   */
  private boolean sendSmsUsingPost(String mobileNumber, String smsText, RequestContext context) {
    logger.debug(
        context, "Msg91SmsProvider@Sending " + smsText + "  to mobileNumber " + mobileNumber);
    logger.debug(
        context,
        "Msg91SmsProvider@SMS Provider parameters \n"
            + "Gateway - "
            + baseUrl
            + "\n"
            + "authKey - "
            + authKey
            + "\n"
            + "sender - "
            + sender
            + "\n"
            + "country - "
            + country
            + "\n"
            + "smsMethodType - "
            + smsMethodType
            + "\n"
            + "smsRoute - "
            + smsRoute
            + "\n");

    CloseableHttpClient httpClient = null;
    try {
      httpClient = HttpClients.createDefault();

      String path = null;

      if (validateSettings(mobileNumber, smsText)) {
        String tempMobileNumber = removePlusFromMobileNumber(mobileNumber);

        logger.debug(
            context, "Msg91SmsProvider - after removePlusFromMobileNumber " + tempMobileNumber);
        // add dlt template id header
        String templateId = getTemplateId(smsText, MSG_91_PROVIDER);
        if (StringUtils.isBlank(templateId)) {
          logger.info(context, "dlt template id is empty for sms : " + smsText);
        }
        path = baseUrl + postUrl;
        logger.debug(context, "Msg91SmsProvider -Executing request - " + path);

        HttpPost httpPost = new HttpPost(path);

        // add content-type headers
        httpPost.setHeader("content-type", "application/json");

        // add authkey header
        httpPost.setHeader("authkey", authKey);
        logger.debug(context, "Msg91SmsProvider -request header- " + httpPost.getAllHeaders());

        List<String> mobileNumbers = new ArrayList<>();
        mobileNumbers.add(tempMobileNumber);

        // create sms
        Sms sms = new Sms(getDoubleEncodedSMS(smsText), mobileNumbers);

        List<Sms> smsList = new ArrayList<>();
        smsList.add(sms);

        // create body
        ProviderDetails providerDetails =
            new ProviderDetails(sender, smsRoute, country, 1, smsList, templateId);
        String providerDetailsString = JsonUtil.toJson(providerDetails, context);
        providerDetailsString = providerDetailsString.replaceAll("dlt_TE_ID", "DLT_TE_ID");

        if (!JsonUtil.isStringNullOREmpty(providerDetailsString)) {
          logger.debug(context, "Msg91SmsProvider - Body - " + providerDetailsString);

          HttpEntity entity =
              new ByteArrayEntity(providerDetailsString.getBytes(StandardCharsets.UTF_8));
          httpPost.setEntity(entity);

          CloseableHttpResponse response = httpClient.execute(httpPost);
          StatusLine sl = response.getStatusLine();
          response.close();
          if (sl.getStatusCode() != 200) {
            logger.info(
                context,
                "SMS code for "
                    + tempMobileNumber
                    + " could not be sent: "
                    + sl.getStatusCode()
                    + " - "
                    + sl.getReasonPhrase());
          }
          logger.info(context, "Status code for Msg91SmsProvider : " + sl.getStatusCode());
          return sl.getStatusCode() == 200;
        } else {
          return false;
        }
      } else {
        logger.debug(context, "Msg91SmsProvider - Some mandatory parameters are empty!");
        return false;
      }
    } catch (IOException e) {
      logger.error(context, "Error occurred :", e);
      return false;
    } catch (Exception e) {
      logger.info(context, "Msg91SmsProvider - Error in converting providerDetails to string!");
      return false;
    } finally {
      closeHttpResource(httpClient);
    }
  }

  /**
   * Sends an SMS using the HTTP GET method.
   *
   * @param mobileNumber The recipient's mobile number.
   * @param smsText The content of the SMS.
   * @param context Request context for logging.
   * @return true if the request was successful (HTTP 200).
   */
  public boolean sendSmsGetMethod(String mobileNumber, String smsText, RequestContext context) {
    CloseableHttpClient httpClient = null;
    try {
      httpClient = HttpClients.createDefault();
      String path = null;
      if (validateSettings(mobileNumber, smsText)) {

        String tempMobileNumber = removePlusFromMobileNumber(mobileNumber);

        logger.debug(
            context, "Msg91SmsProvider - after removePlusFromMobileNumber " + tempMobileNumber);

        path =
            getCompletePath(
                baseUrl + getUrl,
                sender,
                smsRoute,
                tempMobileNumber,
                authKey,
                URLEncoder.encode(getDoubleEncodedSMS(smsText), "UTF-8"));

        logger.debug(context, "Msg91SmsProvider -Executing request - " + path);

        HttpGet httpGet = new HttpGet(path);

        CloseableHttpResponse response = httpClient.execute(httpGet);
        StatusLine sl = response.getStatusLine();
        response.close();
        if (sl.getStatusCode() != 200) {
          logger.info(
              "SMS code for "
                  + tempMobileNumber
                  + " could not be sent: "
                  + sl.getStatusCode()
                  + " - "
                  + sl.getReasonPhrase());
        }
        logger.info(context, "Status code for Msg91SmsProvider : " + sl.getStatusCode());
        return sl.getStatusCode() == 200;

      } else {
        logger.debug(context, "Msg91SmsProvider - Some mandatory parameters are empty!");
        return false;
      }
    } catch (IOException e) {
      logger.error(context, "Error occurred : ", e);
      return false;
    } finally {
      closeHttpResource(httpClient);
    }
  }

  /**
   * Removes the '+' symbol from the start of a mobile number if present.
   *
   * @param mobileNumber Mobiler number to process.
   * @return Mobile number without the leading '+'.
   */
  private String removePlusFromMobileNumber(String mobileNumber) {
    logger.debug("Msg91SmsProvider - removePlusFromMobileNumber " + mobileNumber);

    if (mobileNumber.startsWith("+")) {
      return mobileNumber.substring(1);
    }
    return mobileNumber;
  }

  /**
   * Constructs the complete URL path for a GET request.
   *
   * @param gateWayUrl The base gateway URL.
   * @param sender The sender identifier.
   * @param smsRoute The SMS route.
   * @param mobileNumber Recipient mobile number.
   * @param authKey Authentication key.
   * @param smsText Encoded SMS text.
   * @return Fully constructed URL string.
   */
  private String getCompletePath(
      String gateWayUrl,
      String sender,
      String smsRoute,
      String mobileNumber,
      String authKey,
      String smsText) {
    StringBuilder builder = new StringBuilder();
    builder.append(gateWayUrl).append("sender=").append(sender).append("&route=").append(smsRoute);
    builder.append("&mobiles=").append(mobileNumber).append("&authkey=").append(authKey);
    builder.append("&message=").append(getDoubleEncodedSMS(smsText));
    return builder.toString();
  }

  /**
   * Safely closes the HTTP client resource.
   *
   * @param httpClient The client instance to close.
   */
  private void closeHttpResource(CloseableHttpClient httpClient) {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (IOException e) {
        logger.error("Error occurred while closing http connection", e);
      }
    }
  }

  /**
   * Validates if mandatory settings and parameters are present for a specific request.
   *
   * @param phone Recipient phone number.
   * @param smsText SMS content.
   * @return true if all mandatory parameters are present.
   */
  private boolean validateSettings(String phone, String smsText) {
    if (!JsonUtil.isStringNullOREmpty(sender)
        && !JsonUtil.isStringNullOREmpty(smsRoute)
        && !JsonUtil.isStringNullOREmpty(phone)
        && !JsonUtil.isStringNullOREmpty(authKey)
        && !JsonUtil.isStringNullOREmpty(country)
        && !JsonUtil.isStringNullOREmpty(smsText)) {
      return true;
    }
    logger.info("SMS value is not configure properly.");
    return false;
  }

  /**
   * Validates global mandatory settings.
   *
   * @return true if global mandatory settings are present.
   */
  private static boolean validateSettings() {
    if (!JsonUtil.isStringNullOREmpty(sender)
        && !JsonUtil.isStringNullOREmpty(smsRoute)
        && !JsonUtil.isStringNullOREmpty(authKey)
        && !JsonUtil.isStringNullOREmpty(country)) {
      return true;
    }
    logger.info("SMS value is not configure properly.");
    return false;
  }

  @Override
  public boolean send(List<String> phoneNumber, String smsText, RequestContext context) {
    List<String> phoneNumberList = null;
    logger.debug(context, "Msg91SmsProvider@Sending " + smsText + "  to mobileNumber ");
    logger.debug(
        context,
        "Msg91SmsProvider@SMS Provider parameters \n"
            + "Gateway - "
            + baseUrl
            + "\n"
            + "authKey - "
            + authKey
            + "\n"
            + "sender - "
            + sender
            + "\n"
            + "country - "
            + country
            + "\n"
            + "smsMethodType - "
            + smsMethodType
            + "\n"
            + "smsRoute - "
            + smsRoute
            + "\n");
    if (JsonUtil.isStringNullOREmpty(smsText)) {
      logger.debug(context, "can't sent empty msg.");
      return false;
    }
    phoneNumberList = validatePhoneList(phoneNumber);
    if (phoneNumberList == null || phoneNumberList.isEmpty()) {
      logger.debug(context, "can't sent msg with empty phone list.");
      return false;
    }
    CloseableHttpClient httpClient = null;
    try {
      httpClient = HttpClients.createDefault();

      String path = null;
      // add dlt template id header
      String templateId = getTemplateId(smsText, MSG_91_PROVIDER);
      if (StringUtils.isBlank(templateId)) {
        logger.info(context, "dlt template id is empty for sms : " + smsText);
      }
      path = baseUrl + postUrl;
      logger.debug(context, "Msg91SmsProvider -Executing request - " + path);
      HttpPost httpPost = new HttpPost(path);

      // add content-type headers
      httpPost.setHeader("content-type", "application/json");

      // add authkey header
      httpPost.setHeader("authkey", authKey);

      // create sms
      Sms sms = new Sms(getDoubleEncodedSMS(smsText), phoneNumberList);

      List<Sms> smsList = new ArrayList<>();
      smsList.add(sms);

      // create body
      ProviderDetails providerDetails =
          new ProviderDetails(sender, smsRoute, country, 1, smsList, templateId);
      String providerDetailsString = JsonUtil.toJson(providerDetails, context);
      providerDetailsString = providerDetailsString.replaceAll("dlt_TE_ID", "DLT_TE_ID");

      if (!JsonUtil.isStringNullOREmpty(providerDetailsString)) {
        logger.debug(context, "Msg91SmsProvider - Body - " + providerDetailsString);

        HttpEntity entity =
            new ByteArrayEntity(providerDetailsString.getBytes(StandardCharsets.UTF_8));
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        StatusLine sl = response.getStatusLine();
        response.close();
        if (sl.getStatusCode() != 200) {
          logger.info(
              context,
              "SMS code for "
                  + phoneNumberList
                  + " could not be sent: "
                  + sl.getStatusCode()
                  + " - "
                  + sl.getReasonPhrase());
        }
        logger.info(context, "Status code for Msg91SmsProvider : " + sl.getStatusCode());
        return sl.getStatusCode() == 200;
      } else {
        return false;
      }

    } catch (IOException e) {
      logger.error(context, "error in converting providerDetails to String", e);
      return false;
    } catch (Exception e) {
      logger.error(
          context, "Msg91SmsProvider : send : error in converting providerDetails to String", e);
      return false;
    } finally {
      closeHttpResource(httpClient);
    }
  }

  /**
   * Validates the list of phone numbers, removing any that are null, empty, or invalid (length
   * &lt; 10).
   *
   * @param phones List of phone numbers to validate.
   * @return Validated list of phone numbers.
   */
  private List<String> validatePhoneList(List<String> phones) {
    if (phones != null) {
      Iterator<String> itr = phones.iterator();
      while (itr.hasNext()) {
        String phone = itr.next();
        if (JsonUtil.isStringNullOREmpty(phone) || phone.length() < 10) {
          itr.remove();
        }
      }
    }
    return phones;
  }

  /**
   * Returns a UTF-8 encoded string.
   *
   * @param smsText Original SMS text.
   * @return Encoded string.
   */
  private String getDoubleEncodedSMS(String smsText) {
    String smsUtf8 = new String(smsText.getBytes(), StandardCharsets.UTF_8);
    String doubleEncodedSMS = new String(smsUtf8.getBytes(), StandardCharsets.UTF_8);
    return doubleEncodedSMS;
  }
}
