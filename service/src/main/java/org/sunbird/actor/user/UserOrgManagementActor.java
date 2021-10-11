package org.sunbird.actor.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.AssociationMechanism;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import org.sunbird.util.user.UserUtil;

public class UserOrgManagementActor extends BaseActor {

  private final UserOrgService userOrgService = UserOrgServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    String operation = request.getOperation();
    switch (operation) {
      case "insertUserOrgDetails":
        insertUserOrgDetails(request);
        break;
      case "updateUserOrgDetails":
        updateUserOrgDetails(request);
        break;

      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void insertUserOrgDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String callerId = (String) requestMap.remove(JsonKey.CALLER_ID);
    // Register user to given orgId(not root orgId)
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    if (StringUtils.isNotBlank(callerId)) {
      requestMap.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SYSTEM_UPLOAD);
    }
    if (StringUtils.isNotBlank(organisationId)) {
      requestMap.put(JsonKey.HASHTAGID, organisationId);
      userOrgService.registerUserToOrg(requestMap, request.getRequestContext());
    }
    if ((StringUtils.isNotBlank(organisationId)
            && StringUtils.isNotBlank((String) requestMap.get(JsonKey.ROOT_ORG_ID))
            && !organisationId.equalsIgnoreCase((String) requestMap.get(JsonKey.ROOT_ORG_ID)))
        || StringUtils.isBlank(organisationId)) {
      // Add user to root org
      requestMap.put(JsonKey.ORGANISATION_ID, requestMap.get(JsonKey.ROOT_ORG_ID));
      requestMap.put(JsonKey.HASHTAGID, requestMap.get(JsonKey.ORGANISATION_ID));
      userOrgService.registerUserToOrg(requestMap, request.getRequestContext());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void updateUserOrgDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String callerId = (String) requestMap.remove(JsonKey.CALLER_ID);
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    List<Map<String, Object>> userOrgListDb =
        UserUtil.getUserOrgDetails(
            false, (String) requestMap.get(JsonKey.ID), request.getRequestContext());
    Map<String, Object> userOrgDbMap = new HashMap<>();
    if (CollectionUtils.isNotEmpty(userOrgListDb)) {
      userOrgListDb.forEach(
          userOrg -> userOrgDbMap.put((String) userOrg.get(JsonKey.ORGANISATION_ID), userOrg));
    }
    if (StringUtils.isNotBlank(organisationId)) {
      Map<String, Object> userOrg = (Map<String, Object>) userOrgDbMap.get(organisationId);
      requestMap.put(JsonKey.ASSOCIATION_TYPE, getAssociationType(userOrg, callerId, requestMap));
      userOrgService.upsertUserOrgData(requestMap, request.getRequestContext());
    }
    if ((StringUtils.isNotBlank(organisationId)
            && !organisationId.equalsIgnoreCase((String) requestMap.get(JsonKey.ROOT_ORG_ID)))
        || StringUtils.isBlank(organisationId)) {
      Map<String, Object> userOrg =
          (Map<String, Object>) userOrgDbMap.get(requestMap.get(JsonKey.ROOT_ORG_ID));
      requestMap.put(JsonKey.ASSOCIATION_TYPE, getAssociationType(userOrg, callerId, requestMap));
      userOrgService.upsertUserOrgData(requestMap, request.getRequestContext());
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private int getAssociationType(
      Map<String, Object> userOrg, String callerId, Map<String, Object> requestMap) {
    AssociationMechanism mechanism = new AssociationMechanism();
    if (MapUtils.isNotEmpty(userOrg) && null != userOrg.get(JsonKey.ASSOCIATION_TYPE)) {
      mechanism.setAssociationType((Integer) userOrg.get(JsonKey.ASSOCIATION_TYPE));
    }
    if (StringUtils.isNotBlank(callerId)) {
      mechanism.appendAssociationType(AssociationMechanism.SYSTEM_UPLOAD);
      return mechanism.getAssociationType();
    } else {
      if (null != requestMap.get(JsonKey.ASSOCIATION_TYPE)) {
        mechanism.appendAssociationType((Integer) requestMap.get(JsonKey.ASSOCIATION_TYPE));
      }
      return mechanism.getAssociationType();
    }
  }
}
