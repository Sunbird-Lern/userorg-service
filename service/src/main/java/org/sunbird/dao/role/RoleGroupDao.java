package org.sunbird.dao.role;

import org.sunbird.model.role.RoleGroup;

import java.util.List;

public interface RoleGroupDao {

    /**
     * Get list of role groups.
     *
     * @return List of role groups.
     */
    List<RoleGroup> getRoleGroups();
}
