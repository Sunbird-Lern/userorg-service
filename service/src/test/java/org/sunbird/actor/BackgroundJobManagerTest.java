package org.sunbird.actor;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.dispatch.Futures;
import org.apache.pekko.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.organisation.validator.OrgTypeValidator;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  ProjectUtil.class,
  OrgTypeValidator.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
@Ignore
public class BackgroundJobManagerTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(BackgroundJobManager.class);
  private static CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(Util.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    Promise<Boolean> promise = Futures.promise();
    promise.success(true);

    when(esService.bulkInsert(Mockito.anyString(), Mockito.anyList(), Mockito.any()))
            .thenReturn(promise.future());
    OrgTypeValidator orgTypeValidator = mock(OrgTypeValidator.class);
    PowerMockito.mockStatic(OrgTypeValidator.class);
    when(OrgTypeValidator.getInstance()).thenReturn(orgTypeValidator);
    when(orgTypeValidator.getValueByType(JsonKey.ORG_TYPE_SCHOOL)).thenReturn(2);
  }

  /*@Test
  public void testInsertOrgInfoToEs() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(cassandraGetRecord());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "1321546897");
    reqMap.put(JsonKey.ORGANISATION_TYPE, 2);
    reqObj.getRequest().put(JsonKey.ORGANISATION, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }

  @Test
  public void testInsertOrgInfoToEsFailure() {
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(cassandraGetRecord2());
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.INSERT_ORG_INFO_ELASTIC.getValue());
    Map<String, Object> reqMap = new HashMap<>();
    reqMap.put(JsonKey.ID, "1321546897");
    reqObj.getRequest().put(JsonKey.ORGANISATION, reqMap);
    subject.tell(reqObj, probe.getRef());
    probe.expectNoMessage();
    assertTrue(true);
  }*/

  private static Response cassandraGetRecord() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    String loc =
        "[{\"id\":\"6dd69f1c-ba40-4b3b-8981-4fb6813c5e71\",\"type\":\"district\"},{\"id\":\"e9207c22-41cf-4a0d-81fb-1fbe3e34ae24\",\"type\":\"cluster\"},{\"id\":\"ccc7be29-8e40-4d0a-915b-26ec9228ac4a\",\"type\":\"state\"}]";
    map.put(JsonKey.ORG_LOCATION, loc);
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private static Response cassandraGetRecord2() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ID, "anyId");
    String loc =
        "{\"id\":\"6dd69f1c-ba40-4b3b-8981-4fb6813c5e71\",\"type\":\"district\"},{\"id\":\"e9207c22-41cf-4a0d-81fb-1fbe3e34ae24\",\"type\":\"cluster\"},{\"id\":\"ccc7be29-8e40-4d0a-915b-26ec9228ac4a\",\"type\":\"state\"}";
    map.put(JsonKey.ORG_LOCATION, loc);
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
