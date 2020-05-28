package org.sunbird.location.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.location.dao.impl.LocationDaoFactory;
import org.sunbird.location.util.LocationRequestValidator;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class will handle all location related request.
 *
 * @author Amit Kumar
 */
@ActorConfig(
  tasks = {
    "createLocation",
    "updateLocation",
    "searchLocation",
    "deleteLocation",
    "getRelatedLocationIds"
  },
  asyncTasks = {}
)
public class LocationActor extends BaseLocationActor {

  private LocationDao locationDao = LocationDaoFactory.getInstance();

  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.LOCATION);
    String operation = request.getOperation();
    switch (operation) {
      case "createLocation":
        createLocation(request);
        break;
      case "updateLocation":
        updateLocation(request);
        break;
      case "searchLocation":
        searchLocation(request);
        break;
      case "deleteLocation":
        deleteLocation(request);
        break;
      case "getRelatedLocationIds":
        getRelatedLocationIds(request);
        break;
      default:
        onReceiveUnsupportedOperation("LocationActor");
    }
  }

  private void getRelatedLocationIds(Request request) {
    Response response = new Response();
    List<String> relatedLocationIds =
        getValidatedRelatedLocationIds((List<String>) request.get(JsonKey.LOCATION_CODES));
    response.getResult().put(JsonKey.RESPONSE, relatedLocationIds);
    sender().tell(response, self());
  }

  private void createLocation(Request request) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      UpsertLocationRequest  locationRequest =  ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
      validateUpsertLocnReq(locationRequest, JsonKey.CREATE);
      // put unique identifier in request for Id
      String id = ProjectUtil.generateUniqueId();
      locationRequest.setId(id);
      Location location = mapper.convertValue(locationRequest, Location.class);
      Response response = locationDao.create(location);
      sender().tell(response, self());
      ProjectLogger.log("Insert location data to ES");
      saveDataToES(mapper.convertValue(location, Map.class), JsonKey.INSERT);
      generateTelemetryForLocation(id, mapper.convertValue(location, Map.class), JsonKey.CREATE,request.getContext());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void updateLocation(Request request) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      UpsertLocationRequest  locationRequest =  ProjectUtil.convertToRequestPojo(request, UpsertLocationRequest.class);
      validateUpsertLocnReq(locationRequest, JsonKey.UPDATE);
      Location location = mapper.convertValue(locationRequest, Location.class);
      Response response = locationDao.update(location);
      sender().tell(response, self());
      ProjectLogger.log("Update location data to ES");
      saveDataToES(mapper.convertValue(location, Map.class), JsonKey.UPDATE);
      generateTelemetryForLocation(
          location.getId(), mapper.convertValue(location, Map.class), JsonKey.UPDATE,request.getContext());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void searchLocation(Request request) {
    try {
      Response response = searchLocation(request.getRequest());
      sender().tell(response, self());
      SearchDTO searchDto = Util.createSearchDto(request.getRequest());
      String[] types = {ProjectUtil.EsType.location.getTypeName()};
      generateSearchTelemetryEvent(searchDto, types, response.getResult(),request.getContext());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private Response searchLocation(Map<String, Object> searchMap) {
      return locationDao.search(searchMap);
  }

  private void deleteLocation(Request request) {
    try {
      String locationId = (String) request.getRequest().get(JsonKey.LOCATION_ID);
      LocationRequestValidator.isLocationHasChild(locationId);
      Response response = locationDao.delete(locationId);
      sender().tell(response, self());
      ProjectLogger.log("Delete location data from ES");
      deleteDataFromES(locationId);
      generateTelemetryForLocation(locationId, new HashMap<>(), JsonKey.DELETE, request.getContext());
    } catch (Exception ex) {
      ProjectLogger.log(ex.getMessage(), ex);
      sender().tell(ex, self());
    }
  }

  private void saveDataToES(Map<String, Object> locData, String opType) {
    Request request = new Request();
    request.setOperation(LocationActorOperation.UPSERT_LOCATION_TO_ES.getValue());
    request.getRequest().put(JsonKey.LOCATION, locData);
    request.getRequest().put(JsonKey.OPERATION_TYPE, opType);
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log("LocationActor:saveDataToES: Exception occurred with error message = ", ex.getMessage());
    }
  }

  private void deleteDataFromES(String locId) {
    Request request = new Request();
    request.setOperation(LocationActorOperation.DELETE_LOCATION_FROM_ES.getValue());
    request.getRequest().put(JsonKey.LOCATION_ID, locId);
    try {
      tellToAnother(request);
    } catch (Exception ex) {
      ProjectLogger.log("LocationActor:saveDataToES: Exception occurred with error message = ", ex.getMessage());
    }
  }

  private void validateUpsertLocnReq(UpsertLocationRequest locationRequest, String operation) {
    if (StringUtils.isNotEmpty(locationRequest.getType())) {
      LocationRequestValidator.isValidLocationType(locationRequest.getType());
    }
    LocationRequestValidator.isValidParentIdAndCode(locationRequest, operation);
  }

  public List<String> getValidatedRelatedLocationIds(List<String> codeList) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(codeList);
    List<Location> locationList = getSearchResult(JsonKey.CODE, codeList);
    List<String> locationIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
            locationList.stream().map(Location::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList);
      } else {
        locationIds = getValidatedRelatedLocationSet(locationList);
      }
    } else {
      throwInvalidParameterValueException(codeList);
    }
    locationIdList.addAll(locationIds);
    return locationIdList;
  }

  private List<Location> getSearchResult(String param, Object value) {
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Response response = searchLocation(searchRequestMap);
    if (response != null) {
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      ObjectMapper mapper = new ObjectMapper();
      return responseList
          .stream()
          .map(s -> mapper.convertValue(s, Location.class))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }

  private Location getLocation(String locationId) {
    List<Location> locations = getSearchResult(JsonKey.ID, locationId);
    if (locations.isEmpty()) return null;

    return locations.get(0);
  }

  private void throwInvalidParameterValueException(List<String> codeList) {
    throw new ProjectCommonException(
        ResponseCode.invalidParameterValue.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.invalidParameterValue.getErrorMessage(), codeList, JsonKey.LOCATION_CODE),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  public Set<String> getValidatedRelatedLocationSet(List<Location> locationList) {
    Set<Location> locationSet = new HashSet<>();
    for (Location requestedLocation : locationList) {
      Set<Location> parentLocnSet = getParentLocations(requestedLocation);
      if (CollectionUtils.sizeIsEmpty(locationSet)) {
        locationSet.addAll(parentLocnSet);
      } else {
        for (Location currentLocation : parentLocnSet) {
          String type = currentLocation.getType();
          locationSet
              .stream()
              .forEach(
                  location -> {
                    if (type.equalsIgnoreCase(location.getType())
                        && !(currentLocation.getId().equals(location.getId()))) {
                      throw new ProjectCommonException(
                          ResponseCode.conflictingOrgLocations.getErrorCode(),
                          ProjectUtil.formatMessage(
                              ResponseCode.conflictingOrgLocations.getErrorMessage(),
                              requestedLocation.getCode(),
                              location.getCode(),
                              type),
                          ResponseCode.CLIENT_ERROR.getResponseCode());
                    }
                  });
          locationSet.add(currentLocation);
        }
      }
    }
    return locationSet.stream().map(Location::getId).collect(Collectors.toSet());
  }

  private Set<Location> getParentLocations(Location locationObj) {
    Set<Location> locationSet = new LinkedHashSet<>();
    Location location = locationObj;
    int count = getOrder(location.getType());
    locationSet.add(location);
    while (count > 0) {
      Location parent = null;
      if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
        parent = getLocation(location.getId());
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = getLocation(location.getParentId());
      }
      if (null != parent) {
        locationSet.add(parent);
        location = parent;
      }
      count--;
    }
    return locationSet;
  }

  public int getOrder(String type) {
    return DataCacheHandler.getLocationOrderMap().get(type);
  }
}
