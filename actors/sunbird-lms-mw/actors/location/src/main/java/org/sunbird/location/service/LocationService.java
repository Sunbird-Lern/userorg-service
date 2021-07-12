package org.sunbird.location.service;

import java.util.List;
import java.util.Map;
import org.sunbird.request.RequestContext;

public interface LocationService {
  List<Map<String, String>> getValidatedRelatedLocationIdAndType(
      List<String> codeList, RequestContext context);
}
