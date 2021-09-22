package org.sunbird.service.user.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.dao.user.UserOrgDao;
import org.sunbird.dao.user.impl.UserOrgDaoImpl;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;

public class UserOrgServiceImpl implements UserOrgService {

  private static UserOrgService userOrgService = null;
  private UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();

  public static UserOrgService getInstance() {
    if (userOrgService == null) {
      userOrgService = new UserOrgServiceImpl();
    }
    return userOrgService;
  }

  @Override
  public List<Map<String, Object>> getUserOrgListByUserId(String userId, RequestContext context) {
    Response response = userOrgDao.getUserOrgListByUserId(userId, context);
    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }

  public void deleteUserOrgMapping(
          List<Map<String, Object>> userOrgList, RequestContext context){
    userOrgDao.deleteUserOrgMapping(userOrgList, context);
  }
}
