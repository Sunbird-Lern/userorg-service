package org.sunbird.dao.role;

import java.util.List;
import org.sunbird.model.role.RoleGroup;
import org.sunbird.request.RequestContext;

public interface RoleGroupDao {

  /**
   * Get list of role groups.
   *
   * @return List of role groups.
   */
  List<RoleGroup> getRoleGroups(RequestContext context);
}
