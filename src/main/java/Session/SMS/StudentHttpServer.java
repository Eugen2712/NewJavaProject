package Session.SMS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class StudentHttpServer {
    private static final int PORT = 8080;
    private StudentService studentService;
    private ObjectMapper objectMapper = new ObjectMapper();

    public StudentHttpServer(StudentService studentService) {
        this.studentService = studentService;
    }

    public static void main(String[] args) throws IOException {
        StudentRepository studentRepository = new FileStudentRepository("students.csv");
        StudentService studentService = new StudentService(studentRepository);
        StudentHttpServer server = new StudentHttpServer(studentService);
        server.start();
    }
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/students", new StudentHandler());
        server.createContext("/graduate", new GraduateHandler(studentService));
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }
    private class StudentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!Authentication.authenticate(exchange)) {
                return;
            }
            String method = exchange.getRequestMethod();
            String response = "";
            switch (method) {
                case "GET":
                    Logger.log("New GET request");
                    response = handleGet(exchange);
                    break;
                case "POST":
                    response = handlePost(exchange);
                    break;
                case "PUT":
                    response = handlePut(exchange);
                    break;
                case "PATCH":
                    response = handlePatch(exchange);
                    break;
                case "DELETE":
                    response = handleDelete(exchange);
                    break;
                default:
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private String handleGet(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int page = 0;
            int size = 5;
            if (query != null) {
                String[] queryParam = query.split("&");
                for (String param : queryParam) {
                    String[] keyValue = param.split("=");
                    if ("page".equals(keyValue[0])) {
                        page = Integer.parseInt(keyValue[1]);
                    }
                    if ("id".equals(keyValue[0])) {
                        Student student = studentService.getStudents().stream()
                                .filter(s -> s.getId() == Integer.parseInt(keyValue[1]))
                                .findFirst()
                                .orElse(null);
                        return student != null ? objectMapper.writeValueAsString(student) : "{\"error\": \"Student not found\"}";
                    }
                }
                List<Student> students = studentService.getStudents();
                int fromIndex = page * size;
                int toIndex = Math.min(fromIndex + size, students.size());
                if (fromIndex >= students.size()) {
                    return "[]";
                }
                List<Student> paginatedStudent = students.subList(fromIndex, toIndex);
                return objectMapper.writeValueAsString(paginatedStudent);
            }
            return "";
        }

        private String handlePost(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            int id = jsonNode.get("id").asInt();
            String firstName = jsonNode.get("firstName").asText();
            String lastName = jsonNode.get("lastName").asText();
            int age = jsonNode.get("age").asInt();
            Major major = Major.valueOf(jsonNode.get("major").asText());
            String email = jsonNode.get("email").asText();

            Student student = new UndergradStudent(id, firstName, lastName, age, major, email);
            studentService.addStudent(student);
            studentService.saveStudents();
            return objectMapper.writeValueAsString(student);
        }

        private String handlePut(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(requestBody);
            try {
                int id = jsonNode.get("id").asInt();
                String firstName = jsonNode.get("firstName").asText();
                String lastName = jsonNode.get("lastName").asText();
                int age = jsonNode.get("age").asInt();
                String email = jsonNode.get("email").asText();
                Major major = Major.valueOf(jsonNode.get("major").asText());
                Student student = studentService.getStudents().stream()
                        .filter(s -> s.getId() == id)
                        .findFirst()
                        .orElse(null);
                if (student != null) {
                    student.setFirstName(firstName);
                    student.setLastName(lastName);
                    student.setAge(age);
                    student.setEmail(email);
                    student.major = major;
                    studentService.saveStudents();
                    return objectMapper.writeValueAsString(student);
                } else {
                    return "{\"error\": \"Student not found\"}";
                }
            } catch (Exception e) {
                Logger.log("Error: " + e.getMessage());
            }
            return "";
        }

        private String handlePatch(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(requestBody);

            int id = jsonNode.get("id").asInt();
            Student student = studentService.getStudents().stream()
                    .filter(s -> s.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (student != null) {
                if (jsonNode.has("firstName")) {
                    student.setFirstName(jsonNode.get("firstName").asText());
                }
                if (jsonNode.has("lastName")) {
                    student.setLastName(jsonNode.get("lastName").asText());
                }
                if (jsonNode.has("age")) {
                    student.setAge(jsonNode.get("age").asInt());
                }
                if (jsonNode.has("email")) {
                    student.setEmail(jsonNode.get("email").asText());
                }
                if (jsonNode.has("major")) {
                    student.major = Major.valueOf(jsonNode.get("major").asText());
                }
                studentService.saveStudents();
                return "OK";
            } else {
                return "{\"error\": \"Student not found\"}";
            }

        }

        private String handleDelete(HttpExchange exchange) {
            String query = exchange.getRequestURI().getQuery();
            int id = Integer.parseInt(query.split("=")[1]);
            studentService.deleteStudent(id);
            studentService.saveStudents();
            return "{\"message\": \"Deleted student with ID: " + id + "\"}";
        }
    }
    private class GraduateHandler implements HttpHandler {
        private GraduateStudentProcessor graduateStudentProcessor;

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
}
