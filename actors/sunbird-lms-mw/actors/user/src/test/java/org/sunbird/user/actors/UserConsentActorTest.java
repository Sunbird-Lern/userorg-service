package org.sunbird.user.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        CassandraOperationImpl.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserConsentActorTest {
    private static ActorSystem system = ActorSystem.create("system");
    private final Props props = Props.create(UserConsentActor.class);
    private static Response response = null;
    public static CassandraOperationImpl cassandraOperation;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ServiceFactory.class);
        cassandraOperation = mock(CassandraOperationImpl.class);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    }

    public static Response getSuccessResponse() {
        Map<String, Object> consent = new HashMap<String, Object>();
        consent.put(JsonKey.USER_ID, "test-user");
        consent.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
        consent.put(JsonKey.CONSENT_OBJECTID, "test-collection");
        consent.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
        consent.put(JsonKey.STATUS, "ACTIVE");

        List<Map<String, Object>> consentList =  new ArrayList<Map<String, Object>>();
        consentList.add(consent);

        Response response = new Response();
        response.put(JsonKey.RESPONSE, consentList);
        return response;
    }

    public static Response getSuccessNoRecordResponse() {
        Map<String, Object> consent = new HashMap<String, Object>();

        List<Map<String, Object>> consentList =  new ArrayList<Map<String, Object>>();
        consentList.add(consent);

        Response response = new Response();
        response.put(JsonKey.RESPONSE, consentList);
        return response;
    }

    public static Request getUserConsentRequest(){
        Map<String, Object> filters = new HashMap<String, Object>();
        filters.put(JsonKey.USER_ID, "test-user");
        filters.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
        filters.put(JsonKey.CONSENT_OBJECTID, "test-collection");

        Map<String, Object> consent = new HashMap<String, Object>();
        consent.put("filters", filters);

        Request reqObj = new Request();
        reqObj.setOperation(ActorOperations.GET_USER_CONSENT.getValue());
        reqObj.put("consent",consent);

        return reqObj;
    }

    public static Request updateUserConsentRequest(){
        Map<String, Object> consent = new HashMap<String, Object>();
        consent.put(JsonKey.USER_ID, "test-user");
        consent.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
        consent.put(JsonKey.CONSENT_OBJECTID, "test-collection");
        consent.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
        consent.put(JsonKey.STATUS, "ACTIVE");

        Request reqObj = new Request();
        reqObj.setOperation(ActorOperations.UPDATE_USER_CONSENT.getValue());
        reqObj.put("consent",consent);

        return reqObj;
    }

    @Test
    public void getUserConsentTestSuccess() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        when(cassandraOperation.getRecordsByProperties(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyMap(),
                Mockito.any(RequestContext.class)))
                .thenReturn(getSuccessResponse());

        subject.tell(getUserConsentRequest(), probe.getRef());
        Response res = probe.expectMsgClass(duration("10 second"), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void getUserConsentTestNotFound() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        when(cassandraOperation.getRecordsByProperties(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyMap(),
                Mockito.any(RequestContext.class)))
                .thenReturn(getSuccessNoRecordResponse());

        subject.tell(getUserConsentRequest(), probe.getRef());
        Response res = probe.expectMsgClass(duration("10 second"), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }

    @Test
    public void updateUserConsentTest() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        when(cassandraOperation.getRecordsByProperties(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyMap(),
                Mockito.any(RequestContext.class)))
                .thenReturn(getSuccessResponse());

        when(cassandraOperation.upsertRecord(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyMap(),
                Mockito.any(RequestContext.class)))
                .thenReturn(getSuccessResponse());

        subject.tell(updateUserConsentRequest(), probe.getRef());
        Response res = probe.expectMsgClass(duration("10 second"), Response.class);
        Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
    }
}
