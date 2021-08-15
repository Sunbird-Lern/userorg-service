package org.sunbird.service.location;

import org.sunbird.model.location.Location;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.List;
import java.util.Map;

public interface LocationService {
    Response createLocation(Location location, RequestContext context);
    Response updateLocation(Location location, RequestContext context);
    Response deleteLocation(String locationId, RequestContext context);
    Response searchLocation(Map<String, Object> searchQueryMap, RequestContext context);
    List<Map<String, String>> getValidatedRelatedLocationIdAndType(
            List<String> codeList, RequestContext context);
    public List<String> getValidatedRelatedLocationIds(
            List<String> codeList, RequestContext context);
}
