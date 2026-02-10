package org.sunbird.actor.location.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.GeoLocationJsonKey;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.model.location.UpsertLocationRequest;
import org.sunbird.request.RequestContext;
import org.sunbird.service.location.LocationService;
import org.sunbird.service.location.LocationServiceImpl;
import org.sunbird.common.ProjectUtil;
import scala.concurrent.Future;

/** @author Amit Kumar */
public class LocationRequestValidator extends BaseLocationRequestValidator {

  private final ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private final LocationService locationService = new LocationServiceImpl();
  private final ObjectMapper mapper = new ObjectMapper();

  private static final Map<String, Integer> orderMap = new HashMap<>();
  private static final List<List<String>> locationTypeGroupList = new ArrayList<>();
  private static final List<String> typeList = new ArrayList<>();

  static {
    List<String> subTypeList =
        Arrays.asList(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_VALID_LOCATION_TYPES).split(";"));
    for (String str : subTypeList) {
      List<String> typeList =
          (((Arrays.asList(str.split(","))).stream().map(String::toLowerCase))
              .collect(Collectors.toList()));
      for (int i = 0; i < typeList.size(); i++) {
        orderMap.put(typeList.get(i), i);
      }
    }
  }

  static {
    List<String> subTypeList =
        Arrays.asList(
            ProjectUtil.getConfigValue(GeoLocationJsonKey.SUNBIRD_VALID_LOCATION_TYPES).split(";"));
    for (String str : subTypeList) {
      typeList.addAll(
          ((Arrays.asList(str.split(",")))
                  .stream()
                  .map(
                      x -> {
                        return x.toLowerCase();
                      }))
              .collect(Collectors.toList()));
      locationTypeGroupList.add(
          ((Arrays.asList(str.split(",")))
                  .stream()
                  .map(
                      x -> {
                        return x.toLowerCase();
                      }))
              .collect(Collectors.toList()));
    }
  }

  /**
   * This method will validate location type
   *
   * @param type
   * @return
   */
  public static boolean isValidLocationType(String type) {
    if (null != type && !typeList.contains(type.toLowerCase())) {
      throw new ProjectCommonException(
          ResponseCode.invalidValue,
          ProjectUtil.formatMessage(
              ResponseCode.invalidValue.getErrorMessage(),
              GeoLocationJsonKey.LOCATION_TYPE,
              type,
              typeList),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return true;
  }

  /**
   * This method will validate parentId , parentCode for given type.
   *
   * @param locationRequest represents the location request object which has to be validate.
   * @param opType type of location.
   * @return boolean If parent id and code are valid return true else false.
   */
  public boolean isValidParentIdAndCode(
      UpsertLocationRequest locationRequest, String opType, RequestContext context) {
    String type = locationRequest.getType();
    if (StringUtils.isNotEmpty(type)) {
      List<String> locationTypeList = getLocationSubTypeListForType(type);
      // if type is of top level then no need to validate parentCode and parentId
      if (!locationTypeList.get(0).equalsIgnoreCase(type.toLowerCase())) {
        // while creating new location, if locationType is not top level then type and parent id is
        // mandatory
        if ((StringUtils.isEmpty(locationRequest.getParentCode())
            && StringUtils.isEmpty(locationRequest.getParentId()))) {
          throw new ProjectCommonException(
              ResponseCode.mandatoryParamsMissing,
              ProjectUtil.formatMessage(
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  (GeoLocationJsonKey.PARENT_ID + " or " + GeoLocationJsonKey.PARENT_CODE)),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      } else if (locationTypeList.get(0).equalsIgnoreCase(type.toLowerCase())) {
        if (StringUtils.isNotEmpty(locationRequest.getParentCode())
            || StringUtils.isNotEmpty(locationRequest.getParentId())) {
          throw new ProjectCommonException(
              ResponseCode.parentNotAllowed,
              ProjectUtil.formatMessage(
                  ResponseCode.parentNotAllowed.getErrorMessage(),
                  (GeoLocationJsonKey.PARENT_ID + " or " + GeoLocationJsonKey.PARENT_CODE)),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        // if type is top level then parentCode and parentId is null
        locationRequest.setParentCode(null);
        locationRequest.setParentId(null);
      }
    }
    if (StringUtils.isNotEmpty(locationRequest.getCode())) {
      isValidLocationCode(locationRequest, opType, context);
    }
    validateParentIDAndParentCode(locationRequest, opType, context);
    return true;
  }

  private static List<String> getLocationSubTypeListForType(String type) {
    for (List<String> subList : locationTypeGroupList) {
      if (subList.contains(type.toLowerCase())) {
        return subList;
      }
    }
    return (new ArrayList<>());
  }

  private void validateParentIDAndParentCode(
      UpsertLocationRequest locationRequest, String opType, RequestContext context) {
    String parentCode = locationRequest.getParentCode();
    String parentId = locationRequest.getParentId();
    if (StringUtils.isNotEmpty(parentCode)) {
      Map<String, Object> map = getLocation(parentCode, context);
      parentId = (String) map.get(JsonKey.ID);
      locationRequest.setParentId((String) map.get(JsonKey.ID));
    }
    if (StringUtils.isNotEmpty(parentId)) {
      String operation = GeoLocationJsonKey.PARENT_ID;
      if (StringUtils.isNotEmpty(parentCode)) {
        operation = GeoLocationJsonKey.PARENT_CODE;
      }
      Map<String, Object> parentLocation = getLocationById(parentId, operation, context);
      validateParentLocationType(
          mapper.convertValue(parentLocation, UpsertLocationRequest.class),
          locationRequest,
          opType,
          context);
    }
  }

  /**
   * This method will validate the parent location type means parent should be only one level up
   * from child.
   *
   * @param parentLocation represents parent location request object.
   * @param location represents child location request object.
   * @return boolean If child location has valid hierarchy with parent return true otherwise false.
   */
  private boolean validateParentLocationType(
      UpsertLocationRequest parentLocation,
      UpsertLocationRequest location,
      String opType,
      RequestContext context) {
    int levelLimit = 1;
    String parentType = parentLocation.getType();
    String currentLocType = location.getType();
    Map<String, Object> locn = null;
    if (opType.equalsIgnoreCase(JsonKey.UPDATE)) {
      locn = getLocationById(location.getId(), JsonKey.LOCATION_ID, context);
      currentLocType = (String) locn.get(GeoLocationJsonKey.LOCATION_TYPE);
    }
    Map<String, Integer> currentLocTypeoOrdermap =
        getLocationTypeOrderMap(currentLocType.toLowerCase());
    Map<String, Integer> parentLocTypeoOrdermap =
        getLocationTypeOrderMap(currentLocType.toLowerCase());
    if (currentLocType.equalsIgnoreCase(JsonKey.LOCATION_TYPE_SCHOOL)) {
      levelLimit = 2;
    }
    if (!((currentLocTypeoOrdermap.get(currentLocType.toLowerCase())
            - parentLocTypeoOrdermap.get(parentType.toLowerCase()))
        <= levelLimit)) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          ProjectUtil.formatMessage(
              ResponseCode.invalidParameter.getErrorMessage(),
              (GeoLocationJsonKey.PARENT_ID + " or " + GeoLocationJsonKey.PARENT_CODE)),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return false;
  }

  private static Map<String, Integer> getLocationTypeOrderMap(String type) {
    List<String> list = getLocationSubTypeListForType(type);
    Map<String, Integer> locTypeOrderMap = new LinkedHashMap<>();
    for (int i = 0; i < list.size(); i++) {
      locTypeOrderMap.put(list.get(i), i);
    }
    return locTypeOrderMap;
  }

  /**
   * This method will return location details based on id
   *
   * @param id
   * @return Map<String, Object> location details
   */
  private Map<String, Object> getLocationById(String id, String parameter, RequestContext context) {
    Future<Map<String, Object>> locationF =
        esUtil.getDataByIdentifier(ProjectUtil.EsType.location.getTypeName(), id, context);
    Map<String, Object> location =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(locationF);
    if (MapUtils.isEmpty(location)) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          ProjectUtil.formatMessage(ResponseCode.invalidParameter.getErrorMessage(), parameter),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    return location;
  }

  /**
   * This method will validate location code and if code is valid will return location .
   *
   * @param code Value of location code we are looking for.
   * @return location details Map<String, Object>
   */
  private Map<String, Object> getLocation(String code, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(GeoLocationJsonKey.CODE, code);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FILTERS, filters);
    List<Map<String, Object>> locationMapList =
        getESSearchResult(map, ProjectUtil.EsType.location.getTypeName(), context);
    if (CollectionUtils.isNotEmpty(locationMapList)) {
      return locationMapList.get(0);
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter,
          ProjectUtil.formatMessage(
              ResponseCode.invalidParameter.getErrorMessage(), GeoLocationJsonKey.PARENT_CODE),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * This method will check whether passed locationId is the parent of any location
   *
   * @param locationId
   * @return boolean
   */
  public boolean isLocationHasChild(String locationId, RequestContext context) {
    Map<String, Object> location = getLocationById(locationId, JsonKey.LOCATION_ID, context);
    Map<String, Integer> locTypeoOrdermap =
        getLocationTypeOrderMap(
            ((String) location.get(GeoLocationJsonKey.LOCATION_TYPE)).toLowerCase());
    List<Integer> list = new ArrayList<>(locTypeoOrdermap.values());
    list.sort(Comparator.reverseOrder());
    int order =
        locTypeoOrdermap.get(
            ((String) location.get(GeoLocationJsonKey.LOCATION_TYPE)).toLowerCase());
    // location type with last order can be deleted without validation
    if (order != list.get(0)) {
      Map<String, Object> filters = new HashMap<>();
      filters.put(GeoLocationJsonKey.PARENT_ID, location.get(JsonKey.ID));
      Map<String, Object> map = new HashMap<>();
      map.put(JsonKey.FILTERS, filters);
      List<Map<String, Object>> locationMapList =
          getESSearchResult(map, ProjectUtil.EsType.location.getTypeName(), context);
      if (CollectionUtils.isNotEmpty(locationMapList)) {
        throw new ProjectCommonException(
            ResponseCode.invalidLocationDeleteRequest,
            ResponseCode.invalidLocationDeleteRequest.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return true;
  }

  public List<Map<String, Object>> getESSearchResult(
      Map<String, Object> searchQueryMap, String esType, RequestContext context) {
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, esType, context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
  }

  public boolean isValidLocationCode(
      UpsertLocationRequest locationRequest, String opType, RequestContext context) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(GeoLocationJsonKey.CODE, locationRequest.getCode());
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FILTERS, filters);
    List<Map<String, Object>> locationMapList =
        getESSearchResult(map, ProjectUtil.EsType.location.getTypeName(), context);
    if (!locationMapList.isEmpty()) {
      if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
        throw new ProjectCommonException(
            ResponseCode.errorDuplicateEntry,
            ProjectUtil.formatMessage(
                ResponseCode.errorDuplicateEntry.getErrorMessage(),
                locationRequest.getCode(),
                GeoLocationJsonKey.CODE),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      } else if (opType.equalsIgnoreCase(JsonKey.UPDATE)) {
        Map<String, Object> locn = locationMapList.get(0);
        if (!(((String) locn.get(JsonKey.ID)).equalsIgnoreCase(locationRequest.getId()))) {
          throw new ProjectCommonException(
              ResponseCode.errorDuplicateEntry,
              ProjectUtil.formatMessage(
                  ResponseCode.errorDuplicateEntry.getErrorMessage(),
                  locationRequest.getCode(),
                  GeoLocationJsonKey.CODE),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
    return true;
  }
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
  public List<String> getHierarchyLocationIds(
      List<String> locationIdsList, RequestContext context) {
    Set<String> locationIds = null;
    List<String> codes = new ArrayList<>(locationIdsList);
    List<Location> locationList =
        locationService.locationSearch(JsonKey.ID, locationIdsList, context);
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
        ResponseCode.invalidParameterValue,
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
                          ResponseCode.conflictingOrgLocations,
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
        parent = locationService.getLocationById(location.getId(), context);
      } else if (StringUtils.isNotEmpty(location.getParentId())) {
        parent = locationService.getLocationById(location.getParentId(), context);
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
    return orderMap.get(type);
  }
}
