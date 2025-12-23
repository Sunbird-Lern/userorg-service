package modules;

import org.apache.pekko.routing.FromConfig;
import org.apache.pekko.routing.RouterConfig;
import com.google.inject.AbstractModule;
import org.sunbird.logging.LoggerUtil;
import play.libs.pekko.PekkoGuiceSupport;
import util.ACTORS;

public class ActorStartModule extends AbstractModule implements PekkoGuiceSupport {
  private static LoggerUtil logger = new LoggerUtil(ActorStartModule.class);

  @Override
  protected void configure() {
    logger.debug("binding actors for dependency injection");
    final RouterConfig config = new FromConfig();
    for (ACTORS actor : ACTORS.values()) {
      bindActor(actor.getActorClass(), actor.getActorName(), props -> props.withRouter(config));
    }
    logger.debug("binding completed");
  }
}
