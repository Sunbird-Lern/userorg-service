package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserLookupService {

  public void checkEmailUniqueness(String email, RequestContext context);

  public boolean checkUsernameUniqueness(
      String username, boolean isEncrypted, RequestContext context);

  public void checkEmailUniqueness(User user, String opType, RequestContext context);

  public void checkPhoneUniqueness(String phone, RequestContext context);

  public void checkPhoneUniqueness(User user, String opType, RequestContext context);

  public void checkExternalIdUniqueness(User user, String operation, RequestContext context);

  Response insertRecords(Map<String, Object> userMap, RequestContext context);

  void deleteRecords(List<Map<String, String>> reqList, RequestContext requestContext);

  void insertExternalIdIntoUserLookup(
      List<Map<String, Object>> reqMap, String s, RequestContext requestContext);
}
