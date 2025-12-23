package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.user.validator.UserCreateRequestValidator;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.kafka.KafkaClient;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.service.user.UserLookupService;
import org.sunbird.service.user.impl.UserLookUpServiceImpl;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.util.FormApiUtil;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;

public abstract class UserBaseActor extends BaseActor {

  protected final ObjectMapper mapper = new ObjectMapper();
  protected final UserLookupService userLookupService = UserLookUpServiceImpl.getInstance();
  protected final LocationService locationService = LocationServiceImpl.getInstance();

  @Inject
  @Named("user_telemetry_actor")
  private ActorRef userTelemetryActor;

  @Inject
  @Named("location_actor")
  private ActorRef locationActor;

  protected void generateUserTelemetry(
      Map<String, Object> userMap, Request request, String userId, String operationType) {
    Request telemetryReq = new Request();
    telemetryReq.getRequest().put("userMap", userMap);
    telemetryReq.getRequest().put("userId", userId);
    telemetryReq.getRequest().put(JsonKey.OPERATION_TYPE, operationType);
    telemetryReq.setContext(request.getContext());
    telemetryReq.setOperation("generateUserTelemetry");
    userTelemetryActor.tell(telemetryReq, self());
  }

  protected void generateTelemetryEvent(
      Map<String, Object> requestMap,
      String userId,
      String objectType,
      Map<String, Object> context) {
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    Map<String, Object> targetObject =
        TelemetryUtil.generateTargetObject(userId, JsonKey.USER, JsonKey.UPDATE, null);
    Map<String, Object> telemetryAction = new HashMap<>();

    switch (objectType) {
      case "userLevel":
        telemetryAction.put("AssignRole", "role assigned at user level");
        TelemetryUtil.telemetryProcessingCall(
            telemetryAction, targetObject, correlatedObject, context);
        break;
      case "blockUser":
        telemetryAction.put(JsonKey.BLOCK_USER, "user blocked");
        TelemetryUtil.telemetryProcessingCall(
            JsonKey.BLOCK_USER, telemetryAction, targetObject, correlatedObject, context);
        break;
      case "unblockUser":
        telemetryAction.put(JsonKey.UNBLOCK_USER, "user unblocked");
        TelemetryUtil.telemetryProcessingCall(
            JsonKey.UNBLOCK_USER, telemetryAction, targetObject, correlatedObject, context);
        break;
      case "deleteUser":
        telemetryAction.put(JsonKey.DELETE_USER, "user deleted");
        TelemetryUtil.telemetryProcessingCall(
            JsonKey.DELETE_USER, telemetryAction, targetObject, correlatedObject, context);
        break;
      default:
        // Do Nothing
    }
  }

  protected void removeUnwanted(Map<String, Object> reqMap) {
    reqMap.remove(JsonKey.ADDRESS);
    reqMap.remove(JsonKey.EDUCATION);
    reqMap.remove(JsonKey.JOB_PROFILE);
    reqMap.remove(JsonKey.ORGANISATION);
    reqMap.remove(JsonKey.REGISTERED_ORG);
    reqMap.remove(JsonKey.ROOT_ORG);
    reqMap.remove(JsonKey.IDENTIFIER);
    reqMap.remove(JsonKey.ORGANISATIONS);
    reqMap.remove(JsonKey.IS_DELETED);
    reqMap.remove(JsonKey.EXTERNAL_ID);
    reqMap.remove(JsonKey.ID_TYPE);
    reqMap.remove(JsonKey.EXTERNAL_ID_TYPE);
    reqMap.remove(JsonKey.PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_ID_PROVIDER);
    reqMap.remove(JsonKey.EXTERNAL_IDS);
    reqMap.remove(JsonKey.ORGANISATION_ID);
    reqMap.remove(JsonKey.ROLES);
    Util.getUserDefaultValue()
        .keySet()
        .stream()
        .forEach(
            key -> {
              if (!JsonKey.PASSWORD.equalsIgnoreCase(key)) {
                reqMap.remove(key);
              }
            });
  }

