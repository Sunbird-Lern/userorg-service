package org.sunbird.dao.bulkupload;

import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

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
