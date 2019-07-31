package org.sunbird.learner.actors.qrcodedownload;

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
import org.sunbird.learner.util.ContentSearchUtil;
import org.sunbird.learner.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ActorConfig(
        tasks = {"downloadQRCodes"},
        asyncTasks = {}
)
public class QRCodeDownloadManagementActor extends BaseActor {
    private static final List<String> fields = Arrays.asList("identifier", "dialcodes");
    private static final Map<String, String> filtersHelperMap = new HashMap<String, String>() {{
        put(JsonKey.USER_IDs, JsonKey.CREATED_BY);
        put(JsonKey.STATUS, JsonKey.STATUS);
        put(JsonKey.CONTENT_TYPE, JsonKey.CONTENT_TYPE);
    }};

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String htmlHeader = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "\n" +
            "<head>\n" +
            "    <title>QR Code Generation Sheet</title>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width= device-widthcharset, initial scale = 1.0\">\n" +
            "    <link href=\"https://fonts.googleapis.com/css?family=Pacifico\" rel=\"stylesheet\">\n" +
            "    <style>\n" +
            "        body {\n" +
            "            background-color: linen;\n" +
            "        }\n" +
            "\n" +
            "        /* .ids-list, .dialcodes-list, .images-list {\n" +
            "            width: 30vw;\n" +
            "            border: \n" +
            "        } */\n" +
            "        table,\n" +
            "        th,\n" +
            "        td {\n" +
            "            border: 1px solid black;\n" +
            "            border-collapse: collapse;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "\n" +
            "<body class>\n" +
            "    <table style=\"width:100%\">\n" +
            "        <tr>\n" +
            "            <th>Course Id's</th>\n" +
            "            <th>Dialcodes</th>\n" +
            "            <th>QR Codes</th>\n" +
            "        </tr>";
    private static final String htmlFooter = "</table>\n" +
            "</body>\n" +
            "\n" +
            "</html>";

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
        Map<String, List<String>> dialCodesMap = contents.stream().filter(content -> content.get("dialcodes") != null).collect(Collectors.toMap(content -> (String) ((Map) content).get("identifier"), content -> (List) ((Map) content).get("dialcodes")));
        File file = createHtmlFile(dialCodesMap);
        Response response = uploadToAws(file);
        response.put("identifiers", requestMap.get("userIds"));
        response.put("dialcodes", new ArrayList<String>() {{
            add("YY7Q9T");
        }});
        response.put("qrCodes", new ArrayList<>());
        response.put("htmlLink", "https://sunbirddev.blob.core.windows.net/dial/Sunbird/YY7Q9T.png");
        sender().tell(response, self());
    }

    private Map<String, Object> searchCourses(Map<String, Object> requestMap, Map<String, String> headers) {
        String request = prepareSearchRequest(requestMap);
        Map<String, Object> searchResponse = ContentSearchUtil.searchContentSync(null, request, headers);
        return searchResponse;
    }

    private String prepareSearchRequest(Map<String, Object> requestMap) {
        Map<String, Object> searchRequestMap = new HashMap<String, Object>() {{
            put(JsonKey.FILTERS, requestMap.keySet().stream().filter(key -> filtersHelperMap.containsKey(key)).collect(Collectors.toMap(key -> filtersHelperMap.get(key), key -> requestMap.get(key))));
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

    private File createHtmlFile(Map<String, List<String>> dialCodeMap) {
        File file = null;
        try {
            if (MapUtils.isNotEmpty(dialCodeMap)) {
                file = new File("./index.html");
                StringBuilder htmlFile = new StringBuilder();
                htmlFile.append(htmlHeader);
                dialCodeMap.keySet().forEach(identifier -> {
                    dialCodeMap.get(identifier).forEach(dialCode -> {
                        htmlFile.append(" <tr>\n" +
                                "            <td>" + identifier + "</td>\n" +
                                "            <td>" + dialCode + "</td>\n" +
                                "            <td><img src=\"" + getQRCodeImageUrl(dialCode) + "\" alt=\"" + dialCode + "\"></td>\n" +
                                "        </tr>");
                    });
                });
                htmlFile.append(htmlFooter);
                System.out.println(htmlFile);
                FileUtils.writeStringToFile(file, htmlFile.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private String getQRCodeImageUrl(String dialCode) {
        return "https://sunbirddev.blob.core.windows.net/dial/Sunbird/YY7Q9T.png";
    }

    private Response createResponse(String url, Map<String, List<String>> dialCodes) {
        return null;
    }

    private Response uploadToAws(File file) {
        return new Response();
    }


}
