package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.responsecode.ResponseMessage;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.service.RoleService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.org.UserOrg;
import org.sunbird.user.dao.UserOrgDao;
import org.sunbird.user.dao.impl.UserOrgDaoImpl;

@ActorConfig(
  tasks = {"getRoles", "assignRoles"},
  asyncTasks = {}
)
public class UserRoleActor extends UserBaseActor {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private OrgService orgService = OrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
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
    logger.info(actorMessage.getRequestContext(), "UserRoleActor: assignRoles called");

    Map<String, Object> requestMap = actorMessage.getRequest();
    RoleService.validateRoles((List<String>) requestMap.get(JsonKey.ROLES));

    String hashTagId = getHashTagIdForOrg(requestMap, actorMessage.getRequestContext());
    if (StringUtils.isBlank(hashTagId)) return;

    String userId = (String) requestMap.get(JsonKey.USER_ID);
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    // update userOrg role with requested roles.
    Map<String, Object> userOrgDBMap = new HashMap<>();

    Map<String, Object> searchMap = new LinkedHashMap<>(2);
    searchMap.put(JsonKey.USER_ID, userId);
    searchMap.put(JsonKey.ORGANISATION_ID, organisationId);
    Response res =
        cassandraOperation.getRecordsByCompositeKey(
            JsonKey.SUNBIRD, JsonKey.USER_ORG, searchMap, actorMessage.getRequestContext());
    List<Map<String, Object>> dataList = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    List<Map<String, Object>> responseList = new ArrayList<>();
    dataList
        .stream()
        .forEach(
            (dataMap) -> {
              if (null != dataMap.get(JsonKey.IS_DELETED)
                  && !((boolean) dataMap.get(JsonKey.IS_DELETED))) {
                responseList.add(dataMap);
              }
            });

    if (CollectionUtils.isEmpty(responseList)) {
      ProjectCommonException.throwClientErrorException(ResponseCode.invalidUsrOrgData, null);
    }

    userOrgDBMap.put(JsonKey.ORGANISATION, responseList.get(0));

    UserOrg userOrg = prepareUserOrg(requestMap, hashTagId, userOrgDBMap);
    UserOrgDao userOrgDao = UserOrgDaoImpl.getInstance();

    Response response = userOrgDao.updateUserOrg(userOrg, actorMessage.getRequestContext());
    sender().tell(response, self());
    if (((String) response.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      syncUserRoles(JsonKey.ORGANISATION, userId, actorMessage.getRequestContext());
    } else {
      logger.info(actorMessage.getRequestContext(), "UserRoleActor: No ES call to save user roles");
    }
    generateTelemetryEvent(requestMap, userId, "userLevel", actorMessage.getContext());
  }

  private String getHashTagIdForOrg(Map<String, Object> requestMap, RequestContext context) {

    String externalId = (String) requestMap.get(JsonKey.EXTERNAL_ID);
    String provider = (String) requestMap.get(JsonKey.PROVIDER);
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);

    // try find organisation and fetch hashTagId from organisation.
    Map<String, Object> orgMap;
    String hashTagId = null;
    if (StringUtils.isNotBlank(organisationId)) {
      orgMap = orgService.getOrgById(organisationId, context);
      if (MapUtils.isNotEmpty(orgMap)) {
        hashTagId = (String) orgMap.get(JsonKey.HASHTAGID);
      }
    } else {
      orgMap = orgService.esGetOrgByExternalId(externalId, provider, context);
      if (MapUtils.isNotEmpty(orgMap)) {
        requestMap.put(JsonKey.ORGANISATION_ID, orgMap.get(JsonKey.ORGANISATION_ID));
        hashTagId = (String) orgMap.get(JsonKey.HASHTAGID);
      }
    }
    // throw error if provided orgId or ExtenralId with Provider is not valid
    if (StringUtils.isNotBlank(hashTagId)) {
      return hashTagId;
    } else {
      handleOrgNotFound(externalId, provider, organisationId);
    }
    return "";
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

  private void syncUserRoles(String type, String userId, RequestContext context) {
    Request request = new Request();
    request.setRequestContext(context);
    request.setOperation(ActorOperations.UPDATE_USER_ROLES_ES.getValue());
    request.getRequest().put(JsonKey.TYPE, type);
    request.getRequest().put(JsonKey.USER_ID, userId);
    logger.info(context, "UserRoleActor:syncUserRoles: Syncing to ES");
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      logger.error(
          context,
          "UserRoleActor:syncUserRoles: Exception occurred with error message = " + ex.getMessage(),
          ex);
    }
  }
}
