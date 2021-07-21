package org.sunbird.user.service;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserConsentService {

  Response updateConsent(Map<String, Object> consent, RequestContext context);

  List<Map<String, Object>> getConsent(Map<String, Object> consentReq, RequestContext context);
}
