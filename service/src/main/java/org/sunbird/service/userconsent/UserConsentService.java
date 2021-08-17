package org.sunbird.service.userconsent;

import java.util.List;
import java.util.Map;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserConsentService {

  Response updateConsent(Map<String, Object> consent, RequestContext context);

  List<Map<String, Object>> getConsent(Request consentReq);
}
