package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.testkit.javadsl.TestKit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.user.actors.UserProfileActor;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class,
  Util.class,
})
@PowerMockIgnore({"javax.management.*"})
public class UserProfileActorTest {

  private ActorSystem system = ActorSystem.create("system");

  private final Props props = Props.create(UserProfileActor.class);
  private CassandraOperationImpl cassandraOperation;
  private ElasticSearchService esUtil;

  @Before
  public void beforeEachTest() {

    PowerMockito.mockStatic(ServiceFactory.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mockStatic(ElasticSearchHelper.class);
    PowerMockito.mockStatic(Util.class);
    SearchDTO searchDTO = Mockito.mock(SearchDTO.class);
    when(Util.createSearchDto(Mockito.anyMap())).thenReturn(searchDTO);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    esUtil = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esUtil);
  }

  @Test
  public void testGetMediaTypesSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_MEDIA_TYPES.getValue());
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getSuccessResponse());

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testSetProfileVisibilitySuccess() {
    final String userId = "USER-ID";
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(createGetResponse(true));
    when(esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId))
        .thenReturn(promise.future());
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any()))
        .thenReturn(createGetResponse(true));
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.PROFILE_VISIBILITY.getValue());
    reqObj.put(JsonKey.USER_ID, userId);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res && res.getResponseCode() == ResponseCode.OK);
  }

  @Test
  public void testSetProfileVisibilityFailure() {
    final String userId = "USER-ID";
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(null);
    when(esUtil.getDataByIdentifier(ProjectUtil.EsType.user.getTypeName(), userId))
        .thenReturn(promise.future());
    when(ElasticSearchHelper.getResponseFromFuture(Mockito.any())).thenReturn(null);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.PROFILE_VISIBILITY.getValue());
    reqObj.put(JsonKey.USER_ID, userId);
    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(res.getCode() == ResponseCode.userNotFound.getErrorCode());
  }

  private Map<String, Object> createGetResponse(boolean isSuccess) {
    HashMap<String, Object> response = new HashMap<>();
    if (isSuccess) response.put(JsonKey.CONTENT, "Any-content");
    return response;
  }

  private Response getSuccessResponse() {
    Response response = new Response();
    List<Map<String, Object>> resMapList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    resMapList.add(map);
    response.put(JsonKey.RESPONSE, resMapList);
    return response;
  }
}
