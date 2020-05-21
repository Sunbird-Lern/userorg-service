package org.sunbird.learner.actors.role.dao;

import java.util.List;
import org.sunbird.models.role.Role;

public interface RoleDao {

  /**
   * Get list of roles.
   *
   * @return List of all roles.
   */
  List<Role> getRoles();
}
