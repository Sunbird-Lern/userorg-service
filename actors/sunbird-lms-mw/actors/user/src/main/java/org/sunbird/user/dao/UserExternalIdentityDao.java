package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;

public interface UserExternalIdentityDao {

  public String getUserIdByExternalId(String extId, String provider, String idType);

  public List<Map<String, String>> getUserExternalIds(String userId);

  public List<Map<String, Object>> getUserSelfDeclaredDetails(String userId);
}
