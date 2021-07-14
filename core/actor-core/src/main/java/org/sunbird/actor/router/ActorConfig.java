package org.sunbird.actor.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** @author Mahesh Kumar Gangula */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ActorConfig {
  String[] tasks();

  String[] asyncTasks();

  String dispatcher() default "";
}
