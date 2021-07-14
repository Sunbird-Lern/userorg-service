package org.sunbird.user.service;

import java.util.Map;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserSelfDeclarationService {
  Response saveUserSelfDeclareAttributes(Map<String, Object> requestMap, RequestContext context);

  void updateSelfDeclaration(UserDeclareEntity userDeclareEntity, RequestContext context);
}
