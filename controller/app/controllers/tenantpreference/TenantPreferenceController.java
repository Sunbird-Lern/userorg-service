package controllers.tenantpreference;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import play.mvc.Http;
import play.mvc.Result;

/** Created by arvind on 27/10/17. */
public class TenantPreferenceController extends BaseController {

  @Inject
  @Named("tenant_preference_actor")
  private ActorRef tenantPreferenceManagementActor;

  public CompletionStage<Result> createTenantPreference(Http.Request httpRequest) {
    return handleRequest(
        tenantPreferenceManagementActor,
        ActorOperations.CREATE_TENANT_PREFERENCE.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new TenantPreferenceValidator().validateCreatePreferenceRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> updateTenantPreference(Http.Request httpRequest) {
    return handleRequest(
        tenantPreferenceManagementActor,
        ActorOperations.UPDATE_TENANT_PREFERENCE.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new TenantPreferenceValidator().validateUpdatePreferenceRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }

  public CompletionStage<Result> getTenantPreference(Http.Request httpRequest) {
    return handleRequest(
        tenantPreferenceManagementActor,
        ActorOperations.GET_TENANT_PREFERENCE.getValue(),
        httpRequest.body().asJson(),
        req -> {
          Request request = (Request) req;
          new TenantPreferenceValidator().validateGetPreferenceRequest(request);
          return null;
        },
        null,
        null,
        true,
        httpRequest);
  }
}
