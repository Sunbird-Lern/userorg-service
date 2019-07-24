package controllers.usermanagement;

import controllers.BaseController;
import org.sunbird.common.models.response.Response;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Results;

public class UserFreeUpController extends BaseController {

    public Promise<Result> freeUp(){
        return Promise.pure(Results.ok(Json.toJson(getDummyResponse())));
    }
    /**
     * This is temporary method we use get dummyresponse to check freeup APIs.
     *
     * @return string
     */
    public Response getDummyResponse() {

        Response response=new Response();
        response.put("message","success");
        response.setId("api.user.freeup");
        response.setVer("v1");
        return response;
    }
}
