package org.sunbird.dao.role;

import org.sunbird.model.role.Role;

import java.util.List;

public interface RoleDao {

  /**
   * Get list of roles.
   *
   * @return List of all roles.
   */
  List<Role> getRoles();
}
