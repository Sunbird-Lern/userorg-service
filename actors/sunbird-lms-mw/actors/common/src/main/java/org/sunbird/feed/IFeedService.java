package org.sunbird.feed;

import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.dto.SearchDTO;
import org.sunbird.models.user.Feed;

/** @author anmolgupta this is an interface class for the user feeds */
public interface IFeedService {

  /**
   * this method will be responsible to insert the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param feed
   * @param context
   * @return response
   */
  Response insert(Feed feed, RequestContext context);

  /**
   * this method will be responsible to update the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param feed
   * @param context
   * @return response
   */
  Response update(Feed feed, RequestContext context);

  /**
   * this method will be responsible to get the records by properties from the user_feed table
   *
   * @param properties
   * @param context
   * @return List<Feed>
   */
  List<Feed> getRecordsByProperties(Map<String, Object> properties, RequestContext context);

  /**
   * this method will be holding responsibility to get and search the feed.
   *
   * @param searchDTO
   * @param context
   * @return response
   */
  Response search(SearchDTO searchDTO, RequestContext context);

  /**
   * this method will be holding responsibility to delete the feed from DB and ES.
   *
   * @param id
   * @param context
   */
  void delete(String id, RequestContext context);
}
