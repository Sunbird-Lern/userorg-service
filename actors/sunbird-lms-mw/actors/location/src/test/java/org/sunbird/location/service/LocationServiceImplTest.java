package org.sunbird.location.service;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.Constants;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.location.dao.LocationDao;
import org.sunbird.location.dao.impl.LocationDaoFactory;
import org.sunbird.location.dao.impl.LocationDaoImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocationDaoImpl.class, LocationDaoFactory.class, DataCacheHandler.class})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class LocationServiceImplTest {

  @Test
  public void testGetValidatedRelatedLocationIdAndType() {
    PowerMockito.mockStatic(LocationDaoFactory.class);
    LocationDao locationDao = mock(LocationDaoImpl.class);
    when(LocationDaoFactory.getInstance()).thenReturn(locationDao);
    PowerMockito.when(locationDao.search(Mockito.any(), Mockito.any()))
        .thenReturn(getLocationRecords());
    PowerMockito.mockStatic(DataCacheHandler.class);
    when(DataCacheHandler.getLocationOrderMap()).thenReturn(getLocationOrderMap());
    LocationService locationService = LocationServiceImpl.getInstance();
    List<String> codeList = getCodeList();
    locationService.getValidatedRelatedLocationIdAndType(codeList, new RequestContext());
  }

  public List<String> getCodeList() {
    List<String> codeList = new ArrayList<>();
    codeList.add("code1");
    return codeList;
  }

  private Response getLocationRecords() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new HashMap<>();
    map.put("type", "state");
    map.put("id", "id1");
    map.put("code", "code1");
    list.add(map);
    response.put(Constants.RESPONSE, list);
    return response;
  }

  public static Map<String, Integer> getLocationOrderMap() {
    Map<String, Integer> orderMap = new HashMap<>();
    orderMap.put("state", 1);
    orderMap.put("district", 2);
    return orderMap;
  }
}
