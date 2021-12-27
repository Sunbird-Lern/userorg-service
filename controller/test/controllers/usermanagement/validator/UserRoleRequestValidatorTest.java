package controllers.usermanagement.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;

public class UserRoleRequestValidatorTest {

  @Test(expected = ProjectCommonException.class)
  public void validateAssignRolesRequestV2TestWithInvalidOrgIdType() {
    Request req = new Request();
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.USER_ID, "4567812648949");

    List<Map<String, Object>> roles = new ArrayList<>();
    request.put(JsonKey.ROLES, roles);

    Map<String, Object> role = new HashMap<>();
    roles.add(role);

    role.put(JsonKey.ROLE, "ADMIN");
    role.put(JsonKey.OPERATION, "ADD");

    List<Map<String, Object>> scopes = new ArrayList<>();
    Map<String, Object> scope = new HashMap<>();
    scope.put(JsonKey.ORGANISATION_ID, Arrays.asList("656684932198"));
    scopes.add(scope);
    role.put(JsonKey.SCOPE, scopes);

    req.setRequest(request);
    UserRoleRequestValidator validator = new UserRoleRequestValidator();
    validator.validateAssignRolesRequestV2(req);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateAssignRolesRequestV2TestWithInvalidOrgIdValue() {
    Request req = new Request();
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.USER_ID, "4567812648949");

    List<Map<String, Object>> roles = new ArrayList<>();
    request.put(JsonKey.ROLES, roles);

    Map<String, Object> role = new HashMap<>();
    roles.add(role);

    role.put(JsonKey.ROLE, "ADMIN");
    role.put(JsonKey.OPERATION, "ADD");

    List<Map<String, Object>> scopes = new ArrayList<>();
    Map<String, Object> scope = new HashMap<>();
    scope.put(JsonKey.ORGANISATION_ID, null);
    scopes.add(scope);
    role.put(JsonKey.SCOPE, scopes);

    req.setRequest(request);
    UserRoleRequestValidator validator = new UserRoleRequestValidator();
    validator.validateAssignRolesRequestV2(req);
  }
}
