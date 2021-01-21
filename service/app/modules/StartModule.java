package modules;

import com.google.inject.AbstractModule;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

public class StartModule extends AbstractModule {

  @Override
  protected void configure() {
    System.out.println("StartModule:configure: Start");
    ProjectLogger.log("StartModule:configure: Start", LoggerEnum.INFO.name());
    try {
      bind(SignalHandler.class).asEagerSingleton();
      bind(ApplicationStart.class).asEagerSingleton();
    } catch (Exception | Error e) {
      e.printStackTrace();
    }
    ProjectLogger.log("StartModule:configure: End", LoggerEnum.INFO.name());
  }
}
