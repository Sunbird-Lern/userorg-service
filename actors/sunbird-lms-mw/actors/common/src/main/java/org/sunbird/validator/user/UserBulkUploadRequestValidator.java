package org.sunbird.validator.user;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.StringFormatter;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.user.UserType;

public class UserBulkUploadRequestValidator {

  private UserBulkUploadRequestValidator() {}

  public static void validateUserBulkUploadRequest(Map<String, Object> userMap) {
    validateUserType(userMap);
    validateOrganisationId(userMap);
  }

  public static void validateUserType(Map<String, Object> userMap) {
    List<String> userTypes =
        Stream.of(UserType.values()).map(UserType::getTypeName).collect(Collectors.toList());
    String userType = (String) userMap.get(JsonKey.USER_TYPE);
    if (userTypes.contains(userType.trim().toUpperCase())) {
      userMap.put(JsonKey.USER_TYPE, userType.trim().toUpperCase());
    } else {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidValue,
          MessageFormat.format(
              ResponseCode.invalidValue.getErrorMessage(), JsonKey.USER_TYPE, userType, userTypes));
    }
  }

  public static void validateOrganisationId(Map<String, Object> userMap) {
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
  }
}
