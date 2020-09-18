package org.sunbird.user.service;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;

import java.util.Map;

public interface UserConsentService {

    Response updateConsent(Map<String, Object> consent, RequestContext context);

    Map<String, Object> getConsent(Map<String, Object> consentReq, RequestContext context);
}
