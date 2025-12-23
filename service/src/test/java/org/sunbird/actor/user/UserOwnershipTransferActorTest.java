package org.sunbird.actor.user;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.sunbird.model.user.User;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserRoleService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserRoleServiceImpl;
import org.sunbird.service.user.impl.UserServiceImpl;

import java.time.Duration;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        UserServiceImpl.class,
        UserRoleServiceImpl.class
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
    private static ActorSystem system;
    Props props = Props.create(UserOwnershipTransferActor.class);

    private static Response getSuccessResponse() {
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        return response;
    }

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("system");
    }

    @Before
    public void beforeEachTest() {
        mockStaticDependencies();
    }

    @Test
    public void testOwnershipTransferSuccess() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        request.setRequestContext(new RequestContext());
        String statusMessage = "Ownership transfer process is submitted successfully!";
        subject.tell(request, probe.getRef());
        Response response = probe.expectMsgClass(Duration.ofSeconds(120), Response.class);
        assertEquals(statusMessage, response.getResult().get(JsonKey.STATUS));
    }

    @Test
    public void testOwnershipTransferWithEmptyObjectsList() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        request.getRequest().put("objects", new ArrayList<>());
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        Response response = probe.expectMsgClass(Duration.ofSeconds(120), Response.class);
        assertEquals("Ownership transfer process is submitted successfully!", response.getResult().get(JsonKey.STATUS));
    }

    @Test
    public void testInvalidUser() {
        // Arrange
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequestWithInvalidUser();
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120), ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("given user id under fromUser is not present or blank", errorResponse.getMessage());
        assertEquals(400, errorResponse.getErrorResponseCode());
    }


    private Request createTestRequestWithInvalidUser() {
        Request request = createTestRequest();
        Map<String, Object> invalidUserDetails = new HashMap<>();
        invalidUserDetails.put("userName", "testUserName");
        request.getRequest().put("fromUser", invalidUserDetails);
        return request;
    }


    @Test
    public void testSendResponse() {
        new TestKit(system) {
            {
                Props props = Props.create(UserOwnershipTransferActor.class);
                TestActorRef<UserOwnershipTransferActor> ref = TestActorRef.create(system, props);
                UserOwnershipTransferActor actor = ref.underlyingActor();
                String statusMessage = "Ownership transfer process is submitted successfully!";
                Response response = actor.sendResponse(statusMessage);
                assertEquals("api.user.ownership.transfer", response.getId());
                assertEquals("1.0", response.getVer());
                assertEquals(statusMessage, response.getResult().get(JsonKey.STATUS));
            }
        };
    }

    @Test
    public void testInvalidUserDetails() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        request.getRequest().remove("fromUser");
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120), ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("fromUser key is not present in the data.", errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }


    @Test
    public void testInvalidRoleDetails() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get("fromUser")).remove("roles");
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120),
                ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("Roles key is not present for fromUser", errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }

    @Test
    public void testOwnershipTransferWithInvalidActorRoles() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get(JsonKey.ACTION_BY)).put(JsonKey.USER_ID, "1234");
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120), ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0084", errorResponse.getErrorCode());
        assertEquals("User is restricted from transfering the ownership based on roles!", errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }

    @Test
    public void testOwnershipTransferWithEmptyRoles() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get(JsonKey.FROM_USER)).put(JsonKey.ROLES, Collections.emptyList());
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120), ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("Roles are empty in fromUser details.", errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }

    @Test
    public void testOwnershipTransferWithInvalidUserId() {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get(JsonKey.FROM_USER)).put(JsonKey.USER_ID, "123");
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120),
                ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("given user id under fromUser is not present or blank", errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }


    @Test
    public void testInvalidActorDetails() {
        // Test scenario where "actionBy" details are invalid (e.g., missing "userId" or "userName").
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        Request request = createTestRequest();
        ((Map<String, Object>) request.getRequest().get("actionBy")).remove("userId");
        request.setRequestContext(new RequestContext());
        subject.tell(request, probe.getRef());
        ProjectCommonException errorResponse = probe.expectMsgClass(Duration.ofSeconds(120),
                ProjectCommonException.class);
        assertEquals("UOS_UOWNTRANS0028", errorResponse.getErrorCode());
        assertEquals("given user id under actionBy is not present or blank",
                errorResponse.getMessage());
        assertEquals(ResponseCode.CLIENT_ERROR.getResponseCode(), errorResponse.getErrorResponseCode());
    }

    private void mockStaticDependencies() {
        mockStatic(ServiceFactory.class);
        mockStatic(UserServiceImpl.class);
        mockStatic(UserRoleServiceImpl.class);
        UserService userServiceMock = mock(UserService.class);
        UserRoleService userRoleServiceMock = mock(UserRoleService.class);
        when(UserServiceImpl.getInstance()).thenReturn(userServiceMock);
        when(UserRoleServiceImpl.getInstance()).thenReturn(userRoleServiceMock);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
        when(cassandraOperation.updateRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getSuccessResponse());
        when(cassandraOperation.insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(getSuccessResponse());
        when(userServiceMock.getUserById(eq("c9e6006e-5811-4337-aa7c-48d0f535e3b8"), Mockito.any(RequestContext.class)))
                .thenReturn(getValidUserResponse()); // Return valid user response for valid user ID
        when(userServiceMock.getUserById(eq("1234"), Mockito.any(RequestContext.class)))
                .thenReturn(getValidUserResponse());
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
        PowerMockito.when(userRoleServiceMock.getUserRoles(
                        Mockito.anyString(), Mockito.any(RequestContext.class)))
                .thenReturn(mockRoles);
        PowerMockito.when(userRoleServiceMock.getUserRoles(
                        eq("1234"), Mockito.any(RequestContext.class)))
                .thenReturn(getInvalidRoleResponse());
    }

    private List<Map<String, Object>> getInvalidRoleResponse() {
        Map<String, Object> invalidUserRole = new HashMap<>();
        invalidUserRole.put(JsonKey.USER_ID, "1234");
        invalidUserRole.put(JsonKey.ROLE, "INVALID_ROLE");
        List<Map<String, Object>> userRolesList = new ArrayList<>();
        userRolesList.add(invalidUserRole);
        return userRolesList;
    }

    private User getValidUserResponse() {
        User user = new User();
        user.setId("c9e6006e-5811-4337-aa7c-48d0f535e3b8");
        user.setIsDeleted(false);
        user.setFirstName("Testuser");
        user.setStatus(1);
        return user;
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
        userDetails.put("userName", "Testuser");
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
