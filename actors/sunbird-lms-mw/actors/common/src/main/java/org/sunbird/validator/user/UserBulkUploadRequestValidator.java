package org.sunbird.validator.user;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.user.UserType;

public class UserBulkUploadRequestValidator {

  private UserBulkUploadRequestValidator() {}

  public static void validateUserBulkUploadRequest(Map<String, Object> userMap) {
    validateUserType(userMap);
    /*
        validateOrganisationId(userMap);
    */
  }

  public static void validateUserType(Map<String, Object> userMap) {

    // TODO get list of userType from cache handler
    // TODO: start
    List<String> userTypes =
        Stream.of(UserType.values()).map(UserType::getTypeName).collect(Collectors.toList());
    // Get list of user Sub Types belongs a userType
    Map<String, List<String>> userSubTypesMap = new HashMap<>();
    // TODO: Harcoding values to be removed
    List<String> userSubTypeList = Arrays.asList("BRC", "DAO");
    userSubTypesMap.put(UserType.ADMINISTRATOR.getTypeName(), userSubTypeList);
    userSubTypesMap.put(UserType.TEACHER.getTypeName(), Arrays.asList());
    // TODO: END

    String userType = (String) userMap.get(JsonKey.USER_TYPE);
    if (userTypes.contains(userType)) {
      userMap.put(JsonKey.USER_TYPE, userType);
      String userSubType = (String) userMap.get(JsonKey.USER_SUB_TYPE);
      if (StringUtils.isNotBlank(userSubType)
          && !userSubTypesMap.get(userType).contains(userSubType)) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidValue,
            MessageFormat.format(
                ResponseCode.invalidValue.getErrorMessage(),
                JsonKey.USER_SUB_TYPE,
                userSubType,
                userSubTypesMap.get(userType)));
      }
    } else {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          MessageFormat.format(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.USER_TYPE, userType, userTypes));
    }
  }

  /*public static void validateOrganisationId(Map<String, Object> userMap) {
    String userType = (String) userMap.get(JsonKey.USER_TYPE);
    if (UserType.TEACHER.name().equalsIgnoreCase(userType.trim().toUpperCase())
        && (StringUtils.isBlank((String) userMap.get(JsonKey.ORG_ID))
            && StringUtils.isBlank((String) userMap.get(JsonKey.ORG_EXTERNAL_ID)))) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(),
              (StringFormatter.joinByOr(JsonKey.ORG_ID, JsonKey.ORG_EXTERNAL_ID))));
    }
  }*/
}
