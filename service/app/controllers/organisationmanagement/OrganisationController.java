/** */
package controllers.organisationmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.EsType;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestValidator;
import play.libs.F.Promise;
import play.mvc.Result;

/**
 * This controller will handle all the API related to course, add course , published course, update
 * course, search course.
 *
 * @author Amit Kumar
 */
public class OrganisationController extends BaseController {

  /**
   * This method will do the registration process. Registered organization data will be store inside
   * cassandra DB.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> createOrg() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Create organisation request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateCreateOrg(reqObj);
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> address = (Map<String, Object>) reqObj.getRequest().get(JsonKey.ADDRESS);
      Map<String, Object> orgData = reqObj.getRequest();
      if (null != address && address.size() > 0 && orgData.containsKey(JsonKey.ADDRESS)) {
        innerMap.put(JsonKey.ADDRESS, address);
        orgData.remove(JsonKey.ADDRESS);
      }
      if (orgData.containsKey(JsonKey.PROVIDER)) {
        orgData.put(JsonKey.PROVIDER, ((String) orgData.get(JsonKey.PROVIDER)).toLowerCase());
      }
      if (orgData.containsKey(JsonKey.EXTERNAL_ID)) {
        orgData.put(JsonKey.EXTERNAL_ID, ((String) orgData.get(JsonKey.EXTERNAL_ID)).toLowerCase());
      }
      innerMap.put(JsonKey.ORGANISATION, orgData);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to approve an organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> approveOrg() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Approve organisation request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateOrg(reqObj);
      reqObj.setOperation(ActorOperations.APPROVE_ORG.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update organization data.
   *
   * @return Promise<Result>
   */
  @SuppressWarnings("unchecked")
  public Promise<Result> updateOrg() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Update organisation request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUpdateOrg(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> address = (Map<String, Object>) reqObj.getRequest().get(JsonKey.ADDRESS);
      Map<String, Object> orgData = reqObj.getRequest();
      if (null != address && address.size() > 0 && orgData.containsKey(JsonKey.ADDRESS)) {
        innerMap.put(JsonKey.ADDRESS, address);
        orgData.remove(JsonKey.ADDRESS);
      }
      innerMap.put(JsonKey.ORGANISATION, orgData);
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to update the status of the organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateOrgStatus() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Update organisation request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUpdateOrgStatus(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, null));
    }
  }

  /**
   * This method will provide user profile details based on requested userId.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getOrgDetails() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Get Organisation details request: " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateOrg(reqObj);
      reqObj.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to add an user to the organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> addMemberToOrganisation() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" add member to organisation = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateAddMember(reqObj);
      reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to remove an user to the organization
   *
   * @return Promise<Result>
   */
  public Promise<Result> removeMemberFromOrganisation() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          " remove member from organisation = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to perform the user join organization operation .
   *
   * @return Promise<Result>
   */
  public Promise<Result> joinUserOrganisation() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" join user organisation = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to approve the user joined organization .
   *
   * @return Promise<Result>
   */
  public Promise<Result> approveUserOrganisation() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" approve user organisation = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Method to reject the user organization .
   *
   * @return Promise<Result>
   */
  public Promise<Result> rejectUserOrganisation() {

    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" approve user organisation = " + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      reqObj.setRequest(innerMap);
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will download organization details.
   *
   * @return Promise<Result>
   */
  public Promise<Result> downloadOrgs() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(" Downlaod org data request =" + requestData, LoggerEnum.INFO.name());
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
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Promise<Result> search() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log("Organisation search api call =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      ProjectUtil.updateMapSomeValueTOLowerCase(reqObj);
      reqObj.setOperation(ActorOperations.COMPOSITE_SEARCH.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      List<String> esObjectType = new ArrayList<>();
      esObjectType.add(EsType.organisation.getTypeName());

      if (reqObj.getRequest().containsKey(JsonKey.FILTERS)
          && reqObj.getRequest().get(JsonKey.FILTERS) != null
          && reqObj.getRequest().get(JsonKey.FILTERS) instanceof Map) {
        ((Map) (reqObj.getRequest().get(JsonKey.FILTERS))).put(JsonKey.OBJECT_TYPE, esObjectType);
      } else {
        Map<String, Object> filtermap = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(JsonKey.OBJECT_TYPE, esObjectType);
        filtermap.put(JsonKey.FILTERS, dataMap);
      }
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will fetch list of OrgType.
   *
   * @return Promise<Result>
   */
  public Promise<Result> getOrgTypeList() {
    try {
      ProjectLogger.log("Organisation getOrgTypeList method call", LoggerEnum.INFO.name());
      Request reqObj = new Request();
      reqObj.setOperation(ActorOperations.GET_ORG_TYPE_LIST.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.put(JsonKey.REQUESTED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will create OrgType.
   *
   * @return Promise<Result>
   */
  public Promise<Result> createOrgType() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "Organisation CreateOrgType method call =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateCreateOrgType(reqObj);
      reqObj.setOperation(ActorOperations.CREATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.getRequest().put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * This method will update OrgType.
   *
   * @return Promise<Result>
   */
  public Promise<Result> updateOrgType() {
    try {
      JsonNode requestData = request().body().asJson();
      ProjectLogger.log(
          "Organisation UpdateOrgType method call =" + requestData, LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUpdateOrgType(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_ORG_TYPE.getValue());
      reqObj.setRequestId(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      reqObj.getRequest().put(JsonKey.UPDATED_BY, ctx().flash().get(JsonKey.USER_ID));
      return actorResponseHandler(getActorRef(), reqObj, timeout, null, request());
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }
}
