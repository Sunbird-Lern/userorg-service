package org.sunbird.actorutil.user.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.sunbird.actorutil.user.UserClient;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class UserClientImpl implements UserClient {

  private static LoggerUtil logger = new LoggerUtil(UserClientImpl.class);

  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  public static UserClient userClient = null;

  public static UserClient getInstance() {
    if (userClient == null) {
      synchronized (UserClientImpl.class) {
        if (userClient == null) {
          userClient = new UserClientImpl();
        }
      }
    }
    return userClient;
  }

  @Override
  public String createUser(ActorRef actorRef, Map<String, Object> userMap, RequestContext context) {
    logger.info(context, "createUser called");
    return upsertUser(actorRef, userMap, ActorOperations.CREATE_USER.getValue(), context);
  }

  @Override
  public void updateUser(ActorRef actorRef, Map<String, Object> userMap, RequestContext context) {
    logger.info(context, "updateUser called");
    upsertUser(actorRef, userMap, ActorOperations.UPDATE_USER.getValue(), context);
  }

  @Override
  public void assignRolesToUser(
      ActorRef actorRef, Map<String, Object> userMap, RequestContext context) {
    logger.info(context, "updateUser called");
    upsertUser(actorRef, userMap, ActorOperations.ASSIGN_ROLES.getValue(), context);
  }

  @Override
  public void esVerifyPhoneUniqueness(RequestContext context) {
    esVerifyFieldUniqueness(JsonKey.ENC_PHONE, JsonKey.PHONE, context);
  }

  @Override
  public void esVerifyEmailUniqueness(RequestContext context) {
    esVerifyFieldUniqueness(JsonKey.ENC_EMAIL, JsonKey.EMAIL, context);
  }

  private void esVerifyFieldUniqueness(
      String facetsKey, String objectType, RequestContext context) {
    SearchDTO searchDto = null;
    searchDto = new SearchDTO();
    searchDto.setLimit(0);

    Map<String, String> facets = new HashMap<>();
    facets.put(facetsKey, null);
    List<Map<String, String>> list = new ArrayList<>();
    list.add(facets);
    searchDto.setFacets(list);

    Future<Map<String, Object>> esResponseF =
        esUtil.search(searchDto, ProjectUtil.EsType.user.getTypeName(), context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);

    if (null != esResponse) {
      List<Map<String, Object>> facetsResponse =
          (List<Map<String, Object>>) esResponse.get(JsonKey.FACETS);

      if (CollectionUtils.isNotEmpty(facetsResponse)) {
        Map<String, Object> map = facetsResponse.get(0);
        List<Map<String, Object>> valueList = (List<Map<String, Object>>) map.get("values");

        for (Map<String, Object> value : valueList) {
          long count = (long) value.get(JsonKey.COUNT);
          if (count > 1) {
            throw new ProjectCommonException(
                ResponseCode.errorDuplicateEntries.getErrorCode(),
                MessageFormat.format(
                    ResponseCode.errorDuplicateEntries.getErrorMessage(), objectType),
                ResponseCode.CLIENT_ERROR.getResponseCode());
          }
        }
      }
    }
  }

  private String upsertUser(
      ActorRef actorRef, Map<String, Object> userMap, String operation, RequestContext context) {
    String userId = null;
    Object obj = null;

    Request request = new Request();
    request.setRequest(userMap);
    request.setRequestContext(context);
    request.setOperation(operation);
    request.getContext().put(JsonKey.VERSION, JsonKey.VERSION_2);
    request.getContext().put(JsonKey.CALLER_ID, JsonKey.BULK_USER_UPLOAD);
    request.getContext().put(JsonKey.ROOT_ORG_ID, userMap.get(JsonKey.ROOT_ORG_ID));
    userMap.remove(JsonKey.ROOT_ORG_ID);

    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context, "upsertUser: Exception occurred with error message = " + e.getMessage(), e);
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    if (obj instanceof Response) {
      Response response = (Response) obj;
      userId = (String) response.get(JsonKey.USER_ID);
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else if (obj instanceof Exception) {
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }

    return userId;
  }

  /**
   * Get managed user list for LUA uuid (JsonKey.ID)
   *
   * @param actorRef
   * @param req
   * @param context
   * @return Map<String, Object>
   */
  public Map<String, Object> searchManagedUser(
      ActorRef actorRef, Request req, RequestContext context) {
    logger.debug(context, "UserServiceImpl: searchManagedUser called");

    Map<String, Object> searchRequestMap = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.MANAGED_BY, (String) req.get(JsonKey.ID));
    List<String> objectType = new ArrayList<String>();
    objectType.add("user");
    filters.put(JsonKey.OBJECT_TYPE, objectType);
    searchRequestMap.put(JsonKey.FILTERS, filters);

    String sortByField = (String) req.get(JsonKey.SORTBY);
    if (StringUtils.isNotEmpty(sortByField)) {
      String order = (String) req.get(JsonKey.ORDER);
      Map<String, Object> sortby = new HashMap<>();
      sortby.put(sortByField, StringUtils.isEmpty(order) ? "asc" : order);
      searchRequestMap.put(JsonKey.SORT_BY, sortby);
    }

    Request request = new Request();
    request.setRequestContext(context);
    request.getRequest().putAll(searchRequestMap);
    request.setOperation(ActorOperations.USER_SEARCH.getValue());

    Object obj = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      obj = Await.result(Patterns.ask(actorRef, request, t), t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context,
          "searchManagedUser: Exception occurred with error message = " + e.getMessage(),
          e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    if (obj instanceof Response) {
      Response responseObj = (Response) obj;
      return (Map<String, Object>) responseObj.getResult().get(JsonKey.RESPONSE);
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
  }
}
