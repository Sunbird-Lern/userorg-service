package org.sunbird.user.actors;

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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.service.impl.UserServiceImpl;
import scala.concurrent.Promise;

import java.util.*;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        UserServiceImpl.class,
        ServiceFactory.class,
        ElasticSearchRestHighImpl.class,
        ElasticSearchHelper.class,
        EsClientFactory.class,
        CassandraOperationImpl.class,
        CassandraOperation.class,
        CassandraUtil.class,
})
@PowerMockIgnore({"javax.management.*"})
public class IdentifierFreeUpActorTest {
    private ElasticSearchService elasticSearchService;
    public static CassandraOperation cassandraOperation;
    private Util.DbInfo usrDbInfo = Util.dbInfoMap.get(JsonKey.USER_DB);
    Props props = Props.create(IdentifierFreeUpActor.class);
    ActorSystem system = ActorSystem.create("IdentifierFreeUpActor");

    @Before
    public void beforeEachTest() {
        PowerMockito.mockStatic(ElasticSearchRestHighImpl.class);
        elasticSearchService=PowerMockito.mock(ElasticSearchService.class);
        PowerMockito.mockStatic(EsClientFactory.class);
        when(EsClientFactory.getInstance(JsonKey.REST)).thenReturn(elasticSearchService);
        PowerMockito.mockStatic(ElasticSearchHelper.class);
        cassandraOperation=PowerMockito.mock(CassandraOperation.class);
        PowerMockito.mockStatic(ServiceFactory.class);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    }


    @Test
    public void testFreeUpWhenUserNotExists(){
        String id="wrongUserId";
        Response response=new Response();
        response.put(JsonKey.RESPONSE,new ArrayList<>());
        when(cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), id))
                .thenReturn(response);
        boolean result = testScenario(getFreeUpRequest(ActorOperations.FREEUP_USER_IDENTITY), ResponseCode.invalidUserId);
        assertTrue(result);
    }
    @Test
    public void testFreeUpWhenOnlyFreeUpEmail(){
        String id="anyUserId";
        Request reqObj = new Request();
        Map reqMap = new HashMap<>();
        reqMap.put(JsonKey.ID, "anyUserId");
        reqMap.put(JsonKey.IDENTIFIER, new ArrayList<>(Arrays.asList("email")));
        reqObj.setRequest(reqMap);
        reqObj.setOperation(ActorOperations.FREEUP_USER_IDENTITY.getValue());
        Response response=new Response();
        List<Map<String, Object>> responseList=new ArrayList<>();
        Map<String,Object>userDbMap=new HashMap<>();
        userDbMap.put(JsonKey.EMAIL,"userPrimaryEmail");
        userDbMap.put(JsonKey.PHONE,"9876543210");
        userDbMap.put(JsonKey.PREV_USED_EMAIL,null);
        userDbMap.put(JsonKey.EMAIL_VERIFIED,true);
        userDbMap.put(JsonKey.PHONE_VERIFIED,true);
        userDbMap.put(JsonKey.PREV_USED_PHONE,null);
        userDbMap.put(JsonKey.MASKED_EMAIL,"user*******");
        userDbMap.put(JsonKey.MASKED_PHONE,"98***08908");
        userDbMap.put(JsonKey.FLAGS_VALUE,3);
        userDbMap.put(JsonKey.ID,id);
        responseList.add(userDbMap);
        response.put(JsonKey.RESPONSE,responseList);
        when(cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), id))
                .thenReturn(response);
        when(cassandraOperation.updateRecord(
                Mockito.anyString(), Mockito.anyString(),Mockito.anyMap())).thenReturn(new Response());
        Promise<Boolean> promise = Futures.promise();
        promise.success(true);
        when(elasticSearchService.update(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap())).thenReturn(promise.future());
        when(ElasticSearchHelper.getResponseFromFuture(promise.future())).thenReturn(true);
        boolean result = testScenario(reqObj, null);
        assertTrue(result);
    }

    @Test
    public void testFreeUpWhenOnlyFreeUpPhone(){
        String id="anyUserId";
        Request reqObj = new Request();
        Map reqMap = new HashMap<>();
        reqMap.put(JsonKey.ID, "anyUserId");
        reqMap.put(JsonKey.IDENTIFIER, new ArrayList<>(Arrays.asList("phone")));
        reqObj.setRequest(reqMap);
        reqObj.setOperation(ActorOperations.FREEUP_USER_IDENTITY.getValue());
        Response response=new Response();
        List<Map<String, Object>> responseList=new ArrayList<>();
        Map<String,Object>userDbMap=new HashMap<>();
        userDbMap.put(JsonKey.EMAIL,"userPrimaryEmail");
        userDbMap.put(JsonKey.PHONE,"9876543210");
        userDbMap.put(JsonKey.PREV_USED_EMAIL,null);
        userDbMap.put(JsonKey.EMAIL_VERIFIED,true);
        userDbMap.put(JsonKey.PHONE_VERIFIED,true);
        userDbMap.put(JsonKey.PREV_USED_PHONE,null);
        userDbMap.put(JsonKey.MASKED_EMAIL,"user*******");
        userDbMap.put(JsonKey.MASKED_PHONE,"98***08908");
        userDbMap.put(JsonKey.FLAGS_VALUE,3);
        userDbMap.put(JsonKey.ID,id);
        responseList.add(userDbMap);
        response.put(JsonKey.RESPONSE,responseList);
        when(cassandraOperation.getRecordById(usrDbInfo.getKeySpace(), usrDbInfo.getTableName(), id))
                .thenReturn(response);
        when(cassandraOperation.updateRecord(
                Mockito.anyString(), Mockito.anyString(),Mockito.anyMap())).thenReturn(new Response());
        Promise<Boolean> promise = Futures.promise();
        promise.success(true);
        when(elasticSearchService.update(Mockito.anyString(),Mockito.anyString(),Mockito.anyMap())).thenReturn(promise.future());
        when(ElasticSearchHelper.getResponseFromFuture(promise.future())).thenReturn(true);
        boolean result = testScenario(reqObj, null);
        assertTrue(result);
    }

    public boolean testScenario(Request reqObj, ResponseCode errorCode) {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        subject.tell(reqObj, probe.getRef());

        if (errorCode == null) {
            Response res = probe.expectMsgClass(duration("10 second"), Response.class);
            System.out.println("the success response is"+res);
            return null != res && res.getResponseCode() == ResponseCode.OK;
        } else {
            ProjectCommonException res =
                    probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
            System.out.println("the failure response is  "+res);
            return res.getCode().equals(errorCode.getErrorCode())
                    || res.getResponseCode() == errorCode.getResponseCode();
        }
    }

    private Request getFreeUpRequest(ActorOperations actorOperation) {
        Request reqObj = new Request();
        Map reqMap = new HashMap<>();
        reqMap.put(JsonKey.ID, "wrongUserId");
        reqMap.put(JsonKey.IDENTIFIER, new ArrayList<>());
        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation.getValue());
        return reqObj;
    }
}