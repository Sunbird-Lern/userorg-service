package org.sunbird.dao.user;

import java.util.Map;
import org.sunbird.model.user.UserOrg;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.List;
import java.util.Map;

public interface UserOrgDao {
  Response updateUserOrg(UserOrg userOrg, RequestContext context);

  Response createUserOrg(UserOrg userOrg, RequestContext context);

  Response getUserOrgListByUserId(String userId, RequestContext context);

  Response getUserOrgDetails(String userId, String organisationId, RequestContext context);

  Response insertRecord(Map reqMap, RequestContext context);
  
  void deleteUserOrgMapping(
          List<Map<String, Object>> userOrgList, RequestContext context);
}
