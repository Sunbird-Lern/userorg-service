package org.sunbird.badge.service;

import java.util.List;
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.common.exception.ProjectCommonException;

public interface BadgeClassExtensionService {
  void save(BadgeClassExtension badgeClassExtension);

  List<BadgeClassExtension> search(
      List<String> issuerList,
      List<String> badgeList,
      String rootOrgId,
      String type,
      String subtype,
      List<String> roles);

  BadgeClassExtension get(String badgeId) throws ProjectCommonException;

  void delete(String badgeId);
}
