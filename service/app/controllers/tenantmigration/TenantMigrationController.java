package controllers.tenantmigration;

import controllers.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

public class TenantMigrationController extends BaseController {

  public Result migrate() {
    Request request = createAndInitRequest("migrate", request().body().asJson());

    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.CHANNEL))) {
      return Results.ok(
          Json.parse(
              "{\"id\":\"api.private.user.migrate\",\"ver\":\"v1\",\"ts\":\"2019-05-29 09:10:09:807+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"94c06447-8c72-44d3-abad-3149832271ae\",\"err\":\"MANDATORY_PARAMETER_MISSING\",\"status\":\"MANDATORY_PARAMETER_MISSING\",\"errmsg\":\"Mandatory parameter channel is missing.\"},\"responseCode\":\"CLIENT_ERROR\",\"result\":{}}"));
    }
    if (StringUtils.isBlank((String) request.getRequest().get(JsonKey.USER_ID))) {
      return Results.ok(
          Json.parse(
              "{\"id\":\"api.private.user.migrate\",\"ver\":\"v1\",\"ts\":\"2019-05-29 09:10:09:807+0000\",\"params\":{\"resmsgid\":null,\"msgid\":\"94c06447-8c72-44d3-abad-3149832271ae\",\"err\":\"MANDATORY_PARAMETER_MISSING\",\"status\":\"MANDATORY_PARAMETER_MISSING\",\"errmsg\":\"Mandatory parameter userId is missing.\"},\"responseCode\":\"CLIENT_ERROR\",\"result\":{}}"));
    }
    return Results.ok(
        Json.parse(
            "{\"id\":\"api.private.user.migrate\",\"ver\":\"v1\",\"ts\":\"2019-01-17 16:53:26:286+0530\",\"params\":{\"resmsgid\":null,\"msgid\":\"8e27cbf5-e299-43b0-bca7-8347f7ejk5abcf\",\"err\":null,\"status\":\"success\",\"errmsg\":null},\"responseCode\":\"OK\",\"result\":{\"response\":{\"response\":\"SUCCESS\",\"errors\":[]}}}"));
  }
}
