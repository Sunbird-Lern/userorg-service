package org.sunbird.dao.feed;

import java.util.Map;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

public interface IFeedDao {

  Response insert(Map<String, Object> feedMap, RequestContext context);

  Response update(Map<String, Object> feedMap, RequestContext context);

  Response getFeedsByProperties(Map<String, Object> properties, RequestContext context);

  void delete(String id, String userId, String category, RequestContext context);
}
