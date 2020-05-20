package org.sunbird.user.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.router.RequestRouter;
import org.sunbird.actorutil.systemsettings.impl.SystemSettingClientImpl;
import org.sunbird.bean.ShadowUser;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.Constants;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.feed.FeedUtil;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.feed.impl.FeedServiceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.models.user.Feed;
import org.sunbird.user.UserManagementActorTestBase;
import org.sunbird.user.util.MigrationUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  CassandraOperationImpl.class,
  ElasticSearchService.class,
  MigrationUtils.class,
  SystemSettingClientImpl.class,
  IFeedService.class,
  FeedServiceImpl.class,
  FeedFactory.class,
  ShadowUser.class,
  FeedUtil.class
})
@PowerMockIgnore({"javax.management.*"})
public class TenantMigrationActorTest extends UserManagementActorTestBase {
  Props props = Props.create(TenantMigrationActor.class);
  ActorSystem system = ActorSystem.create("system");

  private ElasticSearchService esUtil;
  private CassandraOperation cassandraOperation = null;
  private static Response response;
  private static IFeedService feedService;

  @Before
  public void beforeEachTest() {
    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    PowerMockito.mockStatic(FeedUtil.class);

    PowerMockito.mockStatic(FeedServiceImpl.class);
    PowerMockito.mockStatic(FeedFactory.class);
    feedService = mock(FeedServiceImpl.class);
    when(FeedFactory.getInstance()).thenReturn(feedService);
    when(FeedServiceImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    when(FeedServiceImpl.getESInstance()).thenReturn(esUtil);
    when(feedService.getRecordsByProperties(Mockito.anyMap()))
        .thenReturn(getFeedList(true))
        .thenReturn(getFeedList(false));

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    esUtil = mock(ElasticSearchService.class);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);

    cassandraOperation = mock(CassandraOperationImpl.class);
    response = new Response();
    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(Constants.RESPONSE, Arrays.asList(getFeedMap()));
    response.getResult().putAll(responseMap);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordsByProperties(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(response);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(cassandraOperation.upsertRecord(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(upsertResponse);

    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);
    PowerMockito.mockStatic(SystemSettingClientImpl.class);
    SystemSettingClientImpl systemSettingClient = mock(SystemSettingClientImpl.class);
    when(SystemSettingClientImpl.getInstance()).thenReturn(systemSettingClient);
    when(systemSettingClient.getSystemSettingByFieldAndKey(
            Mockito.any(ActorRef.class),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyObject()))
        .thenReturn(new HashMap<>());
    PowerMockito.mockStatic(MigrationUtils.class);
    when(MigrationUtils.markUserAsRejected(Mockito.any(ShadowUser.class))).thenReturn(true);
  }

  @Test
  public void testUserMigrateRejectWhenUserFound() {
    when(MigrationUtils.getEligibleUsersById("anyUserId"))
        .thenReturn(getShadowUserAsList(StringUtils.EMPTY, 1));
    boolean result =
        testScenario(
            getMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.REJECT), null, ResponseCode.OK);
    assertTrue(result);
  }

  @Test
  public void testUserMigrateRejectWhenUserNotFound() {
    List<ShadowUser> shadowUserList = new ArrayList<>();
    when(MigrationUtils.getEligibleUsersById("WrongUserId")).thenReturn(shadowUserList);
    boolean result =
        testScenario(
            getFailureMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.REJECT),
            ResponseCode.invalidUserId,
            null);
    assertTrue(result);
  }

