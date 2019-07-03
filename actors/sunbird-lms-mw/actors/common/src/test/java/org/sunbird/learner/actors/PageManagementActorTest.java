package org.sunbird.learner.actors;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.cacheloader.PageCacheLoaderService;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import scala.concurrent.Future;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  Util.class,
  DataCacheHandler.class,
  PageManagementActor.class,
  ContentSearchUtil.class,
  PageCacheLoaderService.class
})
@Ignore
@PowerMockIgnore({"javax.management.*"})
public class PageManagementActorTest {

  private static ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(PageManagementActor.class);
  private static CassandraOperationImpl cassandraOperation;

  private static String sectionId = null;
  private static String sectionId2 = null;
  private static String pageId = "anyID";
  private static String pageIdWithOrg = null;
  private static String pageIdWithOrg2 = null;
  private static Object[] arr = new Object[10];
  private static ObjectMapper objectMapper;
  private static Future<Map<String, Object>> result;

  @BeforeClass
  public static void beforeClass() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    PowerMockito.mockStatic(ContentSearchUtil.class);
    when(ContentSearchUtil.searchContent(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), system.dispatcher()))
        .thenReturn(result);
  }

  @Before
  public void beforeEachTest() throws Exception {

    PowerMockito.mockStatic(ServiceFactory.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    Map<String, Map<String, Object>> pageMap = new ConcurrentHashMap<>();
    Map map = new HashMap();
    map.put(JsonKey.PORTAL_MAP, "anyQuery");
    pageMap.put("NA:Test Page Name Updated", map);
    Map sectionMap = new HashMap();
    sectionMap.put("anyId", getSectionMap());

    PowerMockito.mockStatic(DataCacheHandler.class);
    PowerMockito.when(DataCacheHandler.getPageMap()).thenReturn(pageMap);
    PowerMockito.when(DataCacheHandler.getSectionMap()).thenReturn(sectionMap);

    objectMapper = Mockito.mock(ObjectMapper.class);
    PowerMockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(objectMapper);
    when(cassandraOperation.updateRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordById());
    when(cassandraOperation.insertRecord(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getSuccessResponse());
    when(cassandraOperation.getAllRecords(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordById());
  }

  @Test
  public void testInvalidRequest() {

    Request reqObj = new Request();

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    subject.tell(reqObj, probe.getRef());
    NullPointerException exc =
        probe.expectMsgClass(duration("10 second"), NullPointerException.class);
    assertTrue(null != exc);
  }

  @Test
  public void testInvalidOperationSuccess() {

    Request reqObj = new Request();
    reqObj.setOperation("INVALID_OPERATION");

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.invalidRequestData.getErrorCode()));
  }

  @Test
  public void testCreateSuccessPageWithOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<Map<String, Object>> appMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> appMap = new HashMap<String, Object>();
    appMap.put(JsonKey.ID, sectionId);
    appMap.put(JsonKey.INDEX, new BigInteger("1"));
    appMap.put(JsonKey.GROUP, new BigInteger("1"));
    appMapList.add(appMap);

    pageMap.put(JsonKey.APP_MAP, appMapList);

    List<Map<String, Object>> portalMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> portalMap = new HashMap<String, Object>();
    portalMap.put(JsonKey.ID, sectionId);
    portalMap.put(JsonKey.INDEX, new BigInteger("1"));
    portalMap.put(JsonKey.GROUP, new BigInteger("1"));
    portalMapList.add(portalMap);

    pageMap.put(JsonKey.PORTAL_MAP, portalMapList);

    pageMap.put(JsonKey.PAGE_NAME, "Test Page");
    pageMap.put(JsonKey.ORGANISATION_ID, "ORG1");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordByPropMap(false));
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    pageIdWithOrg = (String) response.get(JsonKey.PAGE_ID);
    assertTrue(null != pageIdWithOrg);
  }

  @Test
  public void testCreatePageSuccessWithoutOrgId() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<Map<String, Object>> appMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> appMap = new HashMap<String, Object>();
    appMap.put(JsonKey.ID, sectionId);
    appMap.put(JsonKey.INDEX, new BigInteger("1"));
    appMap.put(JsonKey.GROUP, new BigInteger("1"));
    appMapList.add(appMap);

    pageMap.put(JsonKey.APP_MAP, appMapList);

    List<Map<String, Object>> portalMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> portalMap = new HashMap<String, Object>();
    portalMap.put(JsonKey.ID, sectionId);
    portalMap.put(JsonKey.INDEX, new BigInteger("1"));
    portalMap.put(JsonKey.GROUP, new BigInteger("1"));
    portalMapList.add(portalMap);

    pageMap.put(JsonKey.PORTAL_MAP, portalMapList);

    pageMap.put(JsonKey.PAGE_NAME, "Test Page3");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(getRecordByPropMap(false));
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(Response.class);
    pageIdWithOrg2 = (String) response.get(JsonKey.PAGE_ID);
    assertTrue(null != pageIdWithOrg2);
  }

  @Test
  public void testCreatePageFailureWithPageAlreadyExists() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.CREATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();
    List<Map<String, Object>> appMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> appMap = new HashMap<String, Object>();
    appMap.put(JsonKey.ID, sectionId);
    appMap.put(JsonKey.INDEX, new BigInteger("1"));
    appMap.put(JsonKey.GROUP, new BigInteger("1"));
    appMapList.add(appMap);

    pageMap.put(JsonKey.APP_MAP, appMapList);

    List<Map<String, Object>> portalMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> portalMap = new HashMap<String, Object>();
    portalMap.put(JsonKey.ID, sectionId);
    portalMap.put(JsonKey.INDEX, new BigInteger("1"));
    portalMap.put(JsonKey.GROUP, new BigInteger("1"));
    portalMapList.add(portalMap);

    pageMap.put(JsonKey.PORTAL_MAP, portalMapList);

    pageMap.put(JsonKey.PAGE_NAME, "Test Page");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraGetRecordByProperty(""));

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(null != exc);
    assertTrue(exc.getCode().equals(ResponseCode.pageAlreadyExist.getErrorCode()));
  }

  @Test
  public void testGetPageSettingSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean pageName = false;
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_PAGE_SETTING.getValue());
    reqObj.getRequest().put(JsonKey.ID, "Test Page");

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByProperty(""));

    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> result = response.getResult();
    Map<String, Object> page = (Map<String, Object>) result.get(JsonKey.PAGE);
    if (null != page.get(JsonKey.NAME) && ((String) page.get(JsonKey.NAME)).equals("anyName")) {
      pageName = true;
    }
    assertTrue(pageName);
  }

  @Test
  public void testGetPageSettingSuccessWithAppMap() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean pageName = false;

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_PAGE_SETTING.getValue());
    reqObj.getRequest().put(JsonKey.ID, "Test Page");

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByProperty(JsonKey.APP_MAP));

    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> result = response.getResult();
    Map<String, Object> page = (Map<String, Object>) result.get(JsonKey.PAGE);
    if (null != page.get(JsonKey.NAME) && ((String) page.get(JsonKey.NAME)).equals("anyName")) {
      pageName = true;
    }
    assertTrue(pageName);
  }

  @Test
  public void testGetPageSettingSuccessWithPortalMap() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean pageName = false;
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_PAGE_SETTING.getValue());
    reqObj.getRequest().put(JsonKey.ID, "Test Page");

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByProperty(JsonKey.PORTAL_MAP));

    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> result = response.getResult();
    Map<String, Object> page = (Map<String, Object>) result.get(JsonKey.PAGE);
    if (null != page.get(JsonKey.NAME) && ((String) page.get(JsonKey.NAME)).equals("anyName")) {
      pageName = true;
    }
    assertTrue(pageName);
  }

  @Test
  public void testGetPageSettingsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean pageName = false;
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_PAGE_SETTINGS.getValue());
    reqObj.getRequest().put(JsonKey.ID, "Test Page");

    when(cassandraOperation.getRecordsByProperty(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
        .thenReturn(cassandraGetRecordByProperty(JsonKey.PORTAL_MAP));

    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> result = response.getResult();

    assertTrue(null != result);
  }

  @Test
  public void testGetAllSectionsSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean section = false;
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_ALL_SECTION.getValue());
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    Map<String, Object> result = response.getResult();
    List<Map<String, Object>> sectionList =
        (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);
    for (Map<String, Object> sec : sectionList) {
      if (null != sec.get(JsonKey.SECTIONS)) {
        section = true;
      }
    }
    assertTrue(section);
  }

  @Test
  @Ignore
  public void testGetSectionSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    boolean section = false;
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_SECTION.getValue());
    reqObj.getRequest().put(JsonKey.ID, sectionId);
    subject.tell(reqObj, probe.getRef());
    Response response = probe.expectMsgClass(Response.class);
    Map<String, Object> result = response.getResult();
    List<Map<String, Object>> sectionList =
        (List<Map<String, Object>>) result.get(JsonKey.RESPONSE);

    for (Map<String, Object> sec : sectionList) {
      if (null != sec.get(JsonKey.SECTIONS)) {
        section = true;
      }
    }
    assertTrue(section);
  }

  @Test
  public void testUpdatePageSuccessWithPageName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();

    pageMap.put(JsonKey.PAGE_NAME, "Test Page");
    pageMap.put(JsonKey.ID, pageId);
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraGetRecordByProperty(""));

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdatePageFailureWithPageName() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();

    pageMap.put(JsonKey.PAGE_NAME, "Test Page");
    pageMap.put(JsonKey.ID, "anyId");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraGetRecordByProperty(""));

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException exc =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    assertTrue(exc.getCode().equals(ResponseCode.pageAlreadyExist.getErrorCode()));
  }

  @Test
  public void testDUpdatePageSuccessWithoutPageAndWithPortalMap() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();

    pageMap.put(JsonKey.ID, pageId);
    pageMap.put(JsonKey.PORTAL_MAP, "anyQuery");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraGetRecordByProperty(JsonKey.PORTAL_MAP));

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testDUpdatePageSuccessWithoutPageAndWithAppMap() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_PAGE.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> pageMap = new HashMap<String, Object>();

    pageMap.put(JsonKey.ID, pageId);
    pageMap.put(JsonKey.APP_MAP, "anyQuery");
    innerMap.put(JsonKey.PAGE, pageMap);
    reqObj.setRequest(innerMap);

    when(cassandraOperation.getRecordsByProperties(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(cassandraGetRecordByProperty(JsonKey.APP_MAP));

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testUpdatePageSection() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Map<String, Object> filterMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    Map<String, Object> searchQueryMap = new HashMap<>();
    List<String> list = new ArrayList<>();
    list.add("Bengali");
    filterMap.put("language", list);
    reqMap.put(JsonKey.FILTERS, filterMap);
    searchQueryMap.put(JsonKey.REQUEST, reqMap);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.UPDATE_SECTION.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();
    Map<String, Object> sectionMap = new HashMap<String, Object>();
    sectionMap.put(JsonKey.ID, sectionId2);
    sectionMap.put(JsonKey.SECTION_DISPLAY, "TOP1");
    sectionMap.put(JsonKey.SECTION_NAME, "Updated Test Section2");
    sectionMap.put(JsonKey.SEARCH_QUERY, searchQueryMap);
    sectionMap.put(JsonKey.SECTION_DATA_TYPE, "course");
    innerMap.put(JsonKey.SECTION, sectionMap);
    reqObj.setRequest(innerMap);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(Response.class);
    assertTrue(null != res.get(JsonKey.RESPONSE));
  }

  @Test
  public void testGetPageData() throws Exception {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    Map<String, Object> header = new HashMap<>();
    header.put("Accept", "application/json");
    header.put("Content-Type", "application/json");
    Map<String, Object> filterMap = new HashMap<>();
    List<String> contentList = new ArrayList<>();
    contentList.add("Story");
    contentList.add("Worksheet");
    contentList.add("Collection");
    contentList.add("Game");
    contentList.add("Collection");
    contentList.add("Course");
    filterMap.put("contentType", contentList);

    List<String> statusList = new ArrayList<>();
    statusList.add("Live");
    filterMap.put("language", statusList);

    List<String> objectTypeList = new ArrayList<>();
    objectTypeList.add("content");
    filterMap.put("objectType", objectTypeList);

    List<String> languageList = new ArrayList<>();
    languageList.add("English");
    languageList.add("Hindi");
    languageList.add("Gujarati");
    languageList.add("Bengali");
    filterMap.put("language", languageList);
    Map<String, Object> compMap = new HashMap<>();
    compMap.put("<=", 2);
    compMap.put(">", 0.5);
    filterMap.put("size", compMap);
    filterMap.put("orthographic_complexity", 1);
    List<String> gradeListist = new ArrayList<>();
    gradeListist.add("Grade 1");
    filterMap.put("gradeLevel", gradeListist);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_PAGE_DATA.getValue());
    reqObj.getRequest().put(JsonKey.SOURCE, "web");
    reqObj.getRequest().put(JsonKey.PAGE_NAME, "Test Page Name Updated");
    reqObj.getRequest().put(JsonKey.FILTERS, filterMap);
    HashMap<String, Object> map = new HashMap<>();
    map.put(JsonKey.PAGE, reqObj.getRequest());
    map.put(JsonKey.HEADER, header);
    reqObj.setRequest(map);

    when(objectMapper.readValue("anyQuery", Object[].class)).thenReturn(arr);
    when(objectMapper.readValue("searchQuery", HashMap.class))
        .thenReturn(getObjectMapperResponse());

    subject.tell(reqObj, probe.getRef());
    probe.expectMsgClass(duration("10 second"), Response.class);
  }

  private static Response getRecordByPropMap(boolean isValid) {
    Response response = new Response();
    List<Map> list = new ArrayList<>();
    Map<String, Object> map = new HashMap();
    map.put(JsonKey.ID, "anyID");
    if (isValid) list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private Response cassandraGetRecordByProperty(String reqMap) {
    Response response = new Response();
    List list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "anyName");
    map.put(JsonKey.ID, "anyID");
    if (!reqMap.equals("")) {
      map.put(reqMap, "anyQuery");
    }
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private HashMap<String, Object> getObjectMapperResponse() {
    HashMap<String, Object> objMap = new HashMap<>();
    Map<String, Object> reqMap = new HashMap<>();
    Map<String, Object> filterMap = new HashMap<>();
    reqMap.put(JsonKey.FILTERS, filterMap);
    objMap.put(JsonKey.REQUEST, reqMap);
    return objMap;
  }

  private Map<String, Object> getSectionMap() {
    Map<String, Object> sectionMap = new HashMap<>();
    sectionMap.put(JsonKey.SEARCH_QUERY, "searchQuery");
    return sectionMap;
  }

  private static Response getSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private static Response cassandraGetRecordById() {
    Response response = new Response();
    List list = new ArrayList();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "anyName");
    map.put(JsonKey.ID, "anyId");
    map.put(JsonKey.SECTIONS, "anySection");
    list.add(map);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }
}
