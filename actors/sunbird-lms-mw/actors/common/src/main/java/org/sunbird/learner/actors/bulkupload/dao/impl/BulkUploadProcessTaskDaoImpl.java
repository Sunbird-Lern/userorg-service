package org.sunbird.learner.actors.bulkupload.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.common.CassandraUtil;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessTaskDao;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;

/**
 * Data access implementation for BulkUploadProcessTask entity.
 *
 * @author arvind
 */
public class BulkUploadProcessTaskDaoImpl implements BulkUploadProcessTaskDao {

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  private ObjectMapper mapper = new ObjectMapper();
  private static final String KEYSPACE_NAME = "sunbird";
  private static final String TABLE_NAME = "bulk_upload_process_task";

  @Override
  public String create(BulkUploadProcessTask bulkUploadProcessTask) {
    Map<String, Object> map = mapper.convertValue(bulkUploadProcessTask, Map.class);
    cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, map);
    return JsonKey.SUCCESS;
  }

  @Override
  public String update(BulkUploadProcessTask bulkUploadProcessTask) {
    Map<String, Map<String, Object>> map = CassandraUtil.batchUpdateQuery(bulkUploadProcessTask);
    Response response =
        cassandraOperation.updateRecord(
            KEYSPACE_NAME,
            TABLE_NAME,
            map.get(JsonKey.NON_PRIMARY_KEY),
            map.get(JsonKey.PRIMARY_KEY));
    return (String) response.get(JsonKey.RESPONSE);
  }

  @Override
  public BulkUploadProcessTask read(BulkUploadProcessTask bulkUploadProcessTask) {
    Response response =
        cassandraOperation.getRecordById(
            KEYSPACE_NAME, TABLE_NAME, CassandraUtil.getPrimaryKey(bulkUploadProcessTask));
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    try {
      String jsonString = mapper.writeValueAsString((Map<String, Object>) list.get(0));
      return mapper.readValue(jsonString, BulkUploadProcessTask.class);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public List<BulkUploadProcessTask> readByPrimaryKeys(Map<String, Object> id) {
    Response response = cassandraOperation.getRecordById(KEYSPACE_NAME, TABLE_NAME, id);
    List<Map<String, Object>> list = (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    TypeReference<List<BulkUploadProcessTask>> mapType =
        new TypeReference<List<BulkUploadProcessTask>>() {};
    List<BulkUploadProcessTask> taskList = mapper.convertValue(list, mapType);
    return taskList;
  }

  @Override
  public String insertBatchRecord(List<BulkUploadProcessTask> records) {
    TypeReference<List<Map<String, Object>>> tRef =
        new TypeReference<List<Map<String, Object>>>() {};
    List<Map<String, Object>> list = mapper.convertValue(records, tRef);
    Response response = cassandraOperation.batchInsert(KEYSPACE_NAME, TABLE_NAME, list);
    return (String) response.get(JsonKey.RESPONSE);
  }

  @Override
  public String updateBatchRecord(List<BulkUploadProcessTask> records) {
    List<Map<String, Map<String, Object>>> list = new ArrayList<>();
    for (BulkUploadProcessTask bulkUploadProcessTask : records) {
      list.add(CassandraUtil.batchUpdateQuery(bulkUploadProcessTask));
    }
    Response response = cassandraOperation.batchUpdate(KEYSPACE_NAME, TABLE_NAME, list);
    return (String) response.get(JsonKey.RESPONSE);
  }
}
