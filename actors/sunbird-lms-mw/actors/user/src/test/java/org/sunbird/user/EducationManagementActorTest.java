package org.sunbird.user;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.*;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;

import java.math.BigInteger;
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
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.actors.EducationManagementActor;
import org.sunbird.user.actors.JobProfileManagementActor;
import org.sunbird.user.dao.AddressDao;
import org.sunbird.user.dao.EducationDao;
import org.sunbird.user.dao.JobProfileDao;
import org.sunbird.user.dao.impl.AddressDaoImpl;
import org.sunbird.user.dao.impl.EducationDaoImpl;
import org.sunbird.user.dao.impl.JobProfileDaoImpl;
import org.sunbird.user.util.UserActorOperations;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, EducationDaoImpl.class, AddressDaoImpl.class})
@PowerMockIgnore({"javax.management.*"})
public class EducationManagementActorTest {

  private static final ActorSystem system = ActorSystem.create("system");
  private static final Props props = Props.create(EducationManagementActor.class);
  private AddressDao addressDao;
  private EducationDao educationDao;


  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(AddressDaoImpl.class);
    PowerMockito.mockStatic(EducationDaoImpl.class);
    addressDao=PowerMockito.mock(AddressDao.class);
    educationDao=PowerMockito.mock(EducationDaoImpl.class);
    when(AddressDaoImpl.getInstance()).thenReturn(addressDao);
    when(EducationDaoImpl.getInstance()).thenReturn(educationDao);
  }


  @Test
  public void testInsertUserEducationWithAddressSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    boolean result = testScenario(getInsertEducationRequestWithAddress("insertUserEducation"), null);
    assertTrue(result);
  }
  @Test
  public void testInsertUserEducationWithOutAddressSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    boolean result = testScenario(getInsertEducationRequestWithOutAddress("insertUserEducation"), null);
    assertTrue(result);
  }

  @Test
  public void testInsertUserEducationWithAddressIdSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    boolean result = testScenario(getInsertEducationRequestWithAddressId("insertUserEducation"), null);
    assertTrue(result);
  }
  @Test
  public void testInsertUserEducationWithOutAddressFailure(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    Response result = testScenario(getInsertEducationInvalidRequest("insertUserEducation"));
    Assert.assertEquals("Error occurred while inserting education details.",((List)result.get(JsonKey.ERROR_MSG)).get(0));
  }

  @Test
  public void testEducationActorWithInvalidOperation(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    Request request=getInsertEducationRequestWithAddress("insertUserEducation");
    request.setOperation("invalidOperation");
    boolean result = testScenario(request,ResponseCode.CLIENT_ERROR);
    Assert.assertTrue(result);
  }

  @Test
  public void testUpdateUserEducationWithAddressIdSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    boolean result = testScenario(getInsertEducationRequestWithAddressId("updateUserEducation"), null);
    assertTrue(result);
  }
  @Test
  public void testUpdateUserEducationWithAddressSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    boolean result = testScenario(getUpdateEducationRequestWithAddress("updateUserEducation"), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserEducationWithOutAddressSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    when(educationDao.getPropertiesValueById(Mockito.anyString(),Mockito.anyString())).thenReturn(getAddressIdInResponse());
    boolean result = testScenario(getInsertEducationRequestWithOutAddress("updateUserEducation"), null);
    assertTrue(result);
  }

  @Test
  public void testUpdateUserEducationWithAddressAndWithoutIdSuccess(){
    Response response=new Response();
    response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
    when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
    when(educationDao.getPropertiesValueById(Mockito.anyString(),Mockito.anyString())).thenReturn(getAddressIdInResponse());
    boolean result = testScenario(getUpdateEducReqWithoutEducId("updateUserEducation"), null);
    assertTrue(result);
  }


  private Response getAddressIdInResponse() {
    List<Map<String, Object>> addressList = new ArrayList<>();
    addressList.add(getAddressRequestWithId());
    Response response = new Response();
    response.put(JsonKey.RESPONSE, addressList);
    return response;
  }

  private Request getInsertEducationRequestWithAddress(String operationName){
    Request request=new Request();
    Map<String,Object> educationMap=getBaseEducationRequest();
    educationMap.put(JsonKey.ADDRESS,getBaseAddressRequest());
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(educationMap);
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,"anyUserId");
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }
  private Request getUpdateEducationRequestWithAddress(String operationName){
    Request request=new Request();
    Map<String,Object> educationMap=getBaseEducationRequest();
    educationMap.replace(JsonKey.IS_DELETED,false);
    educationMap.put(JsonKey.ADDRESS,getBaseAddressRequest());
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(educationMap);
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,"anyUserId");
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }
  private Request getUpdateEducReqWithoutEducId(String operationName){
    Request request=new Request();
    Map<String,Object> educationMap=getBaseEducationRequest();
    educationMap.replace(JsonKey.IS_DELETED,false);
    educationMap.remove(JsonKey.ID);
    educationMap.put(JsonKey.ADDRESS,getBaseAddressRequest());
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(educationMap);
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,"anyUserId");
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }


  private Request getInsertEducationRequestWithAddressId(String operationName){
    Request request=new Request();
    Map<String,Object> educationMap=getBaseEducationRequest();
    educationMap.put(JsonKey.ADDRESS,getAddressRequestWithId());
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(educationMap);
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,"anyUserId");
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }

  private Request getInsertEducationInvalidRequest(String operationName){
    Request request=new Request();
    Map<String,Object> educationMap=getBaseEducationRequest();
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(educationMap);
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,new HashMap<>());
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }

  private Request getInsertEducationRequestWithOutAddress(String operationName){
    Request request=new Request();
    List<Map<String,Object>>educationList=new ArrayList<>();
    educationList.add(getBaseEducationRequest());
    Map<String,Object>reqMap=new HashMap<>();
    reqMap.put(JsonKey.EDUCATION,educationList);
    reqMap.put(JsonKey.ID,"anyUserId");
    request.setOperation(operationName);
    request.setRequest(reqMap);
    return request;
  }


  private  Map<String,Object> getBaseEducationRequest(){
    Map<String,Object> educationMap=new HashMap<>();
    educationMap.put(JsonKey.DEGREE,"anyDegree");
    educationMap.put(JsonKey.NAME,"collegeName");
    educationMap.put("boardOrUniversity","CBSE");
    educationMap.put(JsonKey.YEAR_OF_PASSING,BigInteger.valueOf(2017));
    educationMap.put(JsonKey.COURSE_NAME,"BTECH");
    educationMap.put(JsonKey.GRADE,"A");
    educationMap.put( JsonKey.IS_DELETED,true);
    educationMap.put(JsonKey.PERCENTAGE,"98");
    educationMap.put(JsonKey.ID,ProjectUtil.getUniqueIdFromTimestamp(0));
    return educationMap;
  }



  private Map<String,Object> getBaseAddressRequest() {
    Map<String,Object>addressMap=new HashMap<>();
    addressMap.put(JsonKey.ADD_TYPE, ProjectUtil.AddressType.home.getTypeName());
    addressMap.put(JsonKey.ADDRESS_LINE1,"any address");
    addressMap.put(JsonKey.CITY,"anyCity");
    addressMap.put(JsonKey.TYPE,"DISTRICT");
    addressMap.put(JsonKey.STATE,"KARANATAKA");
    addressMap.put(JsonKey.ZIPCODE,"anyZipCode");
    return addressMap;
  }

  private Map<String,Object> getAddressRequestWithId() {
    Map<String,Object>addressMap=new HashMap<>();
    addressMap.put(JsonKey.ADD_TYPE, ProjectUtil.AddressType.home.getTypeName());
    addressMap.put(JsonKey.ADDRESS_LINE1,"any address");
    addressMap.put(JsonKey.CITY,"anyCity");
    addressMap.put(JsonKey.ID,ProjectUtil.getUniqueIdFromTimestamp(0));
    addressMap.put(JsonKey.TYPE,"DISTRICT");
    addressMap.put(JsonKey.STATE,"KARANATAKA");

    addressMap.put(JsonKey.ZIPCODE,"anyZipCode");
    return addressMap;
  }

  public boolean testScenario(Request reqObj, ResponseCode errorCode) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());

    if (errorCode == null) {
      Response res = probe.expectMsgClass(duration("10 second"), Response.class);
      System.out.println("the error in SUCCESS is"+res.get(JsonKey.ERROR_MSG));
      return null != res && res.getResponseCode() == ResponseCode.OK;
    } else {
      ProjectCommonException res =
              probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res.getCode().equals(errorCode.getErrorCode())
              || res.getResponseCode() == errorCode.getResponseCode();
    }
  }

  public Response testScenario(Request reqObj) {
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    return res;
  }
}
