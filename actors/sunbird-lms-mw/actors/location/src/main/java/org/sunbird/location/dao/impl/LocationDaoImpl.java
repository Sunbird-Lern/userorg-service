package org.sunbird.location.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.MapUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.GeoLocationJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.models.location.Location;
import scala.concurrent.Future;

/** @author Amit Kumar */
public class LocationDaoImpl implements LocationDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = "sunbird";
  private static final String LOCATION_TABLE_NAME = "location";
  private ElasticSearchService esUtil = EsClientFactory.getInstance(JsonKey.REST);
  private static final String DEFAULT_SORT_BY = "ASC";

  @Override
  public Response create(Location location) {
    Map<String, Object> map = mapper.convertValue(location, Map.class);
    Response response = cassandraOperation.insertRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, map);
    // need to send ID along with success msg
    response.put(JsonKey.ID, map.get(JsonKey.ID));
    return response;
  }

  @Override
  public Response update(Location location) {
    Map<String, Object> map = mapper.convertValue(location, Map.class);
    return cassandraOperation.updateRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, map);
  }

  @Override
  public Response delete(String locationId) {
    return cassandraOperation.deleteRecord(KEYSPACE_NAME, LOCATION_TABLE_NAME, locationId);
  }

  @Override
  public Response search(Map<String, Object> searchQueryMap) {
    SearchDTO searchDto = Util.createSearchDto(searchQueryMap);
    addSortBy(searchDto);
    String type = ProjectUtil.EsType.location.getTypeName();
    Future<Map<String, Object>> resultF = esUtil.search(searchDto, type);
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
  public Response read(String locationId) {
    return cassandraOperation.getRecordById(KEYSPACE_NAME, LOCATION_TABLE_NAME, locationId);
  }

  @Override
  public Response getRecordByProperty(Map<String, Object> queryMap) {
    return cassandraOperation.getRecordsByProperty(
        KEYSPACE_NAME,
        LOCATION_TABLE_NAME,
        (String) queryMap.get(GeoLocationJsonKey.PROPERTY_NAME),
        queryMap.get(GeoLocationJsonKey.PROPERTY_VALUE));
  }

  public SearchDTO addSortBy(SearchDTO searchDtO) {
    if (MapUtils.isNotEmpty(searchDtO.getAdditionalProperties())
        && searchDtO.getAdditionalProperties().containsKey(JsonKey.FILTERS)
        && searchDtO.getAdditionalProperties().get(JsonKey.FILTERS) instanceof Map
        && ((Map<String, Object>) searchDtO.getAdditionalProperties().get(JsonKey.FILTERS))
            .containsKey(JsonKey.TYPE)) {
      if (MapUtils.isEmpty(searchDtO.getSortBy())) {
        ProjectLogger.log(
            "LocationDaoImpl:search:addSortBy added sort type name attribute.",
            LoggerEnum.INFO.name());
        searchDtO.getSortBy().put(JsonKey.NAME, DEFAULT_SORT_BY);
      }
    }

    return searchDtO;
  }
}
