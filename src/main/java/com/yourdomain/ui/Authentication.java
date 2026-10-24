package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.Optional;
import java.util.Scanner;

public class Authentication {
    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== CafeClock: Computer Cafe Time ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            if (!scanner.hasNextLine()) {
                return;
            }
            switch (scanner.nextLine().trim()) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    private void register() {
        System.out.println("\n--- Register ---");
        String name = prompt("Full name: ");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        String username = prompt("Username (3+ characters, no spaces): ");
        if (username.length() < 3 || username.contains(" ")) {
            System.out.println("Invalid username.");
            return;
        }
        String password = prompt("Password (6+ characters): ");
        if (password.length() < 6) {
            System.out.println("Password must be at least 6 characters.");
            return;
        }
        System.out.println("Role: 1. Customer  2. Cafe Staff  3. Admin");
        String role = switch (prompt("Choose role: ")) {
            case "1" -> User.ROLE_CUSTOMER;
            case "2" -> User.ROLE_STAFF;
            case "3" -> User.ROLE_ADMIN;
            default -> null;
        };
        if (role == null) {
            System.out.println("Invalid role. Choose 1, 2, or 3.");
            return;
        }

        switch (userDao.register(name, username, password, role)) {
            case SUCCESS -> System.out.println(
                    "Registration successful! Status: PENDING. An Admin must approve your account before you can log in.");
            case DUPLICATE_USERNAME -> System.out.println("That username is already taken.");
            case ERROR -> System.out.println("Registration failed because of a database error.");
        }
    }

    private void login() {
        System.out.println("\n--- Login ---");
        String username = prompt("Username: ");
        String password = prompt("Password: ");

        Optional<User> result = userDao.authenticate(username, password);
        if (result.isEmpty()) {
            System.out.println("Login failed: wrong username or password.");
            return;
        }
        User user = result.get();

        switch (user.status()) {
            case User.STATUS_ACTIVE -> routeUser(user);
            case User.STATUS_PENDING -> System.out.println(
                    "Your account is still PENDING approval by an Admin.");
            case User.STATUS_INACTIVE -> System.out.println(
                    "Your account is Inactive. Please contact an Admin.");
            default -> System.out.println("Unknown account status. Please contact an Admin.");
        }
    }

    // Role comes from the database record, so users only reach their own menu.
    private void routeUser(User user) {
        userDao.logActivity(user.userId(), "Login");
        System.out.println("\nWelcome, " + user.name() + " (" + user.role() + ")!");

        switch (user.role()) {
            case User.ROLE_CUSTOMER -> new CustomerMenu(scanner, userDao, user).show();
            case User.ROLE_STAFF -> new StaffMenu(scanner, userDao, user).show();
            case User.ROLE_ADMIN -> new AdminMenu(scanner, userDao, user).show();
            default -> System.out.println("Unknown role. Access denied.");
        }

        userDao.logActivity(user.userId(), "Logout");
        System.out.println("You have been logged out.");
    }

    private String prompt(String text) {
        System.out.print(text);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }
}
