package org.sunbird.location.service;

import org.sunbird.common.request.RequestContext;

import java.util.List;
import java.util.Map;

public interface LocationService {
    List<Map<String, String>> getValidatedRelatedLocationIdAndType(List<String> codeList, RequestContext context);
}
