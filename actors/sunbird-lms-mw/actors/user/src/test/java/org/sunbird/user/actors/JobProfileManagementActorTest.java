package org.sunbird.user.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.user.dao.AddressDao;
import org.sunbird.user.dao.JobProfileDao;
import org.sunbird.user.dao.impl.AddressDaoImpl;
import org.sunbird.user.dao.impl.JobProfileDaoImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AddressDaoImpl.class,
        ServiceFactory.class,
        JobProfileDaoImpl.class

})
@PowerMockIgnore({"javax.management.*"})
public class JobProfileManagementActorTest {
    private AddressDao addressDao;
    private JobProfileDao jobProfileDao;
    Props props = Props.create(JobProfileManagementActor.class);
    ActorSystem system = ActorSystem.create("JobProfileManagementActor");

    @Before
    public void beforeEachTest() {
        PowerMockito.mockStatic(AddressDaoImpl.class);
        PowerMockito.mockStatic(JobProfileDaoImpl.class);
        addressDao=PowerMockito.mock(AddressDao.class);
        jobProfileDao=PowerMockito.mock(JobProfileDao.class);
        when(AddressDaoImpl.getInstance()).thenReturn(addressDao);
        when(JobProfileDaoImpl.getInstance()).thenReturn(jobProfileDao);

    }


    @Test
    public void testInsertUserJobProfileWithoutAddressSuccess(){
        doNothing().when(jobProfileDao).createJobProfile(Mockito.anyMap());
        boolean result = testScenario(getJobProfileRequest("insertUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testInsertUserJobProfileWithAddressSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithAddress("insertUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testInsertUserJobProfileWithAddressIdSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithAddressId("insertUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testUpdateUserJobProfileWithoutAddressSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequest("updateUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testUpdateUserJobProfileWithoutAddressAndIsDeletedSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithIsDeleted("updateUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testUpdateUserJobProfileWithAddressAndIsDeletedSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithAddress("updateUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testUpdateUserJobProfileWithAddressSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithAddressAndId("updateUserJobProfile"), null);
        assertTrue(result);
    }
    @Test
    public void testUpdateUserJobProfileWithOutAddressSuccess(){
        Response response=new Response();
        response.put(JsonKey.RESPONSE,JsonKey.SUCCESS);
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(response);
        boolean result = testScenario(getJobProfileRequestWithIsDeleted("updateUserJobProfile"), null);
        assertTrue(result);
    }

    @Test
    public void testInsertUserJobProfileWithAddressFailure(){
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(new Response());
        Response result = testScenario(getJobProfileRequestWithAddress("insertUserJobProfile"));
        Assert.assertEquals("Error occurred while inserting job profile details.",((List)result.get(JsonKey.ERROR_MSG)).get(0));
    }

    @Test
    public void testUpdateUserJobProfileWithAddressFailure(){
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(new Response());
        Response result = testScenario(getJobProfileRequestWithAddress("updateUserJobProfile"));
        Assert.assertEquals("Error occurred while updating job profile details.",((List)result.get(JsonKey.ERROR_MSG)).get(0));
    }

    @Test
    public void testInsertUserJobProfileWithInvalidOperation(){
        when(addressDao.upsertAddress(Mockito.anyMap())).thenReturn(new Response());
        boolean result = testScenario(getJobProfileRequestWithAddress("invalidOperation"),ResponseCode.invalidRequestData);
        Assert.assertTrue(result);
    }


    private Request getJobProfileRequest(String actorOperation) {
        Request reqObj = new Request();
        Map jobProfileMap = new HashMap<>();
        jobProfileMap.put(JsonKey.ID,"anyJobId");
        jobProfileMap.put(JsonKey.IS_DELETED,false);
        jobProfileMap.put(JsonKey.JOB_NAME, "anyJobName");
        jobProfileMap.put(JsonKey.ROLE, "TEACHER");
        jobProfileMap.put(JsonKey.JOINING_DATE,null);
        jobProfileMap.put(JsonKey.ORG_NAME,"anyOrgName");
        jobProfileMap.put(JsonKey.ORG_ID,"anyOrgId");
        List<Map<String,Object>> jobProfileList =new ArrayList<>();
        jobProfileList.add(jobProfileMap);
        Map<String,Object>reqMap=new HashMap<>();
        reqMap.put(JsonKey.JOB_PROFILE,jobProfileList);
        reqMap.put(JsonKey.CREATED_BY,"adminUser");

        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation);
        return reqObj;
    }

    private Request getJobProfileRequestWithIsDeleted(String actorOperation) {
        Request reqObj = new Request();
        Map jobProfileMap = new HashMap<>();
        jobProfileMap.put(JsonKey.ID,"anyJobId");
        jobProfileMap.put(JsonKey.IS_DELETED,true);
        jobProfileMap.put(JsonKey.JOB_NAME, "anyJobName");
        jobProfileMap.put(JsonKey.ROLE, "TEACHER");
        jobProfileMap.put(JsonKey.JOINING_DATE,null);
        jobProfileMap.put(JsonKey.ORG_NAME,"anyOrgName");
        jobProfileMap.put(JsonKey.ORG_ID,"anyOrgId");
        List<Map<String,Object>> jobProfileList =new ArrayList<>();
        jobProfileList.add(jobProfileMap);
        Map<String,Object>reqMap=new HashMap<>();
        reqMap.put(JsonKey.JOB_PROFILE,jobProfileList);
        reqMap.put(JsonKey.CREATED_BY,"adminUser");

        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation);
        return reqObj;
    }
    private Request getJobProfileRequestWithAddress(String actorOperation) {
        Request reqObj = new Request();
        Map jobProfileMap = new HashMap<>();
        Map<String,Object>addressMap=new HashMap<>();
        addressMap.put(JsonKey.ADD_TYPE, ProjectUtil.AddressType.home.getTypeName());
        addressMap.put(JsonKey.ADDRESS_LINE1,"any address");
        addressMap.put(JsonKey.CITY,"anyCity");
        addressMap.put(JsonKey.STATE,"KARANATAKA");
        addressMap.put(JsonKey.ZIPCODE,"anyZipCode");
        jobProfileMap.put(JsonKey.JOB_NAME, "anyJobName");
        jobProfileMap.put(JsonKey.ROLE, "TEACHER");
        jobProfileMap.put(JsonKey.IS_DELETED,true);
        jobProfileMap.put(JsonKey.JOINING_DATE,null);
        jobProfileMap.put(JsonKey.ORG_NAME,"anyOrgName");
        jobProfileMap.put(JsonKey.ORG_ID,"anyOrgId");
        jobProfileMap.put(JsonKey.ADDRESS,addressMap);
        List<Map<String,Object>> jobProfileList =new ArrayList<>();
        jobProfileList.add(jobProfileMap);
        Map<String,Object>reqMap=new HashMap<>();
        reqMap.put(JsonKey.JOB_PROFILE,jobProfileList);
        reqMap.put(JsonKey.CREATED_BY,"adminUser");
        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation);
        return reqObj;
    }

