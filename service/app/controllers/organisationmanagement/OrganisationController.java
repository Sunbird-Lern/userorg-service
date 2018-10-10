/** */
package controllers.organisationmanagement;

import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.OrgRequestValidator;
import org.sunbird.common.request.Request;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the API related to course, add course , published course, update
 * course, search course.
 *
 * @author Amit Kumar
 */
public class OrganisationController extends BaseController {

  public Promise<Result> createOrg() {
    Request request = createAndInitRequest(ActorOperations.CREATE_ORG.getValue(),request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(
        request,
        (orgRequest) -> {
          new OrgRequestValidator().validateCreateOrgRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrg() {
    Request request = createAndInitRequest(ActorOperations.UPDATE_ORG.getValue(),request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(
        request,
        (orgRequest) -> {
          new OrgRequestValidator().validateUpdateOrgRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> updateOrgStatus() {
    Request request = createAndInitRequest(ActorOperations.UPDATE_ORG_STATUS.getValue(),request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(
        request,
        (orgRequest) -> {
          new OrgRequestValidator().validateUpdateOrgStatusRequest((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> getOrgDetails() {
    Request request = createAndInitRequest(ActorOperations.GET_ORG_DETAILS.getValue(),request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    return handleRequest(
        request,
        (orgRequest) -> {
          new OrgRequestValidator().validateOrgReqForIdOrExternalIdAndProvider((Request) orgRequest);
          return null;
        },null,null,
        getAllRequestHeaders(request()));
  }

  public Promise<Result> search() {
    Request request = createAndInitRequest(ActorOperations.COMPOSITE_SEARCH.getValue(),request().body().asJson());
    List<String> esObjectType = new ArrayList<>();
    esObjectType.add(EsType.organisation.getTypeName());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    if (request.getRequest().containsKey(JsonKey.FILTERS)
        && request.getRequest().get(JsonKey.FILTERS) != null
        && request.getRequest().get(JsonKey.FILTERS) instanceof Map) {
      ((Map) (request.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, esObjectType);
    } else {
      Map<String, Object> filtermap = new HashMap<>();
      Map<String, Object> dataMap = new HashMap<>();
      dataMap.put(JsonKey.OBJECT_TYPE, esObjectType);
      filtermap.put(JsonKey.FILTERS, dataMap);
      (request.getRequest()).putAll(filtermap);
    }
    return handleRequest(
        request,
        null,null,null,
        getAllRequestHeaders(request()));
  }

  
}
