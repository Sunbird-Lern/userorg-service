package controllers.usermanagement.validator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;

/**
 * @author anmolgupta this class will be responsilbe to validate the Shadow user migration request.
 */
public class ShadowUserMigrateReqValidator extends BaseRequestValidator {

  private Request request;
  private String callerId;
  private static final List<String> allowedActions = getEnumsAsString();
  private static List<String> rejectMandatoryParamsList =
      new ArrayList<>(Arrays.asList(JsonKey.USER_ID));
  private static List<String> acceptMandatoryParamsList =
      new ArrayList<>(Arrays.asList(JsonKey.USER_ID, JsonKey.USER_EXT_ID, JsonKey.CHANNEL));

  private ShadowUserMigrateReqValidator(Request request, String tokenUserId) {
    this.request = request;
    this.callerId = tokenUserId;
  }

  public static ShadowUserMigrateReqValidator getInstance(Request request, String callerId) {
    return new ShadowUserMigrateReqValidator(request, callerId);
  }

  public void validate() {
    validateAction((String) request.getRequest().get(JsonKey.ACTION));
    checkMandatoryFieldsPresent(
        request.getRequest(),
        isActionAccept() ? acceptMandatoryParamsList : rejectMandatoryParamsList);
    String userId = (String) request.getRequest().get(JsonKey.USER_ID);
    if (!StringUtils.equalsIgnoreCase(userId, callerId)) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue.getErrorCode(),
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(), userId, JsonKey.USER_ID),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validateAction(String action) {
    if (!allowedActions.contains(action)) {
      throw new ProjectCommonException(
          ResponseCode.invalidElementInList.getErrorCode(),
          MessageFormat.format(
              ResponseCode.invalidElementInList.getErrorMessage(), JsonKey.ACTION, allowedActions),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private static List<String> getEnumsAsString() {
    List<ProjectUtil.MigrateAction> enums = Arrays.asList(ProjectUtil.MigrateAction.values());
    List<String> actions = new ArrayList<>();
    enums.forEach(
        e -> {
          actions.add(e.getValue());
        });
    return actions;
  }

  private boolean isActionAccept() {
    String action = (String) request.getRequest().get(JsonKey.ACTION);
    return StringUtils.equalsIgnoreCase(action, ProjectUtil.MigrateAction.ACCEPT.getValue());
  }
}
