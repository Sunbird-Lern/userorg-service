package org.sunbird.actorutil.org.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actorutil.org.OrganisationClient;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class OrganisationClientImpl implements OrganisationClient {

  private static LoggerUtil logger = new LoggerUtil(OrganisationClientImpl.class);
  public static OrganisationClient organisationClient = null;

  public static OrganisationClient getInstance() {
    if (organisationClient == null) {
      synchronized (OrganisationClientImpl.class) {
        if (organisationClient == null) {
          organisationClient = new OrganisationClientImpl();
        }
      }
    }
    return organisationClient;
  }

  ObjectMapper objectMapper = new ObjectMapper();
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);

  @Override
  public String createOrg(ActorRef actorRef, Map<String, Object> orgMap, RequestContext context) {
    logger.info(context, "createOrg called");
    return upsertOrg(actorRef, orgMap, ActorOperations.CREATE_ORG.getValue(), context);
  }

  @Override
  public void updateOrg(ActorRef actorRef, Map<String, Object> orgMap, RequestContext context) {
    logger.info(context, "updateOrg called");
    upsertOrg(actorRef, orgMap, ActorOperations.UPDATE_ORG.getValue(), context);
  }

  private String upsertOrg(
      ActorRef actorRef, Map<String, Object> orgMap, String operation, RequestContext context) {
    String orgId = null;
    Object obj = null;

    Request request = new Request();
    request.setRequestContext(context);
    request.setRequest(orgMap);
    request.setOperation(operation);
    request.getContext().put(JsonKey.CALLER_ID, JsonKey.BULK_ORG_UPLOAD);
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context, "upsertOrg: Exception occurred with error message = " + e.getMessage(), e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    if (obj instanceof Response) {
      Response response = (Response) obj;
      orgId = (String) response.get(JsonKey.ORGANISATION_ID);
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else if (obj instanceof Exception) {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return orgId;
  }

  @Override
  public Organisation getOrgById(ActorRef actorRef, String orgId, RequestContext context) {
    logger.info(context, "getOrgById called");
    Organisation organisation = null;

    Request request = new Request();
    request.setRequestContext(context);
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.ORGANISATION_ID, orgId);
    request.setRequest(requestMap);
    request.setOperation(ActorOperations.GET_ORG_DETAILS.getValue());
    Object obj = null;
    try {
      Timeout t = new Timeout(Duration.create(10, TimeUnit.SECONDS));
      Future<Object> future = Patterns.ask(actorRef, request, t);
      obj = Await.result(future, t.duration());
    } catch (ProjectCommonException pce) {
      throw pce;
    } catch (Exception e) {
      logger.error(
          context, "getOrgById: Exception occurred with error message = " + e.getMessage(), e);
      ProjectCommonException.throwServerErrorException(
          ResponseCode.unableToCommunicateWithActor,
          ResponseCode.unableToCommunicateWithActor.getErrorMessage());
    }
    if (obj instanceof Response) {
      ObjectMapper objectMapper = new ObjectMapper();
      Response response = (Response) obj;

      // Convert contact details (received from ES) format from map to
      // JSON string (as in Cassandra contact details are stored as text)
      Map<String, Object> map = (Map) response.get(JsonKey.RESPONSE);
      organisation = objectMapper.convertValue(map, Organisation.class);
    } else if (obj instanceof ProjectCommonException) {
      throw (ProjectCommonException) obj;
    } else if (obj instanceof Exception) {
      throw new ProjectCommonException(
          ResponseCode.SERVER_ERROR.getErrorCode(),
          ResponseCode.SERVER_ERROR.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return organisation;
  }

  @Override
  public Organisation esGetOrgByExternalId(
      String externalId, String provider, RequestContext context) {
    Organisation organisation = null;
    Map<String, Object> map = null;
    SearchDTO searchDto = new SearchDTO();
    Map<String, Object> filter = new HashMap<>();
    filter.put(JsonKey.EXTERNAL_ID, externalId);
    if (StringUtils.isNotEmpty(provider)) {
      filter.put(JsonKey.PROVIDER, provider);
    }
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    Future<Map<String, Object>> esResponseF =
        esUtil.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> esResponse =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(esResponseF);
    List<Map<String, Object>> list = (List<Map<String, Object>>) esResponse.get(JsonKey.CONTENT);
    if (!list.isEmpty()) {
      map = list.get(0);
      map.put(JsonKey.CONTACT_DETAILS, String.valueOf(map.get(JsonKey.CONTACT_DETAILS)));
      organisation = objectMapper.convertValue(map, Organisation.class);
    }
    return organisation;
  }

  @Override
  public Organisation esGetOrgById(String id, RequestContext context) {
    Map<String, Object> map = null;
    Future<Map<String, Object>> mapF =
        esUtil.getDataByIdentifier(ProjectUtil.EsType.organisation.getTypeName(), id, context);

    map = (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(mapF);
    if (MapUtils.isEmpty(map)) {
      return null;
    } else {
      return objectMapper.convertValue(map, Organisation.class);
    }
  }

  @Override
  public List<Organisation> esSearchOrgByFilter(
      Map<String, Object> filter, RequestContext context) {
    SearchDTO searchDto = new SearchDTO();
    searchDto.getAdditionalProperties().put(JsonKey.FILTERS, filter);
    return searchOrganisation(searchDto, context);
  }

  @SuppressWarnings("unchecked")
  private List<Organisation> searchOrganisation(SearchDTO searchDto, RequestContext context) {
    List<Organisation> orgList = new ArrayList<>();
    logger.info(context, "search org.");
    Future<Map<String, Object>> resultF =
        esUtil.search(searchDto, ProjectUtil.EsType.organisation.getTypeName(), context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);

    List<Map<String, Object>> orgMapList = (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
    if (CollectionUtils.isNotEmpty(orgMapList)) {
      for (Map<String, Object> orgMap : orgMapList) {
        orgList.add(objectMapper.convertValue(orgMap, Organisation.class));
      }
      return orgList;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<Organisation> esSearchOrgByIds(
      List<String> orgIds, List<String> outputColumns, RequestContext context) {
    SearchDTO searchDTO = new SearchDTO();

    searchDTO.setFields(outputColumns);

    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.ID, orgIds);

    searchDTO.getAdditionalProperties().put(JsonKey.FILTERS, filters);

    return searchOrganisation(searchDTO, context);
  }
}
