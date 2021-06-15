package org.sunbird.actorutil.user;

import akka.actor.ActorRef;
import java.util.Map;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;

public interface UserClient {

  /**
   * Create user.
   *
   * @param actorRef Actor reference
   * @param userMap User details
   * @param context
   * @return User ID
   */
  String createUser(ActorRef actorRef, Map<String, Object> userMap, RequestContext context);

  /**
   * Update user details.
   *
   * @param actorRef Actor reference
   * @param userMap User details
   * @param context
   */
  void updateUser(ActorRef actorRef, Map<String, Object> userMap, RequestContext context);

  /**
   * Assign user roles.
   *
   * @param actorRef Actor reference
   * @param userMap User details
   * @param context
   */
  void assignRolesToUser(ActorRef actorRef, Map<String, Object> userMap, RequestContext context);

  /**
   * Verify phone uniqueness across all users in the system.
   *
   * @param context
   */
  void esVerifyPhoneUniqueness(RequestContext context);

  /**
   * Verify email uniqueness across all users in the system.
   *
   * @param context
   */
  void esVerifyEmailUniqueness(RequestContext context);

  /**
   * Search user details.
   *
   * @param actorRef Actor reference
   * @param req Search req
   * @param context
   */
  Map<String, Object> searchManagedUser(ActorRef actorRef, Request req, RequestContext context);
}
