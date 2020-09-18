package org.sunbird.user.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;
import org.sunbird.models.UserConsent;
import org.sunbird.models.user.User;
import org.sunbird.user.dao.UserConsentDao;
import org.sunbird.user.dao.UserDao;

import java.util.List;
import java.util.Map;

public class UserConsentDaoImpl implements UserConsentDao {
    private static final String TABLE_NAME = "user_consent";
    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private ObjectMapper mapper = new ObjectMapper();

    private static UserConsentDao consentDao = null;

    public static UserConsentDao getInstance() {
        if (consentDao == null) {
            consentDao = new UserConsentDaoImpl();
        }
        return consentDao;
    }

    @Override
    public Response updateConsent(UserConsent consent, RequestContext context){
        Map<String, Object> map = mapper.convertValue(consent, Map.class);
        return cassandraOperation.upsertRecord(Util.KEY_SPACE_NAME, TABLE_NAME, map, context);
    }

    @Override
    public UserConsent getConsent(String consentId, RequestContext context) {
        UserConsent consentObj = null;
        Response response =
                cassandraOperation.getRecordById(Util.KEY_SPACE_NAME, TABLE_NAME, consentId, context);
        List<Map<String, Object>> responseList =
                (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
        if (CollectionUtils.isNotEmpty(responseList)) {
            Map<String, Object> userMap = responseList.get(0);
            consentObj = mapper.convertValue(userMap, UserConsent.class);
        }
        return consentObj;
    }
}
