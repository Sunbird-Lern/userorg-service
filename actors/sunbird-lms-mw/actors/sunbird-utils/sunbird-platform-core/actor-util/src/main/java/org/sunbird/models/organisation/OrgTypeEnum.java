package org.sunbird.models.organisation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

public enum OrgTypeEnum {
  BOARD("board", 5),
  SCHOOL("school", 2);

  private String orgType;
  private int value;

  OrgTypeEnum(String orgType, int value) {
    this.orgType = orgType;
    this.value = value;
  }

  public String getType() {
    return this.orgType;
  }

  public int getValue() {
    return this.value;
  }

  public static int getValueByType(String type) {
    List<String> orgTypeList = new ArrayList<>();
    for (OrgTypeEnum orgType : OrgTypeEnum.values()) {
      orgTypeList.add(orgType.getType());
      if (orgType.getType().equalsIgnoreCase(type)) {
        return orgType.getValue();
      }
    }
    throw new ProjectCommonException(
        ResponseCode.invalidValue.getErrorCode(),
        MessageFormat.format(
            ResponseCode.invalidValue.getErrorMessage(), JsonKey.ORG_TYPE, type, orgTypeList),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public static String getTypeByValue(int value) {
    List<Integer> orgValueList = new ArrayList<>();
    for (OrgTypeEnum orgType : OrgTypeEnum.values()) {
      orgValueList.add(orgType.getValue());
      if (orgType.getValue() == value) {
        return orgType.getType();
      }
    }
    throw new ProjectCommonException(
        ResponseCode.invalidOrgType.getErrorCode(),
        MessageFormat.format(
            ResponseCode.invalidOrgType.getErrorMessage(), JsonKey.ORG_TYPE, value, orgValueList),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
