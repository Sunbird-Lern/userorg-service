package org.sunbird.user.actors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.Util;
import org.sunbird.user.service.AssociationMechanism;
import org.sunbird.user.util.UserUtil;

@ActorConfig(
  tasks = {"insertUserOrgDetails", "updateUserOrgDetails"},
  asyncTasks = {"insertUserOrgDetails", "updateUserOrgDetails"}
)
public class UserOrgManagementActor extends BaseActor {

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
        onReceiveUnsupportedOperation("UserOrgManagementActor");
    }
  }

  private void insertUserOrgDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    // Register user to given orgId(not root orgId)
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    if (StringUtils.isNotBlank(organisationId)) {
      String callerId = (String) requestMap.remove(JsonKey.CALLER_ID);
      if (StringUtils.isNotBlank(callerId)) {
        requestMap.put(JsonKey.ASSOCIATION_TYPE, AssociationMechanism.SYSTEM_UPLOAD);
      }

      requestMap.put(JsonKey.HASHTAGID, organisationId);
      Util.registerUserToOrg(requestMap, request.getRequestContext());

      if ((StringUtils.isNotBlank(organisationId)
              && StringUtils.isNotBlank((String) requestMap.get(JsonKey.ROOT_ORG_ID))
              && !organisationId.equalsIgnoreCase((String) requestMap.get(JsonKey.ROOT_ORG_ID)))
          || StringUtils.isBlank(organisationId)) {
        // Add user to root org
        requestMap.put(JsonKey.ORGANISATION_ID, requestMap.get(JsonKey.ROOT_ORG_ID));
        requestMap.put(JsonKey.HASHTAGID, requestMap.get(JsonKey.ORGANISATION_ID));
        Util.registerUserToOrg(requestMap, request.getRequestContext());
      }
    }
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());
  }

  private void updateUserOrgDetails(Request request) {
    Map<String, Object> requestMap = request.getRequest();
    String organisationId = (String) requestMap.get(JsonKey.ORGANISATION_ID);
    if (StringUtils.isNotBlank(organisationId)) {
      String callerId = (String) requestMap.remove(JsonKey.CALLER_ID);
      List<Map<String, Object>> userOrgListDb =
          UserUtil.getUserOrgDetails(
              false, (String) requestMap.get(JsonKey.ID), request.getRequestContext());
      Map<String, Object> userOrgDbMap = new HashMap<>();
      if (CollectionUtils.isNotEmpty(userOrgListDb)) {
        userOrgListDb.forEach(
            userOrg -> userOrgDbMap.put((String) userOrg.get(JsonKey.ORGANISATION_ID), userOrg));
      }

      Map<String, Object> userOrg = (Map<String, Object>) userOrgDbMap.get(organisationId);
      requestMap.put(JsonKey.ASSOCIATION_TYPE, getAssociationType(userOrg, callerId, requestMap));
      Util.upsertUserOrgData(requestMap, request.getRequestContext());

      if ((StringUtils.isNotBlank(organisationId)
              && !organisationId.equalsIgnoreCase((String) requestMap.get(JsonKey.ROOT_ORG_ID)))
          || StringUtils.isBlank(organisationId)) {
        Map<String, Object> rootUserOrg =
            (Map<String, Object>) userOrgDbMap.get(requestMap.get(JsonKey.ROOT_ORG_ID));
        requestMap.put(
            JsonKey.ASSOCIATION_TYPE, getAssociationType(rootUserOrg, callerId, requestMap));
        Util.upsertUserOrgData(requestMap, request.getRequestContext());
      }
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
