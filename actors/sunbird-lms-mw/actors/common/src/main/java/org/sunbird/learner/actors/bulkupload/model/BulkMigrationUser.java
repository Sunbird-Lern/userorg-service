package org.sunbird.learner.actors.bulkupload.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.datasecurity.DecryptionService;
import org.sunbird.common.models.util.datasecurity.EncryptionService;
import org.sunbird.common.responsecode.ResponseCode;

import java.sql.Timestamp;
import java.util.Map;

/**
 * @author anmolgupta
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkMigrationUser {
    private static final long serialVersionUID = 1L;
    private String id;
    private String data;
    private String failureResult;
    private String objectType;
    private String organisationId;
    private String processEndTime;
    private String processStartTime;
    private Integer retryCount;
    private Integer status;
    private String successResult;
    private String uploadedBy;
    private String uploadedDate;
    private Integer taskCount;
    private String createdBy;
    private Timestamp createdOn;
    private Timestamp lastUpdatedOn;
    private String storageDetails;
    private Map<String,String> telemetryContext;



    public BulkMigrationUser(BulkMigrationUserBuilder builder) {
        this.id = builder.id;
        this.data = builder.data;
        this.failureResult = builder.failureResult;
        this.objectType = builder.objectType;
        this.organisationId = builder.organisationId;
        this.processEndTime = builder.processEndTime;
        this.processStartTime = builder.processStartTime;
        this.retryCount = builder.retryCount;
        this.status = builder.status;
        this.successResult = builder.successResult;
        this.uploadedBy = builder.uploadedBy;
        this.uploadedDate = builder.uploadedDate;
        this.taskCount = builder.taskCount;
        this.createdBy = builder.createdBy;
        this.createdOn = builder.createdOn;
        this.lastUpdatedOn = builder.lastUpdatedOn;
        this.storageDetails = builder.storageDetails;
        this.telemetryContext=builder.telemetryContext;
    }

    public BulkMigrationUser() {

    }

    public String getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public String getFailureResult() {
        return failureResult;
    }

    public String getObjectType() {
        return objectType;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public String getProcessEndTime() {
        return processEndTime;
    }

    public String getProcessStartTime() {
        return processStartTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Integer getStatus() {
        return status;
    }

    public String getSuccessResult() {
        return successResult;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public String getUploadedDate() {
        return uploadedDate;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Timestamp getCreatedOn() {
        return createdOn;
    }

    public Timestamp getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public String getStorageDetails() {
        return storageDetails;
    }


    public Map<String, String> getTelemetryContext() {
        return telemetryContext;
    }

    public static class BulkMigrationUserBuilder {
        private EncryptionService encryptionService = org.sunbird.common.models.util.datasecurity.impl.ServiceFactory.getEncryptionServiceInstance(null);
        private String id;
        private String data;
        private String failureResult;
        private String objectType;
        private String organisationId;
        private String processEndTime;
        private String processStartTime;
        private Integer retryCount;
        private Integer status;
        private String successResult;
        private String uploadedBy;
        private String uploadedDate;
        private Integer taskCount;
        private String createdBy;
        private Timestamp createdOn;
        private Timestamp lastUpdatedOn;
        private String storageDetails;
        private Map<String,String> telemetryContext;

        public BulkMigrationUserBuilder setTelemetryContext(Map<String, String> telemetryContext) {
            this.telemetryContext = telemetryContext;
            return this;
        }

        public BulkMigrationUserBuilder(String id, String data) {
            this.id = id;
            this.data = encryptData(data);
        }

        public BulkMigrationUserBuilder setFailureResult(String failureResult) {
            this.failureResult = failureResult;
            return this;
        }

        public BulkMigrationUserBuilder setObjectType(String objectType) {
            this.objectType = objectType;
            return this;

        }

        public BulkMigrationUserBuilder setOrganisationId(String organisationId) {
            this.organisationId = organisationId;
            return this;

        }

        public BulkMigrationUserBuilder setProcessEndTime(String processEndTime) {
            this.processEndTime = processEndTime;
            return this;

        }

        public BulkMigrationUserBuilder setProcessStartTime(String processStartTime) {
            this.processStartTime = processStartTime;
            return this;

        }

        public BulkMigrationUserBuilder setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;

        }

        public BulkMigrationUserBuilder setStatus(Integer status) {
            this.status = status;
            return this;

        }

        public BulkMigrationUserBuilder setSuccessResult(String successResult) {
            this.successResult = successResult;
            return this;

        }

        public BulkMigrationUserBuilder setUploadedBy(String uploadedBy) {
            this.uploadedBy = uploadedBy;
            return this;

        }

        public BulkMigrationUserBuilder setUploadedDate(String uploadedDate) {
            this.uploadedDate = uploadedDate;
            return this;

        }

        public BulkMigrationUserBuilder setTaskCount(Integer taskCount) {
            this.taskCount = taskCount;
            return this;

        }

        public BulkMigrationUserBuilder setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;

        }

        public BulkMigrationUserBuilder setCreatedOn(Timestamp createdOn) {
            this.createdOn = createdOn;
            return this;

        }

        public BulkMigrationUserBuilder setLastUpdatedOn(Timestamp lastUpdatedOn) {
            this.lastUpdatedOn = lastUpdatedOn;
            return this;

        }

        public BulkMigrationUserBuilder setStorageDetails(String storageDetails) {
            this.storageDetails = storageDetails;
            return this;
        }

        private String encryptData(String decryptedData) {
            long encStartTime=System.currentTimeMillis();
            try {
                String encryptedData= encryptionService.encryptData(decryptedData);
                ProjectLogger.log("BulkMigrationUser:encryptData:TIME TAKEN TO ENCRYPT DATA in(ms):".concat((System.currentTimeMillis()-encStartTime)+""),LoggerEnum.INFO.name());
                return encryptedData;
            } catch (Exception e) {
                ProjectLogger.log("BulkMigrationUser:encryptData:error occurred while encrypting data", LoggerEnum.ERROR.name());
                throw new ProjectCommonException(
                        ResponseCode.SERVER_ERROR.getErrorCode(),
                        ResponseCode.userDataEncryptionError.getErrorMessage(),
                        ResponseCode.userDataEncryptionError.getResponseCode());
            }
        }

        public BulkMigrationUser build() {
            BulkMigrationUser migrationUser = new BulkMigrationUser(this);
            return migrationUser;
        }
    }

}

