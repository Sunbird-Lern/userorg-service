package org.sunbird.learner.actors.textbook;

import static java.io.File.separator;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sunbird.common.exception.ProjectCommonException.throwClientErrorException;
import static org.sunbird.common.exception.ProjectCommonException.throwServerErrorException;
import static org.sunbird.common.models.util.JsonKey.CHILDREN;
import static org.sunbird.common.models.util.JsonKey.CONTENT;
import static org.sunbird.common.models.util.JsonKey.CONTENT_TYPE;
import static org.sunbird.common.models.util.JsonKey.DOWNLOAD;
import static org.sunbird.common.models.util.JsonKey.HIERARCHY;
import static org.sunbird.common.models.util.JsonKey.MIME_TYPE;
import static org.sunbird.common.models.util.JsonKey.NAME;
import static org.sunbird.common.models.util.JsonKey.SUNBIRD_CONTENT_GET_HIERARCHY_API;
import static org.sunbird.common.models.util.JsonKey.TEXTBOOK;
import static org.sunbird.common.models.util.JsonKey.TEXTBOOK_ID;
import static org.sunbird.common.models.util.JsonKey.TEXTBOOK_TOC_ALLOWED_CONTNET_TYPES;
import static org.sunbird.common.models.util.JsonKey.TEXTBOOK_TOC_ALLOWED_MIMETYPE;
import static org.sunbird.common.models.util.JsonKey.TEXTBOOK_TOC_CSV_TTL;
import static org.sunbird.common.models.util.JsonKey.TOC_URL;
import static org.sunbird.common.models.util.JsonKey.TTL;
import static org.sunbird.common.models.util.JsonKey.VERSION_KEY;
import static org.sunbird.common.models.util.LoggerEnum.ERROR;
import static org.sunbird.common.models.util.LoggerEnum.INFO;
import static org.sunbird.common.models.util.ProjectLogger.log;
import static org.sunbird.common.models.util.ProjectUtil.getConfigValue;
import static org.sunbird.common.models.util.Slug.makeSlug;
import static org.sunbird.common.responsecode.ResponseCode.SERVER_ERROR;
import static org.sunbird.common.responsecode.ResponseCode.invalidTextbook;
import static org.sunbird.common.responsecode.ResponseCode.noChildrenExists;
import static org.sunbird.common.responsecode.ResponseCode.textbookChildrenExist;
import static org.sunbird.content.textbook.FileExtension.Extension.CSV;
import static org.sunbird.content.textbook.TextBookTocUploader.TEXTBOOK_TOC_FOLDER;
import static org.sunbird.content.util.ContentCloudStore.getUri;
import static org.sunbird.content.util.TextBookTocUtil.getObjectFrom;
import static org.sunbird.content.util.TextBookTocUtil.readContent;
import static org.sunbird.content.util.TextBookTocUtil.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.content.textbook.FileExtension;
import org.sunbird.content.textbook.TextBookTocUploader;
import org.sunbird.content.util.TextBookTocUtil;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;

@ActorConfig(
  tasks = {"textbookTocUpload", "textbookTocUrl", "textbookTocUpdate"},
  asyncTasks = {}
)
public class TextbookTocActor extends BaseActor {

