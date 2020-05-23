package org.sunbird.location.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.learner.util.Util;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.location.dao.impl.LocationDaoImpl;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;
import scala.concurrent.Future;

/** @author Amit Kumar */
public class LocationRequestValidator {

  private LocationRequestValidator() {}

  private static LocationDao locationDao = new LocationDaoImpl();
  protected static List<List<String>> locationTypeGroupList = new ArrayList<>();
  protected static List<String> typeList = new ArrayList<>();
  private static ObjectMapper mapper = new ObjectMapper();
  private static ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

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
   * This method will validate location code
   *
   * @param code
   * @return boolean
   */
  public static boolean isValidLocationCode(String code) {
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(GeoLocationJsonKey.PROPERTY_NAME, GeoLocationJsonKey.CODE);
    reqMap.put(GeoLocationJsonKey.PROPERTY_VALUE, code);
    Response response = locationDao.getRecordByProperty(reqMap);
    List<Map<String, Object>> locationMapList =
        (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    return (!locationMapList.isEmpty());
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
          ResponseCode.invalidValue.getErrorCode(),
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
  public static boolean isValidParentIdAndCode(
      UpsertLocationRequest locationRequest, String opType) {
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
              ResponseCode.mandatoryParamsMissing.getErrorCode(),
              ProjectUtil.formatMessage(
                  ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                  (GeoLocationJsonKey.PARENT_ID + " or " + GeoLocationJsonKey.PARENT_CODE)),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      } else if (locationTypeList.get(0).equalsIgnoreCase(type.toLowerCase())) {
        if (StringUtils.isNotEmpty(locationRequest.getParentCode())
            || StringUtils.isNotEmpty(locationRequest.getParentId())) {
          throw new ProjectCommonException(
              ResponseCode.parentNotAllowed.getErrorCode(),
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
      isValidLocationCode(locationRequest, opType);
    }
    validateParentIDAndParentCode(locationRequest, opType);
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

  private static void validateParentIDAndParentCode(
      UpsertLocationRequest locationRequest, String opType) {
    String parentCode = locationRequest.getParentCode();
    String parentId = locationRequest.getParentId();
    if (StringUtils.isNotEmpty(parentCode)) {
      Map<String, Object> map = getLocation(parentCode);
      parentId = (String) map.get(JsonKey.ID);
      locationRequest.setParentId((String) map.get(JsonKey.ID));
    }
    if (StringUtils.isNotEmpty(parentId)) {
      String operation = GeoLocationJsonKey.PARENT_ID;
      if (StringUtils.isNotEmpty(parentCode)) {
        operation = GeoLocationJsonKey.PARENT_CODE;
      }
      Map<String, Object> parentLocation = getLocationById(parentId, operation);
      validateParentLocationType(
          mapper.convertValue(parentLocation, UpsertLocationRequest.class),
          locationRequest,
          opType);
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
  private static boolean validateParentLocationType(
      UpsertLocationRequest parentLocation, UpsertLocationRequest location, String opType) {
    String parentType = parentLocation.getType();
    String currentLocType = location.getType();
    Map<String, Object> locn = null;
    if (opType.equalsIgnoreCase(JsonKey.UPDATE)) {
      locn = getLocationById(location.getId(), JsonKey.LOCATION_ID);
      currentLocType = (String) locn.get(GeoLocationJsonKey.LOCATION_TYPE);
    }
    Map<String, Integer> currentLocTypeoOrdermap =
        getLocationTypeOrderMap(currentLocType.toLowerCase());
    Map<String, Integer> parentLocTypeoOrdermap =
        getLocationTypeOrderMap(currentLocType.toLowerCase());
    if ((currentLocTypeoOrdermap.get(currentLocType.toLowerCase())
            - parentLocTypeoOrdermap.get(parentType.toLowerCase()))
        != 1) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter.getErrorCode(),
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
  private static Map<String, Object> getLocationById(String id, String parameter) {
    Future<Map<String, Object>> locationF =
        esUtil.getDataByIdentifier(ProjectUtil.EsType.location.getTypeName(), id);
    Map<String, Object> location =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(locationF);
    if (MapUtils.isEmpty(location)) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter.getErrorCode(),
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
  private static Map<String, Object> getLocation(String code) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(GeoLocationJsonKey.CODE, code);
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FILTERS, filters);
    List<Map<String, Object>> locationMapList =
        getESSearchResult(
            map,
            ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.location.getTypeName());
    if (CollectionUtils.isNotEmpty(locationMapList)) {
      return locationMapList.get(0);
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidParameter.getErrorCode(),
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
  public static boolean isLocationHasChild(String locationId) {
    Map<String, Object> location = getLocationById(locationId, JsonKey.LOCATION_ID);
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
          getESSearchResult(
              map,
              ProjectUtil.EsIndex.sunbird.getIndexName(),
              ProjectUtil.EsType.location.getTypeName());
      if (CollectionUtils.isNotEmpty(locationMapList)) {
        throw new ProjectCommonException(
            ResponseCode.invalidLocationDeleteRequest.getErrorCode(),
            ResponseCode.invalidLocationDeleteRequest.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    }
    return true;
  }

  public static List<Map<String, Object>> getESSearchResult(
      Map<String, Object> searchQueryMap, String esIndex, String esType) {
    SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, esType);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
    return (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
  }

  public static boolean isValidLocationCode(UpsertLocationRequest locationRequest, String opType) {
    Map<String, Object> filters = new HashMap<>();
    filters.put(GeoLocationJsonKey.CODE, locationRequest.getCode());
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.FILTERS, filters);
    List<Map<String, Object>> locationMapList =
        getESSearchResult(
            map,
            ProjectUtil.EsIndex.sunbird.getIndexName(),
            ProjectUtil.EsType.location.getTypeName());
    if (!locationMapList.isEmpty()) {
      if (opType.equalsIgnoreCase(JsonKey.CREATE)) {
        throw new ProjectCommonException(
            ResponseCode.alreadyExists.getErrorCode(),
            ProjectUtil.formatMessage(
                ResponseCode.alreadyExists.getErrorMessage(),
                GeoLocationJsonKey.CODE,
                locationRequest.getCode()),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      } else if (opType.equalsIgnoreCase(JsonKey.UPDATE)) {
        Map<String, Object> locn = locationMapList.get(0);
        if (!(((String) locn.get(JsonKey.ID)).equalsIgnoreCase(locationRequest.getId()))) {
          throw new ProjectCommonException(
              ResponseCode.alreadyExists.getErrorCode(),
              ProjectUtil.formatMessage(
                  ResponseCode.alreadyExists.getErrorMessage(),
                  GeoLocationJsonKey.CODE,
                  locationRequest.getCode()),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      }
    }
    return true;
  }
}
