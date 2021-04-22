package org.sunbird.learner.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

public enum AssociationMechanismEnum {
  SSO("sso", 1),
  SELF_DECLARATION("self_Declaration", 2),
  SYSTEM_UPLOAD("system_Upload", 3);
  private String type;
  private int value;

  AssociationMechanismEnum(String type, int value) {
    this.type = type;
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public String getType() {
    return type;
  }

  public static int getValueByType(String type) {
    List<String> associationTypeList = new ArrayList<>();
    for (AssociationMechanismEnum associationMechanism : AssociationMechanismEnum.values()) {
      associationTypeList.add(associationMechanism.getType());
      if (associationMechanism.getType().equalsIgnoreCase(type)) {
        return associationMechanism.getValue();
      }
    }
    throw new ProjectCommonException(
        ResponseCode.invalidValue.getErrorCode(),
        MessageFormat.format(
            ResponseCode.invalidValue.getErrorMessage(),
            JsonKey.ASSOCIATION_TYPE,
            type,
            associationTypeList),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
