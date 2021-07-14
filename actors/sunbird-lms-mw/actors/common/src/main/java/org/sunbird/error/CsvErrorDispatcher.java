package org.sunbird.error;

import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;

/**
 * this class will dispatch the errors in the csv format
 *
 * @author anmolgupta
 */
public class CsvErrorDispatcher implements IErrorDispatcher {

  private CsvError error;

  private CsvErrorDispatcher(CsvError error) {
    this.error = error;
  }

  public static CsvErrorDispatcher getInstance(CsvError error) {
    return new CsvErrorDispatcher(error);
  }

  @Override
  public void dispatchError() {
    throw new ProjectCommonException(
        ResponseCode.invalidRequestData.getErrorCode(),
        error.getErrorsList().toString(),
        ResponseCode.CLIENT_ERROR.getResponseCode());
  }
}
