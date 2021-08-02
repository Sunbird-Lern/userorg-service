package org.sunbird.dao.userconsent;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

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
   * @param consentReq consent id.
   * @param context
   * @return UserConsent UserConsent Details.
   */
  List<Map<String, Object>> getConsent(Map<String, Object> consentReq, RequestContext context);
}