  @Test
  public void testUserMigrationAcceptWhenUserNotFound() {
    List<ShadowUser> shadowUserList = new ArrayList<>();
    when(MigrationUtils.getEligibleUsersById("WrongUserId")).thenReturn(shadowUserList);
    boolean result =
        testScenario(
            getFailureMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.ACCEPT),
            ResponseCode.invalidUserId,
            null);
    assertTrue(result);
  }

  public Request getFailureMigrateReq(ActorOperations actorOperation, String action) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "WrongUserId");
    reqMap.put(JsonKey.USER_EXT_ID, "WrongAnyUserExtId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    reqMap.put(JsonKey.ACTION, action);
    reqMap.put(JsonKey.FEED_ID, "anyFeedId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    System.out.println(reqMap);
    return reqObj;
  }

  /** AC->ATTEMPT COUNT, e.g AC1-> Attempt Count 1 */
  @Test
  public void testUserMigrationAcceptWhenUserFoundWithInCorrectExtIdAC1() {
    Map<String, Object> propsMap = new HashMap<>();
    propsMap.put(JsonKey.CHANNEL, "anyChannel");
    when(MigrationUtils.getEligibleUsersById("anyUserId", propsMap))
        .thenReturn(getShadowUserAsList("wrongUserExtId", 1));
    boolean result =
        testScenario(
            getMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.ACCEPT),
            null,
            ResponseCode.invalidUserExternalId);
    assertTrue(result);
  }

  @Test
  public void testUserMigrationAcceptWhenUserFoundWithInCorrectExtIdAC2() {
    when(MigrationUtils.getEligibleUsersById(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getShadowUserAsList("wrongUserExtId", 2));
    boolean result =
        testScenario(
            getMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.ACCEPT),
            ResponseCode.userMigrationFiled,
            null);
    assertTrue(result);
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode, ResponseCode responseCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (responseCode != null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      return null != res && res.getResponseCode() == responseCode;
    }
    if (errorCode != null) {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
          || res.getResponseCode() == errorCode.getResponseCode();
    }
    return true;
  }

  public Request getMigrateReq(ActorOperations actorOperation, String action) {
    Request reqObj = new Request();
    Map reqMap = new HashMap<>();
    reqMap.put(JsonKey.USER_ID, "anyUserId");
    reqMap.put(JsonKey.USER_EXT_ID, "anyUserExtId");
    reqMap.put(JsonKey.CHANNEL, "anyChannel");
    reqMap.put(JsonKey.ACTION, action);
    reqMap.put(JsonKey.FEED_ID, "anyFeedId");
    reqObj.setRequest(reqMap);
    reqObj.setOperation(actorOperation.getValue());
    System.out.println(reqMap);
    return reqObj;
  }

  private List<ShadowUser> getShadowUserAsList(String userExtId, int attemptCount) {
    List<ShadowUser> shadowUserList = new ArrayList<>();
    shadowUserList.add(getShadowUser(userExtId, attemptCount));
    return shadowUserList;
  }

  private ShadowUser getShadowUser(String userExtId, int attemptCount) {
    ShadowUser shadowUser =
        new ShadowUser.ShadowUserBuilder()
            .setChannel("anyChannel")
            .setUserExtId(StringUtils.isNotEmpty(userExtId) ? userExtId : "anyUserExtId")
            .setUserId("anyUserId")
            .setAttemptedCount(attemptCount - 1)
            .setUserStatus(ProjectUtil.Status.ACTIVE.getValue())
            .build();
    return shadowUser;
  }

  private Map<String, Object> getFeedMap() {
    Map<String, Object> fMap = new HashMap<>();
    fMap.put(JsonKey.ID, "123-456-7890");
    fMap.put(JsonKey.USER_ID, "123-456-789");
    fMap.put(JsonKey.CATEGORY, "category");
    return fMap;
  }

  private List<Feed> getFeedList(boolean needId) {
    Feed feed = new Feed();
    feed.setUserId("123-456-7890");
    feed.setCategory("category");
    if (needId) {
      feed.setId("123-456-789");
    }
    Map<String, Object> map = new HashMap<>();
    List<String> channelList = new ArrayList<>();
    channelList.add("SI");
    map.put(JsonKey.PROSPECT_CHANNELS, channelList);
    feed.setData(map);
    List<Feed> feedList = new ArrayList<>();
    feedList.add(feed);
    return feedList;
  }
}
