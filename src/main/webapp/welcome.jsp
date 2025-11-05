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
        <link rel="stylesheet" href="<%= request.getContextPath() %>/css/welcome.css">
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
