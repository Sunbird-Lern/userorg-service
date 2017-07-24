/**
 * 
 */
package controllers.organisationmanagement;

import akka.util.Timeout;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.BaseController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.HeaderParam;
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
      RequestValidator.validateCreateOrg(reqObj);
      reqObj.setOperation(ActorOperations.CREATE_ORG.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> address = (Map<String, Object>) reqObj.getRequest().get(JsonKey.ADDRESS);
      Map<String, Object> orgData = (Map<String, Object>) reqObj.getRequest();
      if (null != address && address.size() > 0 && orgData.containsKey(JsonKey.ADDRESS)) {
        innerMap.put(JsonKey.ADDRESS, address);
        orgData.remove(JsonKey.ADDRESS);
      }
      innerMap.put(JsonKey.ORGANISATION, orgData);
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateOrg(reqObj);
      reqObj.setOperation(ActorOperations.APPROVE_ORG.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res = actorResponseHandler(getRemoteActor(), reqObj, timeout, null, null);
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, null));
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
      RequestValidator.validateUpdateOrg(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_ORG.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      Map<String, Object> address = (Map<String, Object>) reqObj.getRequest().get(JsonKey.ADDRESS);
      Map<String, Object> orgData = (Map<String, Object>) reqObj.getRequest();
      if (null != address && address.size() > 0 && orgData.containsKey(JsonKey.ADDRESS)) {
        innerMap.put(JsonKey.ADDRESS, address);
        orgData.remove(JsonKey.ADDRESS);
      }
      innerMap.put(JsonKey.ORGANISATION, orgData);
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateUpdateOrgStatus(reqObj);
      reqObj.setOperation(ActorOperations.UPDATE_ORG_STATUS.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      if(reqObj.getRequest().get(JsonKey.SOURCE) !=null) {
        reqObj.getRequest().put(JsonKey.SOURCE, ((String)reqObj.getRequest().get(JsonKey.SOURCE)).toLowerCase());
      } if (reqObj.getRequest().get(JsonKey.EXTERNAL_ID) !=null) {
        reqObj.getRequest().put(JsonKey.EXTERNAL_ID, ((String)reqObj.getRequest().get(JsonKey.EXTERNAL_ID)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.PROVIDER) !=null) {
        reqObj.getRequest().put(JsonKey.PROVIDER, ((String)reqObj.getRequest().get(JsonKey.PROVIDER)).toLowerCase());
      }
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res = actorResponseHandler(getRemoteActor(), reqObj, timeout, null, null);
      return res;
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
      RequestValidator.validateOrg(reqObj);
      reqObj.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      if(reqObj.getRequest().get(JsonKey.SOURCE) !=null) {
        reqObj.getRequest().put(JsonKey.SOURCE, ((String)reqObj.getRequest().get(JsonKey.SOURCE)).toLowerCase());
      } if (reqObj.getRequest().get(JsonKey.EXTERNAL_ID) !=null) {
        reqObj.getRequest().put(JsonKey.EXTERNAL_ID, ((String)reqObj.getRequest().get(JsonKey.EXTERNAL_ID)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.PROVIDER) !=null) {
        reqObj.getRequest().put(JsonKey.PROVIDER, ((String)reqObj.getRequest().get(JsonKey.PROVIDER)).toLowerCase());
      }
      innerMap.put(JsonKey.ORGANISATION, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateAddMember(reqObj);
      reqObj.setOperation(ActorOperations.ADD_MEMBER_ORGANISATION.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      if(reqObj.getRequest().get(JsonKey.SOURCE) !=null) {
        reqObj.getRequest().put(JsonKey.SOURCE, ((String)reqObj.getRequest().get(JsonKey.SOURCE)).toLowerCase());
      } if (reqObj.getRequest().get(JsonKey.EXTERNAL_ID) !=null) {
        reqObj.getRequest().put(JsonKey.EXTERNAL_ID, ((String)reqObj.getRequest().get(JsonKey.EXTERNAL_ID)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.USERNAME) !=null) {
        reqObj.getRequest().put(JsonKey.USERNAME, ((String)reqObj.getRequest().get(JsonKey.USERNAME)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.USER_NAME) !=null) {
        reqObj.getRequest().put(JsonKey.USER_NAME, ((String)reqObj.getRequest().get(JsonKey.USER_NAME)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.PROVIDER) !=null) {
        reqObj.getRequest().put(JsonKey.PROVIDER, ((String)reqObj.getRequest().get(JsonKey.PROVIDER)).toLowerCase());
      }
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      ProjectLogger.log(" remove member from organisation = " + requestData,
          LoggerEnum.INFO.name());
      Request reqObj = (Request) mapper.RequestMapper.mapRequest(requestData, Request.class);
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.REMOVE_MEMBER_ORGANISATION.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      if(reqObj.getRequest().get(JsonKey.SOURCE) !=null) {
        reqObj.getRequest().put(JsonKey.SOURCE, ((String)reqObj.getRequest().get(JsonKey.SOURCE)).toLowerCase());
      } if (reqObj.getRequest().get(JsonKey.EXTERNAL_ID) !=null) {
        reqObj.getRequest().put(JsonKey.EXTERNAL_ID, ((String)reqObj.getRequest().get(JsonKey.EXTERNAL_ID)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.USERNAME) !=null) {
        reqObj.getRequest().put(JsonKey.USERNAME, ((String)reqObj.getRequest().get(JsonKey.USERNAME)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.USER_NAME) !=null) {
        reqObj.getRequest().put(JsonKey.USER_NAME, ((String)reqObj.getRequest().get(JsonKey.USER_NAME)).toLowerCase());
      }if (reqObj.getRequest().get(JsonKey.PROVIDER) !=null) {
        reqObj.getRequest().put(JsonKey.PROVIDER, ((String)reqObj.getRequest().get(JsonKey.PROVIDER)).toLowerCase());
      }
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.JOIN_USER_ORGANISATION.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.APPROVE_USER_ORGANISATION.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
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
      RequestValidator.validateUserOrg(reqObj);
      reqObj.setOperation(ActorOperations.REJECT_USER_ORGANISATION.getValue());
      reqObj.setRequest_id(ExecutionContext.getRequestId());
      reqObj.setEnv(getEnvironment());
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.USER_ORG, reqObj.getRequest());
      innerMap.put(JsonKey.REQUESTED_BY,
          getUserIdByAuthToken(request().getHeader(HeaderParam.X_Authenticated_Userid.getName())));
      reqObj.setRequest(innerMap);
      Timeout timeout = new Timeout(Akka_wait_time, TimeUnit.SECONDS);
      Promise<Result> res =
          actorResponseHandler(getRemoteActor(), reqObj, timeout, null, request());
      return res;
    } catch (Exception e) {
      return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
    }
  }

}
