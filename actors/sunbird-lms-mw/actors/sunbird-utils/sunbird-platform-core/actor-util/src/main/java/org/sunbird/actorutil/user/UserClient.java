package org.sunbird.actorutil.user;

import akka.actor.ActorRef;
import java.util.Map;

public interface UserClient {

  /**
   * Create user.
   *
   * @param actorRef Actor reference
   * @param userMap User details
   * @return User ID
   */
  String createUser(ActorRef actorRef, Map<String, Object> userMap);

  /**
   * Update user details.
   *
   * @param actorRef Actor reference
   * @param userMap User details
   */
  void updateUser(ActorRef actorRef, Map<String, Object> userMap);

  /** Verify phone uniqueness across all users in the system. */
  void esVerifyPhoneUniqueness();

  /** Verify email uniqueness across all users in the system. */
  void esVerifyEmailUniqueness();
}
