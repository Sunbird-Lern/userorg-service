package org.sunbird.dao.organisation;

import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface OrgExternalDao {

  Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context);

  void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context);

  String getOrgIdFromOrgExternalIdAndProvider(
      String externalId, String provider, RequestContext context);
}
