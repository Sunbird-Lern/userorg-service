package org.sunbird.actorutil.location.impl;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actorutil.InterServiceCommunication;
import org.sunbird.actorutil.InterServiceCommunicationFactory;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LocationActorOperation;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;

public class LocationClientImpl implements LocationClient {

  private static InterServiceCommunication interServiceCommunication =
      InterServiceCommunicationFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public List<Location> getLocationsByCodes(ActorRef actorRef, List<String> codeList) {
    return getSearchResponse(actorRef, GeoLocationJsonKey.CODE, codeList);
  }

  @Override
  public List<Location> getLocationByIds(ActorRef actorRef, List<String> idsList) {
    return getSearchResponse(actorRef, GeoLocationJsonKey.ID, idsList);
  }

  @Override
  public Location getLocationById(ActorRef actorRef, String id) {
    List<Location> locationList = getSearchResponse(actorRef, JsonKey.ID, id);
    if (CollectionUtils.isNotEmpty(locationList)) {
      return locationList.get(0);
    } else {
      return null;
    }
  }

  private List<Location> getSearchResponse(ActorRef actorRef, String param, Object value) {
    List<Location> response = null;
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Request request = new Request();
    request.setOperation(LocationActorOperation.SEARCH_LOCATION.getValue());
    request.getRequest().putAll(searchRequestMap);
    ProjectLogger.log("LocationClientImpl : callSearchLocation ", LoggerEnum.INFO);
    Object obj = interServiceCommunication.getResponse(actorRef, request);
    if (obj instanceof Response) {
      Response responseObj = (Response) obj;
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) responseObj.getResult().get(JsonKey.RESPONSE);
      return responseList
          .stream()
          .map(s -> mapper.convertValue(s, Location.class))
          .collect(Collectors.toList());
    } else {
      response = new ArrayList<>();
    }
    return response;
  }

  @Override
  public Location getLocationByCode(ActorRef actorRef, String locationCode) {
    String param = GeoLocationJsonKey.CODE;
    Object value = locationCode;
    List<Location> locationList = getSearchResponse(actorRef, param, value);
    if (CollectionUtils.isNotEmpty(locationList)) {
      return locationList.get(0);
    } else {
      return null;
    }
  }

  @Override
  public String createLocation(ActorRef actorRef, UpsertLocationRequest location) {
    Request request = new Request();
    String locationId = null;
    request.getRequest().putAll(mapper.convertValue(location, Map.class));
    Map<String, Object> resLocation = new HashMap<>();
    request.setOperation(LocationActorOperation.CREATE_LOCATION.getValue());
    ProjectLogger.log("LocationClientImpl : callCreateLocation ", LoggerEnum.INFO);
    Object obj = interServiceCommunication.getResponse(actorRef, request);
    checkLocationResponseForException(obj);
    if (obj instanceof Response) {
      Response response = (Response) obj;
      locationId = (String) response.get(JsonKey.ID);
    }
    return locationId;
  }

  @Override
  public void updateLocation(ActorRef actorRef, UpsertLocationRequest location) {
    Request request = new Request();
    request.getRequest().putAll(mapper.convertValue(location, Map.class));
    request.setOperation(LocationActorOperation.UPDATE_LOCATION.getValue());
    ProjectLogger.log("LocationClientImpl : callUpdateLocation ", LoggerEnum.INFO);
    Object obj = interServiceCommunication.getResponse(actorRef, request);
    checkLocationResponseForException(obj);
  }

  @Override
  public List<String> getRelatedLocationIds(ActorRef actorRef, List<String> codes) {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.LOCATION_CODES, codes);

    Request request = new Request();
    request.setOperation(LocationActorOperation.GET_RELATED_LOCATION_IDS.getValue());
    request.getRequest().putAll(requestMap);

    ProjectLogger.log("LocationClientImpl: getRelatedLocationIds called", LoggerEnum.INFO);
    Object obj = interServiceCommunication.getResponse(actorRef, request);
    checkLocationResponseForException(obj);

    if (obj instanceof Response) {
      Response responseObj = (Response) obj;
      List<String> responseList = (List<String>) responseObj.getResult().get(JsonKey.RESPONSE);
      return responseList;
    }

    return new ArrayList<>();
  }

  private void checkLocationResponseForException(Object obj) {
    if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else if (obj instanceof Exception) {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