  private SSOManager ssoManager = SSOServiceFactory.getInstance();
  private Instant startTime = null;
  private Map<String, Object> frameCategories = null;
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    startTime = Instant.now();
    Map<String, Object> outputMapping =
        getObjectFrom(getConfigValue(JsonKey.TEXTBOOK_TOC_OUTPUT_MAPPING), Map.class);
    frameCategories = (Map<String, Object>) outputMapping.get("frameworkCategories");
    if (request
        .getOperation()
        .equalsIgnoreCase(TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue())) {
      upload(request);
    } else if (request
        .getOperation()
        .equalsIgnoreCase(TextbookActorOperation.TEXTBOOK_TOC_URL.getValue())) {
      getTocUrl(request);
    } else {
      onReceiveUnsupportedOperation(request.getOperation());
    }
  }

  @SuppressWarnings("unchecked")
  private void upload(Request request) throws Exception {
    byte[] byteArray = (byte[]) request.getRequest().get(JsonKey.DATA);
    ProjectLogger.log("Sized:TextbookTocActor:upload size of request " + byteArray.length, INFO);
    InputStream inputStream = new ByteArrayInputStream(byteArray);
    Map<String, Object> resultMap = readAndValidateCSV(inputStream);
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for read and validate csv: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    Set<String> dialCodes = (Set<String>) resultMap.get(JsonKey.DIAL_CODES);
    resultMap.remove(JsonKey.DIAL_CODES);
    Map<String, List<String>> reqDialCodeIdentifierMap =
        (Map<String, List<String>>) resultMap.get(JsonKey.DIAL_CODE_IDENTIFIER_MAP);
    resultMap.remove(JsonKey.DIAL_CODE_IDENTIFIER_MAP);
    Set<String> topics = (Set<String>) resultMap.get(JsonKey.TOPICS);
    resultMap.remove(JsonKey.TOPICS);
    Map<Integer, List<String>> rowNumVsContentIdsMap =
        (Map<Integer, List<String>>) resultMap.get(JsonKey.LINKED_CONTENT);
    resultMap.remove(JsonKey.LINKED_CONTENT);
    validateLinkedContents(rowNumVsContentIdsMap);
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for validate linked content: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    resultMap.put(JsonKey.LINKED_CONTENT, false);
    for (Entry<Integer, List<String>> entry : rowNumVsContentIdsMap.entrySet()) {
      if (CollectionUtils.isNotEmpty(entry.getValue())) {
        resultMap.put(JsonKey.LINKED_CONTENT, true);
        break;
      }
    }
    String tbId = (String) request.get(TEXTBOOK_ID);

    Map<String, Object> hierarchy = getHierarchy(tbId);
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for get hirearchy data: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    validateTopics(topics, (String) hierarchy.get(JsonKey.FRAMEWORK));
    validateDialCodesWithReservedDialCodes(dialCodes, hierarchy);
    checkDialCodeUniquenessInTextBookHierarchy(reqDialCodeIdentifierMap, hierarchy);
    request.getRequest().put(JsonKey.DATA, resultMap);
    String mode = ((Map<String, Object>) request.get(JsonKey.DATA)).get(JsonKey.MODE).toString();
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for validate topic and dial codes: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    validateRequest(request, mode, hierarchy);
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for validate request: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    Response response = new Response();
    if (StringUtils.equalsIgnoreCase(mode, JsonKey.CREATE)) {
      response = createTextbook(request, hierarchy);
    } else if (StringUtils.equalsIgnoreCase(mode, JsonKey.UPDATE)) {
      response = updateTextbook(request, hierarchy);
    } else {
      unSupportedMessage();
    }
    ProjectLogger.log(
        "Timed:TextbookTocActor:upload duration for textbook "
            + mode
            + " :"
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    sender().tell(response, sender());
  }

  private void validateLinkedContents(Map<Integer, List<String>> rowNumVsContentIdsMap)
      throws Exception {
    // rowNumVsContentIdsMap convert to contentIdVsrowListMap
    if (MapUtils.isNotEmpty(rowNumVsContentIdsMap)) {
      Map<String, List<Integer>> contentIdVsRowNumMap = new HashMap<>();
      rowNumVsContentIdsMap.forEach(
          (k, v) -> {
            v.forEach(
                contentId -> {
                  if (contentIdVsRowNumMap.containsKey(contentId)) {
                    contentIdVsRowNumMap.get(contentId).add(k);
                  } else {
                    List<Integer> rowNumList = new ArrayList<>();
                    rowNumList.add(k);
                    contentIdVsRowNumMap.put(contentId, rowNumList);
                  }
                });
          });
      callSearchApiForContentIdsValidation(contentIdVsRowNumMap);
    }
  }

  @SuppressWarnings("unchecked")
  private void callSearchApiForContentIdsValidation(Map<String, List<Integer>> contentIdVsRowNumMap)
      throws Exception {
    if (MapUtils.isEmpty(contentIdVsRowNumMap)) {
      ProjectLogger.log(
          "TextbookTocActor:callSearchApiForContentIdsValidation : Content id map is Empty.",
          LoggerEnum.INFO.name());
      return;
    }
    List<String> contentIds = new ArrayList<>();
    contentIds.addAll(contentIdVsRowNumMap.keySet());
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> request = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.STATUS, "Live");
    filters.put(JsonKey.IDENTIFIER, contentIds);
    request.put(JsonKey.FILTERS, filters);
    requestMap.put(JsonKey.REQUEST, request);
    List<String> fields = new ArrayList<>();
    fields.add(JsonKey.IDENTIFIER);
    request.put(JsonKey.FIELDS, fields);
    request.put(JsonKey.LIMIT, contentIds.size());
    String requestUrl =
        getConfigValue(JsonKey.SUNBIRD_CS_BASE_URL)
            + getConfigValue(JsonKey.SUNBIRD_CONTENT_SEARCH_URL);
    HttpResponse<String> updateResponse = null;
    ProjectLogger.log(
        "TextbookTocActor:callSearchApiForContentIdsValidation : requestUrl=" + requestUrl,
        LoggerEnum.INFO.name());
    try {
      updateResponse =
          Unirest.post(requestUrl)
              .headers(getDefaultHeaders())
              .body(mapper.writeValueAsString(requestMap))
              .asString();
      if (null != updateResponse) {
        Response response = mapper.readValue(updateResponse.getBody(), Response.class);
        ProjectLogger.log(
            "TextbookTocActor:callSearchApiForContentIdsValidation : response.getResponseCode().getResponseCode() : "
                + response.getResponseCode().getResponseCode(),
            LoggerEnum.INFO.name());
        if (response.getResponseCode().getResponseCode() == ResponseCode.OK.getResponseCode()) {
          Map<String, Object> result = response.getResult();
          Set<String> searchedContentIds = new HashSet<>();
          if (MapUtils.isNotEmpty(result)) {
            int count = (int) result.get(JsonKey.COUNT);
            if (0 == count) {
              ProjectLogger.log(
                  "TextbookTocActor:callSearchApiForContentIdsValidation : Content id count in response is zero.",
                  LoggerEnum.INFO.name());
              String errorMsg = prepareErrorMsg(contentIdVsRowNumMap, searchedContentIds);
              ProjectCommonException.throwClientErrorException(
                  ResponseCode.errorInvalidLinkedContentId, errorMsg);
            }
            List<Map<String, Object>> content =
                (List<Map<String, Object>>) result.get(JsonKey.CONTENT);
            if (CollectionUtils.isNotEmpty(content)) {
              content.forEach(
                  contentMap -> {
                    searchedContentIds.add((String) contentMap.get(JsonKey.IDENTIFIER));
                  });
              if (searchedContentIds.size() != contentIds.size()) {
                String errorMsg = prepareErrorMsg(contentIdVsRowNumMap, searchedContentIds);
                ProjectCommonException.throwClientErrorException(
                    ResponseCode.errorInvalidLinkedContentId, errorMsg);
              }
            } else {
              ProjectLogger.log(
                  "TextbookTocActor:callSearchApiForContentIdsValidation : Content is Empty.",
                  LoggerEnum.INFO.name());
              throwCompositeSearchFailureError();
            }
          }
        } else {
          ProjectLogger.log(
              "TextbookTocActor:callSearchApiForContentIdsValidation : response.getResponseCode().getResponseCode() is not 200",
              LoggerEnum.INFO.name());
          throwCompositeSearchFailureError();
        }
      } else {
        ProjectLogger.log(
            "TextbookTocActor:callSearchApiForContentIdsValidation : update response is null.",
            LoggerEnum.INFO.name());
        throwCompositeSearchFailureError();
      }
    } catch (Exception e) {
      ProjectLogger.log(
          "TextbookTocActor:validateLinkedContents : Error occurred with message " + e.getMessage(),
          e);
      if (e instanceof ProjectCommonException) {
        throw e;
      }
      throwCompositeSearchFailureError();
    }
  }

  private String prepareErrorMsg(
      Map<String, List<Integer>> contentIdVsRowNumMap, Set<String> searchedContentIds) {
    StringBuilder errorMsg = new StringBuilder();
    contentIdVsRowNumMap
        .keySet()
        .forEach(
            contentId -> {
              if (!searchedContentIds.contains(contentId)) {
                String message =
                    MessageFormat.format(
                        ResponseCode.errorInvalidLinkedContentId.getErrorMessage(),
                        contentId,
                        contentIdVsRowNumMap.get(contentId));
                errorMsg.append(message);
                errorMsg.append(" ");
              }
            });
    return errorMsg.toString();
  }

  private void throwCompositeSearchFailureError() {
    ProjectCommonException.throwServerErrorException(
        ResponseCode.customServerError, "Exception occurred while validating linked content.");
  }

  @SuppressWarnings("unchecked")
  private void checkDialCodeUniquenessInTextBookHierarchy(
      Map<String, List<String>> reqDialCodesIdentifierMap, Map<String, Object> contentHierarchy) {
    if (MapUtils.isNotEmpty(reqDialCodesIdentifierMap)) {
      Map<String, List<String>> hierarchyDialCodeIdentifierMap = new HashMap<>();
      List<String> contentDialCodes = (List<String>) contentHierarchy.get(JsonKey.DIAL_CODES);
      if (CollectionUtils.isNotEmpty(contentDialCodes)) {
        hierarchyDialCodeIdentifierMap.put(
            (String) contentHierarchy.get(JsonKey.IDENTIFIER), contentDialCodes);
      }

      List<Map<String, Object>> children =
          (List<Map<String, Object>>) contentHierarchy.get(JsonKey.CHILDREN);
      hierarchyDialCodeIdentifierMap.putAll(getDialCodeIdentifierMap(children));

      Map<String, String> reqDialCodeMap =
          convertDialcodeToIdentifierMap(reqDialCodesIdentifierMap);
      if (MapUtils.isNotEmpty(hierarchyDialCodeIdentifierMap)) {
        Map<String, String> hierarchyDialCodeMap =
            convertDialcodeToIdentifierMap(hierarchyDialCodeIdentifierMap);
        validateReqDialCode(reqDialCodeMap, hierarchyDialCodeMap);
      }
    }
  }

  private void validateReqDialCode(
      Map<String, String> reqDialCodeMap, Map<String, String> hierarchyDialCodeMap) {
    reqDialCodeMap.forEach(
        (k, v) -> {
          if (StringUtils.isNotBlank(hierarchyDialCodeMap.get(k))
              && !v.equalsIgnoreCase(hierarchyDialCodeMap.get(k))) {
            throwClientErrorException(
                ResponseCode.errorDialCodeAlreadyAssociated,
                MessageFormat.format(
                    ResponseCode.errorDialCodeAlreadyAssociated.getErrorMessage(),
                    k,
                    hierarchyDialCodeMap.get(k)));
          }
        });
  }

  private Map<String, String> convertDialcodeToIdentifierMap(
      Map<String, List<String>> identifierDialCodeMap) {
    Map<String, String> dialCodeIdentifierMap = new HashMap<>();
    if (MapUtils.isNotEmpty(identifierDialCodeMap)) {
      identifierDialCodeMap.forEach(
          (k, v) -> {
            v.forEach(
                dialcode -> {
                  if (dialCodeIdentifierMap.containsKey(dialcode)) {
                    throwClientErrorException(
                        ResponseCode.errorDialCodeDuplicateEntry,
                        MessageFormat.format(
                            ResponseCode.errorDialCodeDuplicateEntry.getErrorMessage(), k, v));
                  } else {
                    dialCodeIdentifierMap.put(dialcode, k);
                  }
                });
          });
    }
    return dialCodeIdentifierMap;
  }

  @SuppressWarnings("unchecked")
  public Map<String, List<String>> getDialCodeIdentifierMap(List<Map<String, Object>> children) {
    Map<String, List<String>> hierarchyDialCodeIdentifierMap = new HashMap<>();
    for (Map<String, Object> child : children) {
      if (CollectionUtils.isNotEmpty((List<String>) child.get(JsonKey.DIAL_CODES))) {
        hierarchyDialCodeIdentifierMap.put(
            (String) child.get(JsonKey.IDENTIFIER), (List<String>) child.get(JsonKey.DIAL_CODES));
      }
      if (CollectionUtils.isNotEmpty((List<Map<String, Object>>) child.get(JsonKey.CHILDREN))) {
        hierarchyDialCodeIdentifierMap.putAll(
            getDialCodeIdentifierMap((List<Map<String, Object>>) child.get(JsonKey.CHILDREN)));
      }
    }
    return hierarchyDialCodeIdentifierMap;
  }

  private void validateTopics(Set<String> topics, String frameworkId) {
    if (CollectionUtils.isNotEmpty(topics)) {
      List<String> frameworkTopics = getRelatedFrameworkTopics(frameworkId);
      Set<String> invalidTopics = new HashSet<>();
      topics.forEach(
          name -> {
            if (!frameworkTopics.contains(name)) {
              invalidTopics.add(name);
            }
          });
      if (CollectionUtils.isNotEmpty(invalidTopics)) {
        throwClientErrorException(
            ResponseCode.errorInvalidTopic,
            MessageFormat.format(
                ResponseCode.errorInvalidTopic.getErrorMessage(),
                StringUtils.join(invalidTopics, ',')));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<String> getRelatedFrameworkTopics(String frameworkId) {
    Response response = TextBookTocUtil.getRelatedFrameworkById(frameworkId);
    Map<String, Object> result = response.getResult();
    List<Map<String, Object>> terms = new ArrayList<>();
    if (MapUtils.isNotEmpty(result)) {
      Map<String, Object> framework = (Map<String, Object>) result.get(JsonKey.FRAMEWORK);
      if (MapUtils.isNotEmpty(framework)) {
        List<Map<String, Object>> categories =
            (List<Map<String, Object>>) framework.get(JsonKey.CATEGORIES);
        if (CollectionUtils.isNotEmpty(categories))
          categories.forEach(
              s -> {
                if (JsonKey.TOPIC.equalsIgnoreCase((String) s.get(JsonKey.CODE))) {
                  terms.addAll((List<Map<String, Object>>) s.get(JsonKey.TERMS));
                }
              });
      }
    }
    return getTopic(terms);
  }

  @SuppressWarnings("unchecked")
  public List<String> getTopic(List<Map<String, Object>> children) {
    List<String> topics = new ArrayList<>();
    for (Map<String, Object> child : children) {
      topics.add((String) child.get(JsonKey.NAME));
      if (null != child.get(JsonKey.CHILDREN)) {
        topics.addAll(getTopic((List<Map<String, Object>>) child.get(JsonKey.CHILDREN)));
      }
    }
    return topics;
  }

  private void validateDialCodesWithReservedDialCodes(
      Set<String> dialCodes, Map<String, Object> textbookData) {
    String channel = (String) textbookData.get(JsonKey.CHANNEL);
    if (CollectionUtils.isNotEmpty(dialCodes)) {
      Set<String> invalidDialCodes = new HashSet<>();
      List<String> searchedDialcodes = callDialcodeSearchApi(dialCodes, channel);
      if (CollectionUtils.isNotEmpty(searchedDialcodes)) {
        dialCodes.forEach(
            dialCode -> {
              if (!searchedDialcodes.contains(dialCode)) {
                invalidDialCodes.add(dialCode);
              }
            });
        if (CollectionUtils.isNotEmpty(invalidDialCodes)) {
          throwInvalidDialCodeError(invalidDialCodes);
        }
      } else if (CollectionUtils.isNotEmpty(dialCodes)) {
        throwInvalidDialCodeError(dialCodes);
      }
    }
  }

  private List<String> callDialcodeSearchApi(Set<String> dialCodes, String channel) {
    Map<String, Object> requestMap = new HashMap<>();
    Map<String, Object> request = new HashMap<>();
    requestMap.put(JsonKey.REQUEST, request);
    Map<String, Object> search = new HashMap<>();
    request.put(JsonKey.SEARCH, search);
    List<String> identifier = new ArrayList<>();
    identifier.addAll(dialCodes);
    search.put(JsonKey.IDENTIFIER, identifier);

    String requestUrl =
        getConfigValue(JsonKey.SUNBIRD_CS_BASE_URL)
            + getConfigValue(JsonKey.SUNBIRD_DIALCODE_SEARCH_API);
    HttpResponse<String> updateResponse = null;
    List<String> resDialcodes = new ArrayList<>();
    try {
      Map<String, String> headers = new HashMap<>();
      headers.putAll(getDefaultHeaders());
      headers.put("X-Channel-Id", channel);
      headers.put(
          "x-authenticated-user-token",
          ssoManager.login(
              getConfigValue(JsonKey.SUNBIRD_SSO_USERNAME),
              getConfigValue(JsonKey.SUNBIRD_SSO_PASSWORD)));
      String reqBody = mapper.writeValueAsString(requestMap);
      ProjectLogger.log(
          "Sized :TextBookTocUtil:callDialcodeSearchApi: size of request "
              + reqBody.getBytes().length,
          INFO);

      updateResponse = Unirest.post(requestUrl).headers(headers).body(reqBody).asString();
      if (null != updateResponse) {
        ProjectLogger.log(
            "Sized :TextBookTocUtil:callDialcodeSearchApi: size of response "
                + updateResponse.getBody().getBytes().length,
            INFO);
        Response response = mapper.readValue(updateResponse.getBody(), Response.class);
        ProjectLogger.log(
            "TextbookTocActor:callDialcodeSearchApi : response.getResponseCode().getResponseCode() : "
                + response.getResponseCode().getResponseCode(),
            LoggerEnum.INFO.name());
        if (response.getResponseCode().getResponseCode() == ResponseCode.OK.getResponseCode()) {
          Map<String, Object> result = response.getResult();
          if (MapUtils.isNotEmpty(result)) {
            List<Map<String, Object>> dialcodes =
                (List<Map<String, Object>>) result.get(JsonKey.DIAL_CODES);
            if (CollectionUtils.isNotEmpty(dialcodes)) {
              dialcodes
                  .stream()
                  .forEach(
                      map -> {
                        resDialcodes.add((String) map.get(JsonKey.IDENTIFIER));
                      });
              return resDialcodes;
            }
          }
        }
      }
    } catch (Exception ex) {
      ProjectLogger.log(
          "TextbookTocActor:callDialcodeSearchApi : Exception occurred with message:"
              + ex.getMessage(),
          ex);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return resDialcodes;
  }

  private void throwInvalidDialCodeError(Set<String> invalidDialCodes) {
    throwClientErrorException(
        ResponseCode.errorInvalidDialCode,
        MessageFormat.format(
            ResponseCode.errorInvalidDialCode.getErrorMessage(),
            StringUtils.join(invalidDialCodes, ',')));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> readAndValidateCSV(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> result = new HashMap<>();
    Map<Integer, List<String>> rowNumVsContentIdsMap = new HashMap<>();
    List<Map<String, Object>> rows = new ArrayList<>();
    String tocMapping = ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_INPUT_MAPPING);
    Map<String, Object> configMap =
        mapper.readValue(tocMapping, new TypeReference<Map<String, Object>>() {});

    Map<String, String> metadata = (Map<String, String>) configMap.get(JsonKey.METADATA);
    Map<String, String> hierarchy = (Map<String, String>) configMap.get(JsonKey.HIERARCHY);
    int max_allowed_content_size =
        Integer.parseInt(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TOC_MAX_FIRST_LEVEL_UNITS));
    String linkedContentKey =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TOC_LINKED_CONTENT_COLUMN_NAME);

    Map<String, String> fwMetadata =
        (Map<String, String>) configMap.get(JsonKey.FRAMEWORK_METADATA);
    String id =
        configMap
            .getOrDefault(JsonKey.IDENTIFIER, StringUtils.capitalize(JsonKey.IDENTIFIER))
            .toString();
    metadata.putAll(fwMetadata);
    CSVParser csvFileParser = null;
    CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
    BOMInputStream bomInputStream =
        new BOMInputStream(
            inputStream,
            ByteOrderMark.UTF_16BE,
            ByteOrderMark.UTF_8,
            ByteOrderMark.UTF_16LE,
            ByteOrderMark.UTF_32BE,
            ByteOrderMark.UTF_32LE);
    String character = StandardCharsets.UTF_8.name();
    if (bomInputStream.hasBOM()) {
      character = bomInputStream.getBOMCharsetName();
      ProjectLogger.log("TextbookTocActor:readAndValidateCSV : BOM charset", LoggerEnum.INFO);
    }
    try (InputStreamReader reader = new InputStreamReader(bomInputStream, character); ) {
      csvFileParser = csvFileFormat.parse(reader);
      HashMap<String, Integer> csvHeaders = new HashMap<>();
      if (MapUtils.isNotEmpty(csvFileParser.getHeaderMap())) {
        csvFileParser
            .getHeaderMap()
            .entrySet()
            .forEach(entry -> csvHeaders.put(entry.getKey().trim(), entry.getValue()));
      }
      String mode = csvHeaders.containsKey(id) ? JsonKey.UPDATE : JsonKey.CREATE;
      result.put(JsonKey.MODE, mode);

      if (null != csvHeaders && !csvHeaders.isEmpty()) {
        metadata.values().removeIf(key -> !csvHeaders.keySet().contains(key));
        hierarchy.values().removeIf(key -> !csvHeaders.keySet().contains(key));
      } else {
        throwClientErrorException(ResponseCode.blankCsvData);
      }

      String mandatoryFields = getConfigValue(JsonKey.TEXTBOOK_TOC_MANDATORY_FIELDS);
      Map<String, String> mandatoryFieldsMap =
          mapper.readValue(mandatoryFields, new TypeReference<Map<String, String>>() {});
      List<String> missingColumns =
          mandatoryFieldsMap
              .values()
              .stream()
              .filter(field -> !csvHeaders.keySet().contains(field))
              .collect(Collectors.toList());
      if (CollectionUtils.isNotEmpty(missingColumns)) {
        throwClientErrorException(
            ResponseCode.mandatoryHeadersMissing,
            MessageFormat.format(
                "Mandatory column(s) {0} are missing",
                String.join(", ", new ArrayList<>(missingColumns))));
      }

      List<CSVRecord> csvRecords = csvFileParser.getRecords();
      validateCSV(csvRecords);
      Set<String> dialCodes = new HashSet<>();
      Set<String> duplicateDialCodes = new LinkedHashSet<>();
      Map<String, List<String>> dialCodeIdentifierMap = new HashMap<>();
      Set<String> topics = new HashSet<>();
      Map<String, Object> bgms = new HashMap<>();
      StringBuilder exceptionMsgs = new StringBuilder();
      for (int i = 0; i < csvRecords.size(); i++) {
        CSVRecord record = csvRecords.get(i);
        Map<String, String> trimMappingRecord = new HashMap<>();
        record
            .toMap()
            .entrySet()
            .forEach(
                entry ->
                    trimMappingRecord.put(
                        entry.getKey().trim(),
                        entry.getValue() != null ? entry.getValue().trim() : entry.getValue()));
        HashMap<String, Object> recordMap = new HashMap<>();
        HashMap<String, Object> hierarchyMap = new HashMap<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
          if (StringUtils.isNotBlank(trimMappingRecord.get(entry.getValue())))
            recordMap.put(entry.getKey(), trimMappingRecord.get(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : hierarchy.entrySet()) {
          if (StringUtils.isNotBlank(trimMappingRecord.get(entry.getValue())))
            hierarchyMap.put(entry.getKey(), trimMappingRecord.get(entry.getValue()));
        }
        validateBGMS(i, bgms, recordMap, metadata);

        if (!(MapUtils.isEmpty(recordMap) && MapUtils.isEmpty(hierarchyMap))) {
          validateQrCodeRequiredAndQrCode(recordMap);
          String dialCode = (String) recordMap.get(JsonKey.DIAL_CODES);
          List<String> dialCodeList = null;
          if (StringUtils.isNotBlank(dialCode)) {
            dialCodeList = new ArrayList<String>(Arrays.asList(dialCode.split(",")));
            for (String dCode : dialCodeList) {
              if (!dialCodes.add(dCode.trim())) {
                duplicateDialCodes.add(dCode.trim());
              }
            }
          }

          String reqTopics = (String) recordMap.get(JsonKey.TOPIC);
          if (StringUtils.isNotBlank(reqTopics)) {
            List<String> topicList = new ArrayList<String>(Arrays.asList(reqTopics.split(",")));
            topicList.forEach(
                s -> {
                  topics.add(s.trim());
                });
          }
          Map<String, Object> map = new HashMap<>();
          if (JsonKey.UPDATE.equalsIgnoreCase(mode)
              && StringUtils.isNotBlank(trimMappingRecord.get(id))) {
            String identifier = trimMappingRecord.get(id);
            map.put(JsonKey.IDENTIFIER, identifier);
            if (CollectionUtils.isNotEmpty(dialCodeList)) {
              dialCodeIdentifierMap.put(identifier, dialCodeList);
            }
          }
          List<String> contentIds = Collections.EMPTY_LIST;
          try {
            contentIds =
                validateLinkedContentAndGetContentIds(
                    max_allowed_content_size, linkedContentKey, record, i + 1);
            rowNumVsContentIdsMap.put(i + 1, contentIds);
          } catch (Exception ex) {
            exceptionMsgs.append(ex.getMessage());
            exceptionMsgs.append(" ");
          }
          map.put(JsonKey.METADATA, recordMap);
          map.put(JsonKey.HIERARCHY, hierarchyMap);
          map.put(JsonKey.CHILDREN, contentIds);
          rows.add(map);
        }
      }
      if (CollectionUtils.isNotEmpty(duplicateDialCodes)) {
        throwClientErrorException(
            ResponseCode.errorDduplicateDialCodeEntry,
            MessageFormat.format(
                ResponseCode.errorDduplicateDialCodeEntry.getErrorMessage(),
                StringUtils.join(duplicateDialCodes, ",")));
      }
      if (StringUtils.isNotBlank(exceptionMsgs.toString())) {
        ProjectCommonException.throwClientErrorException(
            ResponseCode.customClientError, exceptionMsgs.toString());
      }
      result.put(JsonKey.FILE_DATA, rows);
      result.put(JsonKey.DIAL_CODES, dialCodes);
      result.put(JsonKey.TOPICS, topics);
      result.put(JsonKey.DIAL_CODE_IDENTIFIER_MAP, dialCodeIdentifierMap);
      result.put(JsonKey.LINKED_CONTENT, rowNumVsContentIdsMap);
    } catch (IllegalArgumentException e) {
      ProjectCommonException.throwClientErrorException(
          ResponseCode.customClientError, e.getMessage());
    } catch (ProjectCommonException e) {
      throw e;
    } catch (Exception e) {
      throwServerErrorException(ResponseCode.errorProcessingFile);
    } finally {
      try {
        if (null != csvFileParser) csvFileParser.close();
      } catch (IOException e) {
        ProjectLogger.log(
            "TextbookTocActor:readAndValidateCSV : Exception occurred while closing stream",
            LoggerEnum.ERROR);
      }
    }
    return result;
  }

  private void validateBGMS(
      int recordNum,
      Map<String, Object> bgms,
      HashMap<String, Object> recordMap,
      Map<String, String> metadata) {
    if (recordNum == 0) {
      getBgmsData(recordMap, bgms);
    } else {
      handleBGMSMismatchValidation(recordNum, metadata, bgms, recordMap);
    }
    // Removing fields from updating further
    recordMap.remove(JsonKey.BOARD);
    recordMap.remove(JsonKey.MEDIUM);
    recordMap.remove(JsonKey.GRADE_LEVEL);
    recordMap.remove(JsonKey.SUBJECT);
  }

  private void getBgmsData(HashMap<String, Object> recordMap, Map<String, Object> bgms) {
    for (Entry<String, Object> entry : frameCategories.entrySet()) {
      String key = entry.getKey();
      bgms.put(key, recordMap.get(key));
    }
  }

  private void handleBGMSMismatchValidation(
      int recordNum,
      Map<String, String> metadata,
      Map<String, Object> bgms,
      HashMap<String, Object> recordMap) {
    for (Entry<String, Object> entry : frameCategories.entrySet()) {
      String key = entry.getKey();
      if (null != bgms.get(key) && null != recordMap.get(key)) {
        String bgmsKey = ((String) bgms.get(key)).toLowerCase();
        String recordMapKey = ((String) recordMap.get(key)).toLowerCase();
        List<String> bgmsKeyList =
            (Arrays.stream(bgmsKey.split(",")).map(s -> s.trim()).collect(Collectors.toList()));

        Arrays.stream(recordMapKey.split(","))
            .forEach(
                s -> {
                  if (!bgmsKeyList.contains(s.trim())) {
                    throwClientErrorException(
                        ResponseCode.errorBGMSMismatch,
                        MessageFormat.format(
                            ResponseCode.errorBGMSMismatch.getErrorMessage(),
                            metadata.get(key),
                            recordNum + 1));
                  }
                });
      } else if ((null != bgms.get(key) && null == recordMap.get(key))
          || (null == bgms.get(key) && null != recordMap.get(key))) {
        throwClientErrorException(
            ResponseCode.errorBGMSMismatch,
            MessageFormat.format(
                ResponseCode.errorBGMSMismatch.getErrorMessage(),
                metadata.get(key),
                recordNum + 1));
      }
    }
  }

  private List<String> validateLinkedContentAndGetContentIds(
      int max_allowed_content_size, String linkedContentKey, CSVRecord record, int rowNumber) {
    List<String> contentIds = new ArrayList<>();
    for (int i = 1; i <= max_allowed_content_size; i++) {
      String key = MessageFormat.format(linkedContentKey, i).trim();
      if (record.isMapped(key)) {
        String contentId = record.get(key);
        if (StringUtils.isNotBlank(contentId)) {
          if (contentIds.contains(contentId)) {
            String message =
                MessageFormat.format(
                    ResponseCode.errorDuplicateLinkedContentId.getErrorMessage(),
                    record.get(key),
                    rowNumber);
            ProjectCommonException.throwClientErrorException(
                ResponseCode.errorDuplicateLinkedContentId, message);
          }
          contentIds.add(contentId);
        } else {
          break;
        }
      }
    }
    return contentIds;
  }

  private void validateQrCodeRequiredAndQrCode(Map<String, Object> recordMap) {
    if (JsonKey.NO.equalsIgnoreCase((String) recordMap.get(JsonKey.DIAL_CODE_REQUIRED))
        && StringUtils.isNotBlank((String) recordMap.get(JsonKey.DIAL_CODES))) {
      String errorMessage =
          MessageFormat.format(
              ResponseCode.errorConflictingValues.getErrorMessage(),
              JsonKey.QR_CODE_REQUIRED,
              JsonKey.NO,
              JsonKey.QR_CODE,
              recordMap.get(JsonKey.DIAL_CODES));
      throwClientErrorException(ResponseCode.errorConflictingValues, errorMessage);
    }
  }

  private void validateCSV(List<CSVRecord> records) {
    if (CollectionUtils.isEmpty(records)) {
      throwClientErrorException(
          ResponseCode.blankCsvData, ResponseCode.blankCsvData.getErrorMessage());
    }
    Integer allowedNumberOfRecord =
        Integer.valueOf(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_MAX_CSV_ROWS));
    if (CollectionUtils.isNotEmpty(records) && records.size() > allowedNumberOfRecord) {
      throwClientErrorException(
          ResponseCode.csvRowsExceeds,
          ResponseCode.csvRowsExceeds.getErrorMessage() + allowedNumberOfRecord);
    }
  }

  private void getTocUrl(Request request) {
    String textbookId = (String) request.get(TEXTBOOK_ID);
    if (isBlank(textbookId)) {
      log("Invalid TextBook Provided", ERROR.name());
      throwClientErrorException(invalidTextbook, invalidTextbook.getErrorMessage());
    }
    log("Reading Content for TextBook | Id: " + textbookId, INFO.name());
    Map<String, Object> contentHierarchy = getHierarchy(textbookId);
    ProjectLogger.log(
        "Timed:TextbookTocActor:getTocUrl duration for get textbook: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    validateTextBook(contentHierarchy, DOWNLOAD);
    FileExtension fileExtension = CSV.getFileExtension();
    String contentVersionKey = (String) contentHierarchy.get(VERSION_KEY);
    String textBookNameSlug = makeSlug((String) contentHierarchy.get(NAME), true);
    String textBookTocFileName = textbookId + "_" + textBookNameSlug + "_" + contentVersionKey;
    String prefix =
        TEXTBOOK_TOC_FOLDER + separator + textBookTocFileName + fileExtension.getDotExtension();
    log("Fetching TextBook Toc URL from Cloud", INFO.name());

    String cloudPath = getUri(prefix, false);
    ProjectLogger.log(
        "Timed:TextbookTocActor:getTocUrl duration for get cloud path url: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    if (isBlank(cloudPath)) {
      log("Reading Hierarchy for TextBook | Id: " + textbookId, INFO.name());
      ProjectLogger.log(
          "Timed:TextbookTocActor:getTocUrl duration for get hirearchy: "
              + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
          INFO);
      String hierarchyVersionKey = (String) contentHierarchy.get(VERSION_KEY);
      cloudPath =
          new TextBookTocUploader(textBookTocFileName, fileExtension)
              .execute(contentHierarchy, textbookId, hierarchyVersionKey);
      ProjectLogger.log(
          "Timed:TextbookTocActor:getTocUrl duration for processing preparing and uploading: "
              + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
          INFO);
    }

    log("Sending Response for Toc Download API for TextBook | Id: " + textbookId, INFO.name());
    Map<String, Object> textbook = new HashMap<>();
    textbook.put(TOC_URL, cloudPath);
    textbook.put(TTL, getConfigValue(TEXTBOOK_TOC_CSV_TTL));
    Response response = new Response();
    response.put(TEXTBOOK, textbook);

    sender().tell(response, sender());
  }

  @SuppressWarnings("unchecked")
  private void validateTextBook(Map<String, Object> textbook, String mode) {
    List<String> allowedContentTypes =
        asList(getConfigValue(TEXTBOOK_TOC_ALLOWED_CONTNET_TYPES).split(","));
    if (!TEXTBOOK_TOC_ALLOWED_MIMETYPE.equalsIgnoreCase(textbook.get(MIME_TYPE).toString())
        || !allowedContentTypes.contains(textbook.get(CONTENT_TYPE).toString())) {
      throwClientErrorException(invalidTextbook, invalidTextbook.getErrorMessage());
    }
    List<Object> children = (List<Object>) textbook.get(CHILDREN);
    if (JsonKey.CREATE.equalsIgnoreCase(mode)) {
      if (null != children && !children.isEmpty()) {
        throwClientErrorException(textbookChildrenExist, textbookChildrenExist.getErrorMessage());
      }
    } else if (DOWNLOAD.equalsIgnoreCase(mode)) {
      if (null == children || children.isEmpty())
        throwClientErrorException(noChildrenExists, noChildrenExists.getErrorMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private void validateRequest(Request request, String mode, Map<String, Object> textbook)
      throws IOException {
    Set<String> rowsHash = new HashSet<>();
    String mandatoryFields = getConfigValue(JsonKey.TEXTBOOK_TOC_MANDATORY_FIELDS);
    Map<String, String> mandatoryFieldsMap =
        mapper.readValue(mandatoryFields, new TypeReference<Map<String, String>>() {});
    String textbookName =
        textbook.get(JsonKey.NAME) != null
            ? ((String) textbook.get(JsonKey.NAME)).trim()
            : (String) textbook.get(JsonKey.NAME);

    validateTextBook(textbook, mode);

    List<Map<String, Object>> fileData =
        (List<Map<String, Object>>)
            ((Map<String, Object>) request.get(JsonKey.DATA)).get(JsonKey.FILE_DATA);

    for (int i = 0; i < fileData.size(); i++) {
      Map<String, Object> row = fileData.get(i);
      Boolean isAdded =
          rowsHash.add(
              DigestUtils.md5Hex(SerializationUtils.serialize(row.get(HIERARCHY).toString())));
      if (!isAdded) {
        throwClientErrorException(
            ResponseCode.duplicateRows, ResponseCode.duplicateRows.getErrorMessage() + (i + 1));
      }
      Map<String, Object> hierarchy = (Map<String, Object>) row.get(JsonKey.HIERARCHY);

      String name =
          ((String) hierarchy.getOrDefault(StringUtils.capitalize(JsonKey.TEXTBOOK), "")).trim();
      if (isBlank(name) || !StringUtils.equalsIgnoreCase(name, textbookName)) {
        log(
            "Name mismatch. Content has: " + name + " but, file has: " + textbookName,
            null,
            ERROR.name());
        throwClientErrorException(
            ResponseCode.invalidTextbookName, ResponseCode.invalidTextbookName.getErrorMessage());
      }
      for (String field : mandatoryFieldsMap.keySet()) {
        if (!hierarchy.containsKey(field)
            || isBlank(hierarchy.getOrDefault(field, "").toString())) {
          throwClientErrorException(
              ResponseCode.requiredFieldMissing,
              ResponseCode.requiredFieldMissing.getErrorMessage() + mandatoryFieldsMap.values());
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Response createTextbook(Request request, Map<String, Object> textBookHierarchy)
      throws Exception {
    log("Create Textbook called ", INFO.name());
    Map<String, Object> file = (Map<String, Object>) request.get(JsonKey.DATA);
    List<Map<String, Object>> data = (List<Map<String, Object>>) file.get(JsonKey.FILE_DATA);
    if (CollectionUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else {
      log(
          "Create Textbook - UpdateHierarchy input data : " + mapper.writeValueAsString(data),
          LoggerEnum.INFO);
      String tbId = (String) request.get(TEXTBOOK_ID);
      Map<String, Object> nodesModified = new HashMap<>();
      Map<String, Object> hierarchyData = new HashMap<>();
      nodesModified.put(
          tbId,
          new HashMap<String, Object>() {
            {
              put(JsonKey.TB_IS_NEW, false);
              put(JsonKey.TB_ROOT, true);
              put(JsonKey.METADATA, new HashMap<String, Object>());
            }
          });

      hierarchyData.put(
          tbId,
          new HashMap<String, Object>() {
            {
              put(JsonKey.NAME, textBookHierarchy.get(JsonKey.NAME));
              put(CONTENT_TYPE, textBookHierarchy.get(CONTENT_TYPE));
              put(CHILDREN, new ArrayList<>());
              put(JsonKey.TB_ROOT, true);
            }
          });
      for (Map<String, Object> row : data) {
        populateNodes(row, tbId, textBookHierarchy, nodesModified, hierarchyData);
      }

      Map<String, Object> updateRequest = new HashMap<String, Object>();
      Map<String, Object> requestMap = new HashMap<String, Object>();
      Map<String, Object> dataMap = new HashMap<String, Object>();
      Map<String, Object> hierarchy = new HashMap<String, Object>();
      if (MapUtils.isNotEmpty(hierarchyData)) {
        hierarchy.putAll(hierarchyData);
      }
      dataMap.put(JsonKey.NODES_MODIFIED, nodesModified);
      dataMap.put(JsonKey.HIERARCHY, hierarchy);
      requestMap.put(JsonKey.DATA, dataMap);
      updateRequest.put(JsonKey.REQUEST, requestMap);

      ProjectLogger.log(
          "Timed:TextbookTocActor:createTextbook duration for processing create textbook: "
              + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
          INFO);
      log(
          "Create Textbook - UpdateHierarchy Request : " + mapper.writeValueAsString(updateRequest),
          INFO.name());
      return callUpdateHierarchyAndLinkDialCodeApi(
          tbId, updateRequest, nodesModified, (String) textBookHierarchy.get(JsonKey.CHANNEL));
    }
  }

  private Response callUpdateHierarchyAndLinkDialCodeApi(
      String tbId,
      Map<String, Object> updateRequest,
      Map<String, Object> nodesModified,
      String channel)
      throws Exception {
    Response response = new Response();
    updateHierarchy(tbId, updateRequest);
    ProjectLogger.log(
        "Timed:TextbookTocActor:callUpdateHierarchyAndLinkDialCodeApi duration for update hirearchy data: "
            + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
        INFO);
    try {
      linkDialCode(nodesModified, channel, tbId);
      ProjectLogger.log(
          "Timed:TextbookTocActor:callUpdateHierarchyAndLinkDialCodeApi duration for link dial code: "
              + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
          INFO);
    } catch (Exception ex) {
      ProjectLogger.log(
          "TextbookTocActor:callUpdateHierarchyAndLinkDialCodeApi : Exception occurred while linking dial code : ",
          ex);
      response
          .getResult()
          .put(
              JsonKey.ERROR_MSG,
              "Textbook hierarchy metadata got updated but, "
                  + ResponseCode.errorDialCodeLinkingFail.getErrorMessage());
    }
    response.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private void populateNodes(
      Map<String, Object> row,
      String tbId,
      Map<String, Object> tbMetadata,
      Map<String, Object> nodesModified,
      Map<String, Object> hierarchyData) {
    Map<String, Object> hierarchy = (Map<String, Object>) row.get(JsonKey.HIERARCHY);
    hierarchy.remove(StringUtils.capitalize(JsonKey.TEXTBOOK));
    hierarchy.remove(JsonKey.IDENTIFIER);
    String unitType = (String) tbMetadata.get(JsonKey.CONTENT_TYPE) + JsonKey.UNIT;
    String framework = (String) tbMetadata.get(JsonKey.FRAMEWORK);
    int levelCount = 0;
    String code = tbId;
    String parentCode = tbId;
    for (int i = 1; i <= hierarchy.size(); i++) {
      if (StringUtils.isNotBlank((String) hierarchy.get("L:" + i))) {
        String name = (String) hierarchy.get("L:" + i);
        code += name;
        levelCount += 1;
        if (i - 1 > 0) parentCode += (String) hierarchy.get("L:" + (i - 1));
        if (isBlank((String) hierarchy.get("L:" + (i + 1))))
          populateNodeModified(
              name,
              getCode(code),
              (Map<String, Object>) row.get(JsonKey.METADATA),
              unitType,
              framework,
              nodesModified,
              true);
        else
          populateNodeModified(name, getCode(code), null, unitType, framework, nodesModified, true);
        populateHierarchyData(
            tbId, name, getCode(code), getCode(parentCode), levelCount, hierarchyData);
      } else {
        break;
      }
    }
  }

  private String getCode(String code) {
    return DigestUtils.md5Hex(code);
  }

  private Map<String, Object> getTextbook(String tbId) {
    Response response = null;
    Map<String, Object> textbook;
    try {
      response = readContent(tbId, JsonKey.SUNBIRD_CONTENT_READ_API);
      textbook = (Map<String, Object>) response.get(CONTENT);
      if (null == textbook) {
        log("Empty Content fetched | TextBook Id: " + tbId);
        throwServerErrorException(SERVER_ERROR, "Empty Content fetched for TextBook Id: " + tbId);
      }
    } catch (Exception e) {
      log(
          "Error while fetching textbook : " + tbId + " with response " + serialize(response),
          ERROR.name());
      throw e;
    }
    return textbook;
  }

  private Map<String, Object> getHierarchy(String tbId) {
    Response response = null;
    Map<String, Object> hierarchy;
    try {
      response = readContent(tbId, SUNBIRD_CONTENT_GET_HIERARCHY_API);
      hierarchy = (Map<String, Object>) response.get(CONTENT);
      if (null == hierarchy) {
        log("Empty Hierarchy fetched | TextBook Id: " + tbId);
        throwServerErrorException(SERVER_ERROR, "Empty Hierarchy fetched for TextBook Id: " + tbId);
      }
    } catch (Exception e) {
      log(
          "Error while fetching textbook : " + tbId + " with response " + serialize(response),
          ERROR.name());
      throw e;
    }
    return hierarchy;
  }

  @SuppressWarnings("unchecked")
  private Response updateTextbook(Request request, Map<String, Object> textbookHierarchy)
      throws Exception {
    Boolean linkContent =
        (boolean) ((Map<String, Object>) request.get(JsonKey.DATA)).get(JsonKey.LINKED_CONTENT);
    String channel = (String) textbookHierarchy.get(JsonKey.CHANNEL);
    List<Map<String, Object>> data =
        (List<Map<String, Object>>)
            ((Map<String, Object>) request.get(JsonKey.DATA)).get(JsonKey.FILE_DATA);
    String tbId = (String) request.get(TEXTBOOK_ID);
    Set<String> identifierList = new HashSet<>();
    identifierList.add(tbId);
    data.forEach(
        s -> {
          identifierList.add((String) s.get(JsonKey.IDENTIFIER));
        });
    if (CollectionUtils.isEmpty(data)) {
      throw new ProjectCommonException(
          ResponseCode.invalidRequestData.getErrorCode(),
          ResponseCode.invalidRequestData.getErrorMessage(),
          ResponseCode.CLIENT_ERROR.getResponseCode());
    } else {
      log(
          "Update Textbook - UpdateHierarchy input data : " + mapper.writeValueAsString(data),
          INFO.name());
      Map<String, Object> nodesModified = new HashMap<>();
      nodesModified.put(
          tbId,
          new HashMap<String, Object>() {
            {
              put(JsonKey.TB_IS_NEW, false);
              put(JsonKey.TB_ROOT, true);
              put(JsonKey.METADATA, new HashMap<String, Object>());
            }
          });
      Map<String, Object> hierarchyData = new HashMap<>();

      for (Map<String, Object> row : data) {
        Map<String, Object> metadata = (Map<String, Object>) row.get(JsonKey.METADATA);
        Map<String, Object> hierarchy = (Map<String, Object>) row.get(JsonKey.HIERARCHY);
        String id = (String) row.get(JsonKey.IDENTIFIER);
        metadata.remove(JsonKey.IDENTIFIER);

        populateNodeModified(
            (String) hierarchy.get("L:" + (hierarchy.size() - 1)),
            id,
            metadata,
            null,
            null,
            nodesModified,
            false);
      }
      List<Map<String, Object>> hierarchyList = null;
      if (CollectionUtils.isNotEmpty(
          (List<Map<String, Object>>) textbookHierarchy.get(JsonKey.CHILDREN))) {
        hierarchyList =
            getParentChildHierarchy(
                tbId,
                (String) textbookHierarchy.get(JsonKey.NAME),
                (List<Map<String, Object>>) textbookHierarchy.get(JsonKey.CHILDREN));
      }
      ProjectLogger.log(
          "TextbookTocActor:updateTextbook : ParentChildHierarchy structure : "
              + mapper.writeValueAsString(hierarchyList),
          LoggerEnum.INFO.name());
      if (CollectionUtils.isNotEmpty(hierarchyList)) {
        validateTextbookUnitIds(identifierList, hierarchyList);
      }
      if (BooleanUtils.isTrue(linkContent) && CollectionUtils.isNotEmpty(hierarchyList)) {
        Map<String, Object> hierarchy = populateHierarchyDataForUpdate(hierarchyList, tbId);
        data.forEach(
            s -> {
              Map<String, Object> nodeData =
                  (Map<String, Object>) hierarchy.get(s.get(JsonKey.IDENTIFIER));
              if (MapUtils.isNotEmpty(nodeData)
                  && CollectionUtils.isNotEmpty((List<String>) s.get(JsonKey.CHILDREN))) {
                for (String contentId : (List<String>) s.get(JsonKey.CHILDREN)) {
                  if (!((List<String>) nodeData.get(JsonKey.CHILDREN)).contains(contentId)) {
                    ((List<String>) nodeData.get(JsonKey.CHILDREN)).add(contentId);
                  }
                }
              }
            });
        hierarchyData.putAll(hierarchy);
        hierarchyData
            .entrySet()
            .removeIf(
                entry -> {
                  if (!identifierList.contains(entry.getKey())) {
                    return true;
                  }
                  return false;
                });

        ProjectLogger.log(
            "TextbookTocActor:updateTextbook : hierarchyData structure : "
                + mapper.writeValueAsString(hierarchyData),
            LoggerEnum.INFO.name());
      }

      Map<String, Object> updateRequest = new HashMap<String, Object>();
      Map<String, Object> requestMap = new HashMap<String, Object>();
      Map<String, Object> dataMap = new HashMap<String, Object>();

      dataMap.put(JsonKey.NODES_MODIFIED, nodesModified);
      dataMap.put(JsonKey.HIERARCHY, hierarchyData);
      requestMap.put(JsonKey.DATA, dataMap);
      updateRequest.put(JsonKey.REQUEST, requestMap);
      ProjectLogger.log(
          "Timed:TextbookTocActor:updateTextbook duration for processing update: "
              + (Instant.now().toEpochMilli() - startTime.toEpochMilli()),
          INFO);
      log(
          "Update Textbook - UpdateHierarchy Request : " + mapper.writeValueAsString(updateRequest),
          INFO.name());
      return callUpdateHierarchyAndLinkDialCodeApi(
          (String) request.get(TEXTBOOK_ID), updateRequest, nodesModified, channel);
    }
  }

  private void validateTextbookUnitIds(
      Set<String> identifierList, List<Map<String, Object>> hierarchyList) {
    Set<String> textbookUnitIds = new HashSet<>();
    hierarchyList
        .stream()
        .forEach(
            s -> {
              textbookUnitIds.addAll(s.keySet());
            });
    identifierList.forEach(
        textbookUnitId -> {
          if (!textbookUnitIds.contains(textbookUnitId)) {
            ProjectCommonException.throwClientErrorException(
                ResponseCode.errorInvalidTextbookUnitId,
                MessageFormat.format(
                    ResponseCode.errorInvalidTextbookUnitId.getErrorMessage(), textbookUnitId));
          }
        });
  }

  private List<Map<String, Object>> getParentChildHierarchy(
      String parentId, String name, List<Map<String, Object>> children) {
    List<Map<String, Object>> hierarchyList = new ArrayList<>();
    Map<String, Object> hierarchy = new HashMap<>();
    Map<String, Object> node = new HashMap<>();
    node.put(JsonKey.NAME, name);
    List<String> contentIdList = new ArrayList<>();
    node.put(JsonKey.CHILDREN, contentIdList);
    hierarchy.put(parentId, node);
    hierarchyList.add(hierarchy);
    for (Map<String, Object> child : children) {
      contentIdList.add((String) child.get(JsonKey.IDENTIFIER));
      if (null != child.get(JsonKey.CHILDREN)) {
        hierarchyList.addAll(
            getParentChildHierarchy(
                (String) child.get(JsonKey.IDENTIFIER),
                (String) child.get(JsonKey.NAME),
                (List<Map<String, Object>>) child.get(JsonKey.CHILDREN)));
      } else {
        List<Map<String, Object>> newChildren = new ArrayList<>();
        hierarchyList.addAll(
            getParentChildHierarchy(
                (String) child.get(JsonKey.IDENTIFIER),
                (String) child.get(JsonKey.NAME),
                newChildren));
      }
    }
    return hierarchyList;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> populateHierarchyDataForUpdate(
      List<Map<String, Object>> hierarchy, String textbookId) {
    Map<String, Object> hierarchyData = new HashMap<>();
    hierarchy.forEach(
        s -> {
          for (Entry<String, Object> entry : s.entrySet()) {
            if (textbookId.equalsIgnoreCase(entry.getKey())) {
              ((Map<String, Object>) entry.getValue()).put(JsonKey.CONTENT_TYPE, "TextBook");
              ((Map<String, Object>) entry.getValue()).put(JsonKey.TB_ROOT, true);
            } else {
              ((Map<String, Object>) entry.getValue()).put(JsonKey.CONTENT_TYPE, "TextBookUnit");
              ((Map<String, Object>) entry.getValue()).put(JsonKey.TB_ROOT, false);
            }
            hierarchyData.put(entry.getKey(), entry.getValue());
          }
        });
    return hierarchyData;
  }

  @SuppressWarnings("unchecked")
  private void linkDialCode(Map<String, Object> modifiedNodes, String channel, String tbId)
      throws Exception {
    List<Map<String, Object>> content = new ArrayList<>();
    modifiedNodes.forEach(
        (k, v) -> {
          Map<String, Object> value = (Map<String, Object>) v;
          Map<String, Object> metadata = (Map<String, Object>) value.get(JsonKey.METADATA);
          if (MapUtils.isNotEmpty(metadata)) {
            String dialCodeRequired = (String) metadata.get(JsonKey.DIAL_CODE_REQUIRED);
            if (JsonKey.YES.equalsIgnoreCase(dialCodeRequired)) {
              Map<String, Object> linkDialCode = new HashMap<>();
              linkDialCode.put(JsonKey.IDENTIFIER, k);
              if (null != metadata.get(JsonKey.DIAL_CODES)) {
                linkDialCode.put("dialcode", metadata.get(JsonKey.DIAL_CODES));
              } else {
                List<String> dialcodes = new ArrayList<>();
                linkDialCode.put("dialcode", dialcodes);
              }
              content.add(linkDialCode);
            }
          }
        });
    Map<String, Object> request = new HashMap<>();
    request.put(JsonKey.CONTENT, content);
    Map<String, Object> linkDialCoderequest = new HashMap<>();
    linkDialCoderequest.put(JsonKey.REQUEST, request);
    if (CollectionUtils.isNotEmpty(content)) {
      linkDialCodeApiCall(linkDialCoderequest, channel, tbId);
    }
  }

  private Response linkDialCodeApiCall(
      Map<String, Object> updateRequest, String channel, String tbId) throws Exception {
    String requestUrl =
        getConfigValue(JsonKey.EKSTEP_BASE_URL)
            + getConfigValue(JsonKey.LINK_DIAL_CODE_API)
            + "/"
            + tbId;
    HttpResponse<String> updateResponse = null;
    try {
      Map<String, String> headers = getDefaultHeaders();
      headers.put("X-Channel-Id", channel);
      updateResponse =
          Unirest.post(requestUrl)
              .headers(headers)
              .body(mapper.writeValueAsString(updateRequest))
              .asString();
      ProjectLogger.log(
          "TextbookTocActor:linkDialCodeApiCall : Request for link dial code api : "
              + mapper.writeValueAsString(updateRequest),
          LoggerEnum.INFO.name());

      ProjectLogger.log(
          "Sized: TextbookTocActor:linkDialCodeApiCall : size of request : "
              + mapper.writeValueAsString(updateRequest).getBytes().length,
          LoggerEnum.INFO);
      if (null != updateResponse) {
        Response response = mapper.readValue(updateResponse.getBody(), Response.class);
        ProjectLogger.log(
            "Sized: TextbookTocActor:linkDialCodeApiCall : size of response : "
                + updateResponse.getBody().getBytes().length,
            LoggerEnum.INFO);
        if (response.getResponseCode().getResponseCode() == ResponseCode.OK.getResponseCode()) {
          return response;
        } else {
          Map<String, Object> resultMap =
              Optional.ofNullable(response.getResult()).orElse(new HashMap<>());
          String message = "Linking of dial code failed ";
          if (MapUtils.isNotEmpty(resultMap)) {
            Object obj = Optional.ofNullable(resultMap.get(JsonKey.TB_MESSAGES)).orElse("");
            if (obj instanceof List) {
              message += ((List<String>) obj).stream().collect(Collectors.joining(";"));
            } else {
              message += String.valueOf(obj);
            }
          }
          ProjectCommonException.throwClientErrorException(
              ResponseCode.errorDialCodeLinkingClientError,
              MessageFormat.format(
                  ResponseCode.errorDialCodeLinkingClientError.getErrorMessage(), message));
        }
      } else {
        ProjectCommonException.throwClientErrorException(ResponseCode.errorDialCodeLinkingFail);
      }
    } catch (Exception ex) {
      ProjectLogger.log("TextbookTocActor:updateHierarchy : link dial code error ", ex);
      if (ex instanceof ProjectCommonException) {
        throw ex;
      } else {
        throw new ProjectCommonException(
            ResponseCode.errorTbUpdate.getErrorCode(),
            ResponseCode.errorTbUpdate.getErrorMessage(),
            SERVER_ERROR.getResponseCode());
      }
    }
    return null;
  }

  private Response updateHierarchy(String tbId, Map<String, Object> updateRequest)
      throws Exception {

    String requestUrl =
        getConfigValue(JsonKey.EKSTEP_BASE_URL) + getConfigValue(JsonKey.UPDATE_HIERARCHY_API);
    Map<String, String> headers = getDefaultHeaders();
    HttpResponse<String> updateResponse = null;
    try {
      ProjectLogger.log(
          "Sized:updateHierarchy:upload size of request "
              + mapper.writeValueAsString(updateRequest).getBytes().length,
          INFO);
      updateResponse =
          Unirest.patch(requestUrl)
              .headers(headers)
              .body(mapper.writeValueAsString(updateRequest))
              .asString();
    } catch (Exception ex) {
      ProjectLogger.log("TextbookTocActor:updateHierarchy : Update response call ", ex);
    }
    ProjectLogger.log(
        "TextbookTocActor:updateHierarchy : access token  : " + mapper.writeValueAsString(headers),
        LoggerEnum.INFO.name());
    ProjectLogger.log(
        "TextbookTocActor:updateHierarchy : Request for update hierarchy : "
            + mapper.writeValueAsString(updateRequest),
        LoggerEnum.INFO.name());
    if (null != updateResponse) {
      try {
        ProjectLogger.log(
            "TextbookTocActor:updateHierarchy : status response code : "
                + updateResponse.getStatus()
                + "status message "
                + updateResponse.getStatusText(),
            INFO);
        ProjectLogger.log(
            "Sized:updateHierarchy:upload size of response "
                + updateResponse.getBody().getBytes().length,
            INFO);
        Response response = mapper.readValue(updateResponse.getBody(), Response.class);
        if (response.getResponseCode().getResponseCode() == ResponseCode.OK.getResponseCode()) {
          return response;
        } else {
          Map<String, Object> resultMap =
              Optional.ofNullable(response.getResult()).orElse(new HashMap<>());
          String message = "Textbook hierarchy could not be created or updated. ";
          if (MapUtils.isNotEmpty(resultMap)) {
            Object obj = Optional.ofNullable(resultMap.get(JsonKey.TB_MESSAGES)).orElse("");
            if (obj instanceof List) {
              message += ((List<String>) obj).stream().collect(Collectors.joining(";"));
            } else {
              message += String.valueOf(obj);
            }
          }
          throw new ProjectCommonException(
              ResponseCode.errorDialCodeLinkingClientError.getErrorCode(),
              MessageFormat.format(
                  ResponseCode.errorDialCodeLinkingClientError.getErrorMessage(), message),
              ResponseCode.CLIENT_ERROR.getResponseCode());
        }
      } catch (Exception ex) {
        ProjectLogger.log(
            "TextbookTocActor:updateHierarchy : Update response body " + updateResponse.getBody(),
            ex);
        if (ex instanceof ProjectCommonException) {
          throw ex;
        } else {
          throw new ProjectCommonException(
              ResponseCode.errorTbUpdate.getErrorCode(),
              ResponseCode.errorTbUpdate.getErrorMessage(),
              SERVER_ERROR.getResponseCode());
        }
      }
    } else {
      ProjectLogger.log("TextbookTocActor:updateHierarchy : null response ", INFO);
      throw new ProjectCommonException(
          ResponseCode.errorTbUpdate.getErrorCode(),
          ResponseCode.errorTbUpdate.getErrorMessage(),
          SERVER_ERROR.getResponseCode());
    }
  }

  private Map<String, String> getDefaultHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put(
        JsonKey.AUTHORIZATION, JsonKey.BEARER + getConfigValue(JsonKey.SUNBIRD_AUTHORIZATION));
    return headers;
  }

  @SuppressWarnings("unchecked")
  private void populateNodeModified(
      String name,
      String code,
      Map<String, Object> metadata,
      String unitType,
      String framework,
      Map<String, Object> nodesModified,
      boolean isNew) {
    Map<String, Object> node = null;
    if (nodesModified.containsKey(code)) {
      node = (Map<String, Object>) nodesModified.get(code);
      if (MapUtils.isNotEmpty(metadata)) {
        Map<String, Object> newMeta = initializeMetaDataForModifiedNode(metadata);
        ((Map<String, Object>) node.get(JsonKey.METADATA)).putAll(newMeta);
      }
    } else {
      Map<String, Object> newMeta = initializeMetaDataForModifiedNode(metadata);
      node = new HashMap<String, Object>();
      node.put(JsonKey.TB_IS_NEW, isNew);
      node.put(JsonKey.TB_ROOT, false);
      if (StringUtils.isNotBlank(name)) newMeta.put(JsonKey.NAME, name);
      newMeta.put(JsonKey.MIME_TYPE, JsonKey.COLLECTION_MIME_TYPE);
      if (StringUtils.isNotBlank(unitType)) newMeta.put(JsonKey.CONTENT_TYPE, unitType);
      if (StringUtils.isNotBlank(framework)) newMeta.put(JsonKey.FRAMEWORK, framework);
      node.put(JsonKey.METADATA, newMeta);
    }
    if (StringUtils.isNotBlank(code)) {
      nodesModified.put(code, node);
    }
  }

  private Map<String, Object> initializeMetaDataForModifiedNode(Map<String, Object> metadata) {
    Map<String, Object> newMeta = new HashMap<String, Object>();
    if (MapUtils.isNotEmpty(metadata)) {
      List<String> keywords =
          (StringUtils.isNotBlank((String) metadata.get(JsonKey.KEYWORDS)))
              ? asList(((String) metadata.get(JsonKey.KEYWORDS)).split(","))
              : null;
      List<String> gradeLevel =
          (StringUtils.isNotBlank((String) metadata.get(JsonKey.GRADE_LEVEL)))
              ? asList(((String) metadata.get(JsonKey.GRADE_LEVEL)).split(","))
              : null;
      List<String> dialCodes =
          (StringUtils.isNotBlank((String) metadata.get(JsonKey.DIAL_CODES)))
              ? asList(((String) metadata.get(JsonKey.DIAL_CODES)).split(","))
              : null;

      List<String> topics =
          (StringUtils.isNotBlank((String) metadata.get(JsonKey.TOPIC)))
              ? asList(((String) metadata.get(JsonKey.TOPIC)).split(","))
              : null;
      newMeta.putAll(metadata);
      newMeta.remove(JsonKey.KEYWORDS);
      newMeta.remove(JsonKey.GRADE_LEVEL);
      newMeta.remove(JsonKey.DIAL_CODES);
      newMeta.remove(JsonKey.TOPIC);
      if (JsonKey.NO.equalsIgnoreCase((String) newMeta.get(JsonKey.DIAL_CODE_REQUIRED))) {
        newMeta.put(JsonKey.DIAL_CODE_REQUIRED, JsonKey.NO);
      } else if (JsonKey.YES.equalsIgnoreCase((String) newMeta.get(JsonKey.DIAL_CODE_REQUIRED))) {
        newMeta.put(JsonKey.DIAL_CODE_REQUIRED, JsonKey.YES);
      }
      if (CollectionUtils.isNotEmpty(keywords)) newMeta.put(JsonKey.KEYWORDS, keywords);
      if (CollectionUtils.isNotEmpty(gradeLevel)) newMeta.put(JsonKey.GRADE_LEVEL, gradeLevel);
      if (CollectionUtils.isNotEmpty(dialCodes)) {
        List<String> dCodes = new ArrayList<>();
        dialCodes.forEach(
            s -> {
              dCodes.add(s.trim());
            });
        newMeta.put(JsonKey.DIAL_CODES, dCodes);
      }
      if (CollectionUtils.isNotEmpty(topics)) {
        List<String> topicList = new ArrayList<>();
        topics.forEach(
            s -> {
              topicList.add(s.trim());
            });
        newMeta.put(JsonKey.TOPIC, topicList);
      }
    }
    return newMeta;
  }

  private void populateHierarchyData(
      String tbId,
      String name,
      String code,
      String parentCode,
      int levelCount,
      Map<String, Object> hierarchyData) {
    if (levelCount == 1) {
      parentCode = tbId;
    }
    if (null != hierarchyData.get(code)) {
      ((Map<String, Object>) hierarchyData.get(code)).put(JsonKey.NAME, name);
    } else {
      hierarchyData.put(
          code,
          new HashMap<String, Object>() {
            {
              put(JsonKey.NAME, name);
              put(CHILDREN, new ArrayList<>());
              put(JsonKey.TB_ROOT, false);
            }
          });
    }

    if (null != hierarchyData.get(parentCode)) {
      List<String> children =
          ((List) ((Map<String, Object>) hierarchyData.get(parentCode)).get(CHILDREN));
      if (!children.contains(code)) {
        children.add(code);
      }
    } else {
      String finalCode = code;
      hierarchyData.put(
          parentCode,
          new HashMap<String, Object>() {
            {
              put(JsonKey.NAME, "");
              put(
                  CHILDREN,
                  new ArrayList<String>() {
                    {
                      add(finalCode);
                    }
                  });
              put(JsonKey.TB_ROOT, false);
            }
          });
    }
  }
}
