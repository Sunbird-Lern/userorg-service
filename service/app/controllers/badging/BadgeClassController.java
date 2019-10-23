package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.badging.validator.BadgeClassValidator;
import play.libs.Files;
import play.mvc.Http;
import play.mvc.Result;
import util.Common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.sunbird.common.models.util.BadgingActorOperations;
import org.sunbird.common.models.util.BadgingJsonKey;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * BadgeClassController handles BadgeClass APIs.
 *
 * @author B Vinaya Kumar
 */
public class BadgeClassController extends BaseController {
  /**
   * Create a new badge class for a particular issuer.
   *
   * <p>Request body contains following parameters: issuerId: The ID of the Issuer to be owner of
   * the new Badge Class name: The name of the Badge Class description: A short description of the
   * new Badge Class. image: An image to represent the Badge Class. criteria: Either a text string
   * or a URL of a remotely hosted page describing the criteria rootOrgId: Root organisation ID
   * type: Badge class type (user / content) subtype: Badge class subtype (e.g. award) roles: JSON
   * array of roles (e.g. [ "OFFICIAL_TEXTBOOK_BADGE_ISSUER" ])
   *
   * @return Return a promise for create badge class API result.
   */
  public CompletionStage<Result> createBadgeClass(Http.Request httpRequest) {
    ProjectLogger.log("createBadgeClass called", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.CREATE_BADGE_CLASS.getValue(), httpRequest);

      HashMap<String, Object> map = new HashMap<>();

      Http.MultipartFormData multipartBody = httpRequest.body().asMultipartFormData();

      if (multipartBody != null) {
        Map<String, String[]> data = multipartBody.asFormUrlEncoded();
        for (Map.Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }

        List<Http.MultipartFormData.FilePart<Files.TemporaryFile>> imageFilePart = multipartBody.getFiles();
        if (imageFilePart.size() > 0) {
          InputStream inputStream = new FileInputStream(imageFilePart.get(0).getRef().path().toFile());
          byte[] imageByteArray = IOUtils.toByteArray(inputStream);
          map.put(JsonKey.IMAGE, imageByteArray);
        }
      }

      request.setRequest(map);

      new BadgeClassValidator().validateCreateBadgeClass(request, Common.getRequestHeadersInArray(httpRequest.getHeaders().toMap()));

      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("createBadgeClass: exception = ", e);

      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Get details of requsted badge class.
   *
   * @param badgeId The ID of the Badge Class whose details to view
   * @return Return a promise for get badge class API result.
   */
  public CompletionStage<Result> getBadgeClass(String badgeId, Http.Request httpRequest) {
    ProjectLogger.log("getBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.GET_BADGE_CLASS.getValue(), httpRequest);
      request.put(BadgingJsonKey.BADGE_ID, badgeId);

      new BadgeClassValidator().validateGetBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("getBadgeClass: exception = ", e);

      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Get list of badge classes for given issuer(s) and matching given context.
   *
   * <p>Request containing following filters: issuerList: List of Issuer IDs whose badge classes are
   * to be listed badgeList: List of badge IDs whose badge classes are to be listed rootOrgId: Root
   * organisation ID type: Badge class type (user / content) subtype: Badge class subtype (e.g.
   * award) roles: JSON array of roles (e.g. [ "OFFICIAL_TEXTBOOK_BADGE_ISSUER" ])
   *
   * @return Return a promise for search badge class API result.
   */
  public CompletionStage<Result> searchBadgeClass(Http.Request httpRequest) {
    ProjectLogger.log("searchBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      JsonNode bodyJson = httpRequest.body().asJson();

      Request request =
          createAndInitRequest(BadgingActorOperations.SEARCH_BADGE_CLASS.getValue(), bodyJson, httpRequest);

      new BadgeClassValidator().validateSearchBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("searchBadgeClass: exception = ", e);

      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }

  /**
   * Delete a badge class that has never been issued.
   *
   * @param badgeId The ID of the Badge Class to delete
   * @return Return a promise for delete badge class API result.
   */
  public CompletionStage<Result> deleteBadgeClass(String badgeId, Http.Request httpRequest) {
    ProjectLogger.log("deleteBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.DELETE_BADGE_CLASS.getValue(), httpRequest);
      request.put(BadgingJsonKey.BADGE_ID, badgeId);

      new BadgeClassValidator().validateDeleteBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, httpRequest);
    } catch (Exception e) {
      ProjectLogger.log("deleteBadgeClass: exception = ", e);

      return CompletableFuture.completedFuture(createCommonExceptionResponse(e, httpRequest));
    }
  }
}
