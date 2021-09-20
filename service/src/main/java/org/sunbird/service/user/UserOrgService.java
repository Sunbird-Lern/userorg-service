package org.sunbird.service.user;

import java.util.Map;
import org.sunbird.request.RequestContext;

public interface UserOrgService {
  public void registerUserToOrg(Map<String, Object> userMap, RequestContext context);
}
