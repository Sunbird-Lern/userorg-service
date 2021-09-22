package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.model.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserSelfDeclarationService {
  Response saveUserSelfDeclareAttributes(Map<String, Object> requestMap, RequestContext context);

  void updateSelfDeclaration(UserDeclareEntity userDeclareEntity, RequestContext context);

  List<Map<String, Object>> fetchUserDeclarations(String userId, RequestContext context);

  Response updateSelfDeclaration(Map<String, Object> updateFieldsMap,
                                 Map<String, Object> compositeKey, RequestContext context);
}
