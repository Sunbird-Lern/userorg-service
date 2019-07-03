package org.sunbird.learner.actors.coursebatch.dao;

import java.util.Map;
import org.sunbird.common.models.response.Response;
import org.sunbird.models.course.batch.CourseBatch;

public interface CourseBatchDao {

  /**
   * Create course batch.
   *
   * @param courseBatch Course batch information to be created
   * @return Response containing identifier of created course batch
   */
  Response create(CourseBatch courseBatch);

  /**
   * Update course batch.
   *
   * @param courseBatchMap Course batch information to be updated
   * @return Response containing status of course batch update
   */
  Response update(Map<String, Object> courseBatchMap);

  /**
   * Read course batch for given identifier.
   *
   * @param courseBatchId Course batch identifier
   * @return Course batch information
   */
  CourseBatch readById(String courseBatchId);

  /**
   * Delete specified course batch.
   *
   * @param courseBatchId Course batch identifier
   * @return Response containing status of course batch delete
   */
  Response delete(String courseBatchId);
}
