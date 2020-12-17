package org.sunbird.user.service;

import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.models.user.UserDeclareEntity;

public interface UserSelfDeclarationService {
  Response saveUserSelfDeclareAttributes(Map<String, Object> requestMap, RequestContext context);

  void updateSelfDeclaration(UserDeclareEntity userDeclareEntity, RequestContext context);
}
