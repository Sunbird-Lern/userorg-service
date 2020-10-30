package org.sunbird.user.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.*;
import org.junit.Assert;
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
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;
import org.sunbird.user.util.UserLookUp;
import org.sunbird.user.util.UserUtil;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserDao.class,
  UserDaoImpl.class,
  UserUtility.class,
  UserLookUp.class,
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  ElasticSearchService.class,
  UserUtil.class
})
@PowerMockIgnore({"javax.management.*"})
public class UpdateUserNameActorTest {

  Props props = Props.create(UpdateUserNameActor.class);
  ActorSystem system = ActorSystem.create("system");

  private ElasticSearchService esUtil;
  private CassandraOperation cassandraOperation = null;
  private Response response;

  @Before
  public void beforeEachTest() throws Exception {
    UserDao userDao = PowerMockito.mock(UserDao.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    Mockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    PowerMockito.mockStatic(UserUtility.class);
    Mockito.when(userDao.getUserById(Mockito.anyString(), Mockito.any()))
        .thenReturn(getValidUserResponse1());
    List<User> userList = new ArrayList<>();
    userList.add(getValidUserResponse1());
    userList.add(getValidUserResponse2());
    Mockito.when(userDao.searchUser(Mockito.anyMap(), Mockito.any())).thenReturn(userList);
    // Mockito.when(userDao.getUserById("invalidUserId", null)).thenReturn(null);

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esUtil = mock(ElasticSearchService.class);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);

    cassandraOperation = mock(CassandraOperationImpl.class);
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getUserMap()));
    response.getResult().putAll(responseMap);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    when(cassandraOperation.getPropertiesValueById(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyList(),
            Mockito.any()))
        .thenReturn(response);
    Response resp = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, JsonKey.SUCCESS);
    resp.getResult().putAll(responseMap2);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(resp);

    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    when(esUtil.update(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(promise.future());
    when(ElasticSearchHelper.getResponseFromFuture(promise.future())).thenReturn(true);

    UserLookUp userLookUp = PowerMockito.mock(UserLookUp.class);
    whenNew(UserLookUp.class).withNoArguments().thenReturn(userLookUp);

    when(userLookUp.getRecordByType(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any()))
        .thenReturn(new ArrayList<>());
    when(userLookUp.insertRecords(Mockito.anyList(), Mockito.any())).thenReturn(new Response());
  }

  @Test
  public void testUpdateUserName() {
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, new ArrayList<>());
    response.getResult().putAll(responseMap);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(response);
    PowerMockito.mockStatic(UserUtil.class);
    when(UserUtil.getUsername(Mockito.anyString(), Mockito.any())).thenReturn("amit1232ndf");
    boolean result = testScenario(getRequest(), null);
    Assert.assertTrue(result);
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  private Request getRequest() {
    Request request = new Request();
    Map<String, Object> reqMap = new HashMap<>();
    List<String> userIds = new ArrayList<>();
    userIds.add("123456789");
    userIds.add("46546546");
    userIds.add("5465216566");
    reqMap.put(JsonKey.USER_IDs, userIds);
    reqMap.put("dryRun", false);
    request.setRequest(reqMap);
    request.setOperation("updateUserName");
    return request;
  }

  private Map<String, Object> getUserMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.USERNAME, "category");
    fMap.put(JsonKey.FIRST_NAME, "first");
    fMap.put(JsonKey.LAST_NAME, "last");
    return fMap;
  }

  private User getValidUserResponse1() {
    User user = new User();
    user.setId("ValidUserId1");
    user.setEmail("anyEmail1@gmail.com");
    user.setChannel("TN");
    user.setPhone("9876543210");
    user.setMaskedEmail("any****@gmail.com");
    user.setMaskedPhone("987*****0");
    user.setIsDeleted(false);
    user.setFlagsValue(3);
    user.setUserType("TEACHER");
    user.setUserId("ValidUserId");
    user.setFirstName("Demo Name");
    user.setUserName("validUserName1");
    return user;
  }

  private User getValidUserResponse2() {
    User user = new User();
    user.setId("ValidUserId2");
    user.setEmail("anyEmail2@gmail.com");
    user.setChannel("TN");
    user.setPhone("9876543210");
    user.setMaskedEmail("any****@gmail.com");
    user.setMaskedPhone("987*****0");
    user.setIsDeleted(false);
    user.setFlagsValue(3);
    user.setUserType("TEACHER");
    user.setUserId("ValidUserId");
    user.setFirstName("Demo Name");
    user.setUserName("validUserName2");
    return user;
  }
}
