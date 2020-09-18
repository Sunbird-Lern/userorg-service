package org.sunbird.user.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;

import java.util.Map;

public interface UserConsentDao {

    /**
     * This method will update existing user info or throw ProjectCommonException.
     *
     * @param consent UserConsent Details.
     * @param context
     */
    Response updateConsent(Map<String, Object> consent, RequestContext context);

    /**
     * This method will get UserConsent based on userId and return UserConsent if found else throw
     * ProjectCommonException.
     *
     * @param consentId consent id.
     * @param context
     * @return UserConsent UserConsent Details.
     */
    Map<String, Object> getConsent(String consentId, RequestContext context);

}
