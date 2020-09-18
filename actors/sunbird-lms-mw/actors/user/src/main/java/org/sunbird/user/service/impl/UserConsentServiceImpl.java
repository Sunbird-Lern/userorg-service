package org.sunbird.user.service.impl;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.user.dao.UserConsentDao;
import org.sunbird.user.dao.impl.UserConsentDaoImpl;
import org.sunbird.user.service.UserConsentService;

import java.util.Map;

public class UserConsentServiceImpl implements UserConsentService {

    private static UserConsentDao userConsentDao = UserConsentDaoImpl.getInstance();

    private static UserConsentService consentService = null;

    public static UserConsentService getInstance() {
        if (consentService == null) {
            consentService = new UserConsentServiceImpl();
        }
        return consentService;
    }

    public Response updateConsent(Map<String, Object> consent, RequestContext context){
        return userConsentDao.updateConsent(consent, context);
    }

    public Map<String, Object> getConsent(String consentId, RequestContext context){
        return userConsentDao.getConsent(consentId, context);
    }
}
