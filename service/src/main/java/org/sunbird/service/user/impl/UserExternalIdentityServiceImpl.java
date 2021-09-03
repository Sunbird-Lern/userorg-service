package org.sunbird.service.user.impl;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.dao.user.UserExternalIdentityDao;
import org.sunbird.dao.user.impl.UserExternalIdentityDaoImpl;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.UserExternalIdentityService;
import org.sunbird.util.user.UserExternalIdentityAdapter;
import org.sunbird.util.user.UserUtil;

public class UserExternalIdentityServiceImpl implements UserExternalIdentityService {
  private static UserExternalIdentityDao userExternalIdentityDao =
      new UserExternalIdentityDaoImpl();

  @Override
  public List<Map<String, Object>> getSelfDeclaredDetails(
      String userId, String orgId, String role, RequestContext context) {
    // Todo:For new Update Api
    return null;
  }

  @Override
  public List<Map<String, String>> getSelfDeclaredDetails(String userId, RequestContext context) {
    List<Map<String, String>> externalIds = new ArrayList<>();
    List<Map<String, Object>> dbSelfDeclareExternalIds =
        userExternalIdentityDao.getUserSelfDeclaredDetails(userId, context);
    if (CollectionUtils.isNotEmpty(dbSelfDeclareExternalIds)) {
      externalIds =
          UserExternalIdentityAdapter.convertSelfDeclareFieldsToExternalIds(
              dbSelfDeclareExternalIds.get(0));
    }
    return externalIds;
  }

  @Override
  public List<Map<String, String>> getUserExternalIds(String userId, RequestContext context) {
    return userExternalIdentityDao.getUserExternalIds(userId, context);
  }

  @Override
  public List<Map<String, String>> getExternalIds(
      String userId, boolean mergeDeclaration, RequestContext context) {
    List<Map<String, String>> dbResExternalIds = getUserExternalIds(userId, context);
    if (mergeDeclaration) {
      List<Map<String, String>> dbSelfDeclaredExternalIds = getSelfDeclaredDetails(userId, context);
      if (CollectionUtils.isNotEmpty(dbSelfDeclaredExternalIds)) {
        dbResExternalIds.addAll(dbSelfDeclaredExternalIds);
      }
    }
    return dbResExternalIds;
  }

  /**
   * Fetch userid using channel info from usr_external_identity table
   *
   * @param extId
   * @param provider
   * @param idType
   * @param context
   * @return
   */
  @Override
  public String getUserV1(String extId, String provider, String idType, RequestContext context) {
    Map<String, String> providerOrgMap =
        UserUtil.fetchOrgIdByProvider(Arrays.asList(provider), context);
    String orgId = UserUtil.getCaseInsensitiveOrgFromProvider(provider, providerOrgMap);
    return userExternalIdentityDao.getUserIdByExternalId(extId.toLowerCase(), orgId, context);
  }

  /**
   * Fetch userid using orgId info to support usr_external_identity table
   *
   * @param extId
   * @param orgId
   * @param idType
   * @param context
   * @return
   */
  @Override
  public String getUserV2(String extId, String orgId, String idType, RequestContext context) {
    return userExternalIdentityDao.getUserIdByExternalId(extId.toLowerCase(), orgId, context);
  }
}
