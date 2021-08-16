package org.sunbird.actor.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.models.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.util.ProjectUtil;

/**
 * This class will contains method to validate the location api request.
 *
 * @author Amit Kumar
 */
public class LocationRequestValidator extends BaseLocationRequestValidator {

  Map<String, Integer> orderMap = new HashMap<>();
  private LocationService locationService = new LocationServiceImpl();

  /**
   * This method will validate the list of location code whether its valid or not. If valid will
   * return the locationId List.
   *
   * @param codeList List of location code.
   * @return List of location id.
   */
  public List<String> getValidatedLocationIds(List<String> codeList, RequestContext context) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(codeList);
    List<Location> locationList = locationService.locationSearch(JsonKey.CODE, codeList, context);
    List<String> locationIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
            locationList.stream().map(Location::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList, JsonKey.LOCATION_CODE);
      } else {
        locationIds = getValidatedLocationSet(locationList, context);
      }
    } else {
      throwInvalidParameterValueException(codeList, JsonKey.LOCATION_CODE);
    }
    locationIdList.addAll(locationIds);
    return locationIdList;
  }

  /**
   * This method will validate the list of location ids whether its valid or not. If valid will
   * return the hierarchy List.
   *
   * @return List of locationIds.
   */
  public List<String> getHierarchyLocationIds(List<String> locationIdsList, RequestContext context) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(locationIdsList);
    List<Location> locationList = locationService.locationSearch(JsonKey.ID, locationIdsList, context);
    List<String> locationIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
            locationList.stream().map(Location::getId).collect(Collectors.toList());
        List<String> invalidIdsList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidIdsList, JsonKey.LOCATION_IDS);
      } else {
        locationIds = getValidatedLocationSet(locationList, context);
      }
    } else {
      throwInvalidParameterValueException(locationIdsList, JsonKey.LOCATION_IDS);
    }

    locationIdList.addAll(locationIds);
    return locationIdList;
  }

  private void throwInvalidParameterValueException(List<String> invalidList, String attributeName) {
    throw new ProjectCommonException(
        ResponseCode.invalidParameterValue.getErrorCode(),
        ProjectUtil.formatMessage(
            ResponseCode.invalidParameterValue.getErrorMessage(), invalidList, attributeName),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  /**
   * This method will validate the location hierarchy and return the locationIds list.
   *
   * @param locationList List of location.
   * @return Set of locationId.
   */
  public Set<String> getValidatedLocationSet(List<Location> locationList, RequestContext context) {
    Set<Location> locationSet = new HashSet<>();
    for (Location requestedLocation : locationList) {
      Set<Location> parentLocnSet = getParentLocations(requestedLocation, context);
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

  private Set<Location> getParentLocations(Location locationObj, RequestContext context) {
    Set<Location> locationSet = new LinkedHashSet<>();
    Location location = locationObj;
    int count = getOrder(location.getType());
    locationSet.add(location);
    while (count > 0) {
      Location parent = null;
      if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
        parent = locationService.getLocationById(JsonKey.ID,location.getId(), context);
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = locationService.getLocationById(JsonKey.ID,location.getParentId(), context);
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
    orderMap = locationTypeOrderInit();
    return orderMap.get(type);
  }

  public Map<String, Integer> locationTypeOrderInit(){
    Map<String, Integer> orderMapTemp  = new HashMap<>();
    List<String> subTypeList =
            Arrays.asList(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_VALID_LOCATION_TYPES).split(";"));
    for (String str : subTypeList) {
      List<String> typeList =
              (((Arrays.asList(str.split(","))).stream().map(String::toLowerCase))
                      .collect(Collectors.toList()));
      for (int i = 0; i < typeList.size(); i++) {
        orderMapTemp.put(typeList.get(i), i);
      }
    }
    return orderMapTemp;
  }
}
