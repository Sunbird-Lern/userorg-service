package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.actorutil.org.impl.OrganisationClientImpl;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.service.RoleService;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;

@ActorConfig(
  tasks = {"getRoles", "assignRoles"},
  asyncTasks = {}
)
public class UserRoleActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();

    switch (operation) {
      case "getRoles":
        getRoles();
        break;

      case "assignRoles":
        assignRoles(request);
        break;

      default:
        onReceiveUnsupportedOperation("UserRoleActor");
    }
  }

  private void getRoles() {
    ProjectLogger.log("UserRoleActor: getRoles called", LoggerEnum.INFO.name());
    Response response = DataCacheHandler.getRoleResponse();
    if (response == null) {
      response = RoleService.getUserRoles();
      DataCacheHandler.setRoleResponse(response);
    }
    sender().tell(response, self());
  }

  @SuppressWarnings("unchecked")
  private void assignRoles(Request actorMessage) {
    ProjectLogger.log("UserRoleActor: assignRoles called");

    Map<String, Object> requestMap = actorMessage.getRequest();
    RoleService.validateRoles((List<String>) requestMap.get(JsonKey.ROLES));

    boolean orgNotFound = initializeHashTagIdFromOrg(requestMap);
    if (orgNotFound) return;

    String userId = (String) requestMap.get(JsonKey.USER_ID);
    String hashTagId = (String) requestMap.get(JsonKey.HASHTAGID);
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    // update userOrg role with requested roles.
    Map<String, Object> userOrgDBMap = new HashMap<>();

    Map<String, Object> searchMap = new HashMap<>();
    searchMap.put(JsonKey.USER_ID, userId);
    searchMap.put(JsonKey.ORGANISATION_ID, organisationId);
    searchMap.put(JsonKey.IS_DELETED, false);
    Response res =
        cassandraOperation.getRecordsByProperties(JsonKey.SUNBIRD, JsonKey.USER_ORG, searchMap);
    List<Map<String, Object>> responseList = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      userOrgDBMap.put(JsonKey.ORGANISATION, responseList.get(0));
    }

    if (MapUtils.isEmpty(userOrgDBMap)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUsrOrgData, null);
    }

    UserOrg userOrg = prepareUserOrg(requestMap, hashTagId, userOrgDBMap);
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();

    Response response = userOrgDao.updateUserOrg(userOrg);
    sender().tell(response, self());
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      syncUserRoles(requestMap, JsonKey.ORGANISATION, userId, organisationId);
    } else {
      ProjectLogger.log("UserRoleActor: No ES call to save user roles");
    }
    generateTelemetryEvent(requestMap, userId, "userLevel");
  }

  private boolean initializeHashTagIdFromOrg(Map<String, Object> requestMap) {

    String externalId = (String) requestMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) requestMap.get(JsonKey.PROVIDER);
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);

    // try find organisation and fetch hashTagId from organisation.
    Map<String, Object> map = null;
    Organisation organisation = null;
    OrganisationClient orgClient = new OrganisationClientImpl();
    if (StringUtils.isNotBlank(organisationId)) {

      organisation =
          orgClient.getOrgById(
              getActorRef(ActorOperations.GET_ORG_DETAILS.getValue()), organisationId);
      if (organisation != null) {
        requestMap.put(JsonKey.HASHTAGID, organisation.getHashTagId());
      }
    } else {
      organisation = orgClient.esGetOrgByExternalId(externalId, provider);
      if (organisation != null) {
        requestMap.put(JsonKey.ORGANISATION_ID, organisation.getId());
        requestMap.put(JsonKey.HASHTAGID, organisation.getHashTagId());
      }
    }
    // throw error if provided orgId or ExtenralId with Provider is not valid
    boolean orgNotFound = MapUtils.isEmpty(map) && organisation == null;
    if (orgNotFound) {
      handleOrgNotFound(externalId, provider, organisationId);
    }
    return orgNotFound;
  }

  private void handleOrgNotFound(String externalId, String provider, String organisationId) {
    String errorMsg =
        StringUtils.isNotEmpty(organisationId)
            ? ProjectUtil.formatMessage(
                ResponseMessage.Message.INVALID_PARAMETER_VALUE,
                organisationId,
                JsonKey.ORGANISATION_ID)
            : ProjectUtil.formatMessage(
                ResponseMessage.Message.INVALID_PARAMETER_VALUE,
                StringFormatter.joinByComma(externalId, provider),
                StringFormatter.joinByAnd(JsonKey.EXTERNAL_ID, JsonKey.PROVIDER));
    ProjectCommonException exception =
        new ProjectCommonException(
            ResponseCode.invalidParameterValue.getErrorCode(),
            errorMsg,
            ResponseCode.CLIENT_ERROR.getResponseCode());
    sender().tell(exception, self());
  }

  private UserOrg prepareUserOrg(
      Map<String, Object> requestMap, String hashTagId, Map<String, Object> userOrgDBMap) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> userOrgMap = (Map<String, Object>) userOrgDBMap.get(JsonKey.ORGANISATION);
    UserOrg userOrg = mapper.convertValue(userOrgMap, UserOrg.class);
    List<String> roles = (List<String>) requestMap.get(JsonKey.ROLES);
    if (!roles.contains(ProjectUtil.UserRole.PUBLIC.name())) {
      roles.add(ProjectUtil.UserRole.PUBLIC.name());
    }
    userOrg.setRoles(roles);

    if (StringUtils.isNotBlank(hashTagId)) {
      userOrg.setHashTagId(hashTagId);
    }
    userOrg.setUpdatedDate(ProjectUtil.getFormattedDate());
    userOrg.setUpdatedBy((String) requestMap.get(JsonKey.REQUESTED_BY));
    return userOrg;
  }

  private void syncUserRoles(
      Map<String, Object> tempMap, String type, String userid, String orgId) {
    ProjectLogger.log("UserRoleActor: syncUserRoles called");

    Request request = new Request();
    request.setOperation(ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    request.getRequest().put(JsonKey.ROLES, tempMap.get(JsonKey.ROLES));
    request.getRequest().put(JsonKey.TYPE, type);
    request.getRequest().put(JsonKey.USER_ID, userid);
    request.getRequest().put(JsonKey.ORGANISATION_ID, orgId);
    ProjectLogger.log("UserRoleActor:syncUserRoles: Syncing to ES");
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log(
          "UserRoleActor:syncUserRoles: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
  }
}
