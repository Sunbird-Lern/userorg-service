package modules;

import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import play.api.Application;
import play.api.Play;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import sun.misc.Signal;

@Singleton
public class SignalHandler {

  private static final FiniteDuration STOP_DELAY = Duration.create(40, TimeUnit.SECONDS);

  private volatile boolean isShuttingDown = false;

  @Inject
  public SignalHandler(ActorSystem actorSystem, Provider<Application> applicationProvider) {
    Signal.handle(
        new Signal("TERM"),
        signal -> {
          isShuttingDown = true;
          ProjectLogger.log(
              "Termination required, swallowing SIGTERM to allow current requests to finish",
              LoggerEnum.INFO.name());
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
