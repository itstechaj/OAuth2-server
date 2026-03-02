package com.oauth2.loginserver.controller;

import com.oauth2.loginserver.entity.User;
import com.oauth2.loginserver.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "auth_server_url", required = false) String authServerUrl,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "signup_success", required = false) String signupSuccess,
            Model model) {

        model.addAttribute("authServerUrl", authServerUrl != null ? authServerUrl : "http://localhost:9000/login");
        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
        }
        if (signupSuccess != null) {
            model.addAttribute("signupSuccess", "Account created successfully! Please log in.");
        }
        return "login";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "auth_server_url", required = false) String authServerUrl,
            Model model) {

        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("signupError", "Username already exists. Please choose another.");
            model.addAttribute("authServerUrl", authServerUrl != null ? authServerUrl : "http://localhost:9000/login");
            return "login";
        }

        // Create user (plain text password for demo)
        User user = new User(username, password);
        userRepository.save(user);

        // Redirect back to login page with success message
        String redirectUrl = "/login?signup_success=true";
        if (authServerUrl != null) {
            redirectUrl += "&auth_server_url=" + authServerUrl;
        }
        return "redirect:" + redirectUrl;
    }
}
