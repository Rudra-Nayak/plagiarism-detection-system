package api;

import algorithms.KMPMatcher;
import algorithms.SimilarityCalculator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Lightweight, native Java web server listening on port 8080.
 * Exposes a POST endpoint at /api/analyze.
 * Zero external dependencies.
 */
public class AppServer {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/analyze", new AnalyzeHandler());
            // Create a default executor (null uses a default implementation)
            server.setExecutor(null);
            System.out.println("Backend AppServer is running on port " + PORT + "...");
            System.out.println("Endpoint available: POST http://localhost:" + PORT + "/api/analyze");
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS Preflight request
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1); // No Content
                exchange.close();
                return;
            }

            // Handle main POST request
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    // Start timer
                    long startTime = System.nanoTime();

                    // Read request body
                    String body = readRequestBody(exchange);

                    // Manually parse JSON strings
                    String sourceText = extractJsonString(body, "sourceText");
                    String suspiciousText = extractJsonString(body, "suspiciousText");

                    // Calculate Cosine Similarity and track collisions
                    int[] collisionContainer = new int[1];
                    double similarityScore = SimilarityCalculator.calculateCosineSimilarity(
                        sourceText, suspiciousText, collisionContainer
                    );

                    // Run Knuth-Morris-Pratt (KMP) matching engine
                    List<KMPMatcher.MatchSpan> matches = KMPMatcher.findMatches(sourceText, suspiciousText);

                    // Calculate mapping tokens (words) to report in DSA Metrics card
                    int sourceWordCount = preprocessing.TextNormalizer.tokenize(sourceText).size();
                    int suspiciousWordCount = preprocessing.TextNormalizer.tokenize(suspiciousText).size();
                    int totalTokensMapped = sourceWordCount + suspiciousWordCount;

                    // Stop timer and convert to milliseconds
                    long endTime = System.nanoTime();
                    double executionTimeMs = (endTime - startTime) / 1000000.0;

                    // Formulate JSON response string
                    String jsonResponse = buildJsonResponse(
                        similarityScore, 
                        executionTimeMs, 
                        collisionContainer[0], 
                        totalTokensMapped, 
                        matches
                    );

                    // Send response
                    byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                    setCorsHeaders(exchange);
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }

                } catch (Exception e) {
                    System.err.println("Error processing request: " + e.getMessage());
                    e.printStackTrace();
                    
                    String errorJson = "{\"error\":\"Internal Server Error: " + escapeJson(e.getMessage()) + "\"}";
                    byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);
                    setCorsHeaders(exchange);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, errorBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                    }
                }
            } else {
                // Method Not Allowed
                setCorsHeaders(exchange);
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        }

        /**
         * Sets the standard CORS headers to allow cross-origin requests from the browser frontend.
         */
        private void setCorsHeaders(HttpExchange exchange) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }

        /**
         * Reads the input stream of the request into a String.
         */
        private String readRequestBody(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }

        /**
         * Extracts a string value for a key in a JSON object.
         * Assumes standard JSON structure from JS fetch (keys and values enclosed in double quotes).
         */
        private String extractJsonString(String json, String key) {
            String searchKey = "\"" + key + "\"";
            int keyIdx = json.indexOf(searchKey);
            if (keyIdx == -1) return "";

            int colonIdx = json.indexOf(":", keyIdx + searchKey.length());
            if (colonIdx == -1) return "";

            int startQuote = json.indexOf("\"", colonIdx + 1);
            if (startQuote == -1) return "";

            StringBuilder sb = new StringBuilder();
            boolean isEscaped = false;

            for (int i = startQuote + 1; i < json.length(); i++) {
                char c = json.charAt(i);

                if (isEscaped) {
                    if (c == '"') sb.append('"');
                    else if (c == '\\') sb.append('\\');
                    else if (c == 'n') sb.append('\n');
                    else if (c == 'r') sb.append('\r');
                    else if (c == 't') sb.append('\t');
                    else if (c == 'f') sb.append('\f');
                    else if (c == 'b') sb.append('\b');
                    else if (c == '/') sb.append('/');
                    else sb.append('\\').append(c);
                    isEscaped = false;
                } else if (c == '\\') {
                    isEscaped = true;
                } else if (c == '"') {
                    break; // Closing double-quote found, exit
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        /**
         * Formulates the JSON response from inputs.
         */
        private String buildJsonResponse(double similarityScore, double executionTimeMs, int collisions, int tokensMapped, List<KMPMatcher.MatchSpan> matches) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"similarityScore\":").append(String.format("%.2f", similarityScore)).append(",");
            sb.append("\"executionTimeMs\":").append(String.format("%.4f", executionTimeMs)).append(",");
            sb.append("\"collisions\":").append(collisions).append(",");
            sb.append("\"tokensMapped\":").append(tokensMapped).append(",");
            sb.append("\"matches\":[");

            for (int i = 0; i < matches.size(); i++) {
                KMPMatcher.MatchSpan match = matches.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"text\":\"").append(escapeJson(match.text)).append("\",");
                sb.append("\"suspiciousStart\":").append(match.suspiciousStart).append(",");
                sb.append("\"suspiciousEnd\":").append(match.suspiciousEnd).append(",");
                sb.append("\"sourceStart\":").append(match.sourceStart).append(",");
                sb.append("\"sourceEnd\":").append(match.sourceEnd);
                sb.append("}");
            }
            sb.append("]");
            sb.append("}");
            return sb.toString();
        }

        /**
         * Escapes string values for valid JSON output.
         */
        private String escapeJson(String input) {
            if (input == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == '"') sb.append("\\\"");
                else if (c == '\\') sb.append("\\\\");
                else if (c == '\n') sb.append("\\n");
                else if (c == '\r') sb.append("\\r");
                else if (c == '\t') sb.append("\\t");
                else if (c < 32) {
                    String hex = Integer.toHexString(c);
                    sb.append("\\u").append("0000".substring(hex.length())).append(hex);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
