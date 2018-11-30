package controllers.textbook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.TextbookActorOperation;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles Textbook TOC APIs.
 *
 * @author gauraw
 */
public class TextbookController extends BaseController {

    public Promise<Result> uploadTOC(String textbookId) {
        try {
            Request request = createAndInitUploadRequest(TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue(), JsonKey.TEXTBOOK);
            request.put(JsonKey.TEXTBOOK_ID, textbookId);
            return actorResponseHandler(getActorRef(), request, timeout, null, request());
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    /**
     * @param textbookId
     * @return
     */
    public Promise<Result> getTocUrl(String textbookId) {
        try {
            return handleRequest(TextbookActorOperation.TEXTBOOK_TOC_URL.getValue(), textbookId, JsonKey.TEXTBOOK_ID);
        } catch (Exception e) {
            return Promise.<Result>pure(createCommonExceptionResponse(e, request()));
        }
    }

    public Request createAndInitUploadRequest(String operation, String objectType) throws IOException {
        ProjectLogger.log("API call for operation : " + operation);
        Request reqObj = new Request();
        Map<String, Object> map = new HashMap<>();
        Http.MultipartFormData body = request().body().asMultipartFormData();
        InputStream inputStream = null;
        if (body != null) {
            List<Http.MultipartFormData.FilePart> filePart = body.getFiles();
            if (filePart != null && !filePart.isEmpty()) {
                inputStream = new FileInputStream(filePart.get(0).getFile());
            }
        } else {
            throw new ProjectCommonException(
                    ResponseCode.invalidData.getErrorCode(),
                    ResponseCode.invalidData.getErrorMessage(),
                    ResponseCode.CLIENT_ERROR.getResponseCode());
        }
        Map<String, Object> resultMap = readAndValidateCSV(inputStream);
        System.out.println("Result Map : "+resultMap);
        reqObj.setOperation(operation);
        reqObj.setRequestId(ExecutionContext.getRequestId());
        reqObj.setEnv(getEnvironment());
        map.put(JsonKey.OBJECT_TYPE, objectType);
        map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
        map.put(JsonKey.FILE, resultMap);
        HashMap<String, Object> innerMap = new HashMap<>();
        innerMap.put(JsonKey.DATA, map);
        reqObj.setRequest(innerMap);
        return reqObj;
    }

    public static Map<String, Object> readAndValidateCSV(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        String tocMapping = ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_INPUT_MAPPING);
        Map<String, Map<String, String>> configMap = mapper.readValue(tocMapping, new TypeReference<Map<String, Object>>() {
        });
        Map<String, String> metadata = configMap.get(JsonKey.METADATA);
        Map<String, String> hierarchy = configMap.get(JsonKey.HIERARCHY);

        CSVParser csvFileParser = null;

        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();

        try {
            InputStreamReader reader = new InputStreamReader(inputStream, "UTF8");
            csvFileParser = csvFileFormat.parse(reader);
            Map<String, Integer> csvHeaders = csvFileParser.getHeaderMap();

            //TODO: Take Value of key "id" from config. (e.g: identifier)
            String mode = csvHeaders.containsKey("id") ? JsonKey.UPDATE : JsonKey.CREATE;
            result.put(JsonKey.MODE, mode);

            if (null != csvHeaders && !csvHeaders.isEmpty()) {
                metadata.values().removeIf(key -> !csvHeaders.keySet().contains(key));
                hierarchy.values().removeIf(key -> !csvHeaders.keySet().contains(key));
            } else {
                // TODO: throw exception here
            }
            List<CSVRecord> csvRecords = csvFileParser.getRecords();

            validateCSV(csvRecords);

            for (int i = 0; i < csvRecords.size(); i++) {
                CSVRecord record = csvRecords.get(i);
                HashMap<String, Object> map = new HashMap<>();
                HashMap<String, Object> recordMap = new HashMap<>();
                HashMap<String, Object> hierarchyMap = new HashMap<>();
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    recordMap.put(entry.getKey(), record.get(entry.getValue()));
                }
                for (Map.Entry<String, String> entry : hierarchy.entrySet()) {
                    hierarchyMap.put(entry.getKey(), record.get(entry.getValue()));
                }
                map.put(JsonKey.METADATA, recordMap);
                map.put(JsonKey.HIERARCHY, hierarchyMap);
                rows.add(map);
            }
            result.put(JsonKey.DATA, rows);
        } catch (Exception e) {
            // TODO: Throw Server Exception
        } finally {
            try {
                csvFileParser.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    private static void validateCSV(List<CSVRecord> records) {
        if (null == records || records.isEmpty()) {
            //TODO: throw invalid file exception
        }
        Integer allowedNumberOfRecord = Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_MAX_CSV_ROWS));
        if (records.size() > allowedNumberOfRecord) {
            // TODO: throw client error
        }
    }

}
