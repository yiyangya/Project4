package edu.heinz.ds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Author: [Your Name]
 * Andrew ID: [Your Andrew ID]
 * 
 * Main servlet that handles cat search requests from Android app.
 * Calls The Cat API and logs requests to MongoDB.
 */
public class CatSearchServlet extends HttpServlet {
    
    private static final String CAT_API_KEY = "live_I2YIEkDYmNwd8Dy0AvswlEhkAWkqOL9FhsuLPnGbJA0dZuiEBq154eLFavlWbsEC";
    private static final String IMAGES_SEARCH_URL = "https://api.thecatapi.com/v1/images/search";
    private static final String BREEDS_SEARCH_URL = "https://api.thecatapi.com/v1/breeds/search";
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Get request parameters
        String breedQuery = request.getParameter("breed");
        String userAgent = request.getHeader("User-Agent");
        String clientIp = getClientIpAddress(request);
        long requestStartTime = System.currentTimeMillis();
        
        // Initialize response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Log entry to be stored in MongoDB
        Document logEntry = new Document();
        logEntry.append("request_timestamp", new Date());
        logEntry.append("client_ip", clientIp);
        logEntry.append("user_agent", userAgent != null ? userAgent : "Unknown");
        logEntry.append("breed_query", breedQuery != null ? breedQuery : "random");
        logEntry.append("request_start_time", requestStartTime);
        
        try {
            String jsonResponse;
            String apiUrl;
            long apiCallStartTime = System.currentTimeMillis();
            
            if (breedQuery == null || breedQuery.trim().isEmpty()) {
                // Get random cat
                apiUrl = IMAGES_SEARCH_URL + "?limit=1&has_breeds=true";
                jsonResponse = callCatApi(apiUrl);
            } else {
                // Search by breed name
                String encodedBreed = URLEncoder.encode(breedQuery.trim(), StandardCharsets.UTF_8);
                String breedsUrl = BREEDS_SEARCH_URL + "?q=" + encodedBreed;
                String breedsResponse = callCatApi(breedsUrl);
                
                String breedId = findBreedId(breedsResponse, breedQuery.trim());
                
                if (breedId != null) {
                    apiUrl = IMAGES_SEARCH_URL + "?breed_ids=" + breedId + "&limit=1";
                } else {
                    // Breed not found, get random
                    apiUrl = IMAGES_SEARCH_URL + "?limit=1&has_breeds=true";
                }
                jsonResponse = callCatApi(apiUrl);
            }
            
            long apiCallEndTime = System.currentTimeMillis();
            long apiLatency = apiCallEndTime - apiCallStartTime;
            
            // Parse and format response for Android app
            JsonObject formattedResponse = formatResponseForAndroid(jsonResponse);
            
            // Log API call information
            logEntry.append("api_url", apiUrl);
            logEntry.append("api_call_start_time", apiCallStartTime);
            logEntry.append("api_call_end_time", apiCallEndTime);
            logEntry.append("api_latency_ms", apiLatency);
            logEntry.append("api_response_status", "success");
            
            // Extract cat information for logging
            if (formattedResponse.has("breedName")) {
                logEntry.append("returned_breed_name", formattedResponse.get("breedName").getAsString());
            }
            if (formattedResponse.has("imageUrl")) {
                logEntry.append("returned_image_url", formattedResponse.get("imageUrl").getAsString());
            }
            
            long requestEndTime = System.currentTimeMillis();
            logEntry.append("request_end_time", requestEndTime);
            logEntry.append("total_request_latency_ms", requestEndTime - requestStartTime);
            logEntry.append("response_status", "success");
            
            // Send response to Android app
            out.print(formattedResponse.toString());
            out.flush();
            
        } catch (Exception e) {
            // Error handling
            long requestEndTime = System.currentTimeMillis();
            logEntry.append("request_end_time", requestEndTime);
            logEntry.append("total_request_latency_ms", requestEndTime - requestStartTime);
            logEntry.append("response_status", "error");
            logEntry.append("error_message", e.getMessage());
            
            // Send error response
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("success", false);
            errorResponse.addProperty("error", "Failed to fetch cat information: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(errorResponse.toString());
            out.flush();
        } finally {
            // Store log entry in MongoDB (don't log dashboard requests)
            if (!isDashboardRequest(request)) {
                try {
                    MongoDatabase database = MongoDbConnection.getDatabase();
                    MongoCollection<Document> collection = database.getCollection(MongoDbConnection.getCollectionName());
                    collection.insertOne(logEntry);
                } catch (Exception e) {
                    // Log error but don't fail the request
                    System.err.println("Failed to log to MongoDB: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Calls The Cat API
     */
    private String callCatApi(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(java.net.URI.create(url));
        request.setHeader("x-api-key", CAT_API_KEY);
        request.setHeader("Content-Type", "application/json");
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
            throw new IOException("Empty response from Cat API");
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * Finds breed ID from breeds search response
     */
    private String findBreedId(String breedsResponse, String searchTerm) {
        try {
            JsonParser parser = new JsonParser();
            JsonArray breedsArray = parser.parse(breedsResponse).getAsJsonArray();
            
            String searchLower = searchTerm.toLowerCase();
            for (int i = 0; i < breedsArray.size(); i++) {
                JsonObject breed = breedsArray.get(i).getAsJsonObject();
                String breedName = breed.has("name") ? breed.get("name").getAsString() : "";
                if (breedName.toLowerCase().contains(searchLower)) {
                    return breed.has("id") ? breed.get("id").getAsString() : null;
                }
            }
        } catch (Exception e) {
            // Return null if parsing fails
        }
        return null;
    }
    
    /**
     * Formats API response for Android app (only send what's needed)
     */
    private JsonObject formatResponseForAndroid(String jsonResponse) {
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(jsonResponse).getAsJsonArray();
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        
        if (jsonArray.size() > 0) {
            JsonObject catData = jsonArray.get(0).getAsJsonObject();
            
            // Extract only what Android app needs
            String imageUrl = catData.has("url") ? catData.get("url").getAsString() : "";
            result.addProperty("imageUrl", imageUrl);
            
            // Extract breed information
            if (catData.has("breeds") && catData.get("breeds").isJsonArray()) {
                JsonArray breeds = catData.get("breeds").getAsJsonArray();
                if (breeds.size() > 0) {
                    JsonObject breed = breeds.get(0).getAsJsonObject();
                    result.addProperty("breedName", breed.has("name") ? breed.get("name").getAsString() : "Unknown");
                    result.addProperty("origin", breed.has("origin") ? breed.get("origin").getAsString() : "Unknown");
                    result.addProperty("temperament", breed.has("temperament") ? breed.get("temperament").getAsString() : "Unknown");
                    result.addProperty("description", breed.has("description") ? breed.get("description").getAsString() : "No description available");
                }
            }
        } else {
            result.addProperty("error", "No cats found");
        }
        
        return result;
    }
    
    /**
     * Get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * Check if request is from dashboard (don't log dashboard requests)
     */
    private boolean isDashboardRequest(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return referer != null && referer.contains("/dashboard");
    }
}

