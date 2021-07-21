package modules;

import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.sunbird.actor.service.SunbirdMWService;
import org.sunbird.auth.verifier.KeyManager;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.learner.util.SchedulerManager;
import org.sunbird.learner.util.Util;
import org.sunbird.util.ProjectUtil;
import play.api.Environment;
import play.api.inject.ApplicationLifecycle;

@Singleton
public class ApplicationStart {
  public static ProjectUtil.Environment env;
  public static String ssoPublicKey = "";

  @Inject
  public ApplicationStart(ApplicationLifecycle applicationLifecycle, Environment environment) {
    setEnvironment(environment);
    ssoPublicKey = System.getenv(JsonKey.SSO_PUBLIC_KEY);
    SunbirdMWService.init();
    checkCassandraConnections();
    // initialize HttpClientUtil class
    HttpClientUtil.getInstance();
    applicationLifecycle.addStopHook(
        () -> {
          return CompletableFuture.completedFuture(null);
        });
    KeyManager.init();
  }

  private void setEnvironment(Environment environment) {
    if (environment.asJava().isDev()) {
      env = ProjectUtil.Environment.dev;
    } else if (environment.asJava().isTest()) {
      env = ProjectUtil.Environment.qa;
    } else {
      env = ProjectUtil.Environment.prod;
    }
  }

  private static void checkCassandraConnections() {
    Util.checkCassandraDbConnections();
    SchedulerManager.schedule();
    // Run quartz scheduler in a separate thread as it waits for 4 minutes
    // before scheduling various jobs.
    new Thread(() -> org.sunbird.common.quartz.scheduler.SchedulerManager.getInstance()).start();
  }
}
