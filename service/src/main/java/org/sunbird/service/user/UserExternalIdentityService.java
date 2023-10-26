package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

public interface UserExternalIdentityService {

  List<Map<String, String>> getSelfDeclaredDetails(String userId, RequestContext context);

  List<Map<String, String>> getUserExternalIds(String userId, RequestContext context);

  List<Map<String, String>> getExternalIds(
      String userId, boolean mergeDeclaration, RequestContext context);

  String getUserV1(String extId, String provider, String idType, RequestContext context);

  String getUserV2(String extId, String orgId, String idType, RequestContext context);

  boolean deleteUserExternalIds(
      List<Map<String, String>> dbUserExternalIds, RequestContext context);
}
