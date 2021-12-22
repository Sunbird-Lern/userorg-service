package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

public interface UserRoleService {
  List<Map<String, Object>> updateUserRole(Map userRequest, RequestContext context);

  List<Map<String, Object>> updateUserRoleV2(Map userRequest, RequestContext context);

  boolean updateUserRoleToES(String identifier, Map<String, Object> data, RequestContext context);

  List<Map<String, Object>> getUserRoles(String userId, RequestContext context);

  List<Map<String, Object>> getUserRoles(String userId, String role, RequestContext context);
}
