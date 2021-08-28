package org.sunbird.service.organisation;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface OrgService {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Map<String, Object> getOrgByExternalIdAndProvider(
      String externalId, String provider, RequestContext context);

  Response createOrganisation(Map<String, Object> orgMap, RequestContext context);

  Response updateOrganisation(Map<String, Object> orgMap, RequestContext context);

  List<Map<String, Object>> organisationSearch(Map<String, Object> filters, RequestContext context);

  void createOrgExternalIdRecord(
          String channel, String externalId, String orgId, RequestContext context);

  void deleteOrgExternalIdRecord(
          String channel, String externalId, RequestContext context);

  String getOrgIdFromSlug(String slug, RequestContext context);

  Map<String, Object> getRootOrgFromChannel(String channel, RequestContext context);

  boolean registerChannel(Map<String, Object> req, RequestContext context);

  boolean updateChannel(Map<String, Object> req, RequestContext context);
}
