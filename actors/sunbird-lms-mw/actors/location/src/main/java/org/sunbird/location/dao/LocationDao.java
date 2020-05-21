package org.sunbird.location.dao;

import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.models.location.Location;

/** @author Amit Kumar */
public interface LocationDao {
  /**
   * @param location Location Details
   * @return response Response
   */
  Response create(Location location);

  /**
   * @param location Location Details
   * @return response Response
   */
  Response update(Location location);

  /**
   * @param locationId its a unique identity for Location
   * @return response Response
   */
  Response delete(String locationId);

  /**
   * @param searchQueryMap Map<String,Object> it contains the filters to search Location from ES
   * @return response Response
   */
  Response search(Map<String, Object> searchQueryMap);

  /**
   * @param locationId
   * @return response Response
   */
  Response read(String locationId);

  /**
   * @param queryMap
   * @return response Response
   */
  Response getRecordByProperty(Map<String, Object> queryMap);
}
