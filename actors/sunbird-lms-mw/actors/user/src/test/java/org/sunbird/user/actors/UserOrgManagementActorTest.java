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
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;

import java.util.HashMap;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Util.class,
        UserUtility.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserOrgManagementActorTest {
    Props props = Props.create(UserOrgManagementActor.class);
    ActorSystem system = ActorSystem.create("UserOrgManagementActor");

    @Before
    public void beforeEachTest() throws Exception {
        PowerMockito.mockStatic(UserUtility.class);
        PowerMockito.mockStatic(Util.class);
        PowerMockito.doNothing().when(Util.class,"registerUserToOrg",Mockito.anyMap());
        when(Util.getHashTagIdFromOrgId(Mockito.anyString())).thenReturn("hashTagId");
        PowerMockito.doNothing().when(Util.class,"upsertUserOrgData",Mockito.anyMap());
    }

    @Test
    public void testInsertUserOrgDetailsSuccess(){
        boolean result=testScenario(getUserOrgInsertRequest(),null);
        Assert.assertTrue(result);
    }
    @Test
    public void testInsertUserOrgDetailsWithInvalidOperations(){
        boolean result=testScenario(getUserOrgInsertFailureRequest(),ResponseCode.CLIENT_ERROR);
        Assert.assertTrue(result);
    }
    @Test
    public void testInsertUserOrgDetailsWithoutCallerIdSuccess() throws Exception {
        Request request=getUserOrgInsertRequest();
        request.getRequest().remove(JsonKey.CALLER_ID);
        boolean result=testScenario(request,null);
        Assert.assertTrue(result);
    }

    @Test
    public void testUpdateUserOrgDetailsSuccess() throws Exception {
        boolean result=testScenario(getUserOrgUpdateRequest(),null);
        Assert.assertTrue(result);
    }


    private Request getUserOrgInsertRequest(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.CALLER_ID,"anyCallerId");
        reqMap.put(JsonKey.ORGANISATION_ID,"anyOrgId");
        reqMap.put(JsonKey.ROOT_ORG_ID,"anyRootOrgId");
        request.setRequest(reqMap);
        request.setOperation("insertUserOrgDetails");
        return request;

    }

    private Request getUserOrgUpdateRequest(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.CALLER_ID,"anyCallerId");
        reqMap.put(JsonKey.ORGANISATION_ID,"anyOrgId");
        reqMap.put(JsonKey.ROOT_ORG_ID,"anyRootOrgId");
        request.setRequest(reqMap);
        request.setOperation("updateUserOrgDetails");
        return request;

    }

    private Request getUserOrgInsertFailureRequest(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.CALLER_ID,"anyCallerId");
        reqMap.put(JsonKey.ORGANISATION_ID,"anyOrgId");
        request.setRequest(reqMap);
        request.setOperation("invalidOperation");
        return request;

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
            System.out.println("the error"+res.getCode());
            return res.getCode().equals(errorCode.getErrorCode())
                    || res.getResponseCode() == errorCode.getResponseCode();
        }
    }

}