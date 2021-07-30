package org.sunbird.user.service.impl;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.user.dao.UserConsentDao;
import org.sunbird.user.dao.impl.UserConsentDaoImpl;
import org.sunbird.user.service.UserConsentService;

public class UserConsentServiceImpl implements UserConsentService {

  private static UserConsentDao userConsentDao = UserConsentDaoImpl.getInstance();

  private static UserConsentService consentService = null;

  public static UserConsentService getInstance() {
    if (consentService == null) {
      consentService = new UserConsentServiceImpl();
    }
    return consentService;
  }

  public Response updateConsent(Map<String, Object> consent, RequestContext context) {
    return userConsentDao.updateConsent(consent, context);
  }

  public List<Map<String, Object>> getConsent(
      Map<String, Object> consentReq, RequestContext context) {
    return userConsentDao.getConsent(consentReq, context);
  }
}
