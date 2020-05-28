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
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;

import java.util.HashMap;
import java.util.Map;

import static akka.testkit.JavaTestKit.duration;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
        UserDao.class,
        UserDaoImpl.class,
        Util.class,
        UserUtility.class
})
@PowerMockIgnore({"javax.management.*"})
public class ResetPasswordActorTest {

    private UserDao userDao;
    Props props = Props.create(ResetPasswordActor.class);
    ActorSystem system = ActorSystem.create("ResetPasswordActor");


    @Before
    public void beforeEachTest(){
        userDao=PowerMockito.mock(UserDao.class);
        PowerMockito.mockStatic(UserDaoImpl.class);
        when(UserDaoImpl.getInstance()).thenReturn(userDao);
        PowerMockito.mockStatic(UserUtility.class);
        PowerMockito.mockStatic(Util.class);
        when(Util.getUserRequiredActionLink(Mockito.anyMap(),Mockito.anyBoolean())).thenReturn("/url/password");
        when(Util.getSunbirdLoginUrl()).thenReturn("/resource/url");
        when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
        when(userDao.getUserById("ValidUserId")).thenReturn(getValidUserResponse());

    }

    @Test
    public void testResetPasswordWithInvalidUserIdFailure(){
        when(userDao.getUserById("invalidUserId")).thenReturn(null);
        boolean result=testScenario(getInvalidRequest(),ResponseCode.userNotFound);
        Assert.assertTrue(result);

    }

    @Test
    public void testResetPasswordWithKeyPhoneSuccess() throws Exception {
        when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
        boolean result=testScenario(getValidRequestWithKeyPhone(),null);
        Assert.assertTrue(result);
    }
    @Test
    public void testResetPasswordWithKeyEmailSuccess() throws Exception {
        when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
        boolean result=testScenario(getValidRequestWithKeyEmail(),null);
        Assert.assertTrue(result);
    }
    @Test
    public void testResetPasswordWithKeyPrevUsedEmailSuccess() throws Exception {
        when(UserUtility.decryptUserData(Mockito.anyMap())).thenReturn(getUserDbMap());
        boolean result=testScenario(getValidRequestWithKeyPrevUsedEmail(),null);
        Assert.assertTrue(result);
    }
    @Test
    public void testResetPasswordWithKeyPrevUsedPhoneSuccess() throws Exception {
        boolean result=testScenario(getValidRequestWithKeyPrevUsedPhone(),null);
        Assert.assertTrue(result);
    }


    private Request getInvalidRequest(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"invalidUserId");
        reqMap.put(JsonKey.TYPE, "phone");
        request.setRequest(reqMap);
        request.setOperation("resetPassword");
        return request;
    }
    private Request getValidRequestWithKeyPhone(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"ValidUserId");
        reqMap.put(JsonKey.TYPE, "phone");
        request.setRequest(reqMap);
        request.setOperation("resetPassword");
        return request;
    }
    private Request getValidRequestWithKeyPrevUsedPhone(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"ValidUserId");
        reqMap.put(JsonKey.TYPE, "prevUsedPhone");
        request.setRequest(reqMap);
        request.setOperation("resetPassword");
        return request;
    }
    private Request getValidRequestWithKeyEmail(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"ValidUserId");
        reqMap.put(JsonKey.TYPE, "email");
        request.setRequest(reqMap);
        request.setOperation("resetPassword");
        return request;
    }
    private Request getValidRequestWithKeyPrevUsedEmail(){
        Request request=new Request();
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"ValidUserId");
        reqMap.put(JsonKey.TYPE, "prevUsedEmail");
        request.setRequest(reqMap);
        request.setOperation("resetPassword");
        return request;
    }

    private User getValidUserResponse(){
        User user=new User();
        user.setId("ValidUserId");
        user.setEmail("anyEmail@gmail.com");
        user.setChannel("TN");
        user.setPhone("9876543210");
        user.setMaskedEmail("any****@gmail.com");
        user.setMaskedPhone("987*****0");
        user.setIsDeleted(false);
        user.setFlagsValue(3);
        user.setUserType("TEACHER");
        user.setUserId("ValidUserId");
        user.setFirstName("Demo Name");
        user.setUserName("validUserName");
        return user;
    }


    private Map<String,Object> getUserDbMap(){
        Map<String,Object>userDbMap=new HashMap<>();
        userDbMap.put(JsonKey.SET_PASSWORD_LINK,"/password/link/url");
        userDbMap.put(JsonKey.USERNAME,"validUserName");
        userDbMap.put(JsonKey.CHANNEL,"TN");
        userDbMap.put(JsonKey.EMAIL,"anyEmail@gmail.com");
        userDbMap.put(JsonKey.PHONE,"9876543210");
        userDbMap.put(JsonKey.FLAGS_VALUE,3);
        userDbMap.put(JsonKey.USER_TYPE,"TEACHER");
        userDbMap.put(JsonKey.MASKED_PHONE,"987*****0");
        userDbMap.put(JsonKey.USER_ID,"ValidUserId");
        userDbMap.put(JsonKey.ID,"ValidUserId");
        userDbMap.put(JsonKey.FIRST_NAME,"Demo Name");
        userDbMap.put(JsonKey.REDIRECT_URI,"/resource/url");
        userDbMap.put(JsonKey.IS_DELETED,false);
        return userDbMap;
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

}