package org.sunbird.learner.actors.role.group.dao;

import java.util.List;
import org.sunbird.models.role.group.RoleGroup;

public interface RoleGroupDao {

  /**
   * Get list of role groups.
   *
   * @return List of role groups.
   */
  List<RoleGroup> getRoleGroups();
}
