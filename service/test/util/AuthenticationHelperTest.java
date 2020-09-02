package util;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  ServiceFactory.class,
  CassandraOperationImpl.class,
  org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.class
})
@PowerMockIgnore({"javax.management.*"})
public class AuthenticationHelperTest {
  @Test
  public void verifyUserAccessToken() {
    List<Map<String, Object>> tokenMapList = new ArrayList<Map<String, Object>>();
    Map<String, Object> tokenMap = new HashMap<String, Object>();
    tokenMap.put(JsonKey.USER_ID, "123-456-789");
    tokenMapList.add(tokenMap);
    Response response = new Response();
    response.getResult().put(JsonKey.RESPONSE, tokenMapList);

    PowerMockito.mockStatic(ServiceFactory.class);
    CassandraOperationImpl cassandraOperationImpl = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperationImpl);
    Util.DbInfo userAuth = Util.dbInfoMap.get(JsonKey.USER_AUTH_DB);
    when(cassandraOperationImpl.getRecordById(
            userAuth.getKeySpace(), userAuth.getTableName(), "token"))
        .thenReturn(response);

    String userId = AuthenticationHelper.verifyUserAccessToken("token");
    assertTrue("123-456-789".equalsIgnoreCase(userId));
  }
}
