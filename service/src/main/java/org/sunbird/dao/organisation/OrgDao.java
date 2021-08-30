package org.sunbird.dao.organisation;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface OrgDao {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Response create(Map<String, Object> orgMap, RequestContext context);

  Response update(Map<String, Object> orgMap, RequestContext context);

  Response search(Map<String, Object> searchQueryMap, RequestContext context);

  List<Map<String, Object>> getOrgByIds(List<String> orgIds, RequestContext context);
}
