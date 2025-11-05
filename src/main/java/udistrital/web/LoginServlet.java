package udistrital.web;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Simple servlet that validates login credentials for the demo login form.
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends HttpServlet {

    private static final Map<String, String> ALLOWED_USERS;

    static {
        Map<String, String> users = new HashMap<String, String>();
        users.put("user@example.com", "password123");
        users.put("admin@example.com", "adminpass");
        ALLOWED_USERS = Collections.unmodifiableMap(users);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = trimToEmpty(request.getParameter("email"));
        String password = trimToEmpty(request.getParameter("password"));

        if (isAuthenticated(email, password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("authenticatedUser", email);
            request.setAttribute("userEmail", email);
            request.getRequestDispatcher("/welcome.jsp").forward(request, response);
            return;
        }

        request.setAttribute("errorMessage", "Invalid email or password. Please try again.");
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }

    private boolean isAuthenticated(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            return false;
        }
        String expectedPassword = ALLOWED_USERS.get(email.toLowerCase());
        return expectedPassword != null && expectedPassword.equals(password);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
