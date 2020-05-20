package org.sunbird.actorutil.org;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import org.sunbird.models.organisation.Organisation;

public interface OrganisationClient {

  /**
   * Create organisation.
   *
   * @param actorRef Actor reference
   * @param orgMap Organisation details
   * @return Organisation ID
   */
  String createOrg(ActorRef actorRef, Map<String, Object> orgMap);

  /**
   * Update organisation details.
   *
   * @param actorRef Actor reference
   * @param orgMap Organisation details
   */
  void updateOrg(ActorRef actorRef, Map<String, Object> orgMap);

  /**
   * Get details of organisation for given ID.
   *
   * @param actorRef Actor reference
   * @param orgId Organisation ID
   * @return Organisation details
   */
  Organisation getOrgById(ActorRef actorRef, String orgId);

  /**
   * Get details of organisation for given external ID and provider.
   *
   * @param externalId External ID
   * @param provider provider
   * @return Organisation details
   */
  Organisation esGetOrgByExternalId(String externalId, String provider);

  /**
   * Get details of organisation for given ID.
   *
   * @param orgId Organisation ID
   * @return Organisation details
   */
  Organisation esGetOrgById(String orgId);

  /**
   * Search organisations using specified filter.
   *
   * @param filter Filter criteria for organisation search
   * @return List of organisations
   */
  List<Organisation> esSearchOrgByFilter(Map<String, Object> filter);

  /**
   * Search organisations by IDs.
   *
   * @param orgIds List of org IDs
   * @param outputColumns List of attributes required in each organisation search result
   * @return List of organisations found
   */
  List<Organisation> esSearchOrgByIds(List<String> orgIds, List<String> outputColumns);
}
