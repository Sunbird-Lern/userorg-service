package org.sunbird.service.feed;

import java.util.List;
import java.util.Map;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

/** @author anmolgupta this is an interface class for the user feeds */
public interface IFeedService {

  /**
   * this method will be responsible to insert the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param request
   * @param context
   * @return response
   */
  Response insert(Request request, RequestContext context);

  /**
   * this method will be responsible to update the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param request
   * @param context
   * @return response
   */
  Response update(Request request, RequestContext context);

  /**
   * this method will be responsible to get the records by userId from the user_feed table
   *
   * @param properties
   * @param context
   * @return List<Feed>
   */
  List<Feed> getFeedsByProperties(Map<String, Object> properties, RequestContext context);

  /**
   * this method will be holding responsibility to delete the feed from DB and ES.
   *
   * @param request
   * @param context
   */
  void delete(Request request, RequestContext context);
}
