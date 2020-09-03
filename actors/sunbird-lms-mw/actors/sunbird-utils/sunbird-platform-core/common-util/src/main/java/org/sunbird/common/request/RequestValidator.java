package org.sunbird.common.request;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;

/**
 * This call will do validation for all incoming request data.
 *
 * @author Manzarul
 */
public final class RequestValidator {
  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  private RequestValidator() {}

  /**
   * This method will validate bulk user upload requested data.
   *
   * @param reqObj Request
   */
  public static void validateUploadUser(Map<String, Object> reqObj) {
    if (StringUtils.isBlank((String) reqObj.get(JsonKey.ORGANISATION_ID))
        && (StringUtils.isBlank((String) reqObj.get(JsonKey.ORG_EXTERNAL_ID))
            || StringUtils.isBlank((String) reqObj.get(JsonKey.ORG_PROVIDER)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (ProjectUtil.formatMessage(
                  ResponseMessage.Message.OR_FORMAT,
                  JsonKey.ORGANISATION_ID,
                  ProjectUtil.formatMessage(
                      ResponseMessage.Message.AND_FORMAT,
                      JsonKey.ORG_EXTERNAL_ID,
                      JsonKey.ORG_PROVIDER)))),
          ERROR_CODE);
    }
    if (null == reqObj.get(JsonKey.FILE)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ProjectUtil.formatMessage(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.FILE),
          ERROR_CODE);
    }
  }

  public static void validateSyncRequest(Request request) {
    String operation = (String) request.getRequest().get(JsonKey.OPERATION_FOR);
    if ((null != operation) && (!operation.equalsIgnoreCase("keycloak"))) {
      if (request.getRequest().get(JsonKey.OBJECT_TYPE) == null) {
        throw new ProjectCommonException(
            ResponseCode.dataTypeError.getErrorCode(),
            ResponseCode.dataTypeError.getErrorMessage(),
            ERROR_CODE);
      }
      List<String> list =
          new ArrayList<>(Arrays.asList(new String[] {JsonKey.USER, JsonKey.ORGANISATION}));
      if (!list.contains(request.getRequest().get(JsonKey.OBJECT_TYPE))) {
        throw new ProjectCommonException(
            ResponseCode.invalidObjectType.getErrorCode(),
            ResponseCode.invalidObjectType.getErrorMessage(),
            ERROR_CODE);
      }
    }
  }

  public static void validateSendMail(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.SUBJECT))) {
      throw new ProjectCommonException(
          ResponseCode.emailSubjectError.getErrorCode(),
          ResponseCode.emailSubjectError.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.BODY))) {
      throw new ProjectCommonException(
          ResponseCode.emailBodyError.getErrorCode(),
          ResponseCode.emailBodyError.getErrorMessage(),
          ERROR_CODE);
    }
    if (CollectionUtils.isEmpty((List<String>) (request.getRequest().get(JsonKey.RECIPIENT_EMAILS)))
        && CollectionUtils.isEmpty(
            (List<String>) (request.getRequest().get(JsonKey.RECIPIENT_USERIDS)))
        && MapUtils.isEmpty(
            (Map<String, Object>) (request.getRequest().get(JsonKey.RECIPIENT_SEARCH_QUERY)))
        && CollectionUtils.isEmpty(
            (List<String>) (request.getRequest().get(JsonKey.RECIPIENT_PHONES)))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              StringFormatter.joinByOr(
                  StringFormatter.joinByComma(
                      JsonKey.RECIPIENT_EMAILS,
                      JsonKey.RECIPIENT_USERIDS,
                      JsonKey.RECIPIENT_PHONES),
                  JsonKey.RECIPIENT_SEARCH_QUERY)),
          ERROR_CODE);
    }
  }

  public static void validateFileUpload(Request reqObj) {

    if (StringUtils.isBlank((String) reqObj.get(JsonKey.CONTAINER))) {
      throw new ProjectCommonException(
          ResponseCode.storageContainerNameMandatory.getErrorCode(),
          ResponseCode.storageContainerNameMandatory.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /** @param reqObj */
  public static void validateCreateOrgType(Request reqObj) {
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.NAME))) {
      throw createExceptionInstance(ResponseCode.orgTypeMandatory.getErrorCode());
    }
  }

  /** @param reqObj */
  public static void validateUpdateOrgType(Request reqObj) {
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.NAME))) {
      throw createExceptionInstance(ResponseCode.orgTypeMandatory.getErrorCode());
    }
    if (StringUtils.isBlank((String) reqObj.getRequest().get(JsonKey.ID))) {
      throw createExceptionInstance(ResponseCode.orgTypeIdRequired.getErrorCode());
    }
  }

  /**
   * Method to validate not for userId, title, note, courseId, contentId and tags
   *
   * @param request
   */
  @SuppressWarnings("rawtypes")
  public static void validateNote(Request request) {
    if (StringUtils.isBlank((String) request.get(JsonKey.USER_ID))) {
      throw new ProjectCommonException(
          ResponseCode.userIdRequired.getErrorCode(),
          ResponseCode.userIdRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.TITLE))) {
      throw new ProjectCommonException(
          ResponseCode.titleRequired.getErrorCode(),
          ResponseCode.titleRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.NOTE))) {
      throw new ProjectCommonException(
          ResponseCode.noteRequired.getErrorCode(),
          ResponseCode.noteRequired.getErrorMessage(),
          ERROR_CODE);
    }
    if (StringUtils.isBlank((String) request.get(JsonKey.CONTENT_ID))
        && StringUtils.isBlank((String) request.get(JsonKey.COURSE_ID))) {
      throw new ProjectCommonException(
          ResponseCode.contentIdError.getErrorCode(),
          ResponseCode.contentIdError.getErrorMessage(),
          ERROR_CODE);
    }
    if (request.getRequest().containsKey(JsonKey.TAGS)
        && ((request.getRequest().get(JsonKey.TAGS) instanceof List)
            && ((List) request.getRequest().get(JsonKey.TAGS)).isEmpty())) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ERROR_CODE);
    } else if (request.getRequest().get(JsonKey.TAGS) instanceof String) {
      throw new ProjectCommonException(
          ResponseCode.invalidTags.getErrorCode(),
          ResponseCode.invalidTags.getErrorMessage(),
          ERROR_CODE);
    }
  }

  /**
   * Method to validate noteId
   *
   * @param noteId
   */
  public static void validateNoteId(String noteId) {
    if (StringUtils.isBlank(noteId)) {
      throw createExceptionInstance(ResponseCode.invalidNoteId.getErrorCode());
    }
  }

  /**
   * Method to validate
   *
   * @param request
   */
  public static void validateRegisterClient(Request request) {

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.CLIENT_NAME))) {
      throw createExceptionInstance(ResponseCode.invalidClientName.getErrorCode());
    }
  }

  /**
   * Method to validate the request for updating the client key
   *
   * @param clientId
   * @param masterAccessToken
   */
  public static void validateUpdateClientKey(String clientId, String masterAccessToken) {
    validateClientId(clientId);
    if (StringUtils.isBlank(masterAccessToken)) {
      throw createExceptionInstance(ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  /**
   * Method to validate the request for updating the client key
   *
   * @param id
   * @param type
   */
  public static void validateGetClientKey(String id, String type) {
    validateClientId(id);
    if (StringUtils.isBlank(type)) {
      throw createExceptionInstance(ResponseCode.invalidRequestData.getErrorCode());
    }
  }

  /**
   * Method to validate clientId.
   *
   * @param clientId
   */
  public static void validateClientId(String clientId) {
    if (StringUtils.isBlank(clientId)) {
      throw createExceptionInstance(ResponseCode.invalidClientId.getErrorCode());
    }
  }

  /**
   * Method to validate notification request data.
   *
   * @param request Request
   */
  @SuppressWarnings("unchecked")
  public static void validateSendNotification(Request request) {
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.TO))) {
      throw createExceptionInstance(ResponseCode.invalidTopic.getErrorCode());
    }
    if (request.getRequest().get(JsonKey.DATA) == null
        || !(request.getRequest().get(JsonKey.DATA) instanceof Map)
        || ((Map<String, Object>) request.getRequest().get(JsonKey.DATA)).size() == 0) {
      throw createExceptionInstance(ResponseCode.invalidTopicData.getErrorCode());
    }

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.TYPE))) {
      throw createExceptionInstance(ResponseCode.invalidNotificationType.getErrorCode());
    }
    if (!(JsonKey.FCM.equalsIgnoreCase((String) request.getRequest().get(JsonKey.TYPE)))) {
      throw createExceptionInstance(ResponseCode.notificationTypeSupport.getErrorCode());
    }
  }

  @SuppressWarnings("rawtypes")
  public static void validateGetUserCount(Request request) {
    if (!validateListType(request, JsonKey.LOCATION_IDS)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.LOCATION_IDS, JsonKey.LIST);
    }
    if (null == request.getRequest().get(JsonKey.LOCATION_IDS)
        && ((List) request.getRequest().get(JsonKey.LOCATION_IDS)).isEmpty()) {
      throw createExceptionInstance(ResponseCode.locationIdRequired.getErrorCode());
    }

    if (!validateBooleanType(request, JsonKey.USER_LIST_REQ)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.USER_LIST_REQ, "Boolean");
    }

    if (null != request.getRequest().get(JsonKey.USER_LIST_REQ)
        && (Boolean) request.getRequest().get(JsonKey.USER_LIST_REQ)) {
      throw createExceptionInstance(ResponseCode.functionalityMissing.getErrorCode());
    }

    if (!validateBooleanType(request, JsonKey.ESTIMATED_COUNT_REQ)) {
      throw createDataTypeException(
          ResponseCode.dataTypeError.getErrorCode(), JsonKey.ESTIMATED_COUNT_REQ, "Boolean");
    }

    if (null != request.getRequest().get(JsonKey.ESTIMATED_COUNT_REQ)
        && (Boolean) request.getRequest().get(JsonKey.ESTIMATED_COUNT_REQ)) {
      throw createExceptionInstance(ResponseCode.functionalityMissing.getErrorCode());
    }
  }

  /**
   * if the request contains that key and key is not instance of List then it will return false.
   * other cases it will return true.
   *
   * @param request Request
   * @param key String
   * @return boolean
   */
  private static boolean validateListType(Request request, String key) {
    return !(request.getRequest().containsKey(key)
        && null != request.getRequest().get(key)
        && !(request.getRequest().get(key) instanceof List));
  }

  /**
   * If the request contains the key and key value is not Boolean type then it will return false ,
   * for any other case it will return true.
   *
   * @param request Request
   * @param key String
   * @return boolean
   */
  private static boolean validateBooleanType(Request request, String key) {
    return !(request.getRequest().containsKey(key)
        && null != request.getRequest().get(key)
        && !(request.getRequest().get(key) instanceof Boolean));
  }

  private static ProjectCommonException createDataTypeException(
      String errorCode, String key1, String key2) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.getResponse(errorCode).getErrorMessage(), key1, key2),
        ERROR_CODE);
  }

  private static ProjectCommonException createExceptionInstance(String errorCode) {
    return new ProjectCommonException(
        ResponseCode.getResponse(errorCode).getErrorCode(),
        ResponseCode.getResponse(errorCode).getErrorMessage(),
        ERROR_CODE);
  }
}
