package org.sunbird.actor.notification;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClientUtil.class, ProjectUtil.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class BackgroundNotificationActorTest {

  private static final Props props = Props.create(BackGroundNotificationActor.class);
  private ActorSystem system = ActorSystem.create("system");

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ProjectUtil.class);
  }

  @Before
  public void beforeTest() {
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(HttpClientUtil.class);
  }

  @Test
  public void callNotificationServiceTest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation("processNotification");

    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
  }
}
