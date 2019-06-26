package org.sunbird.learner.actors.textbook;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import com.google.common.base.Joiner;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.mashape.unirest.request.body.RequestBodyEntity;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.content.util.TextBookTocUtil;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextBookTocUtil.class, ProjectUtil.class, Unirest.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class TextbookTocActorTest {

  private static ActorSystem system;
  private static final Props props =
      Props.create(org.sunbird.learner.actors.textbook.TextbookTocActor.class);

  private static final String VALID_HEADER =
      "Identifier,Medium,Grade,Subject,Textbook Name,Level 1 Textbook Unit,Description,QR Code Required?,QR Code,Purpose of Content to be linked,Mapped Topics,Keywords\n";
  private static final String TEXTBOOK_TOC_INPUT_MAPPING =
      getFileAsString("FrameworkForTextbookTocActorTest.json");
  private static final String MANDATORY_VALUES =
      getFileAsString("MandatoryValueForTextbookTocActorTest.json");
  private static final String CONTENT_TYPE = "any";
  private static final String IDENTIFIER = "do_1126788813057638401122";
  private static final String TEXTBOOK_NAME = "test";
  private static final String UNIT_NAME = "unit1";

  @Before
  public void setUp() {
    PowerMockito.mockStatic(TextBookTocUtil.class);
    PowerMockito.mockStatic(ProjectUtil.class);
    PowerMockito.mockStatic(Unirest.class);
    system = ActorSystem.create("system");
    when(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_MAX_CSV_ROWS)).thenReturn("5");
    when(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_MANDATORY_FIELDS))
        .thenReturn(MANDATORY_VALUES);
    when(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_ALLOWED_CONTNET_TYPES))
        .thenReturn(CONTENT_TYPE);
    when(ProjectUtil.getConfigValue(JsonKey.EKSTEP_BASE_URL)).thenReturn("http://www.abc.com/");
    when(ProjectUtil.getConfigValue(JsonKey.UPDATE_HIERARCHY_API)).thenReturn("");
    when(ProjectUtil.getConfigValue(JsonKey.CASSANDRA_WRITE_BATCH_SIZE)).thenReturn("10");
    when(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_INPUT_MAPPING))
        .thenReturn(TEXTBOOK_TOC_INPUT_MAPPING);
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TOC_MAX_FIRST_LEVEL_UNITS)).thenReturn("30");
    when(ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TOC_LINKED_CONTENT_COLUMN_NAME))
        .thenReturn("Linked Content {0}");
    when(ProjectUtil.getConfigValue(JsonKey.TEXTBOOK_TOC_OUTPUT_MAPPING))
        .thenReturn(
            "{\"identifier\":\"Identifier\",\"frameworkCategories\":{\"board\":\"Board\",\"medium\":\"Medium\",\"gradeLevel\":\"Grade\",\"subject\":\"Subject\"},\"hierarchy\":{\"Textbook\":\"Textbook Name\",\"L:1\":\"Level 1 Textbook Unit\",\"L:2\":\"Level 2 Textbook Unit\",\"L:3\":\"Level 3 Textbook Unit\",\"L:4\":\"Level 4 Textbook Unit\"},\"metadata\":{\"description\":\"Description\",\"topic\":\"Mapped Topics\",\"keywords\":\"Keywords\",\"purpose\":\"Purpose of Content to be linked\",\"dialcodeRequired\":\"QR Code Required?\",\"dialcodes\":\"QR Code\"}}");
  }

  @Ignore
  @Test
  public void testUpdateFailureWithIncorrectTocData() throws IOException, UnirestException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "", "", false);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2096", "", "", true);
    mockResponseFromDialCodeSearch();
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorInvalidDialCode.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithDailcodeNotReq() throws IOException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.NO, "2019", "", "", true);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorConflictingValues.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithDuplicateEntry() throws IOException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "", "", false);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "", "", true);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorDduplicateDialCodeEntry.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithBlankCsv() throws IOException {
    mockRequiredMethods(false);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, VALID_HEADER);
    Assert.assertEquals(ResponseCode.blankCsvData.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithInvalidTopic() throws IOException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "topi", "abc", true);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorInvalidTopic.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithInvalidDailcode() throws IOException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2089", "", "", true);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorInvalidDialCode.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateFailureWithTocDataNotUnique() throws IOException {
    mockRequiredMethods(true);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "", "", true);
    ProjectCommonException res = (ProjectCommonException) doRequest(true, tocData.toString());
    Assert.assertEquals(ResponseCode.errorDialCodeAlreadyAssociated.getErrorCode(), res.getCode());
  }

  @Ignore
  @Test
  public void testUpdateSuccess() throws UnirestException, IOException {
    mockRequiredMethods(false);
    StringBuffer tocData = new StringBuffer(VALID_HEADER);
    tocData = addTocDataRow(tocData, JsonKey.YES, "2019", "", "", true);
    mockResponseFromUpdateHierarchy();
    Response response = (Response) doRequest(false, tocData.toString());
    Assert.assertNotNull(response);
  }

  private void mockResponseFromUpdateHierarchy() throws UnirestException {
    HttpRequestWithBody http = Mockito.mock(HttpRequestWithBody.class);
    RequestBodyEntity entity = Mockito.mock(RequestBodyEntity.class);
    HttpResponse<String> response = Mockito.mock(HttpResponse.class);
    when(Unirest.patch(Mockito.anyString())).thenReturn(http);
    when(http.headers(Mockito.anyMap())).thenReturn(http);
    when(http.body(Mockito.anyString())).thenReturn(entity);
    when(entity.asString()).thenReturn(response);
    when(response.getBody()).thenReturn("{\"responseCode\" :\"OK\" }");
  }

  private void mockResponseFromDialCodeSearch() throws UnirestException {
    HttpRequestWithBody http = Mockito.mock(HttpRequestWithBody.class);
    RequestBodyEntity entity = Mockito.mock(RequestBodyEntity.class);
    HttpResponse<String> response = Mockito.mock(HttpResponse.class);
    when(Unirest.post(Mockito.anyString())).thenReturn(http);
    when(http.headers(Mockito.anyMap())).thenReturn(http);
    when(http.body(Mockito.anyString())).thenReturn(entity);
    when(entity.asString()).thenReturn(response);
    when(response.getBody()).thenReturn("{\"responseCode\" :\"OK\" }");
  }

  private Object doRequest(boolean error, String data) throws IOException {
    TestKit probe = new TestKit(system);
    ActorRef toc = system.actorOf(props);
    Request request = new Request();
    request.setContext(
        new HashMap<String, Object>() {
          {
            put("userId", "test");
          }
        });
    request.put(JsonKey.TEXTBOOK_ID, "do_1126788813057638401122");
    InputStream stream = new ByteArrayInputStream(data.getBytes());
    byte[] byteArray = IOUtils.toByteArray(stream);
    request.getRequest().put(JsonKey.DATA, byteArray);
    request.put(JsonKey.DATA, byteArray);
    request.setOperation(TextbookActorOperation.TEXTBOOK_TOC_UPLOAD.getValue());
    toc.tell(request, probe.getRef());
    if (error) {
      ProjectCommonException res =
          probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
      return res;
    }
    Response response = probe.expectMsgClass(duration("10 second"), Response.class);
    return response;
  }

  private void mockRequiredMethods(boolean error) {
    when(TextBookTocUtil.getRelatedFrameworkById(Mockito.anyString())).thenReturn(new Response());
    when(TextBookTocUtil.readContent(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(getReadContentTextbookData());
  }

  private Response getReadHierarchy(boolean error) {
    Response response = new Response();
    List<String> tocData = new ArrayList<>();
    Map<String, Object> content = new HashMap<>();
    if (error) {
      content.put(JsonKey.IDENTIFIER, "do_112678881305763840115");
    } else {
      content.put(JsonKey.IDENTIFIER, "do_1126788813057638401122");
    }
    tocData.add("2019");
    content.put(JsonKey.DIAL_CODES, tocData);
    content.put(JsonKey.CHILDREN, new ArrayList<>());
    response.put(JsonKey.CONTENT, content);
    return response;
  }

  private Response getReadContentTextbookData() {
    Response response = new Response();
    Map<String, Object> textBookdata = new HashMap<>();
    Map<String, Integer> reserveDialCodes = new HashMap<>();
    reserveDialCodes.put("2019", 1);
    textBookdata.put(JsonKey.RESERVED_DIAL_CODES, reserveDialCodes);
    textBookdata.put(JsonKey.CONTENT_TYPE, CONTENT_TYPE);
    textBookdata.put(JsonKey.MIME_TYPE, "application/vnd.ekstep.content-collection");
    textBookdata.put(JsonKey.NAME, "test");
    response.put(JsonKey.CONTENT, textBookdata);
    return response;
  }

  private static String getFileAsString(String fileName) {
    File file = null;

    try {
      file = new File(TextbookTocActorTest.class.getClassLoader().getResource(fileName).getFile());
      Path path = Paths.get(file.getPath());
      List<String> fileData = Files.readAllLines(path);
      StringBuffer result = new StringBuffer();
      result.append(fileData.get(0));
      for (int num = 1; num < fileData.size(); num++) {
        result.append("\n" + fileData.get(num));
      }
      return result.toString();
    } catch (FileNotFoundException e) {
      ProjectLogger.log(e.getMessage(), e);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return null;
  }

  private StringBuffer addTocDataRow(
      StringBuffer tocData,
      String isQrCodeReq,
      String qrCode,
      String mappedTopic,
      String keywords,
      boolean isLastEntry) {

    tocData.append(
        Joiner.on(',')
            .join(
                IDENTIFIER,
                "",
                "",
                "",
                TEXTBOOK_NAME,
                UNIT_NAME,
                "",
                isQrCodeReq,
                qrCode,
                "",
                mappedTopic,
                (isLastEntry ? keywords : keywords + "\n")));

    return tocData;
  }
}
