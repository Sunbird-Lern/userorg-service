package org.sunbird.dao.location.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.location.LocationDao;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;
import scala.concurrent.Future;

/** @author Amit Kumar */
public class LocationDaoImpl implements LocationDao {

  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private final ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String LOCATION_TABLE_NAME = "location";

  @Override
  public Response create(Location location, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(location, Map.class);
    Response response =
        cassandraOperation.insertRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, map, context);
    // need to send ID along with success msg
    response.put(JsonKey.ID, map.get(JsonKey.ID));
    return response;
  }

  @Override
  public Response update(Location location, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(location, Map.class);
    return cassandraOperation.updateRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, map, context);
  }

  @Override
  public Response delete(String locationId, RequestContext context) {
    return cassandraOperation.deleteRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, locationId, context);
  }

  @Override
  public Response search(Map<String, Object> searchQueryMap, RequestContext context) {
    SearchDTO searchDto = ElasticSearchHelper.createSearchDTO(searchQueryMap);
    addSortBy(searchDto);
    String type = ProjectUtil.EsType.location.getTypeName();
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, type, context);
    Map<String, Object> result =
        (Map<String, Object>) ElasticSearchHelper.getResponseFromFuture(resultF);
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
  public Response read(String locationId, RequestContext context) {
    return cassandraOperation.getRecordById(
            KEYSPACE_NAME, LOCATION_TABLE_NAME, locationId, context);
  }

  @Override
  public Response getLocationsByIds(
      List<String> locationIds, List<String> locationFields, RequestContext context) {
    return cassandraOperation.getPropertiesValueById(
            KEYSPACE_NAME, LOCATION_TABLE_NAME, locationIds, locationFields, context);
  }

  @Override
  public Response getRecordByProperty(Map<String, Object> queryMap, RequestContext context) {
    Map<String, Object> searchQueryMap = new HashMap<>();
    searchQueryMap.put(JsonKey.FILTERS, queryMap);
    return search(searchQueryMap, context);
  }

  @Override
  public String saveLocationToEs(String id, Map<String, Object> data, RequestContext context) {
    String type = ProjectUtil.EsType.location.getTypeName();
    Future<String> responseF = esUtil.save(type, id, data, context);
    return (String) ElasticSearchHelper.getResponseFromFuture(responseF);
  }

  public SearchDTO addSortBy(SearchDTO searchDtO) {
    if (MapUtils.isNotEmpty(searchDtO.getAdditionalProperties())
        && searchDtO.getAdditionalProperties().containsKey(JsonKey.FILTERS)
        && searchDtO.getAdditionalProperties().get(JsonKey.FILTERS) instanceof Map
        && ((Map<String, Object>) searchDtO.getAdditionalProperties().get(JsonKey.FILTERS))
            .containsKey(JsonKey.TYPE)) {
      if (MapUtils.isEmpty(searchDtO.getSortBy())) {
        String DEFAULT_SORT_BY = "ASC";
        searchDtO.getSortBy().put(JsonKey.NAME, DEFAULT_SORT_BY);
      }
    }

    return searchDtO;
  }
}
