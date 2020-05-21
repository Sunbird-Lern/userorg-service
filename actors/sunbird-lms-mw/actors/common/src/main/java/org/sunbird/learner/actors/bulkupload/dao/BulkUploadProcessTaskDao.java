package org.sunbird.learner.actors.bulkupload.dao;

import java.util.List;
import java.util.Map;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcessTask;

/**
 * Class to provide Data access operation for the bulk_upload_process_task table.
 *
 * @author arvind.
 */
public interface BulkUploadProcessTaskDao {

  /**
   * Method to insert individual record of bulk upload file as bulk upload process task entry in
   * database.
   *
   * @param bulkUploadProcessTasks Pojo representing the table.
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String create(BulkUploadProcessTask bulkUploadProcessTasks);

  /**
   * Method to update bulk upload process task entry in database.
   *
   * @param bulkUploadProcessTasks Pojo representing the table.
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String update(BulkUploadProcessTask bulkUploadProcessTasks);

  /**
   * Method to read the record from database on basis of primary key represented by Pojo.
   *
   * @param BulkUploadProcessTask Pojo representing the table.
   * @return BulkUploadProcessTask
   */
  BulkUploadProcessTask read(BulkUploadProcessTask BulkUploadProcessTask);

  /**
   * Method to read from database on basis of primary key.Here map represents the primary
   * keys(composite key).
   *
   * @param compositeKey Composite key.
   * @return List of records.
   */
  List<BulkUploadProcessTask> readByPrimaryKeys(Map<String, Object> compositeKey);

  /**
   * Method to perform the batch insert.
   *
   * @param records List of records to be insert into database.
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String insertBatchRecord(List<BulkUploadProcessTask> records);

  /**
   * Method to perform the batch update.
   *
   * @param records List of records to be update into database.
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String updateBatchRecord(List<BulkUploadProcessTask> records);
}
