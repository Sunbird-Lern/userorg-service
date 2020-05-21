package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.models.user.User;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Future;

@ActorConfig(
  tasks = {
    "upsertUserDetailsToES",
    "upsertUserAddressToES",
    "upsertUserEducationToES",
    "upsertUserJobProfileToES",
    "upsertUserOrgDetailsToES"
  },
  asyncTasks = {
    "upsertUserDetailsToES",
    "upsertUserAddressToES",
    "upsertUserEducationToES",
    "upsertUserJobProfileToES",
    "upsertUserOrgDetailsToES"
  }
)
public class UserBackgroundJobActor extends BaseActor {

  private static ObjectMapper mapper = new ObjectMapper();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public void onReceive(Request request) throws Throwable {
    Map<String, Object> userDetails = request.getRequest();
    String operation = request.getOperation();
    switch (operation) {
      case "upsertUserDetailsToES":
        saveUserDataToES(userDetails);
        break;
      case "upsertUserAddressToES":
        saveUserAddressToES(userDetails);
        break;
      case "upsertUserEducationToES":
        saveUserEducationToES(userDetails);
        break;
      case "upsertUserJobProfileToES":
        saveUserJobProfileToES(userDetails);
        break;
      case "upsertUserOrgDetailsToES":
        saveUserOrgDetailsToES(userDetails);
        break;
      default:
        onReceiveUnsupportedOperation("UserBackgroundJobActor");
        break;
    }
  }

  private void saveUserOrgDetailsToES(Map<String, Object> userDetails) {
    Map<String, Object> userOrgMap = new HashMap<>();
    userOrgMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    userOrgMap.put(
        JsonKey.ORGANISATIONS,
        UserUtil.getActiveUserOrgDetails((String) userDetails.get(JsonKey.ID)));
    ProjectLogger.log("Updating saveUserOrgDetailsToES");
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userOrgMap);
  }

  private void saveUserJobProfileToES(Map<String, Object> userDetails) {
    ProjectLogger.log("Updating saveUserJobProfileToES");
    Map<String, Object> jobProfileMap = new HashMap<>();
    jobProfileMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    jobProfileMap.put(JsonKey.JOB_PROFILE, userDetails.get(JsonKey.JOB_PROFILE));
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        jobProfileMap);
  }

  private void saveUserEducationToES(Map<String, Object> userDetails) {
    ProjectLogger.log("Updating saveUserEducationToES");
    Map<String, Object> educationMap = new HashMap<>();
    educationMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    educationMap.put(JsonKey.EDUCATION, userDetails.get(JsonKey.EDUCATION));
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        educationMap);
  }

  private void saveUserAddressToES(Map<String, Object> userDetails) {
    ProjectLogger.log("Updating saveUserAddressToES");
    Map<String, Object> addressMap = new HashMap<>();
    addressMap.put(JsonKey.ID, userDetails.get(JsonKey.ID));
    addressMap.put(JsonKey.ADDRESS, userDetails.get(JsonKey.ADDRESS));
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        addressMap);
  }

  private void saveUserDataToES(Map<String, Object> userDetails) {
    ProjectLogger.log("Updating saveUserDataToES");
    userDetails.remove(JsonKey.PASSWORD);
    User user = mapper.convertValue(userDetails, User.class);
    userDetails = mapper.convertValue(user, Map.class);
    upsertDataToElastic(
        ProjectUtil.EsIndex.sunbird.getIndexName(),
        ProjectUtil.EsType.user.getTypeName(),
        (String) userDetails.get(JsonKey.ID),
        userDetails);
  }

  private void upsertDataToElastic(
      String indexName, String typeName, String id, Map<String, Object> userDetails) {

    Future<Boolean> bool = esUtil.upsert(typeName, id, userDetails);

    ProjectLogger.log(
        "Getting ES save response for type , identifier=="
            + typeName
            + "  "
            + id
            + "  "
            + ElasticSearchHelper.getResponseFromFuture(bool),
        LoggerEnum.INFO.name());
  }
}
