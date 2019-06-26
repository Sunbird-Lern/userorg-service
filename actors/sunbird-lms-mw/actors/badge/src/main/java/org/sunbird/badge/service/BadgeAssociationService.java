package org.sunbird.badge.service;

import java.util.List;
import java.util.Map;

public interface BadgeAssociationService {

  /*
   * This method will return map for badgeAssociations in content-metadta
   *
   * @param Map<String, Object> badgeMap
   * @return Map<String, Object> associatedBadgeMetadata
   */
  public Map<String, Object> getBadgeAssociationMapForContentUpdate(Map<String, Object> badgeMap);

  /*
   * This method will return create map for badgeAssociations in cassandraDB
   *
   * @param Map<String, Object> badgeMap
   * @return Map<String, Object> associatedBadgeMetadata
   */
  public Map<String, Object> getCassandraBadgeAssociationCreateMap(
      Map<String, Object> badgeMap, String requestedBy, String contentId);

  /*
   * This method will return update map for badgeAssociations in cassandraDB
   *
   * @param Map<String, Object> badgeMap
   * @return Map<String, Object> associatedBadgeMetadata
   */
  public Map<String, Object> getCassandraBadgeAssociationUpdateMap(
      String associationId, String requestedBy);

  public void syncToES(List<Map<String, Object>> badgeAssociationMapList, boolean toBeCreated);
}
