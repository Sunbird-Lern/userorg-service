package org.sunbird.service.user.impl;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.UserOrg;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.common.ProjectUtil;

public class UserOrgServiceImpl implements UserOrgService {
  private static UserOrgServiceImpl userOrgService = null;
  private final LoggerUtil logger = new LoggerUtil(UserOrgServiceImpl.class);
  private final UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();

  public static UserOrgService getInstance() {
    if (userOrgService == null) {
      userOrgService = new UserOrgServiceImpl();
    }
    return userOrgService;
  }

  @Override
  public void registerUserToOrg(Map<String, Object> userMap, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>();
    reqMap.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
    reqMap.put(JsonKey.USER_ID, userMap.get(JsonKey.ID));
    reqMap.put(JsonKey.ORGANISATION_ID, userMap.get(JsonKey.ORGANISATION_ID));
    reqMap.put(JsonKey.ORG_JOIN_DATE, ProjectUtil.getFormattedDate());
    reqMap.put(JsonKey.IS_DELETED, false);
    reqMap.put(JsonKey.ASSOCIATION_TYPE, userMap.get(JsonKey.ASSOCIATION_TYPE));
    if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.HASHTAGID))) {
      reqMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.HASHTAGID));
    }
    try {
      userOrgDao.insertRecord(reqMap, context);
    } catch (Exception e) {
      logger.error(context, e.getMessage(), e);
    }
  }

  public List<Map<String, Object>> getUserOrgListByUserId(String userId, RequestContext context) {
    Response response = userOrgDao.getUserOrgListByUserId(userId, context);
    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }

  public void deleteUserOrgMapping(List<Map<String, Object>> userOrgList, RequestContext context) {
    userOrgDao.deleteUserOrgMapping(userOrgList, context);
  }

  public void softDeleteOldUserOrgMapping(
      List<Map<String, Object>> userOrgList, RequestContext context) {
    for (Map<String, Object> userOrgMap : userOrgList) {
      Response response =
          userOrgDao.getUserOrgDetails(
              (String) userOrgMap.get(JsonKey.USER_ID),
              (String) userOrgMap.get(JsonKey.ORGANISATION_ID),
              context);
      List<Map<String, Object>> resList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (!resList.isEmpty()) {
        Map<String, Object> res = resList.get(0);
        try {
          UserOrg userOrg = new UserOrg();
          userOrg.setUserId((String) res.get(JsonKey.USER_ID));
          userOrg.setOrganisationId((String) res.get(JsonKey.ORGANISATION_ID));
          userOrg.setDeleted(true);
          userOrgDao.updateUserOrg(userOrg, context);
        } catch (Exception e) {
          logger.error(context, "upsertUserOrgData exception : " + e.getMessage(), e);
        }
      }
    }
  }

  @Override
  public void upsertUserOrgData(Map<String, Object> userMap, RequestContext context) {
    Response response =
        userOrgDao.getUserOrgDetails(
            (String) userMap.get(JsonKey.ID),
            (String) userMap.get(JsonKey.ORGANISATION_ID),
            context);
    List<Map<String, Object>> resList = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (!resList.isEmpty()) {
      Map<String, Object> res = resList.get(0);
      Map<String, Object> reqMap = new WeakHashMap<>();
      reqMap.put(JsonKey.ID, res.get(JsonKey.ID));
      if (null != userMap.get(JsonKey.ROLES)) {
        reqMap.put(JsonKey.ROLES, userMap.get(JsonKey.ROLES));
      }
      reqMap.put(JsonKey.UPDATED_BY, userMap.get(JsonKey.UPDATED_BY));
      reqMap.put(JsonKey.ASSOCIATION_TYPE, userMap.get(JsonKey.ASSOCIATION_TYPE));
      reqMap.put(JsonKey.IS_DELETED, false);
      reqMap.put(JsonKey.UPDATED_DATE, ProjectUtil.getFormattedDate());
      if (StringUtils.isNotEmpty((String) userMap.get(JsonKey.HASHTAGID))) {
        reqMap.put(JsonKey.HASHTAGID, userMap.get(JsonKey.HASHTAGID));
      }
      try {
        ObjectMapper mapper = new ObjectMapper();
        UserOrg userOrg = mapper.convertValue(reqMap, UserOrg.class);
        userOrgDao.updateUserOrg(userOrg, context);
      } catch (Exception e) {
        logger.error(context, "upsertUserOrgData exception : " + e.getMessage(), e);
      }
    } else {
      registerUserToOrg(userMap, context);
    }
  }
}
