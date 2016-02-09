package data.hci.gdata.Network;


import com.google.gson.JsonObject;

import retrofit.RestAdapter;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Created by user on 2016-01-25.
 */
public class RestClient {
    private static RestClient instance;

    private RestAdapter restAdapter;
    private RestRequest restRequest;


    private static final String url = "http://203.246.112.184:8081/GData";

    public static RestClient newInstance(){
        if(instance == null){
            instance = new RestClient();
        }
        return instance;
    }

    public RestClient(){
        restAdapter = new RestAdapter.Builder()
                .setEndpoint(url).build();
        restRequest = restAdapter.create(RestRequest.class);

    }

    public interface RestRequest{

        @FormUrlEncoded
        @POST("/requestRecommend")
        void requestRecommend(@Field("reqLat") double Latitude,
                               @Field("reqLong") double Longitude,
                               retrofit.Callback<JsonObject> recommendCallback);

    }

    /**
     * 추천 장소 요청
     * */
    public void requestRecommend(double Latitude, double Longitude, retrofit.Callback<JsonObject> recommendCallback){
        restRequest.requestRecommend(Latitude, Longitude, recommendCallback);
    }
}
