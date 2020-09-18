package org.sunbird.user.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.UserConsent;

public interface UserConsentDao {

    /**
     * This method will update existing user info or throw ProjectCommonException.
     *
     * @param consent UserConsent Details.
     * @param context
     */
    Response updateConsent(UserConsent consent, RequestContext context);

    /**
     * This method will get UserConsent based on userId and return UserConsent if found else throw
     * ProjectCommonException.
     *
     * @param consentId consent id.
     * @param context
     * @return UserConsent UserConsent Details.
     */
    UserConsent getConsent(String consentId, RequestContext context);

}