  protected void validateAndGetLocationCodes(Request userRequest) {
    Object locationCodes = userRequest.getRequest().get(JsonKey.LOCATION_CODES);
    UserCreateRequestValidator.validateLocationCodesDataType(locationCodes);
    if (CollectionUtils.isNotEmpty((List) locationCodes)) {
      List<Location> locationList = getLocationList(locationCodes, userRequest.getRequestContext());
      String stateCode = UserCreateRequestValidator.validateAndGetStateLocationCode(locationList);
      List<String> allowedLocationTypeList =
          getStateLocationTypeConfig(stateCode, userRequest.getRequestContext());
      String stateId = null;
      List<String> set = new ArrayList<>();
      for (Location location : locationList) {
        UserCreateRequestValidator.isValidLocationType(location.getType(), allowedLocationTypeList);
        if (location.getType().equalsIgnoreCase(JsonKey.STATE)) {
          stateId = location.getId();
        }
        set.add(location.getCode());
      }
      if (StringUtils.isNotBlank((String) userRequest.getRequest().get(JsonKey.ORG_EXTERNAL_ID))) {
        userRequest.getRequest().put(JsonKey.STATE_ID, stateId);
      }
      userRequest.getRequest().put(JsonKey.LOCATION_CODES, set);
    }
  }

  protected List<String> getStateLocationTypeConfig(String stateCode, RequestContext context) {
    Map<String, List<String>> locationTypeConfigMap = DataCacheHandler.getLocationTypeConfig();
    if (MapUtils.isEmpty(locationTypeConfigMap)
        || CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
      Map<String, Object> userProfileConfigMap = FormApiUtil.getProfileConfig(stateCode, context);
      // If config is not available check the default profile config
      if (MapUtils.isEmpty(userProfileConfigMap) && !JsonKey.DEFAULT_PERSONA.equals(stateCode)) {
        stateCode = JsonKey.DEFAULT_PERSONA;
        if (CollectionUtils.isEmpty(locationTypeConfigMap.get(stateCode))) {
          userProfileConfigMap = FormApiUtil.getProfileConfig(stateCode, context);
          if (MapUtils.isNotEmpty(userProfileConfigMap)) {
            List<String> locationTypeList =
                FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
            if (CollectionUtils.isNotEmpty(locationTypeList)) {
              locationTypeConfigMap.put(stateCode, locationTypeList);
            }
          }
        }
      } else {
        List<String> locationTypeList = FormApiUtil.getLocationTypeConfigMap(userProfileConfigMap);
        if (CollectionUtils.isNotEmpty(locationTypeList)) {
          locationTypeConfigMap.put(stateCode, locationTypeList);
        }
      }
    }
    return locationTypeConfigMap.get(stateCode);
  }

  protected List<Location> getLocationList(Object locationCodes, RequestContext context) {
    // As of now locationCode can take array of only location codes and map of location Codes which
    // include type and code of the location
    List<Location> locationList = new ArrayList<>();
    if (((List) locationCodes).get(0) instanceof String) {
      List<String> locations = (List<String>) locationCodes;
      Map<String, Object> filters = new HashMap<>();
      Map<String, Object> searchRequestMap = new HashMap<>();
      filters.put(JsonKey.CODE, locations);
      searchRequestMap.put(JsonKey.FILTERS, filters);
      Response searchResponse = locationService.searchLocation(searchRequestMap, context);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) searchResponse.getResult().get(JsonKey.RESPONSE);
      locationList =
          responseList
              .stream()
              .map(s -> mapper.convertValue(s, Location.class))
              .collect(Collectors.toList());
    }

