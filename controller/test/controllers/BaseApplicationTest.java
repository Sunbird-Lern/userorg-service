package controllers;

import org.apache.pekko.actor.ActorSelection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import modules.OnRequestHandler;
import modules.StartModule;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.keys.JsonKey;
import org.sunbird.response.Response;
import org.sunbird.response.ResponseParams;
import org.sunbird.telemetry.util.TelemetryWriter;
import play.Application;
import play.inject.Bindings;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import util.ACTORS;
import util.RequestInterceptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.management.*",
  "javax.net.ssl.*",
  "javax.security.*",
  "jdk.internal.reflect.*",
  "javax.crypto.*",
  "javax.script.*",
  "javax.xml.*",
  "com.sun.org.apache.xerces.*",
  "org.xml.*"
})
@PrepareForTest({RequestInterceptor.class, TelemetryWriter.class, ActorSelection.class})
@Ignore
public abstract class BaseApplicationTest {
  protected Application application;

  public <T> void setup(ACTORS actor, Class<T> actorClass) {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "userId");
    try {
      application =
          new GuiceApplicationBuilder()
              .in(new File("path/to/app"))
//              .in(Mode.TEST)
              .disable(StartModule.class)
              .overrides(Bindings.bind(actor.getActorClass()).to(actorClass))
              .build();
      Helpers.start(application);
      mockStatic(RequestInterceptor.class);
      mockStatic(TelemetryWriter.class);
      PowerMockito.when(RequestInterceptor.verifyRequestData(Mockito.anyObject(), Mockito.anyMap()))
          .thenReturn(userAuthentication);
      mockStatic(OnRequestHandler.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public <T> void setup(List<ACTORS> actors, Class<T> actorClass) {
    Map userAuthentication = new HashMap<String, String>();
    userAuthentication.put(JsonKey.USER_ID, "userId");
    try {
      GuiceApplicationBuilder applicationBuilder =
          new GuiceApplicationBuilder()
              .in(new File("path/to/app"))
//              .in(Mode.TEST)
              .disable(StartModule.class);
      for (ACTORS actor : actors) {
        applicationBuilder =
            applicationBuilder.overrides(Bindings.bind(actor.getActorClass()).to(actorClass));
      }
      application = applicationBuilder.build();
      Helpers.start(application);
      mockStatic(RequestInterceptor.class);
      mockStatic(TelemetryWriter.class);
      PowerMockito.when(RequestInterceptor.verifyRequestData(Mockito.anyObject(), Mockito.anyMap()))
          .thenReturn(userAuthentication);
      mockStatic(OnRequestHandler.class);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Result performTest(String url, String method) {
    Http.RequestBuilder req = new Http.RequestBuilder().uri(url).method(method);
    Result result = Helpers.route(application, req);
    return result;
  }

  public Result performTest(String url, String method, Map map) {
    String data = mapToJson(map);
    Http.RequestBuilder req;
    if (StringUtils.isNotBlank(data)) {
      JsonNode json = Json.parse(data);
      req = new Http.RequestBuilder().bodyJson(json).uri(url).method(method);
    } else {
      req = new Http.RequestBuilder().uri(url).method(method);
    }
    // req.headers(new Http.Headers(headerMap));
    Result result = Helpers.route(application, req);
    return result;
  }

  public String mapToJson(Map map) {
    ObjectMapper mapperObj = new ObjectMapper();
    String jsonResp = "";

    if (map != null) {
      try {
        jsonResp = mapperObj.writeValueAsString(map);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return jsonResp;
  }

  public String getResponseCode(Result result) {
    String responseStr = Helpers.contentAsString(result);
    ObjectMapper mapper = new ObjectMapper();

    try {
      Response response = mapper.readValue(responseStr, Response.class);
      ResponseParams params = response.getParams();
      if (result.status() != 200) {
        return response.getResponseCode().name();
      } else {
        return params.getStatus();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  public int getResponseStatus(Result result) {
    return result.status();
  }
}
