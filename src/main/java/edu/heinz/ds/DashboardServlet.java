package edu.heinz.ds;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Author: [Your Name]
 * Andrew ID: [Your Andrew ID]
 * 
 * Dashboard servlet that displays analytics and logs from MongoDB
 */
public class DashboardServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            MongoDatabase database = MongoDbConnection.getDatabase();
            MongoCollection<Document> collection = database.getCollection(MongoDbConnection.getCollectionName());
            
            // Get all logs
            List<Document> allLogs = new ArrayList<>();
            for (Document doc : collection.find().sort(new Document("request_timestamp", -1))) {
                allLogs.add(doc);
            }
            
            // Calculate analytics
            Map<String, Object> analytics = calculateAnalytics(allLogs);
            
            // Set attributes for JSP
            request.setAttribute("analytics", analytics);
            request.setAttribute("logs", allLogs);
            request.setAttribute("totalRequests", allLogs.size());
            
            // Forward to JSP
            request.getRequestDispatcher("/dashboard/dashboard.jsp").forward(request, response);
            
        } catch (Exception e) {
            response.setContentType("text/html");
            response.getWriter().println("<h1>Error loading dashboard</h1>");
            response.getWriter().println("<p>" + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate analytics from logs
     */
    private Map<String, Object> calculateAnalytics(List<Document> logs) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (logs.isEmpty()) {
            analytics.put("topBreeds", new ArrayList<>());
            analytics.put("averageLatency", 0.0);
            analytics.put("totalRequests", 0);
            analytics.put("successRate", 0.0);
            return analytics;
        }
        
        // 1. Top 10 most searched breeds
        Map<String, Integer> breedCounts = new HashMap<>();
        for (Document log : logs) {
            String breedQuery = log.getString("breed_query");
            if (breedQuery != null && !breedQuery.equals("random")) {
                breedCounts.put(breedQuery, breedCounts.getOrDefault(breedQuery, 0) + 1);
            }
        }
        List<Map.Entry<String, Integer>> topBreeds = new ArrayList<>(breedCounts.entrySet());
        topBreeds.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map<String, Object>> topBreedsList = new ArrayList<>();
        for (int i = 0; i < Math.min(10, topBreeds.size()); i++) {
            Map<String, Object> breedInfo = new HashMap<>();
            breedInfo.put("breed", topBreeds.get(i).getKey());
            breedInfo.put("count", topBreeds.get(i).getValue());
            topBreedsList.add(breedInfo);
        }
        analytics.put("topBreeds", topBreedsList);
        
        // 2. Average API latency
        long totalLatency = 0;
        int latencyCount = 0;
        for (Document log : logs) {
            if (log.containsKey("api_latency_ms")) {
                totalLatency += log.getLong("api_latency_ms");
                latencyCount++;
            }
        }
        double avgLatency = latencyCount > 0 ? (double) totalLatency / latencyCount : 0.0;
        analytics.put("averageLatency", Math.round(avgLatency * 100.0) / 100.0);
        
        // 3. Total requests
        analytics.put("totalRequests", logs.size());
        
        // 4. Success rate
        int successCount = 0;
        for (Document log : logs) {
            String status = log.getString("response_status");
            if ("success".equals(status)) {
                successCount++;
            }
        }
        double successRate = logs.size() > 0 ? (double) successCount / logs.size() * 100 : 0.0;
        analytics.put("successRate", Math.round(successRate * 100.0) / 100.0);
        
        // 5. Most common user agents (top 5)
        Map<String, Integer> userAgentCounts = new HashMap<>();
        for (Document log : logs) {
            String userAgent = log.getString("user_agent");
            if (userAgent != null && !userAgent.equals("Unknown")) {
                // Extract device info from user agent
                String device = extractDeviceInfo(userAgent);
                userAgentCounts.put(device, userAgentCounts.getOrDefault(device, 0) + 1);
            }
        }
        List<Map.Entry<String, Integer>> topDevices = new ArrayList<>(userAgentCounts.entrySet());
        topDevices.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        List<Map<String, Object>> topDevicesList = new ArrayList<>();
        for (int i = 0; i < Math.min(5, topDevices.size()); i++) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("device", topDevices.get(i).getKey());
            deviceInfo.put("count", topDevices.get(i).getValue());
            topDevicesList.add(deviceInfo);
        }
        analytics.put("topDevices", topDevicesList);
        
        return analytics;
    }
    
    /**
     * Extract device information from user agent string
     */
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Android")) {
            // Extract Android version/model if possible
            if (userAgent.contains("Mobile")) {
                return "Android Mobile";
            }
            return "Android";
        }
        return "Other";
    }
}

