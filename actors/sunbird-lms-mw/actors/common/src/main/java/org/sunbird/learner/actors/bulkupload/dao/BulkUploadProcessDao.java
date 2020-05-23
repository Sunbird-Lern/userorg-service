package org.sunbird.learner.actors.bulkupload.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;

/** Created by arvind on 24/4/18. */
public interface BulkUploadProcessDao {

  /**
   * @param bulkUploadProcess
   * @return response Response
   */
  Response create(BulkUploadProcess bulkUploadProcess);

  /**
   * @param bulkUploadProcess
   * @return response Response
   */
  Response update(BulkUploadProcess bulkUploadProcess);

  /**
   * @param id
   * @return response Response
   */
  BulkUploadProcess read(String id);
}
