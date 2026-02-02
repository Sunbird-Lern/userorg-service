package org.sunbird.service.user.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.user.UserLookupDao;
import org.sunbird.dao.user.impl.UserLookupDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.response.ResponseMessage;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.common.ProjectUtil;

public class UserLookUpServiceImpl implements UserLookupService {

  private final LoggerUtil logger = new LoggerUtil(UserLookUpServiceImpl.class);
  private final UserLookupDao userLookupDao = UserLookupDaoImpl.getInstance();
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
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorParamExists,
            MessageFormat.format(ResponseCode.errorParamExists.getErrorMessage(), JsonKey.EMAIL));
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
    logger.debug(context, "UserLookUpServiceImpl:checkEmailUniqueness started");
    String email = user.getEmail();
    if (StringUtils.isNotBlank(email)) {
      List<Map<String, Object>> userMapList = userLookupDao.getEmailByType(email, context);
      if (!userMapList.isEmpty()) {
        if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorParamExists,
              MessageFormat.format(ResponseCode.errorParamExists.getErrorMessage(), JsonKey.EMAIL));
        } else {
          Map<String, Object> userMap = userMapList.get(0);
          if (!(((String) userMap.get(JsonKey.USER_ID)).equalsIgnoreCase(user.getId()))) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.errorParamExists,
                MessageFormat.format(
                    ResponseCode.errorParamExists.getErrorMessage(), JsonKey.EMAIL));
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
        ProjectCommonException.throwClientErrorException(
            ResponseCode.errorParamExists,
            MessageFormat.format(ResponseCode.errorParamExists.getErrorMessage(), JsonKey.PHONE));
      }
    }
  }

  public void checkPhoneUniqueness(User user, String opType, RequestContext context) {
    logger.debug(context, "UserLookUpServiceImpl:checkPhoneUniqueness started");
    // Get Phone configuration if not found , by default phone will be unique across
    // the application
    String phone = user.getPhone();
    if (StringUtils.isNotBlank(phone)) {
      List<Map<String, Object>> userMapList = userLookupDao.getPhoneByType(phone, context);
      if (!userMapList.isEmpty()) {
        if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorParamExists,
              MessageFormat.format(ResponseCode.errorParamExists.getErrorMessage(), JsonKey.PHONE));
        } else {
          Map<String, Object> userMap = userMapList.get(0);
          if (!(((String) userMap.get(JsonKey.USER_ID)).equalsIgnoreCase(user.getId()))) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.errorParamExists,
                MessageFormat.format(
                    ResponseCode.errorParamExists.getErrorMessage(), JsonKey.PHONE));
          }
        }
      }
    }
  }

  public void checkExternalIdUniqueness(User user, String operation, RequestContext context) {
    logger.debug(context, "UserLookUpServiceImpl:checkExternalIdUniqueness started");
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
                      ResponseCode.externalIdAssignedToOtherUser,
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
  public Response insertRecords(Map<String, Object> userMap, RequestContext context) {
    String userId = (String) userMap.get(JsonKey.ID);
    logger.debug(context, "UserLookUpServiceImpl:insertRecords called for userId: " + userId);
    Response response = null;
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> lookUp = new HashMap<>();
    if (userMap.get(JsonKey.PHONE) != null) {
      lookUp.put(JsonKey.TYPE, JsonKey.PHONE);
      lookUp.put(JsonKey.USER_ID, userId);
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.PHONE));
      list.add(lookUp);
    }
    if (userMap.get(JsonKey.EMAIL) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.EMAIL);
      lookUp.put(JsonKey.USER_ID, userId);
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.EMAIL));
      list.add(lookUp);
    }
    if (CollectionUtils.isNotEmpty((List) userMap.get(JsonKey.EXTERNAL_IDS))) {
      Map<String, Object> externalId =
          ((List<Map<String, Object>>) userMap.get(JsonKey.EXTERNAL_IDS))
              .stream()
              .filter(x -> x.get(JsonKey.ID_TYPE).equals(x.get(JsonKey.PROVIDER)))
              .findFirst()
              .orElse(null);
      if (MapUtils.isNotEmpty(externalId)) {
        lookUp = new HashMap<>();
        lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_EXTERNAL_ID);
        lookUp.put(JsonKey.USER_ID, userId);
        // provider is the orgId, not the channel
        lookUp.put(
            JsonKey.VALUE, externalId.get(JsonKey.ID) + "@" + externalId.get(JsonKey.PROVIDER));
        list.add(lookUp);
      }
    }
    if (userMap.get(JsonKey.USERNAME) != null) {
      lookUp = new HashMap<>();
      lookUp.put(JsonKey.TYPE, JsonKey.USER_LOOKUP_FILED_USER_NAME);
      lookUp.put(JsonKey.USER_ID, userId);
      lookUp.put(JsonKey.VALUE, userMap.get(JsonKey.USERNAME));
      list.add(lookUp);
    }
    if (CollectionUtils.isNotEmpty(list)) {
      response = userLookupDao.insertRecords(list, context);
    }
    return response;
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
        ResponseCode.errorParamExists,
        ProjectUtil.formatMessage(
            ResponseCode.errorParamExists.getErrorMessage(),
            ProjectUtil.formatMessage(
                ResponseMessage.Message.EXTERNAL_ID_FORMAT, externalId, idType, provider)),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  private static void throwExternalIDNotFoundException(
      String externalId, String idType, String provider) {
    throw new ProjectCommonException(
        ResponseCode.resourceNotFound,
        ProjectUtil.formatMessage(
            ResponseMessage.Message.EXTERNALID_NOT_FOUND, externalId, idType, provider),
        ResponseCode.RESOURCE_NOT_FOUND.getResponseCode());
  }
}
