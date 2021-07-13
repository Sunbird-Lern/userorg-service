package org.sunbird.user.service.impl;

import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.datasecurity.EncryptionService;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.exception.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.Util;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.models.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.user.dao.UserLookupDao;
import org.sunbird.user.dao.impl.UserLookupDaoImpl;
import org.sunbird.user.service.UserLookupService;
import org.sunbird.util.ProjectUtil;

public class UserLookUpServiceImpl implements UserLookupService {
  private static LoggerUtil logger = new LoggerUtil(UserLookUpServiceImpl.class);
  private static CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static EncryptionService encryptionService =
      org.sunbird.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(
          null);
  private static Util.DbInfo userLookUp = Util.dbInfoMap.get(JsonKey.USER_LOOKUP);
  private static UserLookupDao userLookupDao = UserLookupDaoImpl.getInstance();
  private static UserLookupService userLookupService = null;

  public static UserLookupService getInstance() {
    if (userLookupService == null) {
      userLookupService = new UserLookUpServiceImpl();
    }
    return userLookupService;
  }

  public void checkEmailUniqueness(String email, RequestContext context) {
    // Get email configuration if not found , by default email will be unique across
    // the application
    if (StringUtils.isNotBlank(email)) {
      List<Map<String, Object>> userMapList = userLookupDao.getEmailByType(email, context);
      if (CollectionUtils.isNotEmpty(userMapList)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.emailAlreadyExistError, null);
      }
    }
  }

  public boolean checkUsernameUniqueness(
      String username, boolean isEncrypted, RequestContext context) {
    List<Map<String, Object>> userMapList =
        userLookupDao.getRecordByType(
            JsonKey.USER_LOOKUP_FILED_USER_NAME, username, !isEncrypted, context);
    if (CollectionUtils.isNotEmpty(userMapList)) {
      return false;
    }
    return true;
  }

  public void checkEmailUniqueness(User user, String opType, RequestContext context) {
    String email = user.getEmail();
    if (StringUtils.isNotBlank(email)) {
      List<Map<String, Object>> userMapList = userLookupDao.getEmailByType(email, context);
      if (!userMapList.isEmpty()) {
        if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
          ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
        } else {
          Map<String, Object> userMap = userMapList.get(0);
          if (!(((String) userMap.get(JsonKey.USER_ID)).equalsIgnoreCase(user.getId()))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.emailInUse, null);
          }
        }
      }
    }
  }

  public void checkPhoneUniqueness(String phone, RequestContext context) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    if (StringUtils.isNotBlank(phone)) {
      List<Map<String, Object>> userMapList = userLookupDao.getPhoneByType(phone, context);
      if (CollectionUtils.isNotEmpty(userMapList)) {
        ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
      }
    }
  }

  public void checkPhoneUniqueness(User user, String opType, RequestContext context) {
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phone = user.getPhone();
    if (StringUtils.isNotBlank(phone)) {
      List<Map<String, Object>> userMapList = userLookupDao.getPhoneByType(phone, context);
      if (!userMapList.isEmpty()) {
        if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
          ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
        } else {
          Map<String, Object> userMap = userMapList.get(0);
          if (!(((String) userMap.get(JsonKey.USER_ID)).equalsIgnoreCase(user.getId()))) {
            ProjectCommonException.throwClientErrorException(ResponseCode.PhoneNumberInUse, null);
          }
        }
      }
    }
  }

  public void checkExternalIdUniqueness(User user, String operation, RequestContext context) {
    if (CollectionUtils.isNotEmpty(user.getExternalIds())) {
      for (Map<String, String> externalId : user.getExternalIds()) {
        if (StringUtils.isNotBlank(externalId.get(JsonKey.ID))
            && StringUtils.isNotBlank(externalId.get(JsonKey.PROVIDER))
            && StringUtils.isNotBlank(externalId.get(JsonKey.ID_TYPE))) {
          String externalIdWithOrg =
              externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER);
          List<Map<String, Object>> externalIdsRecord =
              userLookupDao.getRecordByType(
                  JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID, externalIdWithOrg, false, context);
          if (CollectionUtils.isNotEmpty(externalIdsRecord)) {
            if (JsonKey.CREATE.equalsIgnoreCase(operation)) {
              throwUserAlreadyExistsException(
                  externalId.get(JsonKey.ID),
                  externalId.get(JsonKey.ID_TYPE),
                  externalId.get(JsonKey.PROVIDER));
            } else if (JsonKey.UPDATE.equalsIgnoreCase(operation)) {
              // If end user will try to add,edit or remove other user extIds throw exception
              String userId = (String) externalIdsRecord.get(0).get(JsonKey.USER_ID);
              if (!(user.getUserId().equalsIgnoreCase(userId))) {
                if (JsonKey.ADD.equalsIgnoreCase(externalId.get(JsonKey.OPERATION))
                    || StringUtils.isBlank(externalId.get(JsonKey.OPERATION))) {
                  throw new ProjectCommonException(
                      ResponseCode.externalIdAssignedToOtherUser.getErrorCode(),
                      ProjectUtil.formatMessage(
                          ResponseCode.externalIdAssignedToOtherUser.getErrorMessage(),
                          externalId.get(JsonKey.ID),
                          externalId.get(JsonKey.ID_TYPE),
                          externalId.get(JsonKey.PROVIDER)),
                      ResponseCode.CLIENT_ERROR.getResponseCode());
                } else {
                  throwExternalIDNotFoundException(
                      externalId.get(JsonKey.ID),
                      externalId.get(JsonKey.ID_TYPE),
                      externalId.get(JsonKey.PROVIDER));
                }
              }
            }
          } else {
            // if user will try to delete non existing extIds
            if (JsonKey.UPDATE.equalsIgnoreCase(operation)
                && JsonKey.REMOVE.equalsIgnoreCase(externalId.get(JsonKey.OPERATION))) {
              throwExternalIDNotFoundException(
                  externalId.get(JsonKey.ID),
                  externalId.get(JsonKey.ID_TYPE),
                  externalId.get(JsonKey.PROVIDER));
            }
          }
        }
      }
    }
  }

  @Override
  public Response insertRecords(List<Map<String, Object>> list, RequestContext context) {
    return userLookupDao.insertRecords(list, context);
  }

  @Override
  public void deleteRecords(List<Map<String, String>> reqList, RequestContext context) {
    userLookupDao.deleteRecords(reqList, context);
  }

  @Override
  public void insertExternalIdIntoUserLookup(
      List<Map<String, Object>> reqMap, String s, RequestContext requestContext) {
    userLookupDao.insertExternalIdIntoUserLookup(reqMap, s, requestContext);
  }

  private static void throwUserAlreadyExistsException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.userAlreadyExists.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.userAlreadyExists.getErrorMessage(),
            ProjectUtil.formatMessage(
                ResponseMessage.Message.EXTERNAL_ID_FORMAT, externalId, idType, provider)),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.externalIdNotFound.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.externalIdNotFound.getErrorMessage(), externalId, idType, provider),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
