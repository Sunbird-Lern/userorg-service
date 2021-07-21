package org.sunbird.actorutil.location;

import akka.actor.ActorRef;
import java.util.List;
import org.sunbird.models.location.Location;
import org.sunbird.models.location.apirequest.UpsertLocationRequest;
import org.sunbird.request.RequestContext;

/**
 * This interface defines methods supported by Location service.
 *
 * @author Amit Kumar
 */
public interface LocationClient {

  /**
   * @desc This method will fetch location details by list of code.
   * @param actorRef Actor reference.
   * @param codeList List of location code.
   * @param context
   * @return List of location.
   */
  List<Location> getLocationsByCodes(
      ActorRef actorRef, List<String> codeList, RequestContext context);

  public List<Location> getLocationByIds(
      ActorRef actorRef, List<String> idsList, RequestContext context);
  /**
   * @desc This method will fetch location details by id.
   * @param actorRef Actor reference.
   * @param id Location id.
   * @param context
   * @return Location details.
   */
  Location getLocationById(ActorRef actorRef, String id, RequestContext context);

  /**
   * @desc This method will fetch location details by code.
   * @param actorRef Actor reference.
   * @param locationCode location code.
   * @param context
   * @return Location details.
   */
  Location getLocationByCode(ActorRef actorRef, String locationCode, RequestContext context);

  List<Location> getLocationByCodes(
      ActorRef actorRef, List<String> locationCode, RequestContext context);

  /**
   * @desc This method will create Location and returns the response.
   * @param actorRef Actor reference.
   * @param location Location details.
   * @param context
   * @return Location id.
   */
  String createLocation(ActorRef actorRef, UpsertLocationRequest location, RequestContext context);

  /**
   * @desc This method will update location details.
   * @param actorRef Actor reference.
   * @param location Location details.
   * @param context
   */
  void updateLocation(ActorRef actorRef, UpsertLocationRequest location, RequestContext context);

  /**
   * @desc For given location codes, fetch location IDs (including, if any, those of its parent or
   *     ancestor(s) locations).
   * @param actorRef Actor reference.
   * @param codes List of location codes.
   * @param context
   * @return List of related location IDs
   */
  List<String> getRelatedLocationIds(ActorRef actorRef, List<String> codes, RequestContext context);
}
