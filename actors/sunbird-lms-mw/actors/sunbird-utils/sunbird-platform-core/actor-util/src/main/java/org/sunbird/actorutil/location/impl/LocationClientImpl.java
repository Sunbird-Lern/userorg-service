package org.sunbird.actorutil.location.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.actorutil.location.LocationClient;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class LocationClientImpl implements LocationClient {
  private static LoggerUtil logger = new LoggerUtil(LocationClientImpl.class);
  private ObjectMapper mapper = new ObjectMapper();
  public static LocationClient locationClient = null;

  public static synchronized LocationClient getInstance() {
    if (locationClient == null) locationClient = new LocationClientImpl();
    return locationClient;
  }

  @Override
  public List<Location> getLocationsByCodes(
      ActorRef actorRef, List<String> codeList, RequestContext context) {
    return getSearchResponse(actorRef, GeoLocationJsonKey.CODE, codeList, context);
  }

  @Override
  public List<Location> getLocationByIds(
      ActorRef actorRef, List<String> idsList, RequestContext context) {
    return getSearchResponse(actorRef, GeoLocationJsonKey.ID, idsList, context);
  }

  @Override
  public Location getLocationById(ActorRef actorRef, String id, RequestContext context) {
    List<Location> locationList = getSearchResponse(actorRef, JsonKey.ID, id, context);
    if (CollectionUtils.isNotEmpty(locationList)) {
      return locationList.get(0);
    } else {
      return null;
    }
  }

  private List<Location> getSearchResponse(
      ActorRef actorRef, String param, Object value, RequestContext context) {
    List<Location> response = null;
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> searchRequestMap = new HashMap<>();
    filters.put(param, value);
    searchRequestMap.put(JsonKey.FILTERS, filters);
    Request request = new Request();
    request.setOperation(LocationActorOperation.SEARCH_LOCATION.getValue());
    request.getRequest().putAll(searchRequestMap);
    logger.info(context, "callSearchLocation ");
    try {
      Object obj = actorCall(actorRef, request, context);
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
    } catch (Exception ex) {
      response = new ArrayList<>();
    }
    return response;
  }

  @Override
  public Location getLocationByCode(
      ActorRef actorRef, String locationCode, RequestContext context) {
    String param = GeoLocationJsonKey.CODE;
    Object value = locationCode;
    List<Location> locationList = getSearchResponse(actorRef, param, value, context);
    if (CollectionUtils.isNotEmpty(locationList)) {
      return locationList.get(0);
    } else {
      return null;
    }
  }

  @Override
  public List<Location> getLocationByCodes(
      ActorRef actorRef, List<String> locationCode, RequestContext context) {
    String param = GeoLocationJsonKey.CODE;
    Object value = locationCode;
    List<Location> locationList = getSearchResponse(actorRef, param, value, context);
    if (CollectionUtils.isNotEmpty(locationList)) {
      return locationList;
    } else {
      return null;
    }
  }

  @Override
  public String createLocation(
      ActorRef actorRef, UpsertLocationRequest location, RequestContext context) {
    Request request = new Request();
    String locationId = null;
    request.getRequest().putAll(mapper.convertValue(location, Map.class));
    Map<String, Object> resLocation = new HashMap<>();
    request.setOperation(LocationActorOperation.CREATE_LOCATION.getValue());
    logger.info(context, "callCreateLocation ");
    Object obj = actorCall(actorRef, request, context);
    if (obj instanceof Response) {
      Response response = (Response) obj;
      locationId = (String) response.get(JsonKey.ID);
    }
    return locationId;
  }

  @Override
  public void updateLocation(
      ActorRef actorRef, UpsertLocationRequest location, RequestContext context) {
    Request request = new Request();
    request.getRequest().putAll(mapper.convertValue(location, Map.class));
    request.setOperation(LocationActorOperation.UPDATE_LOCATION.getValue());
    logger.info(context, "callUpdateLocation ");
    Object obj = actorCall(actorRef, request, context);
  }

  @Override
  public List<String> getRelatedLocationIds(
      ActorRef actorRef, List<String> codes, RequestContext context) {
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.LOCATION_CODES, codes);

    Request request = new Request();
    request.setOperation(LocationActorOperation.GET_RELATED_LOCATION_IDS.getValue());
    request.getRequest().putAll(requestMap);

    logger.info(context, "getRelatedLocationIds called");
    Object obj = actorCall(actorRef, request, context);

    if (obj instanceof Response) {
      Response responseObj = (Response) obj;
      List<String> responseList = (List<String>) responseObj.getResult().get(JsonKey.RESPONSE);
      return responseList;
    }

    return new ArrayList<>();
  }

  private Object actorCall(ActorRef actorRef, Request request, RequestContext context) {
    Object obj = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context,
          "Unable to communicate with actor: Exception occurred with error message = "
              + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    checkLocationResponseForException(obj);
    return obj;
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
