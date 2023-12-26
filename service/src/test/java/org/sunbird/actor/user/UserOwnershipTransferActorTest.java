package org.sunbird.actor.user;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        UserServiceImpl.class
})
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.security.*",
        "jdk.internal.reflect.*",
        "javax.crypto.*"
})
public class UserOwnershipTransferActorTest {

    private static final CassandraOperationImpl cassandraOperation = PowerMockito.mock(CassandraOperationImpl.class);
    Props props = Props.create(UserOwnershipTransferActor.class);
    ActorSystem system = ActorSystem.create("userOwnershipTransferActor");

    private static Response getSuccessResponse() {
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        return response;
    }

    private static boolean computeResult(Object msg) {
        if (msg instanceof Response) {
            Response res = (Response) msg;
            return res.getResponseCode() == ResponseCode.OK;
        } else if (msg instanceof ProjectCommonException) {
            ProjectCommonException res = (ProjectCommonException) msg;
            if (res.getErrorCode().equals(ResponseCode.invalidRequestData.getErrorCode())) {
                return res.getErrorResponseCode() == ResponseCode.CLIENT_ERROR.getResponseCode();
            } else if (res.getErrorCode().equals(ResponseCode.invalidParameter.getErrorCode())) {
                return res.getErrorResponseCode() == ResponseCode.invalidParameter.getResponseCode();
            } else if (res.getErrorCode().equals(ResponseCode.dataTypeError.getErrorCode())) {
                return res.getErrorResponseCode() == ResponseCode.dataTypeError.getResponseCode();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Before
    public void beforeEachTest() {
        mockStaticDependencies();
    }

    @After
    public void afterEachTest() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    private boolean testActorBehavior(Request request, ResponseCode errorCode) {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);

        try {
            subject.tell(request, probe.getRef());
            Object msg = probe.expectMsgAnyClassOf(duration("30 second"), Response.class, ProjectCommonException.class);
            return computeResult(msg);
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void testOwnershipTransferSuccess() {
        Request request = createTestRequest();
        request.setRequestContext(new RequestContext());
        boolean result = testActorBehavior(request, null);
        assertTrue(result);
    }

    @Test
    public void testInvalidUserDetails() {
        Request request = createTestRequest();
        request.getRequest().remove("fromUser");
        request.setRequestContext(new RequestContext());
        boolean response = testActorBehavior(request, null);
        assertFalse(response);
    }

    @Test
    public void testInvalidRoleDetails() {
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get("fromUser")).remove("roles");
        request.setRequestContext(new RequestContext());
        boolean response = testActorBehavior(request, null);
        assertFalse(response);
    }

    @Test
    public void testInvalidActorDetails() {
        // Test scenario where "actionBy" details are invalid (e.g., missing "userId" or "userName").
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get("actionBy")).remove("userId");
        request.setRequestContext(new RequestContext());
        boolean response = testActorBehavior(request, null);
        assertFalse(response);
    }
    private void mockStaticDependencies() {
        mockStatic(ServiceFactory.class);
        mockStatic(UserServiceImpl.class);
        UserService userServiceMock = mock(UserService.class);
        when(UserServiceImpl.getInstance()).thenReturn(userServiceMock);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
        Promise<Boolean> booleanPromise = Futures.promise();
        booleanPromise.success(true);
        when(cassandraOperation.updateRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getSuccessResponse());
        when(cassandraOperation.insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getSuccessResponse());

        Response response2 = new Response();
        Map<String, Object> user = new HashMap<>();
        user.put(JsonKey.ID, "c9e6006e-5811-4337-aa7c-48d0f535e3b8");
        user.put(JsonKey.IS_DELETED, false);
        user.put(JsonKey.FIRST_NAME, "Testuser");
        user.put(JsonKey.STATUS, 1);
        List<Map<String, Object>> userList = new ArrayList<>();
        userList.add(user);
        response2.getResult().put(JsonKey.RESPONSE, userList);
        PowerMockito.when(cassandraOperation.getRecordById(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                .thenReturn(response2);

        Response mockResponse = new Response();
        List<Map<String, Object>> mockRoles = new ArrayList<>();
        Map<String, Object> mockRoleData = new HashMap<>();
        mockRoleData.put(JsonKey.ROLE, "ORG_ADMIN");
        mockRoles.add(mockRoleData);
        mockResponse.getResult().put(JsonKey.RESPONSE, mockRoles);
        PowerMockito.when(cassandraOperation.getRecordById(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyMap(),
                        Mockito.any(RequestContext.class)))
                .thenReturn(mockResponse);
    }

    private Request createTestRequest() {
        Request request = new Request();
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("context", "User Deletion");
        reqMap.put("organisationId", "0137038836873134080");
        reqMap.put("actionBy", getUserDetailsMap());
        reqMap.put("fromUser", getUserDetailsMap());
        reqMap.put("toUser", getUserDetailsMap());
        reqMap.put("objects", getObjectsList());
        request.setRequest(reqMap);
        request.setOperation(ActorOperations.USER_OWNERSHIP_TRANSFER.getValue());
        return request;
    }

    private Map<String, Object> getUserDetailsMap() {
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("userId", "c9e6006e-5811-4337-aa7c-48d0f535e3b8");
        userDetails.put("userName", "TestUser");
        userDetails.put("roles", List.of("ORG_ADMIN"));
        return userDetails;
    }

    private List<Map<String, Object>> getObjectsList() {
        List<Map<String, Object>> objects = new ArrayList<>();
        Map<String, Object> object1 = new HashMap<>();
        object1.put("objectType", "Content");
        object1.put("identifier", "do_id12");
        object1.put("primaryCategory", "ExplanationContent");
        object1.put("name", "TestContent");
        objects.add(object1);

        Map<String, Object> object2 = new HashMap<>();
        object2.put("objectType", "Program");
        object2.put("identifier", "programId1");
        object2.put("name", "TestProgram");
        objects.add(object2);

        return objects;
    }
}
