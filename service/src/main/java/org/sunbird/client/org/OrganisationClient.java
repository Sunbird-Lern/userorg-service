package org.sunbird.client.org;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.request.RequestContext;

public interface OrganisationClient {

  /**
   * Create organisation.
   *
   * @param actorRef Actor reference
   * @param orgMap Organisation details
   * @param context
   * @return Organisation ID
   */
  String createOrg(ActorRef actorRef, Map<String, Object> orgMap, RequestContext context);

  /**
   * Update organisation details.
   *
   * @param actorRef Actor reference
   * @param orgMap Organisation details
   * @param context
   */
  void updateOrg(ActorRef actorRef, Map<String, Object> orgMap, RequestContext context);

  /**
   * Get details of organisation for given ID.
   *
   * @param actorRef Actor reference
   * @param orgId Organisation ID
   * @param context
   * @return Organisation details
   */
  Organisation getOrgById(ActorRef actorRef, String orgId, RequestContext context);

  /**
   * Get details of organisation for given external ID and provider.
   *
   * @param externalId External ID
   * @param provider provider
   * @param context
   * @return Organisation details
   */
  Organisation esGetOrgByExternalId(String externalId, String provider, RequestContext context);

  /**
   * Get details of organisation for given ID.
   *
   * @param orgId Organisation ID
   * @param context
   * @return Organisation details
   */
  Organisation esGetOrgById(String orgId, RequestContext context);

  /**
   * Search organisations using specified filter.
   *
   * @param filter Filter criteria for organisation search
   * @param context
   * @return List of organisations
   */
  List<Organisation> esSearchOrgByFilter(Map<String, Object> filter, RequestContext context);

  /**
   * Search organisations by IDs.
   *
   * @param orgIds List of org IDs
   * @param outputColumns List of attributes required in each organisation search result
   * @param context
   * @return List of organisations found
   */
  List<Organisation> esSearchOrgByIds(
      List<String> orgIds, List<String> outputColumns, RequestContext context);
}
