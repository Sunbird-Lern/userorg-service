package org.sunbird.dao.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

public interface UserExternalIdentityDao {

  public String getUserIdByExternalId(String extId, String provider, RequestContext context);

  public List<Map<String, String>> getUserExternalIds(String userId, RequestContext context);

  public List<Map<String, Object>> getUserSelfDeclaredDetails(
      String userId, RequestContext context);

  public void deleteUserExternalId(Map<String, String> map, RequestContext context);
}
