package modules;

import org.apache.pekko.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.util.ProjectUtil;
import play.api.Application;
import play.api.Play;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import sun.misc.Signal;

@Singleton
public class SignalHandler {
  private static LoggerUtil logger = new LoggerUtil(SignalHandler.class);

  private static long stopDelay = Long.parseLong(ProjectUtil.getConfigValue("sigterm_stop_delay"));
  private static final FiniteDuration STOP_DELAY = Duration.create(stopDelay, TimeUnit.SECONDS);

  private volatile boolean isShuttingDown = false;

  @Inject
  public SignalHandler(ActorSystem actorSystem, Provider<Application> applicationProvider) {
    Signal.handle(
        new Signal("TERM"),
        signal -> {
          isShuttingDown = true;
          logger.info(
              "Termination required, swallowing SIGTERM to allow current requests to finish");
          actorSystem
              .scheduler()
              .scheduleOnce(
                  STOP_DELAY,
                  () -> {
                    Play.stop(applicationProvider.get());
                  },
                  actorSystem.dispatcher());
        });
  }

  public boolean isShuttingDown() {
    return isShuttingDown;
  }
}
