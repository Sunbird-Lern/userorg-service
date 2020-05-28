package org.sunbird.common.request;

import java.util.Map;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.responsecode.ResponseCode;

/**
 * Request validator class for user tenant migration request.
 * @author Amit Kumar
 *
 */
public class UserTenantMigrationRequestValidator extends UserRequestValidator {
	
  /**
   * This method will validate the user migration request.
   * @param request user migration request body
   */
  public void validateUserTenantMigrateRequest(Request request) {
    Map<String, Object> req = request.getRequest();
    validateParam(
        (String) req.get(JsonKey.CHANNEL), ResponseCode.mandatoryParamsMissing, JsonKey.CHANNEL);
    validateParam(
        (String) req.get(JsonKey.USER_ID), ResponseCode.mandatoryParamsMissing, JsonKey.USER_ID);
    externalIdsValidation(request, JsonKey.CREATE);
  }
}
