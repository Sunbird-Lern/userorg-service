package controllers.organisationmanagement;

import org.apache.pekko.actor.ActorRef;
import controllers.BaseController;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Named;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import org.sunbird.validator.BaseRequestValidator;
import org.sunbird.validator.orgvalidator.OrgRequestValidator;
import play.mvc.Http;
import play.mvc.Result;

public class OrgController extends BaseController {

  @Inject
  @Named("org_management_actor")
  private ActorRef organisationManagementActor;

  @Inject
  @Named("search_handler_actor")
  private ActorRef searchHandlerActor;

  public CompletionStage<Result> createOrg(Http.Request httpRequest) {
    return handleRequest(
        organisationManagementActor,
        ActorOperations.CREATE_ORG.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateCreateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> updateOrg(Http.Request httpRequest) {
    return handleRequest(
        organisationManagementActor,
        ActorOperations.UPDATE_ORG.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> updateOrgStatus(Http.Request httpRequest) {
    return handleRequest(
        organisationManagementActor,
        ActorOperations.UPDATE_ORG_STATUS.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgStatusRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> getOrgDetails(Http.Request httpRequest) {
    return handleRequest(
        organisationManagementActor,
        ActorOperations.GET_ORG_DETAILS.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateOrgReference((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> search(Http.Request httpRequest) {
    return handleSearchRequest(
        searchHandlerActor,
        ActorOperations.ORG_SEARCH.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new BaseRequestValidator().validateSearchRequest((Request) orgRequest);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        ProjectUtil.EsType.organisation.getTypeName(),
        httpRequest);
  }

  public CompletionStage<Result> searchV2(Http.Request httpRequest) {
    return handleSearchRequest(
        searchHandlerActor,
        ActorOperations.ORG_SEARCH_V2.getValue(),
        httpRequest.body().asJson(),
        orgRequest -> {
          new BaseRequestValidator().validateSearchRequest((Request) orgRequest);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(httpRequest),
        ProjectUtil.EsType.organisation.getTypeName(),
        httpRequest);
  }

  /**
   * This method to upload the encryption keys on cloud storage and save the same in Org metadata.
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> addKey(Http.Request httpRequest) {
    Http.MultipartFormData body = httpRequest.body().asMultipartFormData();
    return handleRequest(
        organisationManagementActor,
        ActorOperations.ADD_ENCRYPTION_KEY.getValue(),
        orgRequest -> {
          new OrgRequestValidator().validateEncryptionKeyRequest((Request) orgRequest, body);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }
}
