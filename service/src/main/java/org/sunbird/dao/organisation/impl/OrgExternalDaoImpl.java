package org.sunbird.dao.organisation.impl;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.organisation.OrgExternalDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.Map;

public class OrgExternalDaoImpl implements OrgExternalDao {

    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final String KEYSPACE_NAME = JsonKey.SUNBIRD;
    private static final String ORG_EXT_TABLE_NAME = JsonKey.ORG_EXT_ID_DB;

    @Override
    public Response addOrgExtId(Map<String, Object> orgExtMap, RequestContext context) {
        return cassandraOperation.insertRecord(KEYSPACE_NAME, ORG_EXT_TABLE_NAME, orgExtMap, context);
    }

    @Override
    public void deleteOrgExtId(Map<String, String> orgExtMap, RequestContext context) {
        cassandraOperation.deleteRecord(KEYSPACE_NAME, ORG_EXT_TABLE_NAME, orgExtMap, context);
    }
}
