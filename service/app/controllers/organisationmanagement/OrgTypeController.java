package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgTypeRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrgTypeController extends BaseController {

  public Promise<Result> createOrgType() {
    return handleRequest(
        ActorOperations.CREATE_ORG_TYPE.getValue(),
        request().body().asJson(),
        request -> {
          new OrgTypeRequestValidator().validateCreateOrgTypeRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrgType() {
    return handleRequest(
        ActorOperations.UPDATE_ORG_TYPE.getValue(),
        request().body().asJson(),
        request -> {
          new OrgTypeRequestValidator().validateUpdateOrgTypeRequest((Request) request);
          return null;
        },
        getAllRequestHeaders(request()));
  }

  public Promise<Result> listOrgType() {
    return handleRequest(
        ActorOperations.GET_ORG_TYPE_LIST.getValue(),
        request().body().asJson(),
        null,
        null,
        null,
        getAllRequestHeaders((request())),
        false);
  }
}
