import Model.ChannelPool;
import Model.LiftRide;
import Model.Message;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "SkiersServlet", urlPatterns = "/skiers/*", loadOnStartup = 1)
public class SkiersServlet extends HttpServlet {
    private ChannelPool channelPool;
    private final static String QUEUE_NAME = "LIFTRIDE";

    @Override
    public void init() throws ServletException {
        System.out.println("Servlet initializing...");
        super.init();
        try {
            this.channelPool = new ChannelPool();
            Channel channel = channelPool.takeChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channelPool.add(channel);
        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new ServletException("Failed to initialize ChannelPool or declare queue", e);
        }
        System.out.println("Servlet initialized.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        Message message = new Message();
        String urlPath = req.getPathInfo();
        String resJson = null;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // 404 - URL Not Exists
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            message.setMessage("Missing Path Parameters");
            resJson = gson.toJson(message);
            PrintWriter out = res.getWriter();
            out.write(resJson);
            out.flush();
            return;
        }

        // 400 - URL Wrong Format
        if (!isValUrlPath(urlPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            message.setMessage("Invalid URL Format");
            resJson = gson.toJson(message);
        } else {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            } finally {
                reader.close();
            }
            String json = sb.toString();

            // 400 - Invalid LiftRide Information
            if (!isValLiftRide(json)) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                message.setMessage("Invalid LiftRide Inputs");
                resJson = gson.toJson(message);
            } else {
                String[] pathData = handlePathData(urlPath);
                String resortID = pathData[0];
                String seasonID = pathData[1];
                String dayID = pathData[2];
                String skierID = pathData[3];
                LiftRide liftRide = gson.fromJson(json, LiftRide.class);
                liftRide.setResortID(resortID);
                liftRide.setSeasonID(seasonID);
                liftRide.setDayID(dayID);
                liftRide.setSkierID(skierID);
                String jsonMessage = gson.toJson(liftRide);

                try {
                    Channel channel = channelPool.takeChannel();
                    channel.basicPublish("", QUEUE_NAME, null, jsonMessage.getBytes(StandardCharsets.UTF_8));
                    channelPool.add(channel);
                    // 201
                    res.setStatus(HttpServletResponse.SC_CREATED);
                    message.setMessage("Create and Put into Queue Successfully!");
                    resJson = gson.toJson(message);
                    System.out.println(" [x] Sent '" + jsonMessage + "'");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    message.setMessage("oops...UNSUCCESSFUL");
                    resJson = gson.toJson(message);
                }
            }
        }
        PrintWriter out = res.getWriter();
        out.write(resJson);
        out.flush();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write("It works!");
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (channelPool != null) {
            try {
                channelPool.close();
            } catch (Exception e) {
                System.err.println("Error closing ChannelPool: " + e.getMessage());
            }
        }
        System.out.println("Servlet destroyed.");
    }


    private boolean isUrlValid(String[] urlPath) {
        return true;
    }

    protected boolean isValUrlPath(String urlPath) {
        String[] pathParts = urlPath.split("/");
        if (pathParts.length == 8) {
            return isValSkiersUrl(urlPath);
        }
        return false;
    }

    protected boolean isValSkiersUrl(String urlPath) {
        String[] pathParts = urlPath.split("/");
        if (!pathParts[2].equals("seasons") || !pathParts[4].equals("days") || !pathParts[6].equals("skiers")) {
            return false;
        }
        try {
            for (int i = 1; i < 8; i += 2) {
                Integer.parseInt(pathParts[i]);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isValLiftRide(String json) {
        Gson gson = new Gson();
        LiftRide liftRide = gson.fromJson(json, LiftRide.class);
        return liftRide.getLiftID() != null && liftRide.getTime() != null;
    }

    protected String[] handlePathData(String urlPath) {
        String[] pathParts = urlPath.split("/");
        return new String[]{pathParts[1], pathParts[3], pathParts[5], pathParts[7]};
    }
}

