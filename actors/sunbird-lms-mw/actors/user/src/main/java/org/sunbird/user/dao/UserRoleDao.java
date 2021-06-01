package org.sunbird.user.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.common.request.RequestContext;

public interface UserRoleDao {

  List<Map<String, Object>> createUserRole(Map userRequest, RequestContext context);

  List<Map<String, Object>> getUserRoles(String userId, String role, RequestContext context);
}
