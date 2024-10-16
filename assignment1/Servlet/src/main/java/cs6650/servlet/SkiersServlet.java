package cs6650.servlet;

import com.google.gson.Gson;
import cs6650.servlet.Model.ErrorMessage;
import cs6650.servlet.Model.LiftRide;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "SkiersServlet", value = "/SkiersServlet")
public class SkiersServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Missing path parameters");
            return;
        }

        String[] pathParts = pathInfo.split("/");
        if (pathParts.length != 8) {
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format");
            return;
        }

        try {
            int resortID = Integer.parseInt(pathParts[1]);
            String seasonID = pathParts[3];
            int dayID = Integer.parseInt(pathParts[5]);
            int skierID = Integer.parseInt(pathParts[7]);

            if (dayID < 1 || dayID > 366) {
                sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid dayID, must be between 1 and 366");
                return;
            }

            Gson gson = new Gson();
            LiftRide liftRide = gson.fromJson(req.getReader(), LiftRide.class);

            if (liftRide.getLiftID() <= 0 || liftRide.getTime() <= 0) {
                sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid lift ride data. LiftId and Time must be positive.");
                return;
            }

            res.setStatus(HttpServletResponse.SC_CREATED);
            res.setContentType("application/json");
            gson.toJson(liftRide, res.getWriter());

        } catch (NumberFormatException e) {
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric value in URL");
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
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

    private boolean isUrlValid(String[] urlPath) {
        return true;
    }

    private void sendErrorResponse(HttpServletResponse res, int statusCode, String message) throws IOException {
        res.setStatus(statusCode);
        res.setContentType("application/json");
        Gson gson = new Gson();
        ErrorMessage errorMessage = new ErrorMessage(message, statusCode);
        gson.toJson(errorMessage, res.getWriter());
    }
}