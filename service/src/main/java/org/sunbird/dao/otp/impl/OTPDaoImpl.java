package org.sunbird.dao.otp.impl;

import java.sql.Timestamp;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.otp.OTPDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.PropertiesCache;

public class OTPDaoImpl implements OTPDao {
  private static final LoggerUtil logger = new LoggerUtil(OTPDaoImpl.class);

  private static final String TABLE_NAME = JsonKey.OTP;
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static volatile OTPDao otpDao;

  public static OTPDao getInstance() {
    if (otpDao == null) {
      synchronized (OTPDaoImpl.class) {
        if (otpDao == null) {
          otpDao = new OTPDaoImpl();
        }
      }
    }
    return otpDao;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> getOTPDetails(String type, String key, RequestContext context) {
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.TYPE, type);
    request.put(JsonKey.KEY, key);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.TYPE);
    fields.add(JsonKey.KEY);
    fields.add(JsonKey.ATTEMPTED_COUNT);
    fields.add(JsonKey.CREATED_ON);
    fields.add(JsonKey.OTP);
    List<String> ttlFields = new ArrayList<>();
    ttlFields.add(JsonKey.OTP);
    Response result =
        cassandraOperation.getRecordWithTTLById(
            JsonKey.SUNBIRD, TABLE_NAME, request, ttlFields, fields, context);
    List<Map<String, Object>> otpMapList = (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(otpMapList)) {
      return null;
    }
    return otpMapList.get(0);
  }

  @Override
  public void insertOTPDetails(String type, String key, String otp, RequestContext context) {
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.TYPE, type);
    request.put(JsonKey.KEY, key);
    request.put(JsonKey.OTP, otp);
    request.put(JsonKey.ATTEMPTED_COUNT, 0);
    request.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    String expirationInSeconds =
        PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_OTP_EXPIRATION);
    int ttl = Integer.valueOf(expirationInSeconds);
    cassandraOperation.insertRecordWithTTL(JsonKey.SUNBIRD, TABLE_NAME, request, ttl, context);
  }

  @Override
  public void deleteOtp(String type, String key, RequestContext context) {
    Map<String, String> compositeKeyMap = new HashMap<>();
    compositeKeyMap.put(JsonKey.TYPE, type);
    compositeKeyMap.put(JsonKey.KEY, key);
    cassandraOperation.deleteRecord(JsonKey.SUNBIRD, TABLE_NAME, compositeKeyMap, context);
    logger.debug(context, "OTPDaoImpl:deleteOtp:otp deleted");
  }

  @Override
  public void updateAttemptCount(Map<String, Object> otpDetails, RequestContext context) {
    Map<String, Object> request = new HashMap<>();
    int ttl = (int) otpDetails.get("otp_ttl");
    otpDetails.remove("otp_ttl");
    request.putAll(otpDetails);
    request.remove(JsonKey.KEY);
    request.remove(JsonKey.TYPE);
    Map<String, Object> compositeKey = new HashMap<>();
    compositeKey.put(JsonKey.TYPE, otpDetails.get(JsonKey.TYPE));
    compositeKey.put(JsonKey.KEY, otpDetails.get(JsonKey.KEY));
    cassandraOperation.updateRecordWithTTL(
        JsonKey.SUNBIRD, TABLE_NAME, request, compositeKey, ttl, context);
  }
}
