package controllers.usermanagement.validator;

import org.junit.*;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

import java.util.HashMap;
import java.util.Map;

public class ShadowUserMigrateReqValidatorTest {

    private Request request;
    private ShadowUserMigrateReqValidator shadowUserMigrateReqValidator;


    @Before
    public void setUp() throws Exception {
        request=new Request();

    }

    @After
    public void tearDown() throws Exception {
        request=null;
    }

    @Test(expected= ProjectCommonException.class)
    public void testMigrateReqWithoutMandatoryParamExternalId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.ACTION,"accept");
        reqMap.put(JsonKey.CHANNEL,"TN");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }

    @Test(expected = ProjectCommonException.class)
    public void testMigrateReqWithoutMandatoryParamAction() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }
    @Test(expected = ProjectCommonException.class)
    public void testMigrateReqWithoutMandatoryParamUserId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }
    @Test(expected = ProjectCommonException.class)
    public void testMigrateReqWithInvalidValueAction() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"action_incorrect_value");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }
    @Test(expected = ProjectCommonException.class)
    public void testMigrateReqWithDiffCallerId() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abcD");
        shadowUserMigrateReqValidator.validate();
    }
    @Test()
    public void testMigrateReqSuccess() {
        Map<String,Object> reqMap=new HashMap<>();
        reqMap.put(JsonKey.USER_EXT_ID,"abc_ext_id");
        reqMap.put(JsonKey.USER_ID,"abc");
        reqMap.put(JsonKey.CHANNEL,"TN");
        reqMap.put(JsonKey.ACTION,"accept");
        request.setRequest(reqMap);
        shadowUserMigrateReqValidator=ShadowUserMigrateReqValidator.getInstance(request,"abc");
        shadowUserMigrateReqValidator.validate();
    }
}