package org.sunbird.service.user.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.user.UserExternalIdentityDao;
import org.sunbird.dao.user.impl.UserExternalIdentityDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.service.user.UserExternalIdentityService;
import org.sunbird.util.user.UserExternalIdentityAdapter;
import org.sunbird.util.user.UserUtil;

public class UserExternalIdentityServiceImpl implements UserExternalIdentityService {
  private final UserExternalIdentityDao userExternalIdentityDao = new UserExternalIdentityDaoImpl();
  private final LocationService locationService = LocationServiceImpl.getInstance();
  private static UserExternalIdentityService selfDeclarationService = null;

  public static UserExternalIdentityService getInstance() {
    if (selfDeclarationService == null) {
      selfDeclarationService = new UserExternalIdentityServiceImpl();
    }
    return selfDeclarationService;
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
    decryptUserExternalIds(dbResExternalIds, context);
    return dbResExternalIds;
  }

  private void decryptUserExternalIds(
      List<Map<String, String>> externalIds, RequestContext context) {
    if (CollectionUtils.isNotEmpty(externalIds)) {
      externalIds
          .stream()
          .forEach(
              s -> {
                s.put(JsonKey.ID, s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                s.put(JsonKey.ID_TYPE, s.get(JsonKey.ORIGINAL_ID_TYPE));
                s.put(JsonKey.PROVIDER, s.get(JsonKey.ORIGINAL_PROVIDER));
                if (StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_EXTERNAL_ID))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_ID_TYPE))
                    && StringUtils.isNotBlank(s.get(JsonKey.ORIGINAL_PROVIDER))) {
                  if (JsonKey.DECLARED_EMAIL.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_PHONE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {

                    String decryptedOriginalExternalId =
                        UserUtil.getDecryptedData(s.get(JsonKey.ORIGINAL_EXTERNAL_ID), context);
                    s.put(JsonKey.ID, decryptedOriginalExternalId);

                  } else if (JsonKey.DECLARED_DISTRICT.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))
                      || JsonKey.DECLARED_STATE.equals(s.get(JsonKey.ORIGINAL_ID_TYPE))) {
                    List<String> locationIds = new ArrayList<>(2);
                    locationIds.add(s.get(JsonKey.ORIGINAL_EXTERNAL_ID));
                    Location location =
                        locationService.getLocationById(
                            s.get(JsonKey.ORIGINAL_EXTERNAL_ID), context);
                    if (null != location) {
                      s.put(
                          JsonKey.ID,
                          (location == null
                              ? s.get(JsonKey.ORIGINAL_EXTERNAL_ID)
                              : location.getCode()));
                    }
                  }
                }

                s.remove(JsonKey.EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_EXTERNAL_ID);
                s.remove(JsonKey.ORIGINAL_ID_TYPE);
                s.remove(JsonKey.ORIGINAL_PROVIDER);
                s.remove(JsonKey.CREATED_BY);
                s.remove(JsonKey.LAST_UPDATED_BY);
                s.remove(JsonKey.LAST_UPDATED_ON);
                s.remove(JsonKey.CREATED_ON);
                s.remove(JsonKey.USER_ID);
                s.remove(JsonKey.SLUG);
              });
    }
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

  @Override
  public boolean deleteUserExternalIds(
      List<Map<String, String>> dbUserExternalIds, RequestContext context) {
    for (Map<String, String> extIdMap : dbUserExternalIds) {
      userExternalIdentityDao.deleteUserExternalId(extIdMap, context);
    }
    return true;
  }
}
