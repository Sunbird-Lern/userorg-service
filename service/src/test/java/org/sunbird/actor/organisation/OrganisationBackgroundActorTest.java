package org.sunbird.actor.organisation;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.util.ProjectUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  ProjectUtil.class,
  HttpClientUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class OrganisationBackgroundActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(OrganisationBackgroundActor.class);
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() throws Exception {

    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString())).thenReturn("anyString");

    PowerMockito.mockStatic(HttpClientUtil.class);
    when(HttpClientUtil.post(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyObject(),
            Mockito.any(RequestContext.class)))
        .thenReturn("anyStatus");

    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    Promise<Boolean> promise = Futures.promise();
    promise.success(true);

    when(esService.upsert(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any()))
        .thenReturn(promise.future());
  }

  @Test
  public void testInsertOrgInfoToEs() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPSERT_ORGANISATION_TO_ES.getValue());
    reqObj.getRequest().put(JsonKey.OPERATION_TYPE, JsonKey.INSERT);
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "1321546897");
    reqMap.put(
        JsonKey.ORG_LOCATION,
        "[{\"id\":\"1\",\"type\":\"state\"},{\"id\":\"2\",\"type\":\"district\"}]");
    reqObj.getRequest().put(JsonKey.ORGANISATION, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }
}
