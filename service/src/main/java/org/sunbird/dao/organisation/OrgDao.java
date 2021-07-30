package org.sunbird.dao.organisation;

import java.util.Map;
import org.sunbird.request.RequestContext;

public interface OrgDao {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Map<String, Object> getOrgByExternalId(
      String externalId, String provider, RequestContext context);
}
