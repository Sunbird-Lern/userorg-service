package org.sunbird.service.userconsent.impl;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dao.organisation.impl.OrgDaoImpl;
import org.sunbird.dao.userconsent.UserConsentDao;
import org.sunbird.dao.userconsent.impl.UserConsentDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.user.DateUtil;

public class UserConsentServiceImpl implements UserConsentService {

  private final LoggerUtil logger = new LoggerUtil(UserConsentServiceImpl.class);
  private final UserConsentDao userConsentDao = UserConsentDaoImpl.getInstance();
  private final OrgDao orgDao = OrgDaoImpl.getInstance();

  private static UserConsentService consentService = null;

  enum CONSENT_CONSUMER_TYPE {
    ORGANISATION;
  }

  public static UserConsentService getInstance() {
    if (consentService == null) {
      consentService = new UserConsentServiceImpl();
    }
    return consentService;
  }

  public Response updateConsent(Map<String, Object> consent, RequestContext context) {
    logger.debug(
        context,
        "UserConsentServiceImpl:updateConsent method called : " + consent.get(JsonKey.USER_ID));
    Map consentReq = constructConsentRequest(consent, context);
    Response consentRes = userConsentDao.updateConsent(consentReq, context);
    consentRes.getResult().put(JsonKey.ID, consent.get(JsonKey.ID));
    return consentRes;
  }

  public List<Map<String, Object>> getConsent(Request request) {
    logger.debug(request.getRequestContext(), "UserConsentServiceImpl:getConsent method called : ");
    Map<String, Object> consent =
        (Map<String, Object>)
            request.getRequest().getOrDefault(JsonKey.CONSENT_BODY, new HashMap<String, Object>());
    Map<String, Object> filters =
        (Map<String, Object>) consent.getOrDefault(JsonKey.FILTERS, new HashMap<String, Object>());

    Map<String, Object> getConsentFromDBReq = new HashMap<>();
    getConsentFromDBReq.put(JsonKey.CONSENT_USER_ID, filters.get(JsonKey.USER_ID));
    getConsentFromDBReq.put(JsonKey.CONSENT_CONSUMER_ID, filters.get(JsonKey.CONSENT_CONSUMERID));
    getConsentFromDBReq.put(JsonKey.CONSENT_OBJECT_ID, filters.get(JsonKey.CONSENT_OBJECTID));
    logger.debug(
        request.getRequestContext(),
        "UserConsentServiceImpl:getConsent for userid : " + filters.get(JsonKey.USER_ID));
    return userConsentDao.getConsent(getConsentFromDBReq, request.getRequestContext());
  }

  private Map<String, Object> constructConsentRequest(
      Map<String, Object> consent, RequestContext context) {
    String userId = (String) consent.get(JsonKey.USER_ID);
    String consumerId = (String) consent.get(JsonKey.CONSENT_CONSUMERID);
    String consumerType =
        (String)
            consent.getOrDefault(
                JsonKey.CONSENT_CONSUMERTYPE,
                UserConsentServiceImpl.CONSENT_CONSUMER_TYPE.ORGANISATION.toString());
    String objectId = (String) consent.get(JsonKey.CONSENT_OBJECTID);
    String objectType = (String) consent.getOrDefault(JsonKey.CONSENT_OBJECTTYPE, "");
    String status = (String) consent.get(JsonKey.STATUS);

    String key = getKey(userId, consumerId, objectId);
    Map<String, Object> consentReq = new HashMap<String, Object>();
    consentReq.put(JsonKey.ID, key);
    consentReq.put(JsonKey.CONSENT_USER_ID, userId);
    consentReq.put(JsonKey.CONSENT_CONSUMER_ID, consumerId);
    consentReq.put(JsonKey.CONSENT_OBJECT_ID, objectId);
    consentReq.put(JsonKey.CONSENT_CONSUMER_TYPE, consumerType);
    consentReq.put(JsonKey.CONSENT_OBJECT_TYPE, objectType);
    consentReq.put(JsonKey.STATUS, status);
    consentReq.put(JsonKey.CONSENT_EXPIRY, new Timestamp(calculateConsentExpiryDate()));

    Map<String, Object> getConsentFromDBReq = new HashMap<String, Object>();
    getConsentFromDBReq.put(JsonKey.CONSENT_USER_ID, userId);
    getConsentFromDBReq.put(JsonKey.CONSENT_CONSUMER_ID, consumerId);
    getConsentFromDBReq.put(JsonKey.CONSENT_OBJECT_ID, objectId);
    // Check if consent is already existing
    List<Map<String, Object>> dbCconsentMap =
        userConsentDao.getConsent(getConsentFromDBReq, context);
    if (CollectionUtils.isNotEmpty(dbCconsentMap) && MapUtils.isNotEmpty(dbCconsentMap.get(0))) {
      consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
    } else {
      consentReq.put(JsonKey.CONSENT_CREATED_ON, DateUtil.getCurrentDateTimestamp());
      consentReq.put(JsonKey.CONSENT_LAST_UPDATED_ON, DateUtil.getCurrentDateTimestamp());
    }

    return consentReq;
  }

  public void validateConsumerId(String consumerId, RequestContext context) {
    String custodianOrgId = DataCacheHandler.getConfigSettings().get(JsonKey.CUSTODIAN_ORG_ID);
    if (consumerId.equalsIgnoreCase(custodianOrgId)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameter,
        MessageFormat.format(
          ResponseCode.invalidParameter.getErrorMessage(),
          JsonKey.ORGANISATION + JsonKey.ID));
    }
    Map<String, Object> org = orgDao.getOrgById(consumerId, context);
    if (MapUtils.isEmpty(org)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidParameter,
        MessageFormat.format(
          ResponseCode.invalidParameter.getErrorMessage(),
          JsonKey.ORGANISATION + JsonKey.ID));
    }
  }

  private String getKey(String userId, String consumerId, String objectId) {
    return "usr-consent:" + userId + ":" + consumerId + ":" + objectId;
  }

  private long calculateConsentExpiryDate() {
    int consentExpiry =
        Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.CONSENT_EXPIRY_IN_DAYS));
    return DateUtil.addDaysToDate(new Date(), consentExpiry).getTime();
  }
}
