package org.sunbird.actor.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ActorConfig annotation is used to configure the operations and dispatcher for a {@link
 * org.sunbird.actor.core.BaseActor}. It defines which tasks (operations) are handled by the actor synchronously
 * or asynchronously.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ActorConfig {
  /**
   * The synchronous operations handled by this actor.
   *
   * @return Array of operation names.
   */
  String[] tasks();

  /**
   * The asynchronous (background) operations handled by this actor.
   *
   * @return Array of asynchronous operation names.
   */
  String[] asyncTasks();

  /**
   * The dispatcher name to be used for this actor. If not specified, a default dispatcher is used.
   *
   * @return The dispatcher name.
   */
  String dispatcher() default "";
}
