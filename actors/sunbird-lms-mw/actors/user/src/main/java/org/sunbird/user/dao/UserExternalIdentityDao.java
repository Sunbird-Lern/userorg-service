package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.common.request.RequestContext;

public interface UserExternalIdentityDao {

  public String getUserIdByExternalId(
      String extId, String provider, String idType, RequestContext context);

  public List<Map<String, String>> getUserExternalIds(String userId, RequestContext context);

  public List<Map<String, Object>> getUserSelfDeclaredDetails(
      String userId, RequestContext context);
}
