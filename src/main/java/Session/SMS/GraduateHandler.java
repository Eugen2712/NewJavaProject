package Session.SMS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class GraduateHandler implements HttpHandler {
    private GraduateStudentProcessor graduateStudentProcessor;
    private ObjectMapper objectMapper = new ObjectMapper();

    public GraduateHandler(StudentService studentService) {
        this.graduateStudentProcessor = new GraduateStudentProcessor(studentService);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!Authentication.authenticate(exchange)) {
            return;
        }
        if (!exchange.getRequestMethod().equals("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String requestBody = new String(exchange.getRequestBody().readAllBytes());
        JsonNode jsonNode = objectMapper.readTree(requestBody);
        int studentId = jsonNode.get("id").asInt();
        double gpa = jsonNode.get("gpa").asDouble();
        try {
            Logger.log("New Graduate Student request");
            GraduateStudent graduateStudent = graduateStudentProcessor.graduateStudent(studentId, gpa);
            String response = objectMapper.writeValueAsString(graduateStudent);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            exchange.sendResponseHeaders(400, -1);
        }
    }
}