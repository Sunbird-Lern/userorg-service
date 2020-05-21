package org.sunbird.feed;

import java.util.List;
import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.dto.SearchDTO;
import org.sunbird.models.user.Feed;

/** @author anmolgupta this is an interface class for the user feeds */
public interface IFeedService {

  /**
   * this method will be responsible to insert the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param feed
   * @return response
   */
  Response insert(Feed feed);

  /**
   * this method will be responsible to update the feed in the user_feed table and sync the data
   * with the ES
   *
   * @param feed
   * @return response
   */
  Response update(Feed feed);

  /**
   * this method will be responsible to get the records by properties from the user_feed table
   *
   * @param properties
   * @return List<Feed>
   */
  List<Feed> getRecordsByProperties(Map<String, Object> properties);

  /**
   * this method will be holding responsibility to get and search the feed.
   *
   * @param searchDTO
   * @return response
   */
  Response search(SearchDTO searchDTO);

  /**
   * this method will be holding responsibility to delete the feed from DB and ES.
   *
   * @param id
   */
  void delete(String id);
}
