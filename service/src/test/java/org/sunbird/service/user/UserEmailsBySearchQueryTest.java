package org.sunbird.service.user;

import org.apache.pekko.dispatch.Futures;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.dto.SearchDTO;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.impl.UserServiceImpl;
import scala.concurrent.Promise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserDao.class,
  UserDaoImpl.class,
  ElasticSearchRestHighImpl.class,
  EsClientFactory.class,
  ElasticSearchHelper.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserEmailsBySearchQueryTest {

  private ElasticSearchRestHighImpl esSearch;

  @Before
  public void beforeTest(){
    PowerMockito.mockStatic(EsClientFactory.class);
    esSearch = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esSearch);
  }

  @Test
  public void getUserEmailsBySearchQuery() {
    Map<String, Object> esResponse = new HashMap<>();
    List<Map<String,Object>> contents = new ArrayList<>();
    Map<String , Object> content1 = new HashMap<>();
    content1.put(JsonKey.USER_ID, "1231-45654-5135");
    content1.put(JsonKey.EMAIL, "xyz@xyz.com");
    contents.add(content1);
    Map<String , Object> content2 = new HashMap<>();
    content2.put(JsonKey.USER_ID, "1232-45654-5135");
    content2.put(JsonKey.EMAIL, "xyz.com");
    contents.add(content2);
    esResponse.put(JsonKey.CONTENT, contents);
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);

    when(esSearch.search(Mockito.any(SearchDTO.class), Mockito.anyString(), Mockito.any()))
      .thenReturn(promise.future());
    UserService userService = UserServiceImpl.getInstance();
    Map<String, Object> searchQuery = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.FIRST_NAME, "firstName");
    searchQuery.put(JsonKey.FILTERS, filters);
    List<Map<String, Object>> res = userService.getUserEmailsBySearchQuery(searchQuery, new RequestContext());
    Assert.assertNotNull(res);
  }
}
