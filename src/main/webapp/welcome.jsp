<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%
    String userEmail = (String) request.getAttribute("userEmail");
    if (userEmail == null) {
        response.sendRedirect(request.getContextPath() + "/index.jsp");
        return;
    }
    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMMM d 'at' h:mm a");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Welcome | UITestSelenium</title>
        <style>
            body {
                margin: 0;
                font-family: "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                display: flex;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                background: linear-gradient(135deg, #3f8efc, #7c3aed);
                color: #fff;
            }

            .welcome-card {
                background: rgba(0, 0, 0, 0.35);
                padding: 48px 56px;
                border-radius: 20px;
                box-shadow: 0 24px 48px rgba(15, 23, 42, 0.3);
                max-width: 520px;
                text-align: center;
                backdrop-filter: blur(6px);
            }

            h1 {
                margin: 0 0 12px;
                font-size: 2rem;
            }

            p {
                margin: 0 0 18px;
                font-size: 1.05rem;
                line-height: 1.6;
            }

            a {
                display: inline-block;
                margin-top: 24px;
                padding: 12px 22px;
                border-radius: 999px;
                background: rgba(255, 255, 255, 0.2);
                color: #fff;
                text-decoration: none;
                font-weight: 600;
                transition: background 0.2s ease;
            }

            a:hover,
            a:focus {
                background: rgba(255, 255, 255, 0.3);
            }
        </style>
    </head>
    <body>
        <section class="welcome-card" aria-live="polite">
            <h1>Welcome, <%= userEmail %>!</h1>
            <p>You have successfully authenticated. Your secure session began on
                <strong><%= formatter.format(new Date()) %></strong>.</p>
            <p>This lightweight example demonstrates how to wire a styled login form to Java server-side logic using servlets.</p>
            <a href="<%= request.getContextPath() %>/index.jsp">Sign out</a>
        </section>
    </body>
</html>
