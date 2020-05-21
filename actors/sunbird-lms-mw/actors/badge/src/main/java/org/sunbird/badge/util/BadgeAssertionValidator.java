package org.sunbird.badge.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.badge.model.BadgeClassExtension;
import org.sunbird.badge.service.BadgeClassExtensionService;
import org.sunbird.badge.service.impl.BadgeClassExtensionServiceImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.DbConstant;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;

/**
 * Class to provide badge assertion validation
 *
 * @author arvind
 */
public class BadgeAssertionValidator {

  private static BadgeClassExtensionService badgeClassExtensionService =
      new BadgeClassExtensionServiceImpl();
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  /**
   * Method to check whether root org of recipient and badge are same or not , if not same then
   * throw exception. Current implementation validate root org if the recipient type is user
   *
   * @param recipientId represents the id of recipient to whom badge is going to assign
   * @param recipientType represents the type of recipient .Possible values are - user, content
   * @param badgeId represents the id of the badge
   */
  public static void validateRootOrg(String recipientId, String recipientType, String badgeId) {
    ProjectLogger.log("BadgeAssertionValidator:validateRootOrg: called", LoggerEnum.INFO.name());
    if (JsonKey.USER.equalsIgnoreCase(recipientType)) {
      validateUserRootOrg(recipientId, badgeId);
    }
  }

  private static void validateUserRootOrg(String userId, String badgeId) {
    String userRootOrg = getUserRootOrgId(userId);
    String badgeRootOrg = getBadgeRootOrgId(badgeId);
    ProjectLogger.log(
        MessageFormat.format(
            "BadgeAssertionValidator:validateUserRootOrg: user root org : {0} and org root org : {1}",
            userRootOrg, badgeRootOrg),
        LoggerEnum.INFO.name());
    if (!(StringUtils.equals(userRootOrg, badgeRootOrg))) {
      ProjectLogger.log(
          "BadgeAssertionValidator:validateUserRootOrg: root org mismatch " + userId,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.commonAttributeMismatch.getErrorCode(),
          ResponseCode.commonAttributeMismatch.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.ROOT_ORG,
          BadgingJsonKey.BADGE_TYPE_USER,
          BadgingJsonKey.BADGE);
    }
  }

  private static String getUserRootOrgId(String userId) {
    Response response =
        cassandraOperation.getRecordById(
            DbConstant.sunbirdKeyspaceName.getValue(), DbConstant.userTableName.getValue(), userId);
    List<Map<String, Object>> userResponse =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);

    if (CollectionUtils.isEmpty(userResponse)) {
      ProjectLogger.log(
          "BadgeAssertionValidator:getUserRootOrgId: user not found " + userId,
          LoggerEnum.ERROR.name());
      throw new ProjectCommonException(
          ResponseCode.userNotFound.getErrorCode(),
          ResponseCode.userNotFound.getErrorMessage(),
          ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
    }
    Map<String, Object> userData = userResponse.get(0);
    return (String) userData.get(JsonKey.ROOT_ORG_ID);
  }

  private static String getBadgeRootOrgId(String badgeId) {
    BadgeClassExtension badgeClassExtension = badgeClassExtensionService.get(badgeId);
    return badgeClassExtension.getRootOrgId();
  }
}