    private Request getJobProfileRequestWithAddressAndId(String actorOperation) {
        Request reqObj = new Request();
        Map jobProfileMap = new HashMap<>();
        Map<String,Object>addressMap=new HashMap<>();
        addressMap.put(JsonKey.ADD_TYPE, ProjectUtil.AddressType.home.getTypeName());
        addressMap.put(JsonKey.ADDRESS_LINE1,"any address");
        addressMap.put(JsonKey.CITY,"anyCity");
        addressMap.put(JsonKey.STATE,"KARANATAKA");
        addressMap.put(JsonKey.ZIPCODE,"anyZipCode");
        jobProfileMap.put(JsonKey.JOB_NAME, "anyJobName");
        jobProfileMap.put(JsonKey.ROLE, "TEACHER");
        jobProfileMap.put(JsonKey.IS_DELETED,true);
        jobProfileMap.put(JsonKey.ID,"anyJobId");
        jobProfileMap.put(JsonKey.JOINING_DATE,null);
        jobProfileMap.put(JsonKey.ORG_NAME,"anyOrgName");
        jobProfileMap.put(JsonKey.ORG_ID,"anyOrgId");
        jobProfileMap.put(JsonKey.ADDRESS,addressMap);
        List<Map<String,Object>> jobProfileList =new ArrayList<>();
        jobProfileList.add(jobProfileMap);
        Map<String,Object>reqMap=new HashMap<>();
        reqMap.put(JsonKey.JOB_PROFILE,jobProfileList);
        reqMap.put(JsonKey.CREATED_BY,"adminUser");
        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation);
        return reqObj;
    }
    private Request getJobProfileRequestWithAddressId(String actorOperation) {
        Request reqObj = new Request();
        Map jobProfileMap = new HashMap<>();
        Map<String,Object>addressMap=new HashMap<>();
        addressMap.put(JsonKey.ADD_TYPE, ProjectUtil.AddressType.home.getTypeName());
        addressMap.put(JsonKey.ADDRESS_LINE1,"any address");
        addressMap.put(JsonKey.CITY,"anyCity");
        addressMap.put(JsonKey.ID,"anyId");
        addressMap.put(JsonKey.STATE,"KARANATAKA");
        addressMap.put(JsonKey.ZIPCODE,"anyZipCode");
        jobProfileMap.put(JsonKey.JOB_NAME, "anyJobName");
        jobProfileMap.put(JsonKey.ROLE, "TEACHER");
        jobProfileMap.put(JsonKey.JOINING_DATE,null);
        jobProfileMap.put(JsonKey.ORG_NAME,"anyOrgName");
        jobProfileMap.put(JsonKey.ORG_ID,"anyOrgId");
        jobProfileMap.put(JsonKey.ADDRESS,addressMap);
        List<Map<String,Object>> jobProfileList =new ArrayList<>();
        jobProfileList.add(jobProfileMap);
        Map<String,Object>reqMap=new HashMap<>();
        reqMap.put(JsonKey.JOB_PROFILE,jobProfileList);
        reqMap.put(JsonKey.CREATED_BY,"adminUser");
        reqObj.setRequest(reqMap);
        reqObj.setOperation(actorOperation);
        return reqObj;
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

    public Response testScenario(Request reqObj) {
        TestKit probe = new TestKit(system);
        ActorRef subject = system.actorOf(props);
        subject.tell(reqObj, probe.getRef());
        Response res = probe.expectMsgClass(duration("10 second"), Response.class);
        return res;
    }
}