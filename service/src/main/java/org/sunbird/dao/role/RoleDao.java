package org.sunbird.dao.role;

import java.util.List;
import org.sunbird.model.role.Role;
import org.sunbird.request.RequestContext;

public interface RoleDao {

  /**
   * Get list of roles.
   *
   * @return List of all roles.
   */
  List<Role> getRoles(RequestContext context);
}
