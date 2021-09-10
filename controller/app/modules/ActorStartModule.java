package modules;

import akka.routing.FromConfig;
import akka.routing.RouterConfig;
import com.google.inject.AbstractModule;
import org.sunbird.logging.LoggerUtil;
import play.libs.akka.AkkaGuiceSupport;
import util.ACTORS;

public class ActorStartModule extends AbstractModule implements AkkaGuiceSupport {
  private static LoggerUtil logger = new LoggerUtil(ActorStartModule.class);

  @Override
  protected void configure() {
    logger.info("binding actors for dependency injection");
    final RouterConfig config = new FromConfig();
    for (ACTORS actor : ACTORS.values()) {
      bindActor(actor.getActorClass(), actor.getActorName(), (props) -> props.withRouter(config));
    }
    logger.info("binding completed");
  }
}
