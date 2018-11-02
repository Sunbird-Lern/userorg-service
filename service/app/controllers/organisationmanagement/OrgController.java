package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrgController extends BaseController {

  public Promise<Result> createOrg() {
    return handleRequest(
        ActorOperations.CREATE_ORG.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateCreateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrg() {
    return handleRequest(
        ActorOperations.UPDATE_ORG.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrgStatus() {
    return handleRequest(
        ActorOperations.UPDATE_ORG_STATUS.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgStatusRequest((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getOrgDetails() {
    return handleRequest(
        ActorOperations.GET_ORG_DETAILS.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new OrgRequestValidator().validateOrgReference((Request) orgRequest);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> search() {
    return handleSearchRequest(
        ActorOperations.COMPOSITE_SEARCH.getValue(),
        request().body().asJson(),
        orgRequest -> {
          new BaseRequestValidator().validateSearchRequest((Request) orgRequest);
          return null;
        },
        null,
        null,
        getAllRequestHeaders(request()),
        EsType.organisation.getTypeName());
  }
}
