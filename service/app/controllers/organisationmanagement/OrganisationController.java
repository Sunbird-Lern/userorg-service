/** */
package controllers.organisationmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
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

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
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

  /**
   * This method will download organization details.
   *
   * @return Promise<Result>
   */
  public Promise<Result> downloadOrgs() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "OrganisationController: downloadOrgs called with data = " + requestData,
          LoggerEnum.DEBUG.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      reqObj.setOperation(ActorOperations.DOWNLOAD_ORGS.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will do the organisation search for Elastic search. this will internally call
   * composite search api.
   *
   * @return Promise<Result>
   */
  public Promise<Result> search() {
    Request request = createAndInitRequest(ActorOperations.COMPOSITE_SEARCH.getValue(),request().body().asJson());
    ProjectUtil.updateMapSomeValueTOLowerCase(request);
    if (request.getRequest().containsKey(JsonKey.FILTERS)
        && request.getRequest().get(JsonKey.FILTERS) != null
        && request.getRequest().get(JsonKey.FILTERS) instanceof Map) {
      ((Map) (request.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, EsType.organisation.getTypeName());
    } else {
      Map<String, Object> filtermap = new HashMap<>();
      Map<String, Object> dataMap = new HashMap<>();
      dataMap.put(JsonKey.OBJECT_TYPE, EsType.organisation.getTypeName());
      filtermap.put(JsonKey.FILTERS, dataMap);
      (request.getRequest()).putAll(filtermap);
    }
    return handleRequest(
        request,
        null,null,null,
        getAllRequestHeaders(request()));
  }

  
}
