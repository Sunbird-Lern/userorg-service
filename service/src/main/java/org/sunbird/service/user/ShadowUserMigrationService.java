package org.sunbird.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.ClaimStatus;
import org.sunbird.model.ShadowUser;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

public class ShadowUserMigrationService {
  private static final LoggerUtil logger = new LoggerUtil(ShadowUserMigrationService.class);

  private static final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * this method will search user in userids attribute in shadow_user table
   *
   * @param userId
   * @param context
   * @return
   */
  public static ShadowUser getRecordByUserId(String userId, RequestContext context) {
    ShadowUser shadowUser = null;
    Response response =
        cassandraOperation.searchValueInList(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.SHADOW_USER, JsonKey.USERIDS, userId, context);
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      shadowUser =
          mapper.convertValue(
              ((List) response.getResult().get(JsonKey.RESPONSE)).get(0), ShadowUser.class);
    }
    return shadowUser;
  }

  /**
   * this method will update the record in the shadow_user table
   *
   * @param propertiesMap
   * @param channel
   * @param userExtId
   * @param context
   */
  public static boolean updateRecord(
      Map<String, Object> propertiesMap, String channel, String userExtId, RequestContext context) {
    Map<String, Object> compositeKeysMap = new HashMap<>();
    compositeKeysMap.put(JsonKey.USER_EXT_ID, userExtId);
    compositeKeysMap.put(JsonKey.CHANNEL, channel);
    Response response =
        cassandraOperation.updateRecord(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.SHADOW_USER, propertiesMap, compositeKeysMap, context);
    logger.info(
        context,
        "MigrationUtils:updateRecord:update in cassandra  with userExtId"
            + userExtId
            + ":and response is:"
            + response);
    return true;
  }

  /**
   * this method will mark the user rejected(2) in shadow_user table if the user doesn't want to
   * migrate
   *
   * @param shadowUser
   * @param context
   */
  public static boolean markUserAsRejected(ShadowUser shadowUser, RequestContext context) {
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, ClaimStatus.REJECTED.getValue());
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    boolean isRecordUpdated =
        updateRecord(propertiesMap, shadowUser.getChannel(), shadowUser.getUserExtId(), context);
    logger.info(
        context,
        "MigrationUtils:markUserAsRejected:update in cassandra  with userExtId"
            + shadowUser.getUserExtId());
    return isRecordUpdated;
  }
  /**
   * this method will mark the user Failed(3) in shadow_user table if the user doesn't want to
   * migrate
   *
   * @param shadowUser
   * @param context
   */
  public static boolean updateClaimStatus(
      ShadowUser shadowUser, int claimStatus, RequestContext context) {
    Map<String, Object> propertiesMap = new WeakHashMap<>();
    propertiesMap.put(JsonKey.CLAIM_STATUS, claimStatus);
    propertiesMap.put(JsonKey.UPDATED_ON, new Timestamp(System.currentTimeMillis()));
    updateRecord(propertiesMap, shadowUser.getChannel(), shadowUser.getUserExtId(), context);
    logger.info(
        context,
        "MigrationUtils:markUserAsRejected:update in cassandra  with userExtId"
            + shadowUser.getUserExtId());
    return true;
  }

  /**
   * this method will return all the ELIGIBLE(claimStatus 6) user with same userId and properties
   * from shadow_user table
   *
   * @param userId
   * @param propsMap
   * @param context
   * @return
   */
  public static List<ShadowUser> getEligibleUsersById(
      String userId, Map<String, Object> propsMap, RequestContext context) {
    List<ShadowUser> shadowUsersList = new ArrayList<>();
    Response response =
        cassandraOperation.searchValueInList(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.SHADOW_USER, JsonKey.USERIDS, userId, propsMap, context);
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      ((List) response.getResult().get(JsonKey.RESPONSE))
          .stream()
          .forEach(
              shadowMap -> {
                ShadowUser shadowUser = mapper.convertValue(shadowMap, ShadowUser.class);
                if (shadowUser.getClaimStatus() == ClaimStatus.ELIGIBLE.getValue()) {
                  shadowUsersList.add(shadowUser);
                }
              });
    }
    return shadowUsersList;
  }
  /**
   * this method will return all the ELIGIBLE(claimStatus 6) user with same userId from shadow_user
   * table
   *
   * @param userId
   * @param context
   * @return
   */
  public static List<ShadowUser> getEligibleUsersById(String userId, RequestContext context) {
    List<ShadowUser> shadowUsersList = new ArrayList<>();
    Response response =
        cassandraOperation.searchValueInList(
                ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE), JsonKey.SHADOW_USER, JsonKey.USERIDS, userId, context);
    if (!((List) response.getResult().get(JsonKey.RESPONSE)).isEmpty()) {
      ((List) response.getResult().get(JsonKey.RESPONSE))
          .stream()
          .forEach(
              shadowMap -> {
                ShadowUser shadowUser = mapper.convertValue(shadowMap, ShadowUser.class);
                if (shadowUser.getClaimStatus() == ClaimStatus.ELIGIBLE.getValue()) {
                  shadowUsersList.add(shadowUser);
                }
              });
    }
    return shadowUsersList;
  }
}
