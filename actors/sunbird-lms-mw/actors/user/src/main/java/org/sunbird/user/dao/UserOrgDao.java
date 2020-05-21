package org.sunbird.user.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.models.user.org.UserOrg;

public interface UserOrgDao {
  Response updateUserOrg(UserOrg userOrg);

  Response createUserOrg(UserOrg userOrg);
}
