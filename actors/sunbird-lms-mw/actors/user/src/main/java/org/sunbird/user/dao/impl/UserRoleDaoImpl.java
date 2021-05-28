package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.UserRole;
import org.sunbird.user.dao.UserRoleDao;

public final class UserRoleDaoImpl implements UserRoleDao {

  private static final String TABLE_NAME = JsonKey.USER_ROLES;
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  private ObjectMapper mapper = new ObjectMapper();

  private static UserRoleDaoImpl instance;

  private UserRoleDaoImpl() {}

  public static UserRoleDao getInstance() {
    if (instance == null) {
      // To make thread safe
      synchronized (UserRoleDaoImpl.class) {
        // check again as multiple threads
        // can reach above step
        if (instance == null) instance = new UserRoleDaoImpl();
      }
    }
    return instance;
  }

  @Override
  public List<Map<String, Object>> createUserRole(Map userRequest, RequestContext context) {
    List<String> roles = (List<String>) userRequest.get(JsonKey.ROLES);
    List<Map<String, Object>> userRoleList = new ArrayList<>();
    try {
      String userId = (String) userRequest.get(JsonKey.USER_ID);
      String organisationId = (String) userRequest.get(JsonKey.ORGANISATION_ID);
      List<Map> scopeList = new LinkedList();
      Map<String, String> scopeMap = new HashMap<>();
      scopeMap.put("orgId", organisationId);
      scopeList.add(scopeMap);
      for (String role : roles) {
        UserRole userRole = new UserRole();
        userRole.setRole(role);
        userRole.setUserId(userId);
        userRole.setScope(mapper.writeValueAsString(scopeList));
        userRole.setCreatedBy(userId);
        userRole.setCreatedDate(ProjectUtil.getFormattedDate());
        Map userRoleMap = mapper.convertValue(userRole, Map.class);
        cassandraOperation.insertRecord(Util.KEY_SPACE_NAME, TABLE_NAME, userRoleMap, context);
        userRoleMap.put(JsonKey.SCOPE, scopeList);
        userRoleList.add(userRoleMap);
      }
    } catch (JsonProcessingException e) {
      throw new ProjectCommonException(
          ResponseCode.roleSaveError.getErrorCode(),
          ResponseCode.roleSaveError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return userRoleList;
  }
}
