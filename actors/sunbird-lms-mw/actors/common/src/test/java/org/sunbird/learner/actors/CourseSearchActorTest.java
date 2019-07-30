package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.search.CourseSearchActor;
import org.sunbird.learner.util.Util;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  ElasticSearchHelper.class,
  EsClientFactory.class,
  ProjectUtil.class,
  ElasticSearchRestHighImpl.class,
  Util.class
})
@PowerMockIgnore("javax.management.*")
public class CourseSearchActorTest {
  private ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(CourseSearchActor.class);
  private String courseId = "";
  private ElasticSearchService esService;

  @Before
  public void beforeEachTest() {
    esService = mock(ElasticSearchRestHighImpl.class);
    PowerMockito.mockStatic(EsClientFactory.class);
    PowerMockito.mock(ElasticSearchHelper.class);
    PowerMockito.mock(Util.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);
    Promise<Map<String, Object>> promise = Futures.promise();
    HashMap<String, Object> innerMap = new HashMap<>();
    promise.success(innerMap);
    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(promise.future());
  }

  @Test
  @Ignore
  public void searchCourseOnReceiveTest() throws Exception {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    PowerMockito.doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Map section = invocation.getArgumentAt(0, Map.class);
                Map<String, Object> object = new HashMap<>();
                object.put(JsonKey.IDENTIFIER, courseId);
                Object[] list = new Object[1];
                list[0] = object;
                section.put(JsonKey.CONTENTS, list);
                return null;
              }
            })
        .when(Util.class, "getContentData", Mockito.anyMap());
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.SEARCH_COURSE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.QUERY, "");
    Map<String, Object> filters = new HashMap<>();
    Map<String, Object> map = new HashMap<>();
    List<String> objectType = new ArrayList<String>();
    objectType.add("Content");
    filters.put(JsonKey.OBJECT_TYPE, objectType);
    List<String> mimeType = new ArrayList<String>();
    mimeType.add("application/vnd.ekstep.html-archive");
    filters.put("mimeType", mimeType);
    List<String> status = new ArrayList<String>();
    status.add("Draft");
    status.add("Live");
    filters.put(JsonKey.STATUS, status);
    innerMap.put(JsonKey.FILTERS, filters);
    innerMap.put(JsonKey.LIMIT, 1);
    map.put(JsonKey.SEARCH, innerMap);
    reqObj.setRequest(map);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Object[] objects = ((Object[]) res.getResult().get(JsonKey.RESPONSE));

    if (null != objects && objects.length > 0) {
      Map<String, Object> obj = (Map<String, Object>) objects[0];
      courseId = (String) obj.get(JsonKey.IDENTIFIER);
      System.out.println(courseId);
      Assert.assertTrue(null != courseId);
    }
  }

  @Test
  public void testInvalidOperation() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation("INVALID_OPERATION");

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc = probe.expectMsgClass(ProjectCommonException.class);
    Assert.assertTrue(null != exc);
  }

  @Test
  public void getCourseByIdOnReceiveTest() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_COURSE_BY_ID.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    innerMap.put(JsonKey.ID, courseId);
    innerMap.put(JsonKey.REQUESTED_BY, "user-001");
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    Assert.assertTrue(null != res.get(JsonKey.RESPONSE));
  }
}
