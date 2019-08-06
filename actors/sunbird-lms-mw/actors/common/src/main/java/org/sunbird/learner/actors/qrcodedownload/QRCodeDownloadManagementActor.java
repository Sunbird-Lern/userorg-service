package org.sunbird.learner.actors.qrcodedownload;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.common.util.CloudStorageUtil;

import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static org.sunbird.common.models.util.JsonKey.CLOUD_FOLDER_CONTENT;
import static org.sunbird.common.models.util.JsonKey.CONTENT_AZURE_STORAGE_CONTAINER;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;

/**
 * @Author : Rhea Fernandes
 * This actor is used to create an html file for all the qr code images that are
 * linked to courses that are created userIds given
 */
@ActorConfig(
        tasks = {"downloadQRCodes"},
        asyncTasks = {}
)
public class QRCodeDownloadManagementActor extends BaseActor {
    private static final List<String> fields = Arrays.asList("identifier", "dialcodes", "name");
    private static final Map<String, String> filtersHelperMap = new HashMap<String, String>() {{
        put(JsonKey.USER_IDs, JsonKey.CREATED_BY);
        put(JsonKey.STATUS, JsonKey.STATUS);
        put(JsonKey.CONTENT_TYPE, JsonKey.CONTENT_TYPE);
    }};

    @Override
    public void onReceive(Request request) throws Throwable {
        Util.initializeContext(request, TelemetryEnvKey.QR_CODE_DOWNLOAD);
        ExecutionContext.setRequestId(request.getRequestId());
        String requestedOperation = request.getOperation();
        switch (requestedOperation) {
            case "downloadQRCodes":
                downloadQRCodes(request);
                break;

            default:
                onReceiveUnsupportedOperation(requestedOperation);
                break;
        }
    }

    /**
     * The request must contain list of userIds (Users Ids of people who have created courses)
     * @param request
     */
    private void downloadQRCodes(Request request) {
        Map<String, String> headers = (Map<String, String>) request.getRequest().get(JsonKey.HEADER);
        Map<String, Object> requestMap = (Map<String, Object>) request.getRequest().get(JsonKey.FILTER);
        requestMap.put(JsonKey.CONTENT_TYPE, "course");
        Map<String, Object> searchResponse = searchCourses(requestMap, headers);
        List<Map<String, Object>> contents = (List<Map<String, Object>>) searchResponse.get("contents");
        if (CollectionUtils.isEmpty(contents))
            throw new ProjectCommonException(
                    ResponseCode.errorUserHasNotCreatedAnyCourse.getErrorCode(),
                    ResponseCode.errorUserHasNotCreatedAnyCourse.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        Map<String, List<String>> dialCodesMap = contents.stream()
                .filter(content -> content.get("dialcodes") != null)
                .filter(content -> content.get("name") != null)
                .collect(Collectors.toMap(content -> ((String) content.get("identifier")) + "<<<" + (String) content.get("name"), content -> (List) content.get("dialcodes")));
        File file = generateCSVFile(dialCodesMap);
        Response response = new Response();
        if (null == file)
            throw new ProjectCommonException(
                    ResponseCode.errorProcessingFile.getErrorCode(),
                    ResponseCode.errorProcessingFile.getErrorMessage(),
                    ResponseCode.SERVER_ERROR.getResponseCode());

        response = uploadToAws(file);
        response.put(JsonKey.USER_IDs, requestMap.get("userIds"));
        sender().tell(response, self());
    }

    /**
     * Search call to LP composite search engine
     * @param requestMap
     * @param headers
     * @return
     */
    private Map<String, Object> searchCourses(Map<String, Object> requestMap, Map<String, String> headers) {
        String request = prepareSearchRequest(requestMap);
        Map<String, Object> searchResponse = ContentSearchUtil.searchContentSync(null, request, headers);
        return searchResponse;
    }

    /**
     * Request Preparation for search Request
     * @param requestMap
     * @return
     */
    private String prepareSearchRequest(Map<String, Object> requestMap) {
        Map<String, Object> searchRequestMap = new HashMap<String, Object>() {{
            put(JsonKey.FILTERS, requestMap.keySet().stream()
                    .filter(key -> filtersHelperMap.containsKey(key))
                    .collect(Collectors.toMap(key -> filtersHelperMap.get(key), key -> requestMap.get(key))));
            put(JsonKey.FIELDS, fields);
            put(JsonKey.LIMIT, 200);
        }};
        Map<String, Object> request = new HashMap<String, Object>() {{
            put(JsonKey.REQUEST, searchRequestMap);
        }};
        String requestJson = null;
        try {
            requestJson = new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            ProjectLogger.log("QRCodeDownloadManagement:prepareSearchRequest: Exception occurred with error message = " + e.getMessage(), e);
        }
        return requestJson;
    }

    /**
     * Generates the CSV File for the data provided
     * @param dialCodeMap
     * @return
     */
    private File generateCSVFile(Map<String, List<String>> dialCodeMap) {
        File file = null;
        if (MapUtils.isNotEmpty(dialCodeMap)) {
            try {
                file = new File(UUID.randomUUID().toString() + ".csv");
                StringBuilder csvFile = new StringBuilder();
                csvFile.append("Course Name,Dialcodes,Image Url");
                dialCodeMap.keySet().forEach(name -> {
                    dialCodeMap.get(name).forEach(dialCode -> {
                        csvFile.append("\n");
                        csvFile.append(name.split("<<<")[1]).append(",").append(dialCode).append(",").append(getQRCodeImageUrl(dialCode));
                    });
                });
                FileUtils.writeStringToFile(file, csvFile.toString());
            } catch (IOException e) {
                ProjectLogger.log("QRCodeDownloadManagement:createHtmlFile: Exception occurred with error message = " + e.getMessage(), e);
            }
        }
        return file;
    }

    /**
     * Fetch the QR code Url for the given dialcodes
     * @param dialCode
     * @return
     */
    private String getQRCodeImageUrl(String dialCode) {
        return "https://sunbirddev.blob.core.windows.net/dial/Sunbird/YY7Q9T.png";
    }

    /**
     * Uploading the generated csv to aws
     * @param file
     * @return
     */
    private Response uploadToAws(File file) {
        String objectKey = getConfigValue(CLOUD_FOLDER_CONTENT) + separator + "textbook" + separator + "toc" + separator;
        Response response = new Response();
        try {
            if (file.isFile()) {
                objectKey += file.getName();
                String fileUrl = CloudStorageUtil.upload(CloudStorageUtil.CloudStorageType.AZURE, getConfigValue(CONTENT_AZURE_STORAGE_CONTAINER), objectKey, file.getAbsolutePath());
                if (StringUtils.isBlank(fileUrl))
                    throw new ProjectCommonException(
                            ResponseCode.errorUploadQRCodeHTMLfailed.getErrorCode(),
                            ResponseCode.errorUploadQRCodeHTMLfailed.getErrorMessage(),
                            ResponseCode.SERVER_ERROR.getResponseCode());
                response.put("qrCodeHtmlFile", fileUrl);
                response.put(JsonKey.MESSAGE, "Successfully uploaded file to cloud.");
            }
            FileUtils.deleteQuietly(file);
            return response;
        } catch (Exception e) {
            FileUtils.deleteQuietly(file);
            throw e;
        }
    }
}
