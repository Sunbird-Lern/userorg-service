package org.sunbird.dao.role;

import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.dao.role.impl.RoleDaoImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.role.Role;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperationImpl.class,
  ServiceFactory.class,
  CassandraOperation.class,
  CassandraUtil.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*"
})
public class RoleDaoImplTest {
  private static final String TABLE_NAME = "role";
  private CassandraOperation cassandraOperation;
  private Response response;
  private RoleDao roleDao;
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);

  @Before
  public void setUp() throws Exception {
    response = new Response();
    roleDao = new RoleDaoImpl();
    List<Map<String, Object>> roleList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "TEACHER");
    roleList.add(map);
    response.put(JsonKey.RESPONSE, roleList);
  }

  @Test
  public void testGetRoles() {
    try {
      cassandraOperation = PowerMockito.mock(CassandraOperation.class);
      PowerMockito.mockStatic(ServiceFactory.class);
      when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
      when(cassandraOperation.getAllRecords(KEYSPACE_NAME, TABLE_NAME, null))
          .thenReturn(response);
      List<Role> roleList = roleDao.getRoles(null);
      Assert.assertEquals("TEACHER", roleList.get(0).getName());

    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }
}
