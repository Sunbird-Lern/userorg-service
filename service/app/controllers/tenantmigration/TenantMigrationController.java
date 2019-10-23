package controllers.tenantmigration;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserTenantMigrationRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/** @author Amit Kumar This controller will handle all the request related for user migration. */
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
        req -> {
          Request request = (Request) req;
          new UserTenantMigrationRequestValidator().validateUserTenantMigrateRequest(request);
          return null;
        },
        null,
        null,
        true,
            httpRequest);
  }
}
