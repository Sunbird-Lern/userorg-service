package controllers.location.validator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;

/** Created by arvind on 19/4/18. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
public class LocationRequestValidatorTest {

  LocationRequestValidator validator = new LocationRequestValidator();

  @Test
  public void validateCreateLocationRequestTest() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, "CAL");
    locationData.put(JsonKey.CODE, "CA");
    locationData.put(JsonKey.TYPE, "STATE");
    requestBody.put(JsonKey.DATA, locationData);
    request.setRequest(requestBody);
    try {
      validator.validateCreateLocationRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateCreateLocationRequestTestWithInvalidData() {
    Request request = new Request();
    Map<String, Object> requestBody = new HashMap<>();
    Map<String, Object> locationData = new HashMap<>();
    locationData.put(JsonKey.NAME, "CAL");
    locationData.put(JsonKey.CODE, "CA");
    requestBody.put(JsonKey.DATA, locationData);
    request.setRequest(requestBody);
    validator.validateCreateLocationRequest(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateCreateLocationRequestTestWithEmptyData() {
    Request request = new Request();
    validator.validateCreateLocationRequest(request);
  }

  @Test
  public void validateUpdateLocationRequestTest() {
    Request request = new Request();
    boolean response = false;
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.ID, "123");
    requestBody.put(JsonKey.CODE, "CA");
    requestBody.put(JsonKey.TYPE, "STATE");
    request.setRequest(requestBody);
    try {
      validator.validateUpdateLocationRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateUpdateLocationRequestTestWithInvalidData() {
    Request request = new Request();
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(JsonKey.TYPE, "STATE");
    request.setRequest(requestBody);
    validator.validateUpdateLocationRequest(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateUpdateLocationRequestTestWithEmptyData() {
    Request request = new Request();
    validator.validateUpdateLocationRequest(request);
  }

  @Test
  public void validateDeleteLocationRequestTest() {
    boolean response = false;
    try {
      validator.validateDeleteLocationRequest("123");
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateDeleteLocationRequestTestWithInvalidRequestData() {
    validator.validateDeleteLocationRequest("");
  }

  @Test
  public void validateSearchLocationRequestTest() {
    Request request = new Request();
    Map<String, Object> requestBody = new HashMap<>();
    boolean response = false;
    Map<String, Object> filters = new HashMap<>();
    filters.put(JsonKey.NAME, "CAL");
    filters.put(JsonKey.CODE, "CA");
    filters.put(JsonKey.TYPE, "STATE");
    requestBody.put(JsonKey.FILTERS, filters);
    request.setRequest(requestBody);
    try {
      validator.validateSearchLocationRequest(request);
      response = true;
    } catch (ProjectCommonException e) {
      Assert.assertNull(e);
    }
    assertEquals(true, response);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateSearchLocationRequestTestWithInvalid() {
    Request request = new Request();
    Map<String, Object> requestBody = new HashMap<>();
    List<String> filters = new ArrayList<>();
    filters.add("CAL");
    requestBody.put(JsonKey.FILTERS, filters);
    request.setRequest(requestBody);
    validator.validateSearchLocationRequest(request);
  }

  @Test(expected = ProjectCommonException.class)
  public void validateSearchLocationRequestTestWithEmptyBody() {
    Request request = new Request();
    validator.validateSearchLocationRequest(request);
  }
}
