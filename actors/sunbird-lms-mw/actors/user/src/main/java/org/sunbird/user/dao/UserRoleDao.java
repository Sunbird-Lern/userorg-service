package org.sunbird.user.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.user.UserRole;

public interface UserRoleDao {

  Response createUserRole(UserRole userRole, RequestContext context);

  void createUserRole(Map userRequest, RequestContext context) throws JsonProcessingException;
}
