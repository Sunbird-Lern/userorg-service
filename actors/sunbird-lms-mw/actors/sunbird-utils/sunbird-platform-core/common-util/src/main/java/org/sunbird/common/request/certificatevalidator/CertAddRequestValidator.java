package org.sunbird.common.request.certificatevalidator;

import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.BaseRequestValidator;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;


/**
 * this class is responsible to validate the certificate add request
 *
 * @author anmolgupta
 */
public class CertAddRequestValidator extends BaseRequestValidator {
  public static final String PDF_URL = "pdfUrl";

  private Request request;
  static List<String> mandatoryParamsList =
      Lists.newArrayList(JsonKey.ID, JsonKey.ACCESS_CODE, JsonKey.PDF_URL, JsonKey.USER_ID);

  private CertAddRequestValidator(Request request) {
    this.request = request;
  }

  /**
   * this method we should use to get the instance of the validator class
   *
   * @param request
   * @return
   */
  public static CertAddRequestValidator getInstance(Request request) {
    return new CertAddRequestValidator(request);
  }

  /** this method should be call to validate the request */
  public void validate() {
    checkMandatoryFieldsPresent(request.getRequest(), mandatoryParamsList);
    validateMandatoryJsonData();
  }

  private void validateMandatoryJsonData() {
    validatePresence();
    validateDataType();
  }

  private void validateDataType() {
    if (!(request.get(JsonKey.JSON_DATA) instanceof Map)) {
      throw new ProjectCommonException(
          ResponseCode.dataTypeError.getErrorCode(),
          MessageFormat.format(
              ResponseCode.dataTypeError.getErrorMessage(), JsonKey.JSON_DATA, "MAP"),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
  }

  private void validatePresence() {
    if (null == request.get(JsonKey.JSON_DATA)) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ResponseCode.mandatoryParamsMissing.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          JsonKey.JSON_DATA);
    }
  }

  public void validateDownlaodFileData() {
    if (StringUtils.isBlank((String) request.getRequest().get(PDF_URL))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing.getErrorCode(),
          ResponseCode.mandatoryParamsMissing.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode(),
          PDF_URL);
    }
  }
}
