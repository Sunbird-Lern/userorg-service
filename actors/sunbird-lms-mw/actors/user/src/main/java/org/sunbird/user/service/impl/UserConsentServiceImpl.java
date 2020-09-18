package org.sunbird.user.service.impl;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.UserConsent;
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

    public Response updateConsent(UserConsent consent, RequestContext context){
        return userConsentDao.updateConsent(consent, context);
    }

    public UserConsent getConsent(String consentId, RequestContext context){
        UserConsent consent = userConsentDao.getConsent(consentId, context);
        return consent;
    }
}
