package org.sunbird.learner.organisation.service;

import java.util.Map;
import org.sunbird.common.request.RequestContext;

public interface OrgService {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Map<String, Object> esGetOrgByExternalId(
      String externalId, String provider, RequestContext context);
}
