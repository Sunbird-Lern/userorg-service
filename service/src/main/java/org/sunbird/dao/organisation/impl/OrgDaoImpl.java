package org.sunbird.dao.organisation.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.organisation.OrgDao;
import org.sunbird.dto.SearchDTO;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.Util;
import scala.concurrent.Future;

public class OrgDaoImpl implements OrgDao {
  private final ElasticSearchService esService = EsClientFactory.getInstance(JsonKey.REST);
  private final LoggerUtil logger = new LoggerUtil(OrgDaoImpl.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private static OrgDao orgDao;
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String ORG_TABLE_NAME = JsonKey.ORGANISATION;

  public static OrgDao getInstance() {
    if (orgDao == null) {
      orgDao = new OrgDaoImpl();
    }
    return orgDao;
  }

  @Override
  public Map<String, Object> getOrgById(String orgId, RequestContext context) {
    if (StringUtils.isNotBlank(orgId)) {
      Response response =
          cassandraOperation.getRecordById(KEYSPACE_NAME, ORG_TABLE_NAME, orgId, context);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        Map<String, Object> orgMap = responseList.get(0);
        enrichOrgDetails(orgMap, context);
        return orgMap;
      }
    }
    return Collections.emptyMap();
  }

  @Override
  public List<Map<String, Object>> getOrgByIds(List<String> orgIds, RequestContext context) {
    return getOrgByIds(orgIds, Collections.emptyList(), context);
  }

  @Override
  public List<Map<String, Object>> getOrgByIds(
      List<String> orgIds, List<String> fields, RequestContext context) {
    if (CollectionUtils.isNotEmpty(orgIds)) {
      Response response =
          cassandraOperation.getPropertiesValueById(
                  KEYSPACE_NAME, ORG_TABLE_NAME, orgIds, fields, context);
      List<Map<String, Object>> responseList =
          (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
      if (CollectionUtils.isNotEmpty(responseList)) {
        responseList.stream().forEach(orgMap -> enrichOrgDetails(orgMap, context));
        return responseList;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public String saveOrgToEs(String id, Map<String, Object> data, RequestContext context) {
    String type = ProjectUtil.EsType.organisation.getTypeName();
    Future<String> responseF = esService.save(type, id, data, context);
    return (String) ElasticSearchHelper.getResponseFromFuture(responseF);
  }

  private void enrichOrgDetails(Map<String, Object> orgMap, RequestContext context) {
    String orgLocation = (String) orgMap.get(JsonKey.ORG_LOCATION);
    List orgLocationList = new ArrayList<>();
    if (StringUtils.isNotBlank(orgLocation)) {
      try {
        orgLocationList = mapper.readValue(orgLocation, List.class);
      } catch (Exception e) {
        logger.info(
            context,
            "Exception occurred while converting orgLocation to List<Map<String,String>>.");
      }
    }
    orgMap.put(JsonKey.ORG_LOCATION, orgLocationList);
    orgMap.put(JsonKey.HASHTAGID, orgMap.get(JsonKey.ID));
    orgMap.remove(JsonKey.CONTACT_DETAILS);
    orgMap.putAll(Util.getOrgDefaultValue());
  }

  @Override
  public Response create(Map<String, Object> orgMap, RequestContext context) {
    try {
      List<Map<String, Object>> orgLocation = (List) orgMap.get(JsonKey.ORG_LOCATION);
      if (CollectionUtils.isNotEmpty(orgLocation)) {
        String orgLoc = mapper.writeValueAsString(orgLocation);
        orgMap.put(JsonKey.ORG_LOCATION, orgLoc);
      }
    } catch (JsonProcessingException e) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return cassandraOperation.insertRecord(KEYSPACE_NAME, ORG_TABLE_NAME, orgMap, context);
  }

  @Override
  public Response update(Map<String, Object> orgMap, RequestContext context) {
    try {
      List<Map<String, Object>> orgLocation = (List) orgMap.get(JsonKey.ORG_LOCATION);
      if (CollectionUtils.isNotEmpty(orgLocation)) {
        String orgLoc = mapper.writeValueAsString(orgLocation);
        orgMap.put(JsonKey.ORG_LOCATION, orgLoc);
      }
    } catch (JsonProcessingException e) {
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return cassandraOperation.updateRecord(KEYSPACE_NAME, ORG_TABLE_NAME, orgMap, context);
  }

  @Override
  public Response search(Map<String, Object> searchQueryMap, RequestContext context) {
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(search(searchDto, context));
    Response response = new Response();
    if (result != null) {
      response.put(JsonKey.COUNT, result.get(JsonKey.COUNT));
      response.put(JsonKey.RESPONSE, result.get(JsonKey.CONTENT));
    } else {
      List<Map<String, Object>> list = new ArrayList<>();
      response.put(JsonKey.COUNT, list.size());
      response.put(JsonKey.RESPONSE, list);
    }
    return response;
  }

  @Override
  public Future<Map<String, Object>> search(SearchDTO searchDTO, RequestContext context) {
    String type = ProjectUtil.EsType.organisation.getTypeName();
    return esService.search(searchDTO, type, context);
  }
}
