package org.sunbird.service.location;

import org.sunbird.models.location.Location;
import org.sunbird.request.RequestContext;

import java.util.List;
import java.util.Map;

public interface LocationService {
    List<Map<String, String>> getValidatedRelatedLocationIdAndType(
            List<String> codeList, RequestContext context);

    List<Location> locationSearch(String param, Object value, RequestContext context);
    Location getLocationById(String param, Object value, RequestContext context);
}
