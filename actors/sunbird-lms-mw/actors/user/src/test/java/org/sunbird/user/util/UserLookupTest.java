package org.sunbird.user.util;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.dispatch.Futures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.ElasticSearchRestHighImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.inf.ElasticSearchService;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.models.util.datasecurity.impl.DefaultEncryptionServivceImpl;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.dto.SearchDTO;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.DataCacheHandler;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.models.user.UserDeclareEntity;
import scala.concurrent.Future;
import scala.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceFactory.class,
        CassandraOperationImpl.class,
        DataCacheHandler.class
})
@PowerMockIgnore({"javax.management.*"})
public class UserLookupTest {
    private static Response response;
    public static CassandraOperation cassandraOperation;

    public void beforeEachTest() {
        PowerMockito.mockStatic(DataCacheHandler.class);
        Map<String, String> settingMap = new HashMap<String, String>();
        settingMap.put(JsonKey.PHONE_UNIQUE, "True");
        settingMap.put(JsonKey.EMAIL_UNIQUE, "True");
        when(DataCacheHandler.getConfigSettings()).thenReturn(settingMap);

        PowerMockito.mockStatic(ServiceFactory.class);
        cassandraOperation = mock(CassandraOperationImpl.class);
        when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);

    }

    @Test
    public void checkPhoneUniquenessExist() throws Exception {
        beforeEachTest();

        when(cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), null))
                .thenReturn(getRecordsByCompositeKeyResponse());

        User user = new User();
        user.setPhone("9663890400");
        boolean response = false;
        try {
            new UserLookUp().checkPhoneUniqueness(user, "create", null);
            response = true;
        } catch (ProjectCommonException e) {
            assertEquals(e.getResponseCode(), 400);
        }
        assertFalse(response);
    }

    @Test
    public void checkPhoneExist() {
        beforeEachTest();

        when(cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), null))
                .thenReturn(getRecordsByCompositeKeyResponse());

        boolean response = false;
        try {
            new UserLookUp().checkPhoneUniqueness("9663890400", null);
            response = true;
        } catch (ProjectCommonException e) {
            assertEquals(e.getResponseCode(), 400);
        }
        assertFalse(response);
    }

    @Test
    public void checkEmailExist() throws Exception{
        beforeEachTest();

        when(cassandraOperation.getRecordsByCompositeKey(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyObject(), null))
                .thenReturn(getRecordsByCompositeKeyResponse());
        boolean response = false;
        try {
            new UserLookUp().checkEmailUniqueness("test@test.com", null);
            response = true;
        } catch (ProjectCommonException e) {
            assertEquals(e.getResponseCode(), 400);
        }
        assertFalse(response);
    }

    public Response getRecordsByCompositeKeyResponse(){
        Response response1 = new Response();
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        result.put(JsonKey.ID, "123-456-789");
        responseList.add(result);
        response1.getResult().put(JsonKey.RESPONSE, responseList);

        return response1;
    }
}
