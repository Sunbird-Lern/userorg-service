package org.sunbird.user.actors;

import akka.actor.ActorRef;
import com.typesafe.config.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.SocialMediaType;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;

@ActorConfig(
  tasks = {"profileVisibility", "getMediaTypes"},
  asyncTasks = {}
)
public class UserProfileActor extends UserBaseActor {

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    String operation = request.getOperation();
    switch (operation) {
      case "getMediaTypes":
        getMediaTypes();
        break;
      case "profileVisibility":
        setProfileVisibility(request);
        break;
      default:
        onReceiveUnsupportedMessage("UserProfileActor");
        break;
    }
  }

  private void getMediaTypes() {
    Response response = SocialMediaType.getMediaTypeFromDB();
    sender().tell(response, self());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void setProfileVisibility(Request actorMessage) {
    Map<String, Object> map = (Map) actorMessage.getRequest();

    String userId = (String) map.get(JsonKey.USER_ID);
    List<String> privateList = (List) map.get(JsonKey.PRIVATE);
    List<String> publicList = (List) map.get(JsonKey.PUBLIC);

    validateFields(privateList, JsonKey.PUBLIC_FIELDS);
    validateFields(publicList, JsonKey.PRIVATE_FIELDS);

    Map<String, Object> esPublicUserProfile = getUserService().esGetPublicUserProfileById(userId);
    Map<String, Object> esPrivateUserProfile = getUserService().esGetPrivateUserProfileById(userId);

    updateUserProfile(publicList, privateList, esPublicUserProfile, esPrivateUserProfile);

    updateProfileVisibility(userId, publicList, privateList, esPublicUserProfile);

    getUserService().syncUserProfile(userId, esPublicUserProfile, esPrivateUserProfile);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(response, self());

    generateTelemetryEvent(null, userId, "profileVisibility");
  }

  private void validateFields(List<String> values, String listType) {
    // Remove duplicate entries from the list
    // Visibility of permanent fields cannot be changed
    if (CollectionUtils.isNotEmpty(values)) {
      List<String> distValues = values.stream().distinct().collect(Collectors.toList());
      validateProfileVisibilityFields(distValues, listType, getSystemSettingActorRef());
    }
  }

  public void validateProfileVisibilityFields(
      List<String> fieldList, String fieldTypeKey, ActorRef actorRef) {
    String conflictingFieldTypeKey =
        JsonKey.PUBLIC_FIELDS.equalsIgnoreCase(fieldTypeKey) ? JsonKey.PRIVATE : JsonKey.PUBLIC;

    Config userProfileConfig = Util.getUserProfileConfig(actorRef);

    List<String> fields = userProfileConfig.getStringList(fieldTypeKey);
    List<String> fieldsCopy = new ArrayList<String>(fields);
    fieldsCopy.retainAll(fieldList);

    if (!fieldsCopy.isEmpty()) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.invalidParameterValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              fieldsCopy.toString(),
              StringFormatter.joinByDot(JsonKey.PROFILE_VISIBILITY, conflictingFieldTypeKey)));
    }
  }

  private void updateUserProfile(
      List<String> publicList,
      List<String> privateList,
      Map<String, Object> esPublicUserProfile,
      Map<String, Object> esPrivateUserProfile) {
    Map<String, Object> privateDataMap = null;
    if (CollectionUtils.isNotEmpty(privateList)) {
      privateDataMap = getPrivateFieldMap(privateList, esPublicUserProfile, esPrivateUserProfile);
    }
    if (privateDataMap != null && privateDataMap.size() > 0) {
      resetPrivateFieldsInPublicUserProfile(privateDataMap, esPublicUserProfile);
    }

    addRemovedPrivateFieldsInPublicUserProfile(
        publicList, esPublicUserProfile, esPrivateUserProfile);
  }

  private Map<String, Object> getPrivateFieldMap(
      List<String> privateFieldList, Map<String, Object> data, Map<String, Object> oldPrivateData) {
    Map<String, Object> privateFieldMap = createPrivateFieldMap(data, privateFieldList);
    privateFieldMap.putAll(oldPrivateData);
    return privateFieldMap;
  }

  private Map<String, Object> resetPrivateFieldsInPublicUserProfile(
      Map<String, Object> privateDataMap, Map<String, Object> esPublicUserProfile) {
    for (String field : privateDataMap.keySet()) {
      if ("dob".equalsIgnoreCase(field)) {
        esPublicUserProfile.put(field, null);
      } else if (privateDataMap.get(field) instanceof List) {
        esPublicUserProfile.put(field, new ArrayList<>());
      } else if (privateDataMap.get(field) instanceof Map) {
        esPublicUserProfile.put(field, new HashMap<>());
      } else if (privateDataMap.get(field) instanceof String) {
        esPublicUserProfile.put(field, "");
      } else {
        esPublicUserProfile.put(field, null);
      }
    }
    return esPublicUserProfile;
  }

  private void addRemovedPrivateFieldsInPublicUserProfile(
      List<String> publicList,
      Map<String, Object> esPublicUserProfile,
      Map<String, Object> esPrivateUserProfile) {
    if (CollectionUtils.isNotEmpty(publicList)) {
      for (String field : publicList) {
        if (esPrivateUserProfile.containsKey(field)) {
          esPublicUserProfile.put(field, esPrivateUserProfile.get(field));
          esPrivateUserProfile.remove(field);
        } else {
          ProjectLogger.log(
              "UserProfileActor:addRemovedPrivateFieldsInPublicUserProfile: private index does not have field = "
                  + field);
        }
      }
    }
  }

  private void updateProfileVisibility(
      String userId,
      List<String> publicList,
      List<String> privateList,
      Map<String, Object> esPublicUserProfile) {
    Map<String, String> profileVisibilityMap =
        (Map<String, String>) esPublicUserProfile.get(JsonKey.PROFILE_VISIBILITY);

    if (null == profileVisibilityMap) {
      profileVisibilityMap = new HashMap<>();
    }

    prepareProfileVisibilityMap(profileVisibilityMap, publicList, JsonKey.PUBLIC);
    prepareProfileVisibilityMap(profileVisibilityMap, privateList, JsonKey.PRIVATE);

    if (profileVisibilityMap.size() > 0) {
      saveUserProfileVisibility(userId, profileVisibilityMap);
      esPublicUserProfile.put(JsonKey.PROFILE_VISIBILITY, profileVisibilityMap);
    }
  }

  private void prepareProfileVisibilityMap(
      Map<String, String> profileVisibilityMap, List<String> list, String value) {
    if (list != null) {
      for (String key : list) {
        profileVisibilityMap.put(key, value);
      }
    }
  }

  private void saveUserProfileVisibility(String userId, Map<String, String> privateFieldMap) {
    User user = new User();
    user.setId(userId);
    user.setProfileVisibility(privateFieldMap);

    Response response = getUserDao().updateUser(user);

    String responseStr = (String) response.get(JsonKey.RESPONSE);
    ProjectLogger.log("UserProfileActor:saveUserProfileVisibility: responseStr = " + responseStr);
  }

  private Map<String, Object> createPrivateFieldMap(Map<String, Object> map, List<String> fields) {
    Map<String, Object> privateMap = new HashMap<>();
    if (fields != null && !fields.isEmpty()) {
      for (String field : fields) {
        // If field is nested (e.g. address.someField) then the parent key is marked as private
        if (field.contains(JsonKey.ADDRESS + ".")) {
          privateMap.put(JsonKey.ADDRESS, map.get(JsonKey.ADDRESS));
        } else if (field.contains(JsonKey.EDUCATION + ".")) {
          privateMap.put(JsonKey.EDUCATION, map.get(JsonKey.EDUCATION));
        } else if (field.contains(JsonKey.JOB_PROFILE + ".")) {
          privateMap.put(JsonKey.JOB_PROFILE, map.get(JsonKey.JOB_PROFILE));
        } else if (field.contains(JsonKey.SKILLS + ".")) {
          privateMap.put(JsonKey.SKILLS, map.get(JsonKey.SKILLS));
        } else if (field.contains(JsonKey.BADGE_ASSERTIONS + ".")) {
          privateMap.put(JsonKey.BADGE_ASSERTIONS, map.get(JsonKey.BADGE_ASSERTIONS));
        } else {
          if (!map.containsKey(field)) {
            throw new ProjectCommonException(
                ResponseCode.InvalidColumnError.getErrorCode(),
                ResponseCode.InvalidColumnError.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
          privateMap.put(field, map.get(field));
        }
      }
    }
    return privateMap;
  }
}
