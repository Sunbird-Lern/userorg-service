/** */
package controllers.organisationmanagement;

import controllers.BaseController;
import java.util.Arrays;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.orgvalidator.OrgRequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the API related to course, add course , published course, update
 * course, search course.
 *
 * @author Amit Kumar
 */
public class OrgController extends BaseController {

  public Promise<Result> createOrg() {
    Request request = createAndInitRequest(ActorOperations.CREATE_ORG.getValue(),request().body().asJson());
    ProjectUtil.toLower(request,Arrays.asList(ProjectUtil.getConfigValue(JsonKey.LOWER_CASE_FIELDS).split(",")));
    return handleRequest(
        request,
        orgRequest -> {
          new OrgRequestValidator().validateCreateOrgRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrg() {
    Request request = createAndInitRequest(ActorOperations.UPDATE_ORG.getValue(),request().body().asJson());
    ProjectUtil.toLower(request,Arrays.asList(ProjectUtil.getConfigValue(JsonKey.LOWER_CASE_FIELDS).split(",")));
    return handleRequest(
        request,
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrgStatus() {
    Request request = createAndInitRequest(ActorOperations.UPDATE_ORG_STATUS.getValue(),request().body().asJson());
    ProjectUtil.toLower(request,Arrays.asList(ProjectUtil.getConfigValue(JsonKey.LOWER_CASE_FIELDS).split(",")));
    return handleRequest(
        request,
        orgRequest -> {
          new OrgRequestValidator().validateUpdateOrgStatusRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getOrgDetails() {
    Request request = createAndInitRequest(ActorOperations.GET_ORG_DETAILS.getValue(),request().body().asJson());
    ProjectUtil.toLower(request,Arrays.asList(ProjectUtil.getConfigValue(JsonKey.LOWER_CASE_FIELDS).split(",")));
    return handleRequest(
        request,
        orgRequest -> {
          new OrgRequestValidator().validateOrgReference((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> search() {
    Request request = createAndInitRequest(ActorOperations.COMPOSITE_SEARCH.getValue(),request().body().asJson());
    ProjectUtil.toLower(request,Arrays.asList(ProjectUtil.getConfigValue(JsonKey.LOWER_CASE_FIELDS).split(",")));
    return handleSearchRequest(
        request,
        orgRequest -> {
          new OrgRequestValidator().validateOrgSearchRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()),
        EsType.organisation.getTypeName());
  }

  
}
