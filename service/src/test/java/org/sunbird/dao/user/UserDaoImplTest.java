package org.sunbird.dao.user;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.apache.pekko.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sunbird.common.ElasticSearchHelper;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.User;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  ElasticSearchRestHighImpl.class,
  ElasticSearchHelper.class,
  EsClientFactory.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserDaoImplTest {
  private static CassandraOperation cassandraOperationImpl = null;
  private static ElasticSearchService esService = null;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Response response = new Response();
    Map<String, Object> user = new HashMap<>();
    user.put(JsonKey.ID, "46545665465465");
    user.put(JsonKey.IS_DELETED, false);
    user.put(JsonKey.FIRST_NAME, "firstName");
    List<Map<String, Object>> userList = new ArrayList<>();
    userList.add(user);
    response.getResult().put(JsonKey.RESPONSE, userList);
    when(cassandraOperationImpl.getRecordById(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(response);

    PowerMockito.mockStatic(EsClientFactory.class);
    esService = mock(ElasticSearchRestHighImpl.class);
    when(EsClientFactory.getInstance(Mockito.anyString())).thenReturn(esService);

    Promise<String> promiseL = Futures.promise();
    promiseL.success("4654546-879-54656");
    when(esService.save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promiseL.future());
  }

  @Test
  public void testGetUserDetailsById() {
    UserDao userDao = UserDaoImpl.getInstance();
    Map<String, Object> user = userDao.getUserDetailsById("123-456-789", new RequestContext());
    Assert.assertNotNull(user);
  }

  @Test
  public void testGetUserById() {
    UserDao userDao = UserDaoImpl.getInstance();
    User user = userDao.getUserById("123-456-789", new RequestContext());
    Assert.assertNotNull(user);
  }

  @Test
  public void testGetEsUserById() {
    Map<String, Object> esResponse = new HashMap<>();
    esResponse.put(JsonKey.CONTENT, new ArrayList<>());
    esResponse.put(JsonKey.ID, "userId");
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);

    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    UserDao userDao = new UserDaoImpl();
    Map<String, Object> user = userDao.getEsUserById("123-456-789", new RequestContext());
    Assert.assertNotNull(user);
  }

  @Test
  public void testUpdateUserDataToES() {
    Map<String, Object> esRequest = new HashMap<>();
    Promise<Boolean> promise = Futures.promise();
    promise.success(false);
    when(esService.update(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promise.future());
    UserDao userDao = new UserDaoImpl();
    Boolean bool = userDao.updateUserDataToES("123-456-789", esRequest, new RequestContext());
    Assert.assertFalse(bool);
  }

  @Test
  public void testUpdateUserDataToESWithResponse() {
    Map<String, Object> esRequest = new HashMap<>();
    Promise<Boolean> promise = Futures.promise();
    promise.success(true);
    when(esService.update(
            Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.any()))
        .thenReturn(promise.future());
    UserDao userDao = new UserDaoImpl();
    Boolean bool = userDao.updateUserDataToES("123-456-789", esRequest, new RequestContext());
    Assert.assertTrue(bool);
  }

  @Test(expected = ProjectCommonException.class)
  public void testGetEsUserByIdWithEmptyResponse() {
    Map<String, Object> esResponse = new HashMap<>();
    Promise<Map<String, Object>> promise = Futures.promise();
    promise.success(esResponse);

    when(esService.getDataByIdentifier(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
        .thenReturn(promise.future());
    UserDao userDao = new UserDaoImpl();
    userDao.getEsUserById("123-456-789", new RequestContext());
  }

  @Test
  public void saveToEs() {
    Map<String, Object> data = new HashMap<>();
    data.put(JsonKey.ID, "546546-6787-5476");
    data.put(JsonKey.FIRST_NAME, "name");
    UserDao userDao = UserDaoImpl.getInstance();
    String response = userDao.saveUserToES("546546-6787-5476", data, new RequestContext());
    Assert.assertNotNull(response);
  }
}
