package controllers.tenantmigration;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.usermanagement.validator.MigrationRejectRequestValidator;
import controllers.usermanagement.validator.ShadowUserMigrateReqValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import java.util.concurrent.CompletionStage;

/** @author Amit Kumar This controller will handle all the request related for user migration.
 * @author anmolgupta
 * */
public class TenantMigrationController extends BaseController {

  /**
   * Method to migrate user from one tenant to another.
   *
   * @return Result
   */
  public CompletionStage<Result> userTenantMigrate(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.USER_TENANT_MIGRATE.getValue(),
        httpRequest.body().asJson(),
        null,
        null,
        null,
        true,
            httpRequest);
  }

    public CompletionStage<Result> tenantReject(Http.Request httpRequest) {
        String tokenUserId=httpRequest.flash().get(JsonKey.USER_ID);
        JsonNode jsonNode=httpRequest.body().asJson();
        String userId=jsonNode.get(JsonKey.USER_ID).textValue();
        return handleRequest(
                ActorOperations.REJECT_MIGRATION.getValue(),
                null,
                request -> {
                    MigrationRejectRequestValidator.getInstance(tokenUserId,userId).validate();
                    return null;
                },
                userId,
                JsonKey.USER_ID, false,httpRequest);
    }


    public CompletionStage<Result> shadowUserMigrate(Http.Request httpRequest) {
        String callerId=httpRequest.flash().get(JsonKey.USER_ID);
        return handleRequest(
                ActorOperations.MIGRATE_USER.getValue(),
                request().body().asJson(),
                req -> {
                    Request request = (Request) req;
                    ShadowUserMigrateReqValidator.getInstance(request,callerId).validate();
                    return null;
                },
                null,
                null,
                true,
                httpRequest);
    }

}