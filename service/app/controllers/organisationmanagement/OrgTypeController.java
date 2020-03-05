package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgTypeRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class OrgTypeController extends BaseController {

  public CompletionStage<Result> createOrgType(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_ORG_TYPE.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new OrgTypeRequestValidator().validateCreateOrgTypeRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> updateOrgType(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.UPDATE_ORG_TYPE.getValue(),
        httpRequest.body().asJson(),
        request -> {
          new OrgTypeRequestValidator().validateUpdateOrgTypeRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(httpRequest),
            httpRequest);
  }

  public CompletionStage<Result> listOrgType(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.GET_ORG_TYPE_LIST.getValue(),
        httpRequest.body().asJson(),
        null,
        null,
        null,
        getAllRequestHeaders((httpRequest)),
        false,
            httpRequest);
  }
}
