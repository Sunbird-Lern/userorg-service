package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Test;
import org.mockito.Mockito;
import org.sunbird.operations.ActorOperations;
import org.sunbird.models.organisation.Organisation;
import org.sunbird.user.actors.SSUUserCreateActor;

public class SSUUserCreateActorTest extends UserManagementActorTestBase {

  public final Props props = Props.create(SSUUserCreateActor.class);

  @Test
  public void testCreateUserV3Failure() {
    Organisation organisation = new Organisation();
    organisation.setId("rootOrgId");
    organisation.setChannel("anyChannel");
    organisation.setRootOrgId("rootOrgId");
    organisation.setTenant(false);
    when(organisationClient.esGetOrgById(Mockito.anyString(), Mockito.any()))
        .thenReturn(organisation);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(
        getRequest(true, true, true, getAdditionalMapData(reqMap), ActorOperations.CREATE_SSU_USER),
        probe.getRef());
    Exception ex = probe.expectMsgClass(duration("1000 second"), NullPointerException.class);
    assertNotNull(ex);
  }
}
