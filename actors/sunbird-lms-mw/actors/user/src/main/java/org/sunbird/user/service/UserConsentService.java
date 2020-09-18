package org.sunbird.user.service;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.UserConsent;

public interface UserConsentService {

    Response updateConsent(UserConsent consent, RequestContext context);

    UserConsent getConsent(String consentId, RequestContext context);
}
