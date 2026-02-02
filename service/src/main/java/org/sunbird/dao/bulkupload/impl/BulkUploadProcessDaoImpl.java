package org.sunbird.dao.bulkupload.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.bulkupload.BulkUploadProcessDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.common.ProjectUtil;

/** Created by arvind on 24/4/18. */
public class BulkUploadProcessDaoImpl implements BulkUploadProcessDao {
  private final LoggerUtil logger = new LoggerUtil(BulkUploadProcessDaoImpl.class);
  private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private final ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
  private static final String TABLE_NAME = "bulk_upload_process";

  public static BulkUploadProcessDao bulkUploadProcessDao = null;

  public static synchronized BulkUploadProcessDao getInstance() {
    if (bulkUploadProcessDao == null) bulkUploadProcessDao = new BulkUploadProcessDaoImpl();
    return bulkUploadProcessDao;
  }

  @Override
  public Response create(BulkUploadProcess bulkUploadProcess, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(bulkUploadProcess, Map.class);
    map.put(JsonKey.CREATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    Response response = cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, map, context);
    // need to send ID along with success msg
    response.put(JsonKey.ID, map.get(JsonKey.ID));
    return response;
  }

  @Override
  public Response update(BulkUploadProcess bulkUploadProcess, RequestContext context) {
    Map<String, Object> map = mapper.convertValue(bulkUploadProcess, Map.class);
    if (map.containsKey(JsonKey.CREATED_ON)) {
      map.remove(JsonKey.CREATED_ON);
    }
    map.put(JsonKey.LAST_UPDATED_ON, new Timestamp(Calendar.getInstance().getTimeInMillis()));
    return cassandraOperation.updateRecord(KEYSPACE_NAME, TABLE_NAME, map, context);
  }

  @Override
  public BulkUploadProcess read(String id, RequestContext context) {
    Response response = cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, id, context);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    try {
      String jsonString = mapper.writeValueAsString((Map<String, Object>) list.get(0));
      return mapper.readValue(jsonString, BulkUploadProcess.class);
    } catch (IOException e) {
      logger.error(context, e.getMessage(), e);
    }
    return null;
  }
}
