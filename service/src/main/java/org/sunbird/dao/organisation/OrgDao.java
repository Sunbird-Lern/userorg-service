package org.sunbird.dao.organisation;

import java.util.List;
import java.util.Map;
import org.sunbird.dto.SearchDTO;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Future;

public interface OrgDao {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Response create(Map<String, Object> orgMap, RequestContext context);

  Response update(Map<String, Object> orgMap, RequestContext context);

  Response search(Map<String, Object> searchQueryMap, RequestContext context);

  Future<Map<String, Object>> search(SearchDTO searchDTO, RequestContext context);

  List<Map<String, Object>> getOrgByIds(List<String> orgIds, RequestContext context);

  List<Map<String, Object>> getOrgByIds(
      List<String> orgIds, List<String> fields, RequestContext context);

  String saveOrgToEs(String id, Map<String, Object> data, RequestContext context);
}
