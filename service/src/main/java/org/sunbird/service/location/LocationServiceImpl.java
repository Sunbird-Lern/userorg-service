package org.sunbird.service.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.dao.location.LocationDao;
import org.sunbird.dao.location.impl.LocationDaoFactory;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.DataCacheHandler;
import org.sunbird.common.ProjectUtil;

public class LocationServiceImpl implements LocationService {
  public static LocationService locationService = null;
  private final LocationDao locationDao = LocationDaoFactory.getInstance();

  public static synchronized LocationService getInstance() {
    if (locationService == null) locationService = new LocationServiceImpl();
    return locationService;
  }

  @Override
  public Response createLocation(Location location, RequestContext context) {
    return locationDao.create(location, context);
  }

  @Override
  public Response updateLocation(Location location, RequestContext context) {
    return locationDao.update(location, context);
  }

  @Override
  public Response deleteLocation(String locationId, RequestContext context) {
    return locationDao.delete(locationId, context);
  }

  @Override
  public Response searchLocation(Map<String, Object> searchQueryMap, RequestContext context) {
    return locationDao.search(searchQueryMap, context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Map<String, String>> getValidatedRelatedLocationIdAndType(
      List<String> codeList, RequestContext context) {
    List<Location> locationIdTypeList = locationSearch(JsonKey.CODE, codeList, context);
    List<Map<String, String>> locationIdType = new ArrayList<>();
    List<String> codes = new ArrayList<>(codeList);
    if (CollectionUtils.isNotEmpty(locationIdTypeList)) {
      if (locationIdTypeList.size() != codes.size()) {
        List<String> resCodeList =
            locationIdTypeList.stream().map(Location::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList);
      } else {
        Map<String, Location> locationMap =
            getValidatedRelatedLocation(locationIdTypeList, context);
        locationMap
            .entrySet()
            .forEach(
                m -> {
                  Map<String, String> locationIdTypeMap = new HashMap<>();
                  locationIdTypeMap.put(JsonKey.ID, m.getValue().getId());
                  locationIdTypeMap.put(JsonKey.TYPE, m.getValue().getType());
                  locationIdType.add(locationIdTypeMap);
                });
      }
    } else {
      throwInvalidParameterValueException(codeList);
    }
    return locationIdType;
  }

  public List<String> getValidatedRelatedLocationIds(
      List<String> codeList, RequestContext context) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(codeList);
    List<Location> locationList = locationSearch(JsonKey.CODE, codeList, context);
    List<String> locationIdList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(locationList)) {
      if (locationList.size() != codes.size()) {
        List<String> resCodeList =
            locationList.stream().map(Location::getCode).collect(Collectors.toList());
        List<String> invalidCodeList =
            codes.stream().filter(s -> !resCodeList.contains(s)).collect(Collectors.toList());
        throwInvalidParameterValueException(invalidCodeList);
      } else {
        Map<String, Location> locationMap = getValidatedRelatedLocation(locationList, context);
        locationIds =
            locationMap.values().stream().map(Location::getId).collect(Collectors.toSet());
      }
    } else {
      throwInvalidParameterValueException(codeList);
    }
    locationIdList.addAll(locationIds);
    return locationIdList;
  }

  @SuppressWarnings("unchecked")
  public List<Location> locationSearch(String param, Object value, RequestContext context) {
    Map<String, Object> filter = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    List<Location> locationResponseList = new ArrayList<>();
    filter.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filter);
    Response response = searchLocation(searchRequestMap, context);
    if (response != null) {
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
      ObjectMapper mapper = new ObjectMapper();
      locationResponseList =
          responseList
              .stream()
              .map(s -> mapper.convertValue(s, Location.class))
              .collect(Collectors.toList());
    }
    return locationResponseList;
  }

  private Map<String, Location> getValidatedRelatedLocation(
      List<Location> locationList, RequestContext context) {
    Map<String, Location> locationMap = new HashMap<>();
    for (Location requestedLocation : locationList) {
      Set<Location> parentLocnSet = getParentLocations(requestedLocation, context);
      for (Location currentLocation : parentLocnSet) {
        String type = currentLocation.getType();
        Location location = locationMap.get(type);
        if (null != location) {
          if (!(currentLocation.getId().equals(location.getId()))) {
            throw new ProjectCommonException(
                ResponseCode.conflictingOrgLocations,
                ProjectUtil.formatMessage(
                    ResponseCode.conflictingOrgLocations.getErrorMessage(),
                    requestedLocation.getCode(),
                    location.getCode(),
                    type),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        } else {
          locationMap.put(type, currentLocation);
        }
      }
    }
    return locationMap;
  }

  private Set<Location> getParentLocations(Location locationObj, RequestContext context) {
    Set<Location> locationSet = new LinkedHashSet<>();
    Location location = locationObj;
    int count = getOrder(location.getType());
    locationSet.add(location);
    while (count > 0) {
      Location parent = null;
      if (getOrder(location.getType()) == 0 && StringUtils.isNotEmpty(location.getId())) {
        parent = getLocationById(location.getId(), context);
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = getLocationById(location.getParentId(), context);
      }
      if (null != parent) {
        locationSet.add(parent);
        location = parent;
      }
      count--;
    }
    return locationSet;
  }

  private int getOrder(String type) {
    return DataCacheHandler.getLocationOrderMap().get(type);
  }

  private void throwInvalidParameterValueException(List<String> codeList) {
    throw new ProjectCommonException(
        ResponseCode.invalidParameterValue,
        ProjectUtil.formatMessage(
            ResponseCode.invalidParameterValue.getErrorMessage(), codeList, JsonKey.LOCATION_CODE),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }

  @SuppressWarnings("unchecked")
  public Location getLocationById(String locationId, RequestContext context) {
    Response response = locationDao.read(locationId, context);
    List<Map<String, Object>> responseList =
        (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
    if (CollectionUtils.isNotEmpty(responseList)) {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.convertValue(responseList.get(0), Location.class);
    }
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> getLocationsByIds(
      List<String> locationIds, List<String> locationFields, RequestContext context) {
    Response response = locationDao.getLocationsByIds(locationIds, locationFields, context);
    return (List<Map<String, Object>>) response.getResult().get(JsonKey.RESPONSE);
  }

  @Override
  public String saveLocationToEs(String id, Map<String, Object> data, RequestContext context) {
    return locationDao.saveLocationToEs(id, data, context);
  }
}
