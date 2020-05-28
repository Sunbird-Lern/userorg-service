package org.sunbird.learner.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.health.HealthActor;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class,ServiceFactory.class,CassandraOperation.class, CassandraUtil.class,Util.class, EsClientFactory.class, ElasticSearchService.class, ElasticSearchHelper.class, ElasticSearchRestHighImpl.class})
@PowerMockIgnore({"javax.management.*"})
public class HealthActorTest {

  private static ActorSystem system;
  private Util.DbInfo badgesDbInfo = Util.dbInfoMap.get(JsonKey.BADGES_DB);

  private CassandraOperation cassandraOperation;
  private static final Props props = Props.create(HealthActor.class);

  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("system");
    PowerMockito.mockStatic(ServiceFactory.class);

  }

  @Test
  public void getHealthCheck() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.HEALTH_CHECK.getValue());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("200 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void getACTORHealthCheck() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ACTOR.getValue());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("200 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void getESHealthCheck() {
    ElasticSearchService elasticSearchService=PowerMockito.mock(ElasticSearchService.class);
    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(JsonKey.REST)).thenReturn(elasticSearchService);
    when(elasticSearchService.healthCheck()).thenReturn(promise.future());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.ES.getValue());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("200 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  @PrepareForTest(ServiceFactory.class)
  public void getCASSANDRAHealthCheck() {
    cassandraOperation=PowerMockito.mock(CassandraOperation.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getAllRecords(badgesDbInfo.getKeySpace(), badgesDbInfo.getTableName())).thenReturn(new Response());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CASSANDRA.getValue());
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("200 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }
}
