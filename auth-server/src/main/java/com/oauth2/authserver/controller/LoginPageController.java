package com.oauth2.authserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginPageController {

    @Value("${login-server.url}")
    private String loginServerUrl;

    @GetMapping("/login")
    public String loginRedirect(HttpServletRequest request) {
        // Build the login server URL and pass along any query parameters
        StringBuilder redirectUrl = new StringBuilder(loginServerUrl + "/login");

        // Pass the auth server's login processing URL so login server can POST back
        String authServerLoginUrl = "http://localhost:9000/login";

        String queryString = request.getQueryString();
        redirectUrl.append("?auth_server_url=").append(authServerLoginUrl);

        if (queryString != null) {
            redirectUrl.append("&").append(queryString);
        }

        return "redirect:" + redirectUrl;
    }
}
