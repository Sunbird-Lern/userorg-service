package org.sunbird.learner.actors.role.group.dao.impl;

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
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.role.group.dao.RoleGroupDao;
import org.sunbird.learner.util.Util;
import org.sunbird.models.role.group.RoleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraOperationImpl.class,ServiceFactory.class,CassandraOperation.class, CassandraUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class RoleGroupDaoImplTest {
    private static final String TABLE_NAME = "role_group";
    private CassandraOperation cassandraOperation;
    private Response response;
    private RoleGroupDao roleGroupDao;


    @Before
    public void setUp() throws Exception {
        response=new Response();
        roleGroupDao=new RoleGroupDaoImpl();
        List<Map<String,Object>> roleList=new ArrayList<>();
        Map<String,Object>map=new HashMap<>();
        map.put(JsonKey.NAME,"Flag Reviewer");
        roleList.add(map);
        response.put(JsonKey.RESPONSE,roleList);
    }



    @Test
    public void testGetRoles(){
        try{
            cassandraOperation= PowerMockito.mock(CassandraOperation.class);
            PowerMockito.mockStatic(ServiceFactory.class);
            when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
            when(cassandraOperation.getAllRecords(Util.KEY_SPACE_NAME, TABLE_NAME)).thenReturn(response);
            List<RoleGroup>roleGroups=roleGroupDao.getRoleGroups();
            Assert.assertEquals("Flag Reviewer",roleGroups.get(0).getName());

        }catch (Exception e){
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

}