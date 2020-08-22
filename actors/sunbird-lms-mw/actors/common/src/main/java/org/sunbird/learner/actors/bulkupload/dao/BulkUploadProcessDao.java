package org.sunbird.learner.actors.bulkupload.dao;

import org.sunbird.common.models.response.Response;
import org.sunbird.common.request.RequestContext;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;

/** Created by arvind on 24/4/18. */
public interface BulkUploadProcessDao {

  /**
   * @param bulkUploadProcess
   * @param context
   * @return response Response
   */
  Response create(BulkUploadProcess bulkUploadProcess, RequestContext context);

  /**
   * @param bulkUploadProcess
   * @param context
   * @return response Response
   */
  Response update(BulkUploadProcess bulkUploadProcess, RequestContext context);

  /**
   * @param id
   * @param context
   * @return response Response
   */
  BulkUploadProcess read(String id, RequestContext context);
}
