package controllers.tenantpreference;

import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import org.sunbird.operations.ActorOperations;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

/** Created by arvind on 27/10/17. */
public class TenantPreferenceController extends BaseController {

  public CompletionStage<Result> createTenantPreference(Http.Request httpRequest) {
    return handleRequest(
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
