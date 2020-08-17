package org.sunbird.user.service.impl;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.user.dao.UserExternalIdentityDao;
import org.sunbird.user.dao.impl.UserExternalIdentityDaoImpl;
import org.sunbird.user.service.UserExternalIdentityService;
import org.sunbird.user.util.UserExternalIdentityAdapter;
import org.sunbird.user.util.UserUtil;

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

  /**
   * Fetch userid using channel info from usr_external_identity table
   *
   * @param extId
   * @param provider
   * @param idType
   * @return
   */
  @Override
  public String getUserV1(String extId, String provider, String idType) {
    Map<String, String> providerOrgMap = UserUtil.fetchOrgIdByProvider(Arrays.asList(provider));
    return userExternalIdentityDao.getUserIdByExternalId(
        extId, providerOrgMap.get(provider), idType);
  }

  /**
   * Fetch userid using orgId info to support usr_external_identity table
   *
   * @param extId
   * @param orgId
   * @param idType
   * @return
   */
  @Override
  public String getUserV2(String extId, String orgId, String idType) {
    return userExternalIdentityDao.getUserIdByExternalId(extId, orgId, idType);
  }
}
