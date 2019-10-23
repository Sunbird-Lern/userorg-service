package modules;

import com.google.inject.AbstractModule;

public class StartModule extends AbstractModule {

  @Override
  protected void configure() {
    System.out.println("StartModule:configure: Start");
    try {
      bind(ApplicationStart.class).asEagerSingleton();
    } catch (Exception | Error e) {
      e.printStackTrace();
    }
    System.out.println("StartModule:configure: End");
  }
}
