package org.sunbird.dao.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface UserDeletionStatusDao {

  public Response insertRecord(Map<String, Object> userDeletionStatus, RequestContext context);

  public Response insertRecords(List<Map<String, Object>> reqMap, RequestContext context);

  public List<Map<String, Object>> getRecordByType(
      String type, String value, RequestContext context);
}
