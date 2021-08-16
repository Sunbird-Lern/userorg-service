package org.sunbird.service.role;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  CassandraOperation.class,
  ServiceFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class RoleGroupServiceTest {

  private static CassandraOperation cassandraOperationImpl = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    List<Map<String, Object>> roleList = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.NAME, "Flag_Reviewer");
    map.put(JsonKey.ID, "Flag_Reviewer");
    roleList.add(map);
    response.put(JsonKey.RESPONSE, roleList);
    when(cassandraOperationImpl.getAllRecords(Mockito.anyString(), Mockito.anyString(), Mockito.any(RequestContext.class)))
      .thenReturn(response);
  }

  @Test
  public void getRoleGroupMapTest() {
    RoleGroupService groupService = new RoleGroupService();
    Map<String,Object> group = groupService.getRoleGroupMap("Flag_Reviewer", new RequestContext());
    Assert.assertNotNull(group);
  }
}
