package controllers.badging;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.badging.validator.BadgeClassValidator;
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
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

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
  public F.Promise<Result> createBadgeClass() {
    ProjectLogger.log("createBadgeClass called", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.CREATE_BADGE_CLASS.getValue());

      HashMap<String, Object> map = new HashMap<>();

      Http.MultipartFormData multipartBody = request().body().asMultipartFormData();

      if (multipartBody != null) {
        Map<String, String[]> data = multipartBody.asFormUrlEncoded();
        for (Map.Entry<String, String[]> entry : data.entrySet()) {
          map.put(entry.getKey(), entry.getValue()[0]);
        }

        List<Http.MultipartFormData.FilePart> imageFilePart = multipartBody.getFiles();
        if (imageFilePart.size() > 0) {
          InputStream inputStream = new FileInputStream(imageFilePart.get(0).getFile());
          byte[] imageByteArray = IOUtils.toByteArray(inputStream);
          map.put(JsonKey.IMAGE, imageByteArray);
        }
      }

      request.setRequest(map);

      new BadgeClassValidator().validateCreateBadgeClass(request, request().headers());

      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("createBadgeClass: exception = ", e);

      return F.Promise.pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Get details of requsted badge class.
   *
   * @param badgeId The ID of the Badge Class whose details to view
   * @return Return a promise for get badge class API result.
   */
  public F.Promise<Result> getBadgeClass(String badgeId) {
    ProjectLogger.log("getBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.GET_BADGE_CLASS.getValue());
      request.put(BadgingJsonKey.BADGE_ID, badgeId);

      new BadgeClassValidator().validateGetBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("getBadgeClass: exception = ", e);

      return F.Promise.pure(createCommonExceptionResponse(e, request()));
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
  public F.Promise<Result> searchBadgeClass() {
    ProjectLogger.log("searchBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      JsonNode bodyJson = request().body().asJson();

      Request request =
          createAndInitRequest(BadgingActorOperations.SEARCH_BADGE_CLASS.getValue(), bodyJson);

      new BadgeClassValidator().validateSearchBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("searchBadgeClass: exception = ", e);

      return F.Promise.pure(createCommonExceptionResponse(e, request()));
    }
  }

  /**
   * Delete a badge class that has never been issued.
   *
   * @param badgeId The ID of the Badge Class to delete
   * @return Return a promise for delete badge class API result.
   */
  public F.Promise<Result> deleteBadgeClass(String badgeId) {
    ProjectLogger.log("deleteBadgeClass called.", LoggerEnum.DEBUG.name());

    try {
      Request request = createAndInitRequest(BadgingActorOperations.DELETE_BADGE_CLASS.getValue());
      request.put(BadgingJsonKey.BADGE_ID, badgeId);

      new BadgeClassValidator().validateDeleteBadgeClass(request);

      return actorResponseHandler(getActorRef(), request, timeout, null, request());
    } catch (Exception e) {
      ProjectLogger.log("deleteBadgeClass: exception = ", e);

      return F.Promise.pure(createCommonExceptionResponse(e, request()));
    }
  }
}
