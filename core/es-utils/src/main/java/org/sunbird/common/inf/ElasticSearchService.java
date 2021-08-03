package org.sunbird.common.inf;

import java.util.List;
import java.util.Map;
import org.sunbird.dto.SearchDTO;
import org.sunbird.request.RequestContext;
import scala.concurrent.Future;

public interface ElasticSearchService {
  public static final String _DOC = "_doc";

  /**
   * This method will put a new data entry inside Elastic search. identifier value becomes _id
   * inside ES, so every time provide a unique value while saving it.
   *
   * @param index String ES index name
   * @param identifier ES column identifier as an String
   * @param data Map<String,Object>
   * @param context
   * @return String identifier for created data
   */
  public Future<String> save(
      String index, String identifier, Map<String, Object> data, RequestContext context);

  /**
   * This method will update data based on identifier.take the data based on identifier and merge
   * with incoming data then update it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @param context
   * @return boolean
   */
  public Future<Boolean> update(
      String index, String identifier, Map<String, Object> data, RequestContext context);

  /**
   * This method will provide data form ES based on incoming identifier. we can get data by passing
   * index and identifier values , or all the three index, identifier and type
   *
   * @param index String
   * @param identifier String
   * @param context
   * @return Map<String,Object> or null
   */
  public Future<Map<String, Object>> getDataByIdentifier(
      String index, String identifier, RequestContext context);

  /**
   * This method will remove data from ES based on identifier.
   *
   * @param index String
   * @param identifier String
   * @param context
   */
  public Future<Boolean> delete(String index, String identifier, RequestContext context);

  /**
   * Method to perform the elastic search on the basis of SearchDTO . SearchDTO contains the search
   * criteria like fields, facets, sort by , filters etc. here user can pass single type to search
   * or multiple type or null
   *
   * @param context
   * @return search result as Map.
   */
  public Future<Map<String, Object>> search(
      SearchDTO searchDTO, String index, RequestContext context);

  /**
   * This method will do the health check of elastic search.
   *
   * @return boolean
   */
  public Future<Boolean> healthCheck();

  /**
   * This method will do the bulk data insertion.
   *
   * @param index String index name
   * @param dataList List<Map<String, Object>>
   * @param context
   * @return boolean
   */
  public Future<Boolean> bulkInsert(
      String index, List<Map<String, Object>> dataList, RequestContext context);

  /**
   * This method will upsert data based on identifier.take the data based on identifier and merge
   * with incoming data then update it or if not present already will create it.
   *
   * @param index String
   * @param identifier String
   * @param data Map<String,Object>
   * @param context
   * @return boolean
   */
  public Future<Boolean> upsert(
      String index, String identifier, Map<String, Object> data, RequestContext context);

  /**
   * @param ids List of ids of document
   * @param fields List of fields which needs to captured
   * @param index elastic search index in which search should be done
   * @param context
   * @return Map<String,Map<String,Object>> It will return a map with id as key and the data from ES
   *     as value
   */
  public Future<Map<String, Map<String, Object>>> getEsResultByListOfIds(
      List<String> ids, List<String> fields, String index, RequestContext context);
}
