package org.sunbird.dao.organisation;

import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.Map;

public interface OrgExternalDao {

    Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context);

    void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context);
}