    if (((List) locationCodes).get(0) instanceof Map) {
      locationList = createLocationLists((List<Map<String, String>>) locationCodes);
    }
    return locationList;
  }

  protected List<Location> createLocationLists(List<Map<String, String>> locationCodes) {
    List<Location> locations = new ArrayList<>();
    for (Map<String, String> locationMap : locationCodes) {
      Location location = new Location();
      location.setCode(locationMap.get(JsonKey.CODE));
      location.setType(locationMap.get(JsonKey.TYPE));
      locations.add(location);
    }
    return locations;
  }

  protected void populateLocationCodesFromProfileLocation(Map<String, Object> userMap) {
    if (userMap.containsKey(JsonKey.PROFILE_LOCATION)) {
      userMap.remove(JsonKey.LOCATION_CODES);
      List<Map<String, String>> profLocList =
          (List<Map<String, String>>) userMap.get(JsonKey.PROFILE_LOCATION);
      List<String> locationCodes = new ArrayList<>();
      if (CollectionUtils.isNotEmpty(profLocList)) {
        for (Map<String, String> location : profLocList) {
          if (JsonKey.LOCATION_TYPE_SCHOOL.equals(location.get(JsonKey.LOCATION_TYPE))) {
            userMap.put(JsonKey.ORG_EXTERNAL_ID, location.get(JsonKey.CODE));
          } else {
            locationCodes.add(location.get(JsonKey.CODE));
          }
        }
        userMap.put(JsonKey.LOCATION_CODES, locationCodes);
      }
      userMap.remove(JsonKey.PROFILE_LOCATION);
    }
  }

  protected void populateUserTypeAndSubType(Map<String, Object> userMap) {
    userMap.remove(JsonKey.USER_TYPE);
    userMap.remove(JsonKey.USER_SUB_TYPE);
    if (userMap.containsKey(JsonKey.PROFILE_USERTYPE)) {
      Map<String, Object> userTypeAndSubType =
          (Map<String, Object>) userMap.get(JsonKey.PROFILE_USERTYPE);
      userMap.put(JsonKey.USER_TYPE, userTypeAndSubType.get(JsonKey.TYPE));
      userMap.put(JsonKey.USER_SUB_TYPE, userTypeAndSubType.get(JsonKey.SUB_TYPE));
    }
  }

  protected void populateProfileUserType(
      Map<String, Object> userMap, RequestContext requestContext) {
    Map<String, String> userTypeAndSubType = new HashMap<>();
    userMap.remove(JsonKey.PROFILE_USERTYPE);
    if (userMap.containsKey(JsonKey.USER_TYPE)) {
      userTypeAndSubType.put(JsonKey.TYPE, (String) userMap.get(JsonKey.USER_TYPE));
      if (userMap.containsKey(JsonKey.USER_SUB_TYPE)) {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, (String) userMap.get(JsonKey.USER_SUB_TYPE));
      } else {
        userTypeAndSubType.put(JsonKey.SUB_TYPE, null);
      }
      try {
        ObjectMapper mapper = new ObjectMapper();

        if (!userMap.containsKey(JsonKey.PROFILE_USERTYPES)) {
          List<Map<String, String>> userTypeAndSubTypes = new ArrayList<>();
          userTypeAndSubTypes.add(userTypeAndSubType);
          userMap.put(JsonKey.PROFILE_USERTYPES, mapper.writeValueAsString(userTypeAndSubTypes));
        }
        userMap.put(JsonKey.PROFILE_USERTYPE, mapper.writeValueAsString(userTypeAndSubType));
      } catch (Exception ex) {
        logger.error(requestContext, "Exception occurred while mapping", ex);
        ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
      }
      userMap.remove(JsonKey.USER_TYPE);
      userMap.remove(JsonKey.USER_SUB_TYPE);
    }
  }

  protected void writeDataToKafka(Map<String, Object> data) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String event = mapper.writeValueAsString(data);
      // user_events
      KafkaClient.send(event, ProjectUtil.getConfigValue("sunbird_user_create_sync_topic"));
    } catch (Exception ex) {
      logger.error("Exception occurred while writing event to kafka", ex);
    }
  }

  protected void convertValidatedLocationCodesToIDs(
      Map<String, Object> userMap, RequestContext context) {
    if (userMap.containsKey(JsonKey.LOCATION_IDS)
        && CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_IDS))) {
      userMap.remove(JsonKey.LOCATION_IDS);
    }
    if (!userMap.containsKey(JsonKey.LOCATION_IDS)
        && userMap.containsKey(JsonKey.LOCATION_CODES)
        && !CollectionUtils.isEmpty((List<String>) userMap.get(JsonKey.LOCATION_CODES))) {
      List<Map<String, String>> locationIdTypeList =
          locationService.getValidatedRelatedLocationIdAndType(
              (List<String>) userMap.get(JsonKey.LOCATION_CODES), context);
      if (locationIdTypeList != null && !locationIdTypeList.isEmpty()) {
        try {
          userMap.put(JsonKey.PROFILE_LOCATION, mapper.writeValueAsString(locationIdTypeList));
        } catch (Exception ex) {
          logger.error(context, "Exception occurred while mapping", ex);
          ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
        }

        userMap.remove(JsonKey.LOCATION_CODES);
      } else {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.invalidParameterValue,
            MessageFormat.format(
                ResponseCode.invalidParameterValue.getErrorMessage(),
                JsonKey.LOCATION_CODES,
                userMap.get(JsonKey.LOCATION_CODES)));
      }
    }
  }
}
