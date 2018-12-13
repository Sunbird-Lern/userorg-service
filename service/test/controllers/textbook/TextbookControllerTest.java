package controllers.textbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseControllerTest;
import org.junit.Test;
import org.sunbird.common.models.response.Response;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.route;

public class TextbookControllerTest extends BaseControllerTest {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testUploadTocWithUrl() {
        Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/textbook/toc/upload/do_1126526588628582401237?fileUrl=https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/toc/do_112648449830322176179/download.csv").method("POST");
        Result result = route(req);
        assertEquals(200, result.status());
    }

    @Test
    public void testUploadTocWithBlankCsv() throws IOException {
        Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/textbook/toc/upload/do_1126526588628582401237?fileUrl=https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/hierarchy/do_11265280285332275214363/blank.csv").method("POST");
        Result result = route(req);
        assertEquals(400, result.status());
        Response resp = mapper.readValue(Helpers.contentAsString(result), Response.class);
        assertEquals("BLANK_CSV_DATA", resp.getParams().getStatus());
    }

    @Test
    public void testUploadTocWithRowsExceed() throws IOException {
        Http.RequestBuilder req = new Http.RequestBuilder().uri("/v1/textbook/toc/upload/do_1126526588628582401237?fileUrl=https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/hierarchy/do_11265280285332275214363/rows-exceed.csv").method("POST");
        Result result = route(req);
        assertEquals(400, result.status());
        Response resp = mapper.readValue(Helpers.contentAsString(result), Response.class);
        assertEquals("CSV_ROWS_EXCEEDS", resp.getParams().getStatus());
    }
}
