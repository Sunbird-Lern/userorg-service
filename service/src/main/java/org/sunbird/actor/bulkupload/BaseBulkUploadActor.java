package org.sunbird.actor.bulkupload;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.dao.bulkupload.BulkUploadProcessDao;
import org.sunbird.dao.bulkupload.BulkUploadProcessTaskDao;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessDaoImpl;
import org.sunbird.dao.bulkupload.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.bulkupload.BulkUploadProcess;
import org.sunbird.model.bulkupload.BulkUploadProcessTask;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;
import org.sunbird.util.ProjectUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

/**
 * Actor contains the common functionality for bulk upload.
 *
 * @author arvind.
 */
public abstract class BaseBulkUploadActor extends BaseActor {

  UserService userService = UserServiceImpl.getInstance();

  public void validateBulkUploadFields(
      String[] csvHeaderLine, String[] allowedFields, Boolean allFieldsMandatory) {
    validateBulkUploadFields(csvHeaderLine, allowedFields, allFieldsMandatory, false);
  }

  /**
   * Method to validate whether the header fields are valid.
   *
   * @param csvHeaderLine Array of string represents the header line of file.
   * @param allowedFields List of mandatory header fields.
   * @param allFieldsMandatory Boolean value . If true then all allowed fields should be in the
   *     csvHeaderline . In case of false- csvHeader could be subset of the allowed fields.
   */
  public void validateBulkUploadFields(
      String[] csvHeaderLine, String[] allowedFields, Boolean allFieldsMandatory, boolean toLower) {

    if (ArrayUtils.isEmpty(csvHeaderLine)) {
      throw new ProjectCommonException(
          ResponseCode.emptyHeaderLine,
          ResponseCode.emptyHeaderLine.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }

    if (allFieldsMandatory) {
      Arrays.stream(allowedFields)
          .forEach(
              x -> {
                if (toLower) {
                  x = x.toLowerCase();
                }
                if (!(ArrayUtils.contains(csvHeaderLine, x))) {
                  throw new ProjectCommonException(
                      ResponseCode.mandatoryParamsMissing,
                      ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                      ResponseCode.CLIENT_ERROR.getResponseCode(),
                      x);
                }
              });
    }
    Arrays.stream(csvHeaderLine)
        .forEach(
            x -> {
              if (toLower) {
                x = x.toLowerCase();
              }
              if (!(ArrayUtils.contains(allowedFields, x))) {
                throwInvalidColumnException(x, String.join(", ", allowedFields));
              }
            });
  }

  private void throwInvalidColumnException(String invalidColumn, String validColumns) {
    throw new ProjectCommonException(
        ResponseCode.invalidColumns,
        ResponseCode.invalidColumns.getErrorMessage(),
        ResponseCode.CLIENT_ERROR.getResponseCode(),
        invalidColumn,
        validColumns);
  }

  /**
   * Method to trim all the elements of string array.
   *
   * @param columnArr array of names.
   * @return String[] string array having all attribute names trimmed.
   */
  public String[] trimColumnAttributes(String[] columnArr) {
    for (int i = 0; i < columnArr.length; i++) {
      columnArr[i] = columnArr[i].trim();
    }
    return columnArr;
  }

  public BulkUploadProcess getBulkUploadProcessForFailedStatus(
      String processId, int status, Exception ex) {
    BulkUploadProcess bulkUploadProcess = new BulkUploadProcess();
    bulkUploadProcess.setId(processId);
    bulkUploadProcess.setStatus(status);
    bulkUploadProcess.setFailureResult(ex.getMessage());
    return bulkUploadProcess;
  }

  /**
   * Method to get CsvReader from byte array.
   *
   * @param byteArray represents the content of file in bytes.
   * @param seperator The delimiter to use for separating entries.
   * @param quoteChar The character to use for quoted elements.
   * @param lineNum The number of lines to skip before reading.
   * @return CsvReader.
   * @throws UnsupportedEncodingException
   */
  public CSVReader getCsvReader(byte[] byteArray, char seperator, char quoteChar, int lineNum)
      throws UnsupportedEncodingException {
    InputStreamReader inputStreamReader =
        new InputStreamReader(new ByteArrayInputStream(byteArray), StandardCharsets.UTF_8);
    CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(inputStreamReader);
    CSVReader csvReader = csvReaderBuilder.build();
    return csvReader;
  }

  public List<String[]> parseCsvFile(byte[] byteArray, String processId, RequestContext context)
      throws IOException {
    CSVReader csvReader = null;
    // Create List for holding objects
    List<String[]> rows = new ArrayList<>();
    try {
      csvReader = getCsvReader(byteArray, ',', '"', 0);
      String[] strArray;
      // Read one line at a time
      while ((strArray = csvReader.readNext()) != null) {
        if (ProjectUtil.isNotEmptyStringArray(strArray)) {
          continue;
        }
        List<String> list = new ArrayList<>();
        for (String token : strArray) {
          list.add(token);
        }
        rows.add(list.toArray(list.toArray(new String[strArray.length])));
      }
    } catch (Exception ex) {
      logger.error(context, "Exception occurred while processing csv file : ", ex);
      BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
      BulkUploadProcess bulkUploadProcess =
          getBulkUploadProcessForFailedStatus(
              processId, ProjectUtil.BulkProcessStatus.FAILED.getValue(), ex);
      bulkUploadDao.update(bulkUploadProcess, context);
      throw ex;
    } finally {
      try {
        IOUtils.closeQuietly(csvReader);
      } catch (Exception e) {
        logger.error(context, "Exception occurred while closing csv reader : ", e);
      }
    }
    return rows;
  }

  /**
   * Method to check whether number of lines in the file is permissible or not.
   *
   * @param maxLines Number represents the max allowed lines in the file including the header line
   *     as well.
   * @param actualLines Number represents the number of lines in the file including the header line
   *     as well.
   */
  public void validateFileSizeAgainstLineNumbers(int maxLines, int actualLines) {
    if (actualLines > 0 && actualLines > maxLines) {
      throw new ProjectCommonException(
          ResponseCode.dataSizeError,
          ProjectUtil.formatMessage(ResponseCode.dataSizeError.getErrorMessage(), (maxLines - 1)),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  /**
   * Method to check whether file content is not empty.
   *
   * @param csvLines list of csv lines. Here we are checking file size should greater than 1 since
   *     first line represents the header.
   */
  public void validateEmptyBulkUploadFile(List<String[]> csvLines) {

    if (null != csvLines) {
      if (csvLines.size() < 2) {
        throw new ProjectCommonException(
            ResponseCode.emptyFile,
            ResponseCode.emptyFile.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.emptyFile,
          ResponseCode.emptyFile.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  protected Integer getBatchSize(String key) {
    Integer DEFAULT_BATCH_SIZE = 10;
    Integer batchSize = DEFAULT_BATCH_SIZE;
    try {
      batchSize = Integer.parseInt(ProjectUtil.getConfigValue(key));
    } catch (Exception ex) {
      logger.error("Failed to read cassandra batch size for:" + key, ex);
    }
    return batchSize;
  }

  protected Integer validateAndParseRecords(
      byte[] fileByteArray,
      String processId,
      Map<String, Object> additionalRowFields,
      RequestContext context)
      throws IOException {
    return validateAndParseRecords(
        fileByteArray, processId, additionalRowFields, null, false, context);
  }

  protected Integer validateAndParseRecords(
      byte[] fileByteArray,
      String processId,
      Map<String, Object> additionalRowFields,
      Map<String, Object> csvColumnMap,
      boolean toLowerCase,
      RequestContext context)
      throws IOException {

    Integer sequence = 0;
    Integer count = 0;
    CSVReader csvReader = null;
    String[] csvLine;
    String[] csvColumns = null;
    Map<String, Object> record = new HashMap<>();
    List<BulkUploadProcessTask> records = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();
    try {
      csvReader = getCsvReader(fileByteArray, ',', '"', 0);
      while ((csvLine = csvReader.readNext()) != null) {
        if (ProjectUtil.isNotEmptyStringArray(csvLine)) {
          continue;
        }
        if (sequence == 0) {
          csvColumns = trimColumnAttributes(csvLine);
        } else {
          for (int j = 0; j < csvColumns.length && j < csvLine.length; j++) {
            String value = (csvLine[j].trim().length() == 0 ? null : csvLine[j].trim());
            String coulumn = toLowerCase ? csvColumns[j].toLowerCase() : csvColumns[j];
            if (csvColumnMap != null && csvColumnMap.get(coulumn) != null) {
              record.put((String) csvColumnMap.get(coulumn), value);
            } else {
              record.put(csvColumns[j], value);
            }
          }
          record.putAll(additionalRowFields);
          BulkUploadProcessTask tasks = new BulkUploadProcessTask();
          tasks.setStatus(ProjectUtil.BulkProcessStatus.NEW.getValue());
          tasks.setSequenceId(sequence);
          tasks.setProcessId(processId);
          tasks.setData(mapper.writeValueAsString(record));
          tasks.setCreatedOn(new Timestamp(System.currentTimeMillis()));
          records.add(tasks);
          count++;
          if (count >= getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE)) {
            performBatchInsert(records, context);
            records.clear();
            count = 0;
          }
          record.clear();
        }
        sequence++;
      }
      if (count != 0) {
        performBatchInsert(records, context);
        count = 0;
        records.clear();
      }
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
      BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
      BulkUploadProcess bulkUploadProcess =
          getBulkUploadProcessForFailedStatus(
              processId, ProjectUtil.BulkProcessStatus.FAILED.getValue(), ex);
      bulkUploadDao.update(bulkUploadProcess, context);
      throw ex;
    } finally {
      IOUtils.closeQuietly(csvReader);
    }
    // since one record represents the header
    return sequence - 1;
  }

  protected void performBatchInsert(List<BulkUploadProcessTask> records, RequestContext context) {
    BulkUploadProcessTaskDao bulkUploadProcessTaskDao = new BulkUploadProcessTaskDaoImpl();
    try {
      bulkUploadProcessTaskDao.insertBatchRecord(records, context);
    } catch (Exception ex) {
      logger.error(context, "Cassandra batch insert failed , performing retry logic.", ex);
      for (BulkUploadProcessTask task : records) {
        try {
          bulkUploadProcessTaskDao.create(task, context);
        } catch (Exception exception) {
          logger.error(
              context,
              "Cassandra Insert failed for BulkUploadProcessTask-"
                  + task.getProcessId()
                  + task.getSequenceId(),
              exception);
        }
      }
    }
  }

  protected void performBatchUpdate(List<BulkUploadProcessTask> records, RequestContext context) {
    BulkUploadProcessTaskDao bulkUploadProcessTaskDao = BulkUploadProcessTaskDaoImpl.getInstance();
    try {
      bulkUploadProcessTaskDao.updateBatchRecord(records, context);
    } catch (Exception ex) {
      logger.error(context, "Cassandra batch update failed , performing retry logic.", ex);
      for (BulkUploadProcessTask task : records) {
        try {
          bulkUploadProcessTaskDao.update(task, context);
        } catch (Exception exception) {
          logger.error(
              context,
              "Cassandra Update failed for BulkUploadProcessTask-"
                  + task.getProcessId()
                  + task.getSequenceId(),
              exception);
        }
      }
    }
  }

  protected void validateFileHeaderFields(
      Map<String, Object> req, String[] bulkAllowedFields, Boolean allFieldsMandatory)
      throws IOException {
    validateFileHeaderFields(req, bulkAllowedFields, allFieldsMandatory, false, null, null);
  }

  protected void validateFileHeaderFields(
      Map<String, Object> req,
      String[] bulkAllowedFields,
      Boolean allFieldsMandatory,
      boolean toLower)
      throws IOException {
    validateFileHeaderFields(req, bulkAllowedFields, allFieldsMandatory, toLower, null, null);
  }

  protected void validateFileHeaderFields(
      Map<String, Object> req,
      String[] bulkLocationAllowedFields,
      Boolean allFieldsMandatory,
      boolean toLower,
      List<String> mandatoryColumns,
      Map<String, Object> supportedColumnsMap)
      throws IOException {
    byte[] fileByteArray = (byte[]) req.get(JsonKey.FILE);

    CSVReader csvReader = null;
    Boolean flag = true;
    String[] csvLine;
    try {
      csvReader = getCsvReader(fileByteArray, ',', '"', 0);
      while (flag) {
        csvLine = csvReader.readNext();
        if (csvLine == null) {
          ProjectCommonException.throwClientErrorException(
              ResponseCode.csvFileEmpty, ResponseCode.csvFileEmpty.getErrorMessage());
        }
        if (ProjectUtil.isNotEmptyStringArray(csvLine)) {
          continue;
        }
        csvLine = trimColumnAttributes(csvLine);
        validateBulkUploadFields(csvLine, bulkLocationAllowedFields, allFieldsMandatory, toLower);
        if (mandatoryColumns != null) {
          validateMandatoryColumns(mandatoryColumns, csvLine, supportedColumnsMap);
        }
        flag = false;
      }
      csvLine = csvReader.readNext();
      if (csvLine == null) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.csvFileEmpty, ResponseCode.csvFileEmpty.getErrorMessage());
      }
    } catch (Exception ex) {
      logger.error(
          "BaseBulkUploadActor:validateFileHeaderFields: Exception = " + ex.getMessage(), ex);
      throw ex;
    } finally {
      IOUtils.closeQuietly(csvReader);
    }
  }

  private void validateMandatoryColumns(
      List<String> mandatoryColumns, String[] csvLine, Map<String, Object> supportedColumnsMap) {
    List<String> csvColumns = new ArrayList<>();
    List<String> csvMappedColumns = new ArrayList<>();
    Arrays.stream(csvLine)
        .forEach(
            x -> {
              csvColumns.add(x.toLowerCase());
              csvMappedColumns.add((String) supportedColumnsMap.get(x.toLowerCase()));
            });

    mandatoryColumns.forEach(
        column -> {
          if (!(csvMappedColumns.contains(column))) {
            throw new ProjectCommonException(
                ResponseCode.mandatoryParamsMissing,
                ResponseCode.mandatoryParamsMissing.getErrorMessage(),
                ResponseCode.CLIENT_ERROR.getResponseCode(),
                column);
          }
        });
  }

  public BulkUploadProcess handleUpload(String objectType, String createdBy, RequestContext context)
      throws IOException {
    String processId = ProjectUtil.getUniqueIdFromTimestamp(1);
    Response response = new Response();
    response.getResult().put(JsonKey.PROCESS_ID, processId);
    BulkUploadProcess bulkUploadProcess =
        getBulkUploadProcess(processId, objectType, createdBy, 0, context);
    BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
    Response res = bulkUploadDao.create(bulkUploadProcess, context);
    if (((String) res.get(JsonKey.RESPONSE)).equalsIgnoreCase(JsonKey.SUCCESS)) {
      sender().tell(response, self());
    } else {
      logger.info(
          context,
          "BaseBulkUploadActor:handleUpload: Error creating record in bulk_upload_process.");
      throw new ProjectCommonException(
          ResponseCode.serverError,
          ResponseCode.serverError.getErrorMessage(),
          ResponseCode.SERVER_ERROR.getResponseCode());
    }
    return bulkUploadProcess;
  }

  public void processBulkUpload(
      ActorRef actorRef,
      int recordCount,
      String processId,
      BulkUploadProcess bulkUploadProcess,
      String operation,
      String[] allowedFields,
      RequestContext context) {
    logger.info(
        context, "BaseBulkUploadActor: processBulkUpload called with operation = " + operation);
    BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
    bulkUploadProcess.setTaskCount(recordCount);
    bulkUploadDao.update(bulkUploadProcess, context);

    Request request = new Request();
    request.setRequestContext(context);
    request.put(JsonKey.PROCESS_ID, processId);
    request.put(JsonKey.FIELDS, allowedFields);
    request.setOperation(operation);
    actorRef.tell(request, self());
  }

  public BulkUploadProcess getBulkUploadProcess(
      String processId,
      String objectType,
      String requestedBy,
      Integer taskCount,
      RequestContext context) {
    BulkUploadProcess bulkUploadProcess = new BulkUploadProcess();
    bulkUploadProcess.setId(processId);
    bulkUploadProcess.setObjectType(objectType);
    bulkUploadProcess.setUploadedBy(requestedBy);
    bulkUploadProcess.setUploadedDate(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setCreatedBy(requestedBy);
    bulkUploadProcess.setCreatedOn(new Timestamp(Calendar.getInstance().getTime().getTime()));
    bulkUploadProcess.setProcessStartTime(ProjectUtil.getFormattedDate());
    bulkUploadProcess.setStatus(ProjectUtil.BulkProcessStatus.NEW.getValue());
    bulkUploadProcess.setTaskCount(taskCount);

    try {
      Map<String, Object> user = userService.getUserDetailsById(requestedBy, context);
      if (MapUtils.isNotEmpty(user)) {
        bulkUploadProcess.setOrganisationId((String) user.get(JsonKey.ROOT_ORG_ID));
      }
    } catch (Exception ex) {
      logger.error(context, ex.getMessage(), ex);
    }

    return bulkUploadProcess;
  }
}
