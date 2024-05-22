package org.sunbird.actor.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.dto.SearchDTO;
import org.sunbird.kafka.InstructionEventGenerator;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.Request;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.PropertiesCache;
import org.sunbird.util.user.UserUtil;

public class UserDeletionBackgroundJobActor extends BaseActor {

  private final UserService userService = UserServiceImpl.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    inputKafkaTopic(request);
  }

  private void inputKafkaTopic(Request request) throws Exception {
    Map<String, Object> userDetails = request.getRequest();
    String userId = (String) userDetails.get(JsonKey.USER_ID);
    ArrayList<String> roles = (ArrayList) userDetails.get(JsonKey.USER_ROLES);
    logger.info("UserDeletionBackgroundJobActor::inputKafkaTopic:: userId:: " + userId);
    logger.info("UserDeletionBackgroundJobActor::inputKafkaTopic:: roles size:: " + roles.size());
    User user = userService.getUserById(userId, request.getRequestContext());
    String rootOrgId = user.getRootOrgId();

    String userName = UserUtil.getDecryptedData(user.getUserName(), request.getRequestContext());

    List<Map<String, Object>> suggestedUsersList = new ArrayList<>();
    Map<String, Object> searchQueryMap = new HashMap<>();
    Map<String, Object> searchFilter = new HashMap<>();
    searchFilter.put(JsonKey.ROOT_ORG_ID, rootOrgId);

    if (!roles.contains(JsonKey.ORG_ADMIN)) roles.add(JsonKey.ORG_ADMIN);

    // for each role in the organisation, fetch list of other users to pass it as part of event
    List<String> queryFields = new ArrayList<>();
    queryFields.add(JsonKey.USER_ID);
    searchQueryMap.put(JsonKey.FIELDS, queryFields);
    logger.info("UserDeletionBackgroundJobActor::inputKafkaTopic:: roles size:: " + roles.size());
    roles.forEach(
        role -> {
          searchFilter.put(JsonKey.ROLES + "." + JsonKey.ROLE, role);
          searchQueryMap.put(JsonKey.FILTERS, searchFilter);
          SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
          Map<String, Object> result =
              userService.searchUser(searchDto, request.getRequestContext());
          List<Map<String, Object>> userMapList =
              (List<Map<String, Object>>) result.get(JsonKey.CONTENT);

          if (!userMapList.isEmpty()) {
            Map<String, Object> roleUsersMap = new HashMap<>();
            roleUsersMap.put(JsonKey.ROLE, role);
            List<String> roleUsersList = new ArrayList<>();
            for (Map<String, Object> userMap : userMapList) {
              roleUsersList.add((String) userMap.get(JsonKey.USER_ID));
            }
            roleUsersMap.put(JsonKey.USERS, roleUsersList);
            suggestedUsersList.add(roleUsersMap);
          }
        });

    logger.info(
        "UserDeletionBackgroundJobActor::inputKafkaTopic:: suggestedUsersList size:: "
            + suggestedUsersList.size());

    /* Fetch Managed Users - START */
    List<String> managedUsersList = new ArrayList<>();
    Map<String, Object> managedUsersSearchFilter = new HashMap<>();
    managedUsersSearchFilter.put(JsonKey.ROOT_ORG_ID, rootOrgId);
    managedUsersSearchFilter.put(JsonKey.MANAGED_BY, userId);
    searchQueryMap.put(JsonKey.FILTERS, managedUsersSearchFilter);
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    Map<String, Object> result = userService.searchUser(searchDto, request.getRequestContext());
    List<Map<String, Object>> userMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);

    if (!userMapList.isEmpty()) {
      for (Map<String, Object> userMap : userMapList) {
        managedUsersList.add((String) userMap.get(JsonKey.USER_ID));
      }
    }
    /* Fetch Managed Users - END */

    logger.info(
        "UserDeletionBackgroundJobActor::inputKafkaTopic:: managedUsersList size:: "
            + managedUsersList.size());

    PropertiesCache propertiesCache = PropertiesCache.getInstance();
    String userDeletionTopic = propertiesCache.getProperty(JsonKey.USER_DELETION_TOPIC);

    logger.info(
        "UserDeletionBackgroundJobActor::inputKafkaTopic:: userDeletionTopic:: "
            + userDeletionTopic);

    // data to be passed to event
    Map<String, Object> data = new HashMap<>();

    Map<String, String> objectMap = new HashMap<>();
    objectMap.put(JsonKey.ID, userId);
    objectMap.put(JsonKey.TYPE, JsonKey.USER);
    data.put(JsonKey.OBJECT, objectMap);

    Map<String, Object> cData = new HashMap<>();
    cData.put(JsonKey.ID,request.getRequestContext().getReqId());
    cData.put(JsonKey.TYPE, TelemetryEnvKey.REQUEST_UPPER_CAMEL);
    data.put(JsonKey.CDATA, cData);

    Map<String, Object> eData = new HashMap<>();
    eData.put(JsonKey.ORGANISATION_ID, rootOrgId);
    eData.put(JsonKey.USER_ID, userId);
    eData.put(JsonKey.USERNAME, userName);
    eData.put(JsonKey.SUGGESTED_USERS, suggestedUsersList);
    eData.put(JsonKey.MANAGED_USERS, managedUsersList);
    eData.put(JsonKey.ACTION, JsonKey.DELETE_USER_ACTON);
    eData.put(JsonKey.ITERATION, 1);

    data.put(JsonKey.EDATA, eData);
    logger.info(
        "UserDeletionBackgroundJobActor::inputKafkaTopic:: InstructionEventGenerator.pushInstructionEvent:: ");
    InstructionEventGenerator.pushInstructionEvent(userDeletionTopic, data);
  }
}
