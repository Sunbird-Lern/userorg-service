package org.sunbird.user.service;

import java.util.List;
import java.util.Map;

public interface UserEncryptionService {

  List<String> getDecryptedFields(Map<String, Object> userMap);

  List<String> getEncryptedFields(Map<String, Object> userMap);
}
