package org.sunbird.validators.orgvalidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.response.ResponseCode;
import org.sunbird.keys.JsonKey;
import org.sunbird.operations.userorg.ActorOperations;
import org.sunbird.request.Request;
import org.sunbird.common.ProjectUtil;
import play.libs.Files;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;

public class OrgRequestValidator extends BaseOrgRequestValidator {

  private static final int ERROR_CODE = ResponseCode.CLIENT_ERROR.getResponseCode();

  public void validateCreateOrgRequest(Request orgRequest) {
    validateParam(
        (String) orgRequest.getRequest().get(JsonKey.ORG_TYPE),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ORG_TYPE);
    validateParam(
        (String) orgRequest.getRequest().get(JsonKey.ORG_NAME),
        ResponseCode.mandatoryParamsMissing,
        JsonKey.ORG_NAME);
    if (!(orgRequest.getRequest().containsKey(JsonKey.IS_TENANT))
        || (orgRequest.getRequest().containsKey(JsonKey.IS_TENANT)
            && null == orgRequest.getRequest().get(JsonKey.IS_TENANT))) {
      throw new ProjectCommonException(
          ResponseCode.mandatoryParamsMissing,
          MessageFormat.format(
              ResponseCode.mandatoryParamsMissing.getErrorMessage(), JsonKey.IS_TENANT),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    }
    validateTenantOrgChannel(orgRequest);
    validateLicense(orgRequest);
    validateLocationIdOrCode(orgRequest);
  }

  private void validateLicense(Request orgRequest) {
    if (orgRequest.getRequest().containsKey(JsonKey.IS_TENANT)
        && (boolean) orgRequest.getRequest().get(JsonKey.IS_TENANT)
        && orgRequest.getRequest().containsKey(JsonKey.LICENSE)
        && StringUtils.isBlank((String) orgRequest.getRequest().get(JsonKey.LICENSE))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          MessageFormat.format(
              ResponseCode.invalidParameterValue.getErrorMessage(),
              orgRequest.getRequest().get(JsonKey.LICENSE),
              JsonKey.LICENSE),
          ERROR_CODE);
    }
  }

  public void validateUpdateOrgRequest(Request request) {
    validateOrgReference(request);
    if (request.getRequest().containsKey(JsonKey.ROOT_ORG_ID)
        && StringUtils.isEmpty((String) request.getRequest().get(JsonKey.ROOT_ORG_ID))) {
      throw new ProjectCommonException(
          ResponseCode.invalidParameterValue,
          String.format(ResponseCode.invalidParameterValue.getErrorMessage(), JsonKey.ROOT_ORG_ID),
          ERROR_CODE);
    }
    if (request.getRequest().get(JsonKey.STATUS) != null) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestParameter,
          ProjectUtil.formatMessage(
              ResponseCode.invalidRequestParameter.getErrorMessage(), JsonKey.STATUS),
          ERROR_CODE);
    }

    validateTenantOrgChannel(request);
    validateLocationIdOrCode(request);
  }

  public void validateUpdateOrgStatusRequest(Request request) {
    validateOrgReference(request);

    if (!request.getRequest().containsKey(JsonKey.STATUS)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }

    if (!(request.getRequest().get(JsonKey.STATUS) instanceof Integer)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private void validateLocationIdOrCode(Request orgRequest) {
    validateListParam(orgRequest.getRequest(), JsonKey.LOCATION_IDS, JsonKey.LOCATION_CODE);
    if (orgRequest.getRequest().get(JsonKey.LOCATION_IDS) != null
        && orgRequest.getRequest().get(JsonKey.LOCATION_CODE) != null) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.errorAttributeConflict,
          MessageFormat.format(
              ResponseCode.errorAttributeConflict.getErrorMessage(),
              JsonKey.LOCATION_CODE,
              JsonKey.LOCATION_IDS));
    }
  }

  public void validateEncryptionKeyRequest(Request reqObj, MultipartFormData body) {
    Map<String, Object> map = new HashMap<>();
    byte[] byteArray = null;
    InputStream is = null;
    try {
      if (body != null) {
        Map<String, String[]> data = body.asFormUrlEncoded();
        for (Map.Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }
        List<FilePart<Files.TemporaryFile>> filePart = body.getFiles();
        File f = filePart.get(0).getRef().path().toFile();

        is = new FileInputStream(f);
        byteArray = is.readAllBytes();

        String fileName = filePart.get(0).getFilename();

        validateFileExtension(fileName);
        validatePublicKey(byteArray);

        reqObj.getRequest().putAll(map);
        map.put(JsonKey.FILE_NAME, fileName);
        is.close();
      } else {
        throw new ProjectCommonException(
            ResponseCode.invalidRequestData,
            ResponseCode.invalidRequestData.getErrorMessage(),
            ERROR_CODE);
      }
      map.put(JsonKey.FILE, byteArray);
      HashMap<String, Object> innerMap = new HashMap<>();
      innerMap.put(JsonKey.DATA, map);
      reqObj.setRequest(innerMap);
    } catch (Exception e) {
      throw new ProjectCommonException(
          (ProjectCommonException) e,
          ActorOperations.getOperationCodeByActorOperation(reqObj.getOperation()));
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private void validatePublicKey(byte[] publicKeyBytes) {
    try {
      KeyFactory publicKeyFactory = KeyFactory.getInstance("RSA");
      String publicKeyContent = new String(publicKeyBytes);
      publicKeyContent =
          publicKeyContent
              .replaceAll("\\n", "")
              .replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "");
      X509EncodedKeySpec keySpecX509 =
          new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
      publicKeyFactory.generatePublic(keySpecX509);
    } catch (Exception se) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData,
          ResponseCode.invalidRequestData.getErrorMessage(),
          ERROR_CODE);
    }
  }

  private void validateFileExtension(String fileName) {
    String fileExtension = "";

    if (!StringUtils.isBlank(fileName)) {
      String[] split = fileName.split("\\.");
      if (split.length > 1) {
        fileExtension = split[split.length - 1];
      }
      if (StringUtils.isBlank(fileExtension) || !fileExtension.equalsIgnoreCase("pem")) {
        throw new ProjectCommonException(
            ResponseCode.invalidFileExtension,
            MessageFormat.format(ResponseCode.invalidFileExtension.getErrorMessage(), "pem"),
            ERROR_CODE);
      }
    } else {
      throw new ProjectCommonException(
          ResponseCode.invalidFileExtension,
          MessageFormat.format(ResponseCode.invalidFileExtension.getErrorMessage(), "pem"),
          ERROR_CODE);
    }
  }
}
