package org.sunbird.actor.core;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure an actor with task operations and dispatcher settings.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ActorConfig {
  /**
   * Operation names (tasks) that this actor handles synchronously.
   *
   * @return Array of task operation names.
   */
  String[] tasks();

  /**
   * Operation names (tasks) that this actor handles asynchronously.
   *
   * @return Array of async task operation names.
   */
  String[] asyncTasks();

  /**
   * The name of the dispatcher to use for this actor.
   *
   * @return Dispatcher name, defaults to empty string.
   */
  String dispatcher() default "";
}
