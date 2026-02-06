package org.sunbird.actor.core;

/**
 * RouterException is a custom runtime exception used by the actor routing system when an
 * invalid invocation or routing failure occurs.
 */
public class RouterException extends RuntimeException {

  private static final long serialVersionUID = 7669891026222754334L;

  /**
   * Constructs a new RouterException with the specified detail message.
   *
   * @param message The detail message.
   */
  public RouterException(String message) {
    super(message);
  }
}
