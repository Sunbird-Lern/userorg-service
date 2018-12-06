package controllers.textbook;

import static org.sunbird.common.exception.ProjectCommonException.throwClientErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.*;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.Result;

import java.io.*;
import java.net.URL;
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
        InputStream inputStream = null;

        String fileUrl = request().getQueryString("fileUrl");
        if (StringUtils.isNotBlank(fileUrl)) {
            ProjectLogger.log("Got fileUrl from path parameter: " + fileUrl, LoggerEnum.INFO.name());
            URL url = new URL(fileUrl.trim());
            inputStream = url.openStream();
        } else {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            if (body != null) {
                Map<String, String[]> data = body.asFormUrlEncoded();
                if (MapUtils.isNotEmpty(data) && data.containsKey(JsonKey.FILE_URL)) {
                    fileUrl = data.getOrDefault(JsonKey.FILE_URL, new String[] {""})[0];
                    if (StringUtils.isBlank(fileUrl) || !StringUtils.endsWith(fileUrl, ".csv")) {
                        throwClientErrorException(ResponseCode.csvError, ResponseCode.csvError.getErrorMessage());
                    }
                    URL url = new URL(fileUrl.trim());
                    inputStream = url.openStream();
                } else {
                    List<Http.MultipartFormData.FilePart> filePart = body.getFiles();
                    if (filePart == null || filePart.isEmpty()) {
                        throwClientErrorException(ResponseCode.fileNotFound, ResponseCode.fileNotFound.getErrorMessage());
                    }
                    inputStream = new FileInputStream(filePart.get(0).getFile());
                }
            } else {
                ProjectLogger.log("textbook toc upload request body is empty", LoggerEnum.INFO.name());
                throwClientErrorException(ResponseCode.invalidData, ResponseCode.invalidData.getErrorMessage());
            }
        }

        Map<String, Object> resultMap = readAndValidateCSV(inputStream);
        try {
            if (null != inputStream) {
                inputStream.close();
            }
        } catch (Exception e) {
        }
        reqObj.setOperation(operation);
        reqObj.setRequestId(ExecutionContext.getRequestId());
        reqObj.setEnv(getEnvironment());
        map.put(JsonKey.OBJECT_TYPE, objectType);
        map.put(JsonKey.CREATED_BY, ctx().flash().get(JsonKey.USER_ID));
        map.put(JsonKey.DATA, resultMap);
        reqObj.setRequest(map);
        return reqObj;
    }

    private Map<String, Object> readAndValidateCSV(InputStream inputStream) throws IOException {
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

            String mode = csvHeaders.containsKey(StringUtils.capitalize(JsonKey.IDENTIFIER)) ? JsonKey.UPDATE : JsonKey.CREATE;
            result.put(JsonKey.MODE, mode);

            if (null != csvHeaders && !csvHeaders.isEmpty()) {
                metadata.values().removeIf(key -> !csvHeaders.keySet().contains(key));
                hierarchy.values().removeIf(key -> !csvHeaders.keySet().contains(key));
            } else {
                throwClientErrorException(ResponseCode.requiredHeaderMissing, ResponseCode.requiredHeaderMissing.getErrorMessage());
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
            result.put(JsonKey.FILE_DATA, rows);
        } catch (Exception e) {
            throw new ProjectCommonException(
                    ResponseCode.errorProcessingFile.getErrorCode(),
                    ResponseCode.errorProcessingFile.getErrorMessage(),
                    ResponseCode.SERVER_ERROR.getResponseCode());
        } finally {
            try {
                if (null != csvFileParser)
                csvFileParser.close();
            } catch (IOException e) {
            }
        }
        return result;
    }

    private void validateCSV(List<CSVRecord> records) {
        if (null == records || records.isEmpty()) {
            throwClientErrorException(ResponseCode.blankCsvData, ResponseCode.blankCsvData.getErrorMessage());
        }
        Integer allowedNumberOfRecord = Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_MAX_CSV_ROWS));
        if (records.size() > allowedNumberOfRecord) {
            throwClientErrorException(ResponseCode.csvRowsExceeds, ResponseCode.csvRowsExceeds.getErrorMessage() + allowedNumberOfRecord);
        }
    }

}
