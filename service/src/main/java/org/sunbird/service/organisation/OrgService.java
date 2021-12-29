package org.sunbird.service.organisation;

import java.util.List;
import java.util.Map;
import org.sunbird.dto.SearchDTO;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Future;

public interface OrgService {

  Map<String, Object> getOrgById(String orgId, RequestContext context);

  Organisation getOrgObjById(String orgId, RequestContext context);

  List<Map<String, Object>> getOrgByIds(List<String> orgIds, RequestContext context);

  List<Map<String, Object>> getOrgByIds(
      List<String> orgIds, List<String> fields, RequestContext context);

  Map<String, Object> getOrgByExternalIdAndProvider(
      String externalId, String provider, RequestContext context);

  Response createOrganisation(Map<String, Object> orgMap, RequestContext context);

  Response updateOrganisation(Map<String, Object> orgMap, RequestContext context);

  List<Map<String, Object>> organisationSearch(Map<String, Object> filters, RequestContext context);

  List<Organisation> organisationObjSearch(Map<String, Object> filters, RequestContext context);

  Future<Map<String, Object>> searchOrg(SearchDTO searchDTO, RequestContext context);

  void createOrgExternalIdRecord(
      String channel, String externalId, String orgId, RequestContext context);

  void deleteOrgExternalIdRecord(String channel, String externalId, RequestContext context);

  String getOrgIdFromSlug(String slug, RequestContext context);

  Map<String, Object> getRootOrgFromChannel(String channel, RequestContext context);

  String getRootOrgIdFromChannel(String channel, RequestContext context);

  String getChannel(String rootOrgId, RequestContext context);

  boolean registerChannel(Map<String, Object> req, String operationType, RequestContext context);

  String saveOrgToEs(String id, Map<String, Object> data, RequestContext context);

  boolean checkOrgStatusTransition(Integer currentState, Integer nextState);
}
