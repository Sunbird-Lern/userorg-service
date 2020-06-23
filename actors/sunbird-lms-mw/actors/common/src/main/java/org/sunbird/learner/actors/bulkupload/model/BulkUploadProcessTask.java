package org.sunbird.learner.actors.bulkupload.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.sql.Timestamp;
import org.sunbird.cassandraannotation.ClusteringKey;
import org.sunbird.cassandraannotation.PartitioningKey;

/**
 * Model class to represent the bulk_upload_process_tasks column family.
 *
 * @author arvind.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class BulkUploadProcessTask implements Serializable {

  private static final long serialVersionUID = 1L;

  @PartitioningKey() private String processId;
  @ClusteringKey() private Integer sequenceId;

  private String data;
  private String failureResult;
  private String successResult;
  private Timestamp createdOn;
  private Timestamp lastUpdatedOn;
  private Integer iterationId = new Integer(0);
  private Integer status;

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getFailureResult() {
    return failureResult;
  }

  public void setFailureResult(String failureResult) {
    this.failureResult = failureResult;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  public String getSuccessResult() {
    return successResult;
  }

  public void setSuccessResult(String successResult) {
    this.successResult = successResult;
  }

  public Integer getIterationId() {
    return iterationId;
  }

  public void setIterationId(Integer iterationId) {
    this.iterationId = iterationId;
  }

  public Integer getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(Integer sequenceId) {
    this.sequenceId = sequenceId;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Timestamp getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(Timestamp createdOn) {
    this.createdOn = createdOn;
  }

  public Timestamp getLastUpdatedOn() {
    return lastUpdatedOn;
  }

  public void setLastUpdatedOn(Timestamp lastUpdatedOn) {
    this.lastUpdatedOn = lastUpdatedOn;
  }
}
