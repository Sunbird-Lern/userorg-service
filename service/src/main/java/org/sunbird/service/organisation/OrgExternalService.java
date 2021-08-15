package org.sunbird.service.organisation;

import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.Map;

public interface OrgExternalService {
    String getOrgIdFromOrgExternalIdAndProvider(
            String externalId, String provider, RequestContext context);
    Map<String, Object> getOrgByOrgExternalIdAndProvider(
            String externalId, String provider, RequestContext context);
    Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context);
    void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context);

}
