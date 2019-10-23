package controllers.tenantmigration;

import controllers.BaseController;
import controllers.usermanagement.validator.MigrationRejectRequestValidator;
import controllers.usermanagement.validator.UserSelfMigrationReqValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.UserTenantMigrationRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/** @author Amit Kumar This controller will handle all the request related for user migration.
 * @author anmolgupta
 * */
public class TenantMigrationController extends BaseController {

  /**
   * Method to migrate user from one tenant to another.
   *
   * @return Result
   */
  public Promise<Result> userTenantMigrate() {
      return handleRequest(
              ActorOperations.USER_TENANT_MIGRATE.getValue(),
              request().body().asJson(),
              req -> {
                  Request request = (Request) req;
                  new UserTenantMigrationRequestValidator().validateUserTenantMigrateRequest(request);
                  return null;
              },
              null,
              null,
              true);
  }

    public Promise<Result> tenantReject(String userId) {
      String tokenUserId=ctx().flash().get(JsonKey.USER_ID);
        return handleRequest(
                ActorOperations.REJECT_MIGRATION.getValue(),
                null,
                request -> {
                    MigrationRejectRequestValidator.getInstance(tokenUserId,userId).validate();
                    return null;
                },
                userId,
                JsonKey.USER_ID, false);
    }

    public Promise<Result> userSelfMigrate() {
        String tokenUserId=ctx().flash().get(JsonKey.USER_ID);
        return handleRequest(
                ActorOperations.MIGRATE_USER.getValue(),
                request().body().asJson(),
                req -> {
                    Request request = (Request) req;
                    UserSelfMigrationReqValidator.getInstance(request,tokenUserId).validate();
                    return null;
                },
                null,
                null,
                true);
    }

    }