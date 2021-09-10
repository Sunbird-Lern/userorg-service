package modules;

import com.google.inject.AbstractModule;
import org.sunbird.logging.LoggerUtil;

public class StartModule extends AbstractModule {
  private LoggerUtil logger = new LoggerUtil(StartModule.class);

  @Override
  protected void configure() {
    logger.info("StartModule:configure: Start");
    try {
      bind(SignalHandler.class).asEagerSingleton();
      bind(ApplicationStart.class).asEagerSingleton();
    } catch (Exception | Error e) {
      logger.error("Exception occurred while starting play module", e);
    }
    logger.info("StartModule:configure: End");
  }
}
