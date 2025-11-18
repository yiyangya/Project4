<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="org.bson.Document" %>
<%@ page import="java.text.SimpleDateFormat" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Cat Explorer - Operations Dashboard</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 {
            color: #333;
            border-bottom: 3px solid #4CAF50;
            padding-bottom: 10px;
        }
        h2 {
            color: #555;
            margin-top: 30px;
        }
        .analytics {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        .metric {
            background-color: #f9f9f9;
            padding: 15px;
            border-radius: 5px;
            border-left: 4px solid #4CAF50;
        }
        .metric-label {
            font-size: 14px;
            color: #666;
            margin-bottom: 5px;
        }
        .metric-value {
            font-size: 24px;
            font-weight: bold;
            color: #333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #4CAF50;
            color: white;
            font-weight: bold;
        }
        tr:hover {
            background-color: #f5f5f5;
        }
        .top-breeds {
            margin: 20px 0;
        }
        .breed-item {
            padding: 8px;
            margin: 5px 0;
            background-color: #e8f5e9;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🐱 Cat Explorer - Operations Dashboard</h1>
        
        <%
            Map<String, Object> analytics = (Map<String, Object>) request.getAttribute("analytics");
            List<Document> logs = (List<Document>) request.getAttribute("logs");
            int totalRequests = logs != null ? logs.size() : 0;
        %>
        
        <h2>Operations Analytics</h2>
        <div class="analytics">
            <div class="metric">
                <div class="metric-label">Total Requests</div>
                <div class="metric-value"><%= totalRequests %></div>
            </div>
            <div class="metric">
                <div class="metric-label">Average API Latency</div>
                <div class="metric-value"><%= analytics != null ? analytics.get("averageLatency") : "0" %> ms</div>
            </div>
            <div class="metric">
                <div class="metric-label">Success Rate</div>
                <div class="metric-value"><%= analytics != null ? analytics.get("successRate") : "0" %>%</div>
            </div>
        </div>
        
        <h2>Top 10 Most Searched Breeds</h2>
        <div class="top-breeds">
            <%
                List<Map<String, Object>> topBreeds = (List<Map<String, Object>>) (analytics != null ? analytics.get("topBreeds") : new ArrayList<Map<String, Object>>());
                if (topBreeds == null || topBreeds.isEmpty()) {
            %>
                <p>No breed searches yet.</p>
            <%
                } else {
                    for (Map<String, Object> breed : topBreeds) {
            %>
                <div class="breed-item">
                    <strong><%= breed.get("breed") %></strong> - <%= breed.get("count") %> search(es)
                </div>
            <%
                    }
                }
            %>
        </div>
        
        <h2>Top 5 Devices</h2>
        <div class="top-breeds">
            <%
                List<Map<String, Object>> topDevices = (List<Map<String, Object>>) (analytics != null ? analytics.get("topDevices") : new ArrayList<Map<String, Object>>());
                if (topDevices == null || topDevices.isEmpty()) {
            %>
                <p>No device data yet.</p>
            <%
                } else {
                    for (Map<String, Object> device : topDevices) {
            %>
                <div class="breed-item">
                    <strong><%= device.get("device") %></strong> - <%= device.get("count") %> request(s)
                </div>
            <%
                    }
                }
            %>
        </div>
        
        <h2>Request Logs</h2>
        <table>
            <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Breed Query</th>
                    <th>Breed Name</th>
                    <th>Client IP</th>
                    <th>User Agent</th>
                    <th>API Latency (ms)</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <%
                    if (logs == null || logs.isEmpty()) {
                %>
                    <tr>
                        <td colspan="7" style="text-align: center; padding: 20px;">
                            No logs available yet. Make some requests from the Android app!
                        </td>
                    </tr>
                <%
                    } else {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        for (Document log : logs) {
                            Date timestamp = log.getDate("request_timestamp");
                            String breedQuery = log.getString("breed_query");
                            String breedName = log.getString("returned_breed_name");
                            String clientIp = log.getString("client_ip");
                            String userAgent = log.getString("user_agent");
                            Long apiLatency = log.getLong("api_latency_ms");
                            String status = log.getString("response_status");
                            
                            // Truncate user agent if too long
                            if (userAgent != null && userAgent.length() > 50) {
                                userAgent = userAgent.substring(0, 47) + "...";
                            }
                %>
                    <tr>
                        <td><%= timestamp != null ? dateFormat.format(timestamp) : "N/A" %></td>
                        <td><%= breedQuery != null ? breedQuery : "random" %></td>
                        <td><%= breedName != null ? breedName : "N/A" %></td>
                        <td><%= clientIp != null ? clientIp : "N/A" %></td>
                        <td><%= userAgent != null ? userAgent : "Unknown" %></td>
                        <td><%= apiLatency != null ? apiLatency : "N/A" %></td>
                        <td style="color: <%= "success".equals(status) ? "green" : "red" %>;">
                            <%= status != null ? status.toUpperCase() : "UNKNOWN" %>
                        </td>
                    </tr>
                <%
                        }
                    }
                %>
            </tbody>
        </table>
    </div>
</body>
</html>

