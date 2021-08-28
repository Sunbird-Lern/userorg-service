package org.sunbird.service.user;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.dao.user.UserDao;
import org.sunbird.dao.user.impl.UserDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.keys.JsonKey;
import org.sunbird.request.RequestContext;
import org.sunbird.service.user.impl.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  UserDao.class,
  UserDaoImpl.class
})
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*"
})
public class UserServiceTest {

  @Test(expected = ProjectCommonException.class)
  public void getUserDetailsFailureTest() {
    UserDao userDao = PowerMockito.mock(UserDao.class);
    PowerMockito.mockStatic(UserDaoImpl.class);
    PowerMockito.when(UserDaoImpl.getInstance()).thenReturn(userDao);
    Map<String, Object> user = new HashMap<>();
    PowerMockito.when(userDao.getUserDetailsById(Mockito.anyString(), Mockito.any(RequestContext.class))).thenReturn(user);
    UserService userService = UserServiceImpl.getInstance();
    userService.getDecryptedEmailPhoneByUserId("2123-456-8997", JsonKey.EMAIL, new RequestContext());
  }
}
