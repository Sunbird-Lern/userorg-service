package org.sunbird.actor.user;

import java.time.Duration;

import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.mockito.Mockito;
import org.sunbird.model.organisation.Organisation;
import org.sunbird.operations.ActorOperations;

public class SSUUserCreateActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(SSUUserCreateActor.class);
  @Test
  public void testCreateUserV3Failure() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(orgService.getOrgObjById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSU_USER),
        probe.getRef());
    Exception ex = probe.expectMsgClass(Duration.ofSeconds(1000), NullPointerException.class);
    assertNotNull(ex);
  }
}
