package controllers.organisationmanagement;

import controllers.BaseController;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.OrgMemberRequestValidator;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

public class OrgMemberController extends BaseController {

  /**
   * Method to add an user to the organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> addMemberToOrganisation() {
    Request request = createAndInitRequest(ActorOperations.ADD_MEMBER_ORGANISATION.getValue(),
        request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(request, (orgRequest) -> {
      new OrgMemberRequestValidator().validateAddMember((Request) orgRequest);
      return null;
    }, null, null, getAllRequestHeaders(request()));
  }

  /**
   * Method to remove an user to the organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> removeMemberFromOrganisation() {
    Request request = createAndInitRequest(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue(),
        request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(request, (orgRequest) -> {
      new OrgMemberRequestValidator().validateOrgMemberRequest((Request) orgRequest);
      return null;
    }, null, null, getAllRequestHeaders(request()));
  }
}
