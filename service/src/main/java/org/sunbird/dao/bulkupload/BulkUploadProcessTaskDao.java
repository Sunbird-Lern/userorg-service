package org.sunbird.dao.bulkupload;

import java.util.List;
import java.util.Map;

import org.sunbird.model.bulkupload.BulkUploadProcessTask;
import org.sunbird.request.RequestContext;

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
   * @param context
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String create(BulkUploadProcessTask bulkUploadProcessTasks, RequestContext context);

  /**
   * Method to update bulk upload process task entry in database.
   *
   * @param bulkUploadProcessTasks Pojo representing the table.
   * @param context
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String update(BulkUploadProcessTask bulkUploadProcessTasks, RequestContext context);

  /**
   * Method to read the record from database on basis of primary key represented by Pojo.
   *
   * @param BulkUploadProcessTask Pojo representing the table.
   * @param context
   * @return BulkUploadProcessTask
   */
  BulkUploadProcessTask read(BulkUploadProcessTask BulkUploadProcessTask, RequestContext context);

  /**
   * Method to read from database on basis of primary key.Here map represents the primary
   * keys(composite key).
   *
   * @param compositeKey Composite key.
   * @param context
   * @return List of records.
   */
  List<BulkUploadProcessTask> readByPrimaryKeys(
      Map<String, Object> compositeKey, RequestContext context);

  /**
   * Method to perform the batch insert.
   *
   * @param records List of records to be insert into database.
   * @param context
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String insertBatchRecord(List<BulkUploadProcessTask> records, RequestContext context);

  /**
   * Method to perform the batch update.
   *
   * @param records List of records to be update into database.
   * @param context
   * @return String success if record inserted successfully. Response string got from underlying
   *     database implementation layer.
   */
  String updateBatchRecord(List<BulkUploadProcessTask> records, RequestContext context);
}
