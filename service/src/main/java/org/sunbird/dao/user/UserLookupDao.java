package org.sunbird.dao.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserLookupDao {

  public Response insertRecords(List<Map<String, Object>> reqMap, RequestContext context);

  public void deleteRecords(List<Map<String, String>> reqMap, RequestContext context);

  public Response insertExternalIdIntoUserLookup(
      List<Map<String, Object>> reqMap, String userId, RequestContext context);

  public List<Map<String, Object>> getRecordByType(
      String type, String value, boolean encrypt, RequestContext context);

  public List<Map<String, Object>> getEmailByType(String email, RequestContext context);

  public List<Map<String, Object>> getPhoneByType(String phone, RequestContext context);

  public List<Map<String, Object>> getUsersByUserNames(
      Map<String, Object> partitionKeyMap, RequestContext context);
}
