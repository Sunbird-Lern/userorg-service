package org.sunbird.badge.dao.impl;

import java.util.List;
import java.util.Map;
import org.sunbird.badge.dao.ContentBadgeAssociationDao;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.helper.ServiceFactory;

public class ContentBadgeAssociationDaoImpl implements ContentBadgeAssociationDao {

  private static final String KEYSPACE = "sunbird" + "";
  private static final String TABLE_NAME = "content_badge_association";
  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public Response insertBadgeAssociation(List<Map<String, Object>> contentInfo) {
    return cassandraOperation.batchInsert(KEYSPACE, TABLE_NAME, contentInfo);
  }

  @Override
  public Response updateBadgeAssociation(Map<String, Object> updateMap) {
    return cassandraOperation.updateRecord(KEYSPACE, TABLE_NAME, updateMap);
  }

  @Override
  public void createDataToES(Map<String, Object> badgeMap) {
    esUtil.save(
        ProjectUtil.EsType.badgeassociations.getTypeName(),
        (String) badgeMap.get(JsonKey.ID),
        badgeMap);
  }

  @Override
  public void updateDataToES(Map<String, Object> badgeMap) {
    ProjectLogger.log(
        "ContentBadgeAssociationDaoImpl:updateDataToES: Updating data to ES for associationId: "
            + (String) badgeMap.get(JsonKey.ID),
        LoggerEnum.INFO);
    try {
      esUtil.update(
          ProjectUtil.EsType.badgeassociations.getTypeName(),
          (String) badgeMap.get(JsonKey.ID),
          badgeMap);
    } catch (Exception e) {
      ProjectLogger.log(
          "ContentBadgeAssociationDaoImpl:updateDataToES: Exception occured while Updating data to ES for associationId: "
              + (String) badgeMap.get(JsonKey.ID)
              + " with exception "
              + e.getMessage(),
          LoggerEnum.INFO);
    }
  }
}
