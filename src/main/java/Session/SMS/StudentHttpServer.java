package Session.SMS;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class StudentHttpServer {
    private static final int PORT = 8083;
    private StudentService studentService;

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
        server.createContext("/students", new StudentHandler(studentService));
        server.createContext("/graduate", new GraduateHandler(studentService));
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }
}