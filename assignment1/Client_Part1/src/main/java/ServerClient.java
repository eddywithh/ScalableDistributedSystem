import cs6650.Model.LiftRideEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import com.google.gson.Gson;

public class ServerClient {
    private HttpClient httpClient;

    public ServerClient() {
        httpClient = HttpClients.createDefault();
    }

    public boolean sendLiftRideEvent(LiftRideEvent event) throws IOException {
        int resortID = event.getResortId();
        String  seasonID = event.getSeasonId();
        String dayID = event.getDayId();
        int skierID = event.getSkierId();

        HttpPost postRequest = new HttpPost(String.format(
                "http://localhost:8080/Servlet_war_exploded/skiers/%d/seasons/%s/days/%s/skiers/%d",
                resortID, seasonID, dayID, skierID));

        String json = "{" + "\"time\": " + event.getTime() + "," +
                "\"liftID\": " + event.getLiftID() + "}";
//        System.out.println(json);
        StringEntity entity = new StringEntity(json);
        postRequest.setEntity(entity);
        postRequest.setHeader("Content-Type", "application/json");


        HttpResponse response = httpClient.execute(postRequest);
        int statusCode = response.getStatusLine().getStatusCode();

//        System.out.println("Response Status: " + statusCode);
        try {
            String responseBody = EntityUtils.toString(response.getEntity());
//            System.out.println("Response Body: " + responseBody);
        } catch (IOException e) {
//            System.err.println("Invalid Body: " + e.getMessage());
        }

        if (statusCode == 201) {
//            System.out.println("successful request: ");
            return true;
        } else {
//            System.out.println("failed request: " + statusCode);
            return false;
        }
    }
}
