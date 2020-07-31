package org.sunbird.user.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.user.dao.UserExternalIdentityDao;
import org.sunbird.user.dao.impl.UserExternalIdentityDaoImpl;
import org.sunbird.user.service.UserExternalIdentityService;
import org.sunbird.user.util.UserExternalIdentityAdapter;

public class UserExternalIdentityServiceImpl implements UserExternalIdentityService {
  private static UserExternalIdentityDao userExternalIdentityDao =
      new UserExternalIdentityDaoImpl();

  @Override
  public List<Map<String, Object>> getSelfDeclaredDetails(
      String userId, String orgId, String role) {
    // Todo:For new Update Api
    return null;
  }

  @Override
  public List<Map<String, String>> getSelfDeclaredDetails(String userId) {
    List<Map<String, String>> externalIds = new ArrayList<>();
    List<Map<String, Object>> dbSelfDeclareExternalIds =
        userExternalIdentityDao.getUserSelfDeclaredDetails(userId);
    if (CollectionUtils.isNotEmpty(dbSelfDeclareExternalIds)) {
      externalIds =
          UserExternalIdentityAdapter.convertSelfDeclareFieldsToExternalIds(
              dbSelfDeclareExternalIds.get(0));
    }
    return externalIds;
  }

  @Override
  public List<Map<String, String>> getUserExternalIds(String userId) {
    return userExternalIdentityDao.getUserExternalIds(userId);
  }

  @Override
  public String getUser(String extId, String provider, String idType) {
    return userExternalIdentityDao.getUserIdByExternalId(extId, provider, idType);
  }
}
