package modules;

import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.auth.verifier.KeyManager;
import org.sunbird.helper.CassandraConnectionManager;
import org.sunbird.helper.CassandraConnectionMngrFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.common.ProjectUtil;
import org.sunbird.util.user.SchedulerManager;
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
    checkCassandraConnections();
    HttpClientUtil.getInstance();
    applicationLifecycle.addStopHook(() -> CompletableFuture.completedFuture(null));
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
    checkCassandraDbConnections();
    SchedulerManager.schedule();
  }

  /**
   * This method will check the cassandra data base connection. first it will try to established the
   * data base connection from provided environment variable , if environment variable values are
   * not set then connection will be established from property file.
   */
  private static void checkCassandraDbConnections() {
    CassandraConnectionManager cassandraConnectionManager =
        CassandraConnectionMngrFactory.getInstance();
    String nodes = System.getenv(JsonKey.SUNBIRD_CASSANDRA_IP);
    String[] hosts = null;
    if (StringUtils.isNotBlank(nodes)) {
      hosts = nodes.split(",");
    } else {
      hosts = new String[] {"localhost"};
    }
    cassandraConnectionManager.createConnection(hosts);
  }
}
