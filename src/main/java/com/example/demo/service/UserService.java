package com.example.demo.service;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private static final String FILE_PATH = "users.json";
    private List<User> users = new ArrayList<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService() {
        loadUsers();
    }

    private synchronized void loadUsers() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(FILE_PATH);
            if (file.exists()) {
                users = mapper.readValue(file,
                        mapper.getTypeFactory().constructCollectionType(List.class, User.class));
            } else {
                users = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isUsernameAvailable(String username) {
        loadUsers(); // Презареждаме потребителите
        return users.stream().noneMatch(u -> u.getUsername().equals(username));
    }

    public synchronized void registerUser(User user) {
        loadUsers(); // prezarejdane na potrebiteli
        String hashedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(hashedPassword);
        users.add(user);
        saveUsers();
    }

    public synchronized boolean validateUser(String username, String password) {
        loadUsers(); // prezarejdane
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .map(u -> passwordEncoder.matches(password, u.getPassword()))
                .orElse(false);
    }

    private synchronized void saveUsers() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(FILE_PATH), users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
