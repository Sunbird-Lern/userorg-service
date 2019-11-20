package controllers.feed;

import controllers.BaseController;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.Results;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FeedController extends BaseController {


    public CompletionStage<Result> feed(String userId,Http.Request httpRequest) {
        Response response=new Response();
        response.setResponseCode(ResponseCode.OK);
        response.put(JsonKey.RESPONSE,prepareResponse((int)(Math.random() * 50 + 1),userId));
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        future.complete(Json.toJson(response));
        return future.thenApplyAsync(Results::ok);
    }

    private Map<String,Object> prepareResponse(int randVal,String userId){

        Map<String,Object>responseMap=new HashMap<>();
        List<Map<String,Object>>userFeed=new ArrayList<>();
        Map<String,Object>feedData=new HashMap<>();
        Map<String,Object>feedMap=new HashMap<>();
        if(randVal%2==0) {
            feedMap.put(JsonKey.CHANNEL, new ArrayList<>(Arrays.asList("TN", "RJ", "AP")));
        }
        else{
            feedMap.put(JsonKey.CHANNEL, new ArrayList<>(Arrays.asList("TN")));
        }
        feedMap.put(JsonKey.ORDER,1);
        feedData.put("category","orgMigrationAction");
        feedData.put(JsonKey.USER_ID,userId);
        feedData.put(JsonKey.ID, ProjectUtil.getUniqueIdFromTimestamp(1));
        feedData.put("feedAction","unRead");
        feedData.put("expireOn",System.currentTimeMillis());
        feedData.put("closable",false);
        feedData.put("channel","TN");
        feedData.put(JsonKey.CREATED_ON,System.currentTimeMillis());
        feedData.put(JsonKey.CREATED_BY,userId);
        feedData.put("priority",1);
        feedData.put("feedData",feedMap);
        userFeed.add(feedData);
        responseMap.put("userFeed",userFeed);
        return responseMap;
    }
}
