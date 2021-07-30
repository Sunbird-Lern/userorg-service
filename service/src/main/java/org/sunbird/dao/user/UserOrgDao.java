package org.sunbird.user.dao;

import org.sunbird.models.user.org.UserOrg;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserOrgDao {
  Response updateUserOrg(UserOrg userOrg, RequestContext context);

  Response createUserOrg(UserOrg userOrg, RequestContext context);

  Response getUserOrgListByUserId(String userId, RequestContext context);

  Response getUserOrgDetails(String userId, String organisationId, RequestContext context);
}
