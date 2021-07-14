package org.sunbird.learner.organisation.service;

import java.util.Map;
import org.sunbird.request.RequestContext;

public interface OrgService {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Map<String, Object> getOrgByExternalIdAndProvider(
      String externalId, String provider, RequestContext context);
}
