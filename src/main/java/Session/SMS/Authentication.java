package Session.SMS;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class Authentication {
    private static Set<String> validTokens = new HashSet<>();

    public static boolean validateCredentials(String login, String password) {
        // Implement your login and password validation logic here
        return "admin".equals(login) && "password".equals(password);
    }

    public static void storeToken(String token) {
        validTokens.add(token);
    }

    public static boolean authenticate(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("token");
        if (token != null && validTokens.contains(token)) {
            return true;
        }
        try {
            exchange.sendResponseHeaders(401, -1); // Unauthorized
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
