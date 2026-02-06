package org.sunbird.actor.core;

/**
 * RouterMode defines the possible modes of the actor router.
 * <ul>
 *   <li>OFF: Routing is disabled.</li>
 *   <li>LOCAL: Routing happens within the local actor system.</li>
 *   <li>REMOTE: Routing happens across different actor systems.</li>
 * </ul>
 */
public enum RouterMode {
  OFF,
  LOCAL,
  REMOTE;
}
