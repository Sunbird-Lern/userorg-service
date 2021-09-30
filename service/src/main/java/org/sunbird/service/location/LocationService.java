package org.sunbird.service.location;

import java.util.List;
import java.util.Map;
import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface LocationService {
  Response createLocation(Location location, RequestContext context);

  Response updateLocation(Location location, RequestContext context);

  Response deleteLocation(String locationId, RequestContext context);

  Response searchLocation(Map<String, Object> searchQueryMap, RequestContext context);

  List<Map<String, String>> getValidatedRelatedLocationIdAndType(
      List<String> codeList, RequestContext context);

  List<String> getValidatedRelatedLocationIds(List<String> codeList, RequestContext context);

  List<Location> locationSearch(String param, Object value, RequestContext context);

  Location getLocationById(String locationId, RequestContext context);

  List<Map<String, Object>> getLocationsByIds(
      List<String> locationIds, List<String> locationFields, RequestContext context);

  String saveLocationToEs(String id, Map<String, Object> data, RequestContext context);
}
