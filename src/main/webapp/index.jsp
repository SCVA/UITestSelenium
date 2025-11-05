<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String errorMessage = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Login | UITestSelenium</title>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/css/login.css">
    </head>
    <body>
        <main class="login-container" aria-labelledby="login-title">
            <header class="login-header">
                <h1 id="login-title">Welcome back</h1>
                <p>Sign in to access your UITestSelenium dashboard.</p>
                <% if (errorMessage != null) { %>
                    <div class="alert" role="alert"><%= errorMessage %></div>
                <% } %>
            </header>
            <form class="login-form" action="<%= request.getContextPath() %>/login" method="post">
                <label>
                    Email address
                    <input type="text" name="email" autocomplete="username" placeholder="you@example.com" required>
                </label>
                <label>
                    Password
                    <input type="password" name="password" autocomplete="current-password" placeholder="Enter your password" required>
                </label>
                <div class="login-actions">
                    <button type="submit">Sign in</button>
                </div>
            </form>
            <footer class="login-footer">
                <span>Don't have an account?</span>
                <a href="#">Create one</a>
            </footer>
        </main>
    </body>
</html>
