package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgMemberRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

public class OrgMemberController extends BaseController {

  public CompletionStage<Result> addMemberToOrganisation(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.ADD_MEMBER_ORGANISATION.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgMemberRequestValidator().validateAddMemberRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()), httpRequest);
  }

  public CompletionStage<Result> removeMemberFromOrganisation(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgMemberRequestValidator().validateCommon((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()), httpRequest);
  }
}
