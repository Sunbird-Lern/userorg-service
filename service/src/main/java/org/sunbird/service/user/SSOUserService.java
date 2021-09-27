package org.sunbird.service.user;

import java.util.Map;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public interface SSOUserService {

  Map validateOrgIdAndPrimaryRecoveryKeys(Map<String, Object> userMap, Request actorMessage);

  Response createUserAndPassword(
      Map<String, Object> requestMap, Map<String, Object> userMap, Request request);
}
