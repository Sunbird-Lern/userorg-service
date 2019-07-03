package org.sunbird.badge.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.badge.service.BadgeClassExtensionService;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

public class BadgeClassExtensionServiceImpl implements BadgeClassExtensionService {
  private CassandraOperation cassandraOperation;
  public static final String BADGE_CLASS_EXT_TABLE_NAME = "badge_class_extension";

  public BadgeClassExtensionServiceImpl() {
    this.cassandraOperation = ServiceFactory.getInstance();
  }

  public BadgeClassExtensionServiceImpl(CassandraOperation cassandraOperation) {
    this.cassandraOperation = cassandraOperation;
  }

  @Override
  public void save(BadgeClassExtension badgeClassExtension) {
    Map<String, Object> request = new HashMap<>();

    request.put(JsonKey.ID, badgeClassExtension.getBadgeId());
    request.put(BadgingJsonKey.ISSUER_ID, badgeClassExtension.getIssuerId());

    request.put(JsonKey.ROOT_ORG_ID, badgeClassExtension.getRootOrgId());
    request.put(JsonKey.TYPE, badgeClassExtension.getType());
    request.put(JsonKey.SUBTYPE, badgeClassExtension.getSubtype());
    request.put(JsonKey.ROLES, badgeClassExtension.getRoles());

    cassandraOperation.upsertRecord(Util.KEY_SPACE_NAME, BADGE_CLASS_EXT_TABLE_NAME, request);
  }

  @Override
  public List<BadgeClassExtension> search(
      List<String> issuerList,
      List<String> badgeList,
      String rootOrgId,
      String type,
      String subtype,
      List<String> roles) {
    Map<String, Object> propertyMap = new HashMap<>();

    if (rootOrgId != null) {
      propertyMap.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    }

    if (type != null) {
      propertyMap.put(JsonKey.TYPE, type);
    }

    if (subtype != null) {
      propertyMap.put(JsonKey.SUBTYPE, subtype);
    }

    Response response =
        cassandraOperation.getRecordsByProperties(
            Util.KEY_SPACE_NAME, BADGE_CLASS_EXT_TABLE_NAME, propertyMap);
    List<Map<String, Object>> badgeClassExtList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    return badgeClassExtList
        .stream()
        .map(badgeClassExtMap -> new BadgeClassExtension(badgeClassExtMap))
        .filter(
            badgeClassExt ->
                roles == null
                    || CollectionUtils.isEmpty(badgeClassExt.getRoles())
                    || !Collections.disjoint(roles, badgeClassExt.getRoles()))
        .filter(
            badgeClassExt ->
                CollectionUtils.isEmpty(issuerList)
                    || issuerList.contains(badgeClassExt.getIssuerId()))
        .filter(
            badgeClassExt ->
                CollectionUtils.isEmpty(badgeList)
                    || badgeList.contains(badgeClassExt.getBadgeId()))
        .collect(Collectors.toList());
  }

  @Override
  public BadgeClassExtension get(String badgeId) throws ProjectCommonException {
    Response response =
        cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, BADGE_CLASS_EXT_TABLE_NAME, badgeId);
    List<Map<String, Object>> badgeList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    if ((badgeList == null) || badgeList.isEmpty()) {
      ProjectLogger.log(
          "BadgeClassExtensionServiceImpl:get: Badge not found " + badgeId,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.resourceNotFound.getErrorCode(),
          ResponseCode.resourceNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }

    return new BadgeClassExtension(badgeList.get(0));
  }

  @Override
  public void delete(String badgeId) {
    cassandraOperation.deleteRecord(Util.KEY_SPACE_NAME, BADGE_CLASS_EXT_TABLE_NAME, badgeId);
  }
}
