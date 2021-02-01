package org.sunbird.user.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import org.sunbird.common.request.RequestContext;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.feed.FeedUtil;
import org.sunbird.feed.IFeedService;
import org.sunbird.feed.impl.FeedFactory;
import org.sunbird.feed.impl.FeedServiceImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.organisation.external.identity.service.OrgExternalService;
import org.sunbird.learner.organisation.service.OrgService;
import org.sunbird.learner.organisation.service.impl.OrgServiceImpl;
import org.sunbird.models.user.Feed;
import org.sunbird.user.UserManagementActorTestBase;
import org.sunbird.user.service.UserService;
import org.sunbird.user.service.impl.UserServiceImpl;
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
  FeedUtil.class,
  UserServiceImpl.class,
  UserService.class,
  OrgServiceImpl.class,
  OrgService.class
})
@PowerMockIgnore({"javax.management.*"})
public class TenantMigrationActorTest extends UserManagementActorTestBase {
  Props props = Props.create(TenantMigrationActor.class);
  ActorSystem system = ActorSystem.create("system");

  private ElasticSearchService esUtil;
  private CassandraOperation cassandraOperation = null;
  private static Response response;
  private static IFeedService feedService;
  @Mock private OrgExternalService externalClass;

  @Before
  public void beforeEachTest() {
    ActorRef actorRef = mock(ActorRef.class);
    PowerMockito.mockStatic(RequestRouter.class);
    PowerMockito.mockStatic(FeedUtil.class);
    PowerMockito.mockStatic(UserService.class);
    PowerMockito.mockStatic(UserServiceImpl.class);
    PowerMockito.mockStatic(OrgServiceImpl.class);
    PowerMockito.mockStatic(OrgService.class);

    PowerMockito.mockStatic(FeedServiceImpl.class);
    PowerMockito.mockStatic(FeedFactory.class);
    feedService = mock(FeedServiceImpl.class);
    when(FeedFactory.getInstance()).thenReturn(feedService);
    when(FeedServiceImpl.getCassandraInstance()).thenReturn(cassandraOperation);
    when(feedService.getFeedsByProperties(Mockito.anyMap(), Mockito.any()))
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
            cassandraOperation.getRecordsByProperties(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(RequestContext.class)))
        .thenReturn(response);

    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);
    PowerMockito.when(
            cassandraOperation.upsertRecord(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(RequestContext.class)))
        .thenReturn(upsertResponse);

    when(RequestRouter.getActor(Mockito.anyString())).thenReturn(actorRef);
    PowerMockito.mockStatic(SystemSettingClientImpl.class);
    SystemSettingClientImpl systemSettingClient = mock(SystemSettingClientImpl.class);
    when(SystemSettingClientImpl.getInstance()).thenReturn(systemSettingClient);
    when(systemSettingClient.getSystemSettingByFieldAndKey(
            Mockito.any(ActorRef.class),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyObject(),
            Mockito.any()))
        .thenReturn(new HashMap<>());
    PowerMockito.mockStatic(MigrationUtils.class);
    when(MigrationUtils.markUserAsRejected(
            Mockito.any(ShadowUser.class), Mockito.any(RequestContext.class)))
        .thenReturn(true);
  }

  @Test
  public void testUserMigrateRejectWhenUserFound() {
    when(MigrationUtils.getEligibleUsersById("anyUserId", null))
        .thenReturn(getShadowUserAsList(StringUtils.EMPTY, 1));
    boolean result =
        testScenario(
            getMigrateReq(ActorOperations.MIGRATE_USER, JsonKey.REJECT), null, ResponseCode.OK);
    assertTrue(result);
  }

  @Test
  public void testUserMigrateRejectWhenUserNotFound() {
    List<ShadowUser> shadowUserList = new ArrayList<>();
    when(MigrationUtils.getEligibleUsersById("WrongUserId", null)).thenReturn(shadowUserList);
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
    when(MigrationUtils.getEligibleUsersById("WrongUserId", null)).thenReturn(shadowUserList);
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
    when(MigrationUtils.getEligibleUsersById("anyUserId", propsMap, null))
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
    when(MigrationUtils.getEligibleUsersById(Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
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

  @Test
  public void testUserSelfDeclarationMigrationWhenRecordNotFoundInUserDeclarations() {
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(new Response());
    boolean result =
        testScenario(
            getSelfDeclaredMigrateReq(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE),
            ResponseCode.declaredUserValidatedStatusNotUpdated,
            null);
    assertTrue(result);
  }

  @Test
  public void testUserSelfDeclarationMigrationWithValidatedStatus() {
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSelfDeclarationResponse());
    Response updateResponse = new Response();
    updateResponse.getResult().put(JsonKey.RESPONSE, "FAILED");
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(updateResponse);
    when(cassandraOperation.getRecordsByCompositeKey(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyMap(),
            Mockito.any(RequestContext.class)))
        .thenReturn(getOrgFromCassandra());

    List<Map<String, Object>> listMap = new ArrayList<>();
    listMap.add(new HashMap<String, Object>());
    Map<String, Object> userDetails = new HashMap<>();
    userDetails.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    userDetails.put(JsonKey.ORGANISATIONS, listMap);
    UserService userService = mock(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.esGetPublicUserProfileById(Mockito.anyString(), Mockito.anyObject()))
        .thenReturn(userDetails);
    when(userService.getCustodianOrgId(Mockito.anyObject(), Mockito.anyObject()))
        .thenReturn("anyRootOrgId");

    try {
      OrgExternalService orgExternalService = PowerMockito.mock(OrgExternalService.class);
      PowerMockito.whenNew(OrgExternalService.class)
          .withAnyArguments()
          .thenReturn(orgExternalService);
      when(orgExternalService.getOrgIdFromOrgExternalIdAndProvider(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyObject()))
          .thenReturn("anyRootOrgId");
    } catch (Exception e) {

    }
    OrgService orgService = mock(OrgServiceImpl.class);
    PowerMockito.when(OrgServiceImpl.getInstance()).thenReturn(orgService);
    when(orgService.getOrgById(Mockito.anyString(), Mockito.any(RequestContext.class)))
        .thenReturn(getOrgandLocation());

    /* when(userService.getRootOrgIdFromChannel( Mockito.anyObject(), Mockito.anyObject())).thenReturn("anyRootOrgId");

    PowerMockito.when(
            cassandraOperation.updateRecord(
                    Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(RequestContext.class)))
            .thenReturn(getSuccessUpdateResponse());
    doNothing()
            .when(cassandraOperation)
            .deleteRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any());*/

    boolean result =
        testScenario(
            getSelfDeclaredMigrateReq(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE),
            ResponseCode.errorUserMigrationFailed,
            null);
    assertTrue(result);
  }

  public Map<String, Object> getOrgandLocation() {
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    return map;
  }

  public Response getOrgFromCassandra() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.ORG_ID, "anyOrgId");
    map.put(JsonKey.LOCATION_IDS, new ArrayList<String>(Arrays.asList("anyLocationId")));
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  @Test
  public void testUserSelfDeclarationMigrationWithValidatedStatuswithError() {
    CassandraOperation cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    PowerMockito.when(
            cassandraOperation.getRecordById(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(getSelfDeclarationResponse());
    Response updateResponse = new Response();
    updateResponse.getResult().put(JsonKey.RESPONSE, "FAILED");
    PowerMockito.when(
            cassandraOperation.updateRecord(
                Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(updateResponse);
    List<Map<String, Object>> listMap = new ArrayList<>();
    listMap.add(new HashMap<String, Object>());
    Map<String, Object> userDetails = new HashMap<>();
    userDetails.put(JsonKey.ROOT_ORG_ID, "");
    userDetails.put(JsonKey.ORGANISATIONS, listMap);
    UserService userService = mock(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    when(userService.esGetPublicUserProfileById(Mockito.anyString(), Mockito.anyObject()))
        .thenReturn(userDetails);

    boolean result =
        testScenario(
            getSelfDeclaredMigrateReq(ActorOperations.USER_SELF_DECLARED_TENANT_MIGRATE),
            ResponseCode.parameterMismatch,
            null);
    assertTrue(result);
  }

  public Request getSelfDeclaredMigrateReq(ActorOperations actorOperation) {
    Request reqObj = new Request();
    Map<String, Object> requestMap = new HashMap();
    Map<String, String> externalIdMap = new HashMap();
    List<Map<String, String>> externalIdLst = new ArrayList();
    requestMap.put(JsonKey.USER_ID, "anyUserID");
    requestMap.put(JsonKey.CHANNEL, "anyChannel");
    requestMap.put(JsonKey.ROOT_ORG_ID, "anyRootOrgId");
    externalIdMap.put(JsonKey.ID, "anyID");
    externalIdMap.put(JsonKey.ID_TYPE, "anyIDtype");
    externalIdMap.put(JsonKey.PROVIDER, "anyProvider");
    externalIdLst.add(externalIdMap);
    requestMap.put(JsonKey.EXTERNAL_IDS, externalIdLst);
    requestMap.put(JsonKey.ORG_EXTERNAL_ID, "anyOrgId");
    reqObj.setRequest(requestMap);
    reqObj.setOperation(actorOperation.getValue());
    return reqObj;
  }

  private Response getSelfDeclarationResponse() {
    Response response = new Response();
    Map<String, Object> fMap = new HashMap<>();

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put(JsonKey.ORG_ID, "anyOrgID");
    responseMap.put(JsonKey.PERSONA, "anyPersona");
    responseMap.put(Constants.RESPONSE, Arrays.asList(responseMap));
    response.getResult().putAll(responseMap);
    return response;
  }

  private Response getSuccessUpdateResponse() {
    Response upsertResponse = new Response();
    Map<String, Object> responseMap2 = new HashMap<>();
    responseMap2.put(Constants.RESPONSE, Constants.SUCCESS);
    upsertResponse.getResult().putAll(responseMap2);

    return upsertResponse;
  }
}
