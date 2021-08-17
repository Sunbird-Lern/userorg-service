package org.sunbird.actor.organisation;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.BackgroundJobManager;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.operations.OrganisationActorOperation;
import org.sunbird.request.Request;
import org.sunbird.util.ProjectUtil;
import org.sunbird.util.Util;
import scala.concurrent.Promise;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

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
        PowerMockito.when(ProjectUtil.getConfigValue(Mockito.anyString()))
                .thenReturn("anyString");

        PowerMockito.mockStatic(HttpClientUtil.class);
        when(HttpClientUtil.post(Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
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
        reqObj.setOperation(OrganisationActorOperation.UPSERT_ORGANISATION_TO_ES.getValue());
        reqObj.getRequest().put(JsonKey.OPERATION_TYPE,JsonKey.INSERT);
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
