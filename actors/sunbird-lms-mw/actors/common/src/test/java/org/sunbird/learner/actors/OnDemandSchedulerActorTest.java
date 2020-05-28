package org.sunbird.learner.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.quartz.scheduler.OnDemandSchedulerManager;
import org.sunbird.common.quartz.scheduler.SchedulerManager;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  OnDemandSchedulerManager.class,
  SchedulerManager.class
})
@PowerMockIgnore("javax.management.*")
public class OnDemandSchedulerActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(OnDemandSchedulerActor.class);
  private static OnDemandSchedulerManager onDemandSchedulerManager;
  private static SchedulerManager schedulerManager;
  
  @Before
  public void beforeEachTest() throws Exception {
    schedulerManager= mock(SchedulerManager.class);
    onDemandSchedulerManager = mock(OnDemandSchedulerManager.class);
    //PowerMockito.whenNew(OnDemandSchedulerManager.class).withNoArguments().thenReturn(onDemandSchedulerManager);
    PowerMockito.mockStatic(SchedulerManager.class);
    
    PowerMockito.mockStatic(OnDemandSchedulerManager.class);
    doNothing().when(schedulerManager).schedule();
    when(OnDemandSchedulerManager.getInstance()).thenReturn(onDemandSchedulerManager);
  }
  
  @Test
  public void testOnDemandScheduler() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    List<String> types = new ArrayList<>();
    types.add("shadowuser");
    reqMap.put(JsonKey.TYPE, types);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.ONDEMAND_START_SCHEDULER.getValue());
    boolean result = testScenario(req, null);
    assertTrue(result);
  }
  
  @Test
  public void testOnDemandSchedulerWithInvalidType() {
    Request req = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    List<String> types = new ArrayList<>();
    types.add("shadowuser1");
    reqMap.put(JsonKey.TYPE, types);
    req.setRequest(reqMap);
    req.setOperation(ActorOperations.ONDEMAND_START_SCHEDULER.getValue());
    boolean result = testScenario(req, ResponseCode.invalidParameter);
    assertTrue(result);
  }
  
  private boolean testScenario(Request request, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(request, probe.getRef());
    
    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("100 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
        probe.expectMsgClass(duration("100 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
        || res.getResponseCode() == errorCode.getResponseCode();
    }
  }
}