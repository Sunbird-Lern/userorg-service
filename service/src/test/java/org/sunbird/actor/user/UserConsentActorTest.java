package org.sunbird.actor.user;

import java.time.Duration;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.actor.userconsent.UserConsentActor;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.exception.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.service.userconsent.UserConsentService;
import org.sunbird.service.userconsent.impl.UserConsentServiceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.nullable;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  UserConsentServiceImpl.class,
  UserServiceImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserConsentActorTest {
  private static ActorSystem system = ActorSystem.create("system");
  private final Props props = Props.create(UserConsentActor.class);
  public static CassandraOperationImpl cassandraOperation;
  public static UserService userService;
  public static UserConsentService userConsentService;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(UserConsentServiceImpl.class);
    userConsentService = PowerMockito.mock(UserConsentServiceImpl.class);
    PowerMockito.when(UserConsentServiceImpl.getInstance()).thenReturn(userConsentService);
  }

  public static Response getSuccessResponse() {
    Map<String, Object> consent = new HashMap<String, Object>();
    consent.put(JsonKey.USER_ID, "test-user");
    consent.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
    consent.put(JsonKey.CONSENT_OBJECTID, "test-collection");
    consent.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
    consent.put(JsonKey.STATUS, "ACTIVE");

    List<Map<String, Object>> consentList = new ArrayList<Map<String, Object>>();
    consentList.add(consent);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, consentList);
    return response;
  }

  public static Response getOrg() {
    Map<String, Object> org = new HashMap<String, Object>();
    org.put(JsonKey.ID, "test-organisation");

    List<Map<String, Object>> orgList = new ArrayList<Map<String, Object>>();
    orgList.add(org);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, orgList);
    return response;
  }

  public static Response getSuccessNoRecordResponse() {
    Map<String, Object> consent = new HashMap<String, Object>();

    List<Map<String, Object>> consentList = new ArrayList<Map<String, Object>>();
    consentList.add(consent);

    Response response = new Response();
    response.put(JsonKey.RESPONSE, consentList);
    return response;
  }

  public static Request getUserConsentRequest() {
    Map<String, Object> filters = new HashMap<String, Object>();
    filters.put(JsonKey.USER_ID, "test-user");
    filters.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
    filters.put(JsonKey.CONSENT_OBJECTID, "test-collection");

    Map<String, Object> consent = new HashMap<String, Object>();
    consent.put("filters", filters);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_USER_CONSENT.getValue());
    reqObj.put("consent", consent);

    return reqObj;
  }

  public static Request updateUserConsentRequest() {
    Map<String, Object> consent = new HashMap<String, Object>();
    consent.put(JsonKey.USER_ID, "test-user");
    consent.put(JsonKey.CONSENT_CONSUMERID, "test-organisation");
    consent.put(JsonKey.CONSENT_OBJECTID, "test-collection");
    consent.put(JsonKey.CONSENT_OBJECTTYPE, "Collection");
    consent.put(JsonKey.STATUS, "ACTIVE");

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_USER_CONSENT.getValue());
    reqObj.put("consent", consent);

    return reqObj;
  }
  
  @Test
  public void updateUserConsentTest() {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    
    
    PowerMockito.mockStatic(UserServiceImpl.class);
   
    userService = PowerMockito.mock(UserServiceImpl.class);
    PowerMockito.when(UserServiceImpl.getInstance()).thenReturn(userService);
    
    Response response = new Response();
    response
      .getResult()
      .put(
        JsonKey.ID,
        "usr-consent:529a57aa-6365-4145-9a35-e8cfc934eb4e:0130107621805015045:do_31313966505806233613406");
    doNothing()
      .when(userService)
      .validateUserId(Mockito.any(), nullable(String.class), Mockito.any());
    when(userConsentService.updateConsent(Mockito.any(), Mockito.any())).thenReturn(response);
    
    subject.tell(updateUserConsentRequest(), probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(100), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void getUserConsentTestSuccess() {
    
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    
    Map<String, Object> consentMap = new HashMap();
    List<Map<String, Object>> consentList = new ArrayList<>();
    consentMap.put(JsonKey.ID, "someID");
    consentMap.put(JsonKey.CONSENT_USER_ID, "someCosentUserID");
    consentMap.put(JsonKey.CONSENT_CONSUMER_ID, "someCosentConsumerID");
    consentMap.put(JsonKey.CONSENT_OBJECT_ID, "CONSENT_OBJECT_ID");
    consentMap.put(JsonKey.CONSENT_CONSUMER_TYPE, "organisation");
    consentMap.put(JsonKey.CONSENT_OBJECT_TYPE, "organisation");
    consentMap.put(JsonKey.STATUS, "active");
    consentMap.put(JsonKey.CONSENT_EXPIRY, "CONSENT_EXPIRY");
    consentMap.put(JsonKey.CATEGORIES, "collection");
    consentMap.put(JsonKey.CONSENT_CREATED_ON, "CONSENT_CREATED_ON");
    consentMap.put(JsonKey.CONSENT_LAST_UPDATED_ON, "CONSENT_LAST_UPDATED_ON");
    consentList.add(consentMap);
    when(userConsentService.getConsent(Mockito.any())).thenReturn(consentList);

    subject.tell(getUserConsentRequest(), probe.getRef());
    Response res = probe.expectMsgClass(Duration.ofSeconds(10), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }
  
}
