package org.sunbird.user.service;

import java.util.List;
import java.util.Map;

public interface UserExternalIdentityService {

  List<Map<String, Object>> getSelfDeclaredDetails(String userId, String orgId, String role);

  List<Map<String, String>> getSelfDeclaredDetails(String userId);

  List<Map<String, String>> getUserExternalIds(String userId);

  String getUserV1(String extId, String provider, String idType);

  String getUserV2(String extId, String orgId, String idType);
}
