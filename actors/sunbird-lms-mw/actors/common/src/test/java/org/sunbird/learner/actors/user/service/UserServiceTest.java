package org.sunbird.learner.actors.user.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class, ServiceFactory.class, CassandraOperation.class, CassandraUtil.class,org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class,EncryptionService.class})
@PowerMockIgnore({"javax.management.*"})
public class UserServiceTest {

    private CassandraOperation cassandraOperation;
    private EncryptionService encryptionService;
    private UserService userService=new UserService();
    private Util.DbInfo userDb = Util.dbInfoMap.get(JsonKey.USER_DB);

    @Before
    public void setUp(){
        cassandraOperation= PowerMockito.mock(CassandraOperation.class);
        PowerMockito.mockStatic(ServiceFactory.class);
        encryptionService=PowerMockito.mock(EncryptionService.class);
        PowerMockito.mockStatic(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
        when(org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(null)).thenReturn(encryptionService);
    }

    @Test(expected = Test.None.class)
    public void testCheckKeyUniquenessWhenKeyBlank(){
        try{
            when(cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), "", "")).thenReturn(new Response());
            userService.checkKeyUniqueness("","",false);
        }catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
    @Test()
    public void testCheckKeyUniquenessWhenKeyValueIsUnique(){
        try{
            Response response=new Response();
            List<Map<String, Object>> userMapList =new ArrayList<>();
            response.put(JsonKey.RESPONSE,userMapList);
            when(cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), "key", "value")).thenReturn(response);
            userService.checkKeyUniqueness("key","value",false);
        }catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
    @Test()
    public void testCheckKeyUniquenessWhenEmailValueIsNotUnique(){
        try{
            Response response=new Response();
            List<Map<String, Object>> userMapList =new ArrayList<>();
            Map<String,Object>map=new HashMap<>();
            map.put(JsonKey.NAME,"NAME");
            userMapList.add(map);
            response.put(JsonKey.RESPONSE,userMapList);
            when(cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), "email", "valueNotUnique")).thenReturn(response);
            userService.checkKeyUniqueness("email","valueNotUnique",false);
        }catch (Exception e) {
            Assert.assertEquals("Email already exists.",e.getMessage());
        }
    }
    @Test()
    public void testCheckKeyUniquenessWhenPhoneValueIsNotUnique(){
        try{
            Response response=new Response();
            List<Map<String, Object>> userMapList =new ArrayList<>();
            Map<String,Object>map=new HashMap<>();
            map.put(JsonKey.NAME,"NAME");
            userMapList.add(map);
            response.put(JsonKey.RESPONSE,userMapList);
            when(cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), "phone", "valueNotUnique")).thenReturn(response);
            userService.checkKeyUniqueness("phone","valueNotUnique",false);
        }catch (Exception e) {
            Assert.assertEquals("Phone already in use. Please provide different phone number.",e.getMessage());
        }
    }

    @Test()
    public void testCheckKeyUniquenessWhenPhoneValueIsUnique(){
        try{
            Response response=new Response();
            List<Map<String, Object>> userMapList =new ArrayList<>();
            response.put(JsonKey.RESPONSE,userMapList);
            when(encryptionService.encryptData("valueUnique")).thenReturn("valueUnique");
            when(cassandraOperation.getRecordsByIndexedProperty(userDb.getKeySpace(), userDb.getTableName(), "phone", "valueUnique")).thenReturn(response);
            userService.checkKeyUniqueness("phone","valueUnique",true);
        }catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

}