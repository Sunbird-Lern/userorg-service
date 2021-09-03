package org.sunbird.service.user;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

public interface UserOrgService {
  List<Map<String, Object>> getUserOrgListByUserId(String userId, RequestContext context);
}
