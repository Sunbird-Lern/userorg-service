package org.sunbird.dao.user;

import java.util.List;
import java.util.Map;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserSelfDeclarationDao {
  void insertSelfDeclaredFields(Map<String, Object> extIdMap, RequestContext context);

  UserDeclareEntity upsertUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context);

  List<Map<String, Object>> getUserSelfDeclaredFields(
      UserDeclareEntity userDeclareEntity, RequestContext context);

  List<Map<String, Object>> getUserSelfDeclaredFields(String userId, RequestContext context);

  void deleteUserSelfDeclaredDetails(
      String userId, String orgId, String persona, RequestContext context);

  Response updateUserSelfDeclaredFields(Map<String, Object> updateFieldsMap,
                                        Map<String, Object> compositeKey, RequestContext context);
}
