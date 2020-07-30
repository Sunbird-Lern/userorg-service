package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
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
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.notificationservice.dao.impl.EmailTemplateDaoImpl;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.UserDeclareEntity;
import org.sunbird.user.actors.UserSelfDeclarationManagementActor;
import org.sunbird.user.util.UserActorOperations;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  DataCacheHandler.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,
  EmailTemplateDaoImpl.class,
  Util.class,
  EsClientFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserSelfDeclarationManagementActorTest {

  private static final Props props = Props.create(UserSelfDeclarationManagementActor.class);
  private ActorSystem system = ActorSystem.create("system");
  private static CassandraOperationImpl cassandraOperation;
  private ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void setUp() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EmailTemplateDaoImpl.class);
    PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  @Before
  public void beforeTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraInsertRecord());
    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getCassandraRecordsByProperties());
    cassandraOperation.deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap());
    cassandraOperation.updateRecord(
        Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap());

    PowerMockito.mockStatic(Util.class);
    when(Util.encryptData(Mockito.anyString())).thenReturn("userExtId");
  }

  private Response cassandraInsertRecord() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Response getCassandraRecordsByProperties() {
    Response response = new Response();
    List list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.CREATED_BY, "createdBy");
    map.put(JsonKey.PROVIDER, "anyProvider");
    map.put(JsonKey.USER_ID, "userid1");
    map.put(JsonKey.ORG_ID, "org1");
    map.put(JsonKey.PERSONA, "teacher");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  @Test
  public void testAddUserSelfDeclaredDetailsSuccess() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(UserActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());

    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(addUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);

    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testAddRemoveEditProvidersUserSelfDeclaredDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(UserActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());
    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(addUserDeclaredEntity());
    list.add(removeUserDeclaredEntity());
    list.add(editRoleChangeUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  @Test
  public void testEditOrgChangeUserSelfDeclaredDetailsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request request = new Request();
    request.setOperation(UserActorOperations.UPSERT_USER_SELF_DECLARATIONS.getValue());
    List<UserDeclareEntity> list = new ArrayList<>();
    list.add(editOrgChangeUserDeclaredEntity());
    Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(JsonKey.DECLARATIONS, list);
    request.setRequest(requestMap);
    subject.tell(request, probe.getRef());
    Response response = probe.expectMsgClass(duration("100 second"), Response.class);
    Assert.assertTrue(null != response && response.getResponseCode() == ResponseCode.OK);
    Assert.assertEquals(JsonKey.SUCCESS, response.getResult().get(JsonKey.RESPONSE));
  }

  private UserDeclareEntity addUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("01234848481");
    userDeclareEntity.setPersona(JsonKey.TEACHER.toLowerCase());
    userDeclareEntity.setStatus(JsonKey.PENDING);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.ADD);
    return userDeclareEntity;
  }

  private UserDeclareEntity removeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("018329328293892");
    userDeclareEntity.setPersona(JsonKey.TEACHER.toLowerCase());
    userDeclareEntity.setStatus(JsonKey.PENDING);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.REMOVE);
    return userDeclareEntity;
  }

  private UserDeclareEntity editRoleChangeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("org1");
    userDeclareEntity.setPersona("volunteer");
    userDeclareEntity.setStatus(JsonKey.PENDING);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.EDIT);
    return userDeclareEntity;
  }

  private UserDeclareEntity editOrgChangeUserDeclaredEntity() {
    UserDeclareEntity userDeclareEntity = new UserDeclareEntity();
    userDeclareEntity.setOrgId("org2");
    userDeclareEntity.setPersona("teacher");
    userDeclareEntity.setStatus(JsonKey.PENDING);
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put(JsonKey.DECLARED_EMAIL, "dsadaddasdadadadadE^JD");
    userInfo.put(JsonKey.DECLARED_PHONE, "0890321830");
    userDeclareEntity.setUserInfo(userInfo);
    userDeclareEntity.setOperation(JsonKey.EDIT);
    return userDeclareEntity;
  }
}
