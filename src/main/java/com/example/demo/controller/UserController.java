package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        System.out.println("Регистрация на: " + user.getUsername());

        if (userService.isUsernameAvailable(user.getUsername())) {
            userService.registerUser(user);
            System.out.println("Успешна регистрация!");
            return ResponseEntity.ok("Успешна регистрация!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Името е заето!");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User user) {
        System.out.println("Опит за логване: " + user.getUsername());

        Map<String, String> response = new HashMap<>();
        if (userService.validateUser(user.getUsername(), user.getPassword())) {
            System.out.println("Успешно логване!");
            response.put("message", "Успешно влизане!");
            response.put("status", "Успешно");
            // avatar - otgovor (problem sled login)
            response.put("avatar", "https://api.dicebear.com/9.x/adventurer-neutral/svg?seed=" + user.getUsername());
            return ResponseEntity.ok(response);
        } else {
            System.out.println("Невалидно име или парола!");
            response.put("message", "Невалидно потребителско име или парола!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
