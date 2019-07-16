package org.sunbird.learner.actors.bulkupload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.ProjectUtil.BulkProcessStatus;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessDao;
import org.sunbird.learner.actors.bulkupload.dao.BulkUploadProcessTaskDao;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessDaoImpl;
import org.sunbird.learner.actors.bulkupload.dao.impl.BulkUploadProcessTaskDaoImpl;
import org.sunbird.learner.actors.bulkupload.model.BulkUploadProcess;

/**
 * Actor contains the common functionality for bulk upload.
 *
 * @author arvind.
 */
public abstract class BaseBulkUploadActor extends BaseActor {

  protected BulkUploadProcessTaskDao bulkUploadProcessTaskDao = new BulkUploadProcessTaskDaoImpl();
  protected BulkUploadProcessDao bulkUploadDao = new BulkUploadProcessDaoImpl();
  protected Integer DEFAULT_BATCH_SIZE = 10;
  protected Integer CASSANDRA_BATCH_SIZE = getBatchSize(JsonKey.CASSANDRA_WRITE_BATCH_SIZE);
  protected ObjectMapper mapper = new ObjectMapper();

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
          ResponseCode.emptyHeaderLine.getErrorCode(),
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
                      ResponseCode.mandatoryParamsMissing.getErrorCode(),
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
        ResponseCode.invalidColumns.getErrorCode(),
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
    // RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
    CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(inputStreamReader);
    CSVReader csvReader = csvReaderBuilder.build();
    return csvReader;
  }

  public List<String[]> parseCsvFile(byte[] byteArray, String processId) throws IOException {
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
      ProjectLogger.log("Exception occurred while processing csv file : ", ex);
      BulkUploadProcess bulkUploadProcess =
          getBulkUploadProcessForFailedStatus(processId, BulkProcessStatus.FAILED.getValue(), ex);
      bulkUploadDao.update(bulkUploadProcess);
      throw ex;
    } finally {
      try {
        IOUtils.closeQuietly(csvReader);
      } catch (Exception e) {
        ProjectLogger.log("Exception occurred while closing csv reader : ", e);
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
          ResponseCode.dataSizeError.getErrorCode(),
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
            ResponseCode.emptyFile.getErrorCode(),
            ResponseCode.emptyFile.getErrorMessage(),
            ResponseCode.CLIENT_ERROR.getResponseCode());
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.emptyFile.getErrorCode(),
          ResponseCode.emptyFile.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  protected Integer getBatchSize(String key) {
    Integer batchSize = DEFAULT_BATCH_SIZE;
    try {
      batchSize = Integer.parseInt(ProjectUtil.getConfigValue(key));
    } catch (Exception ex) {
      ProjectLogger.log("Failed to read cassandra batch size for:" + key, ex);
    }
    return batchSize;
  }
}
