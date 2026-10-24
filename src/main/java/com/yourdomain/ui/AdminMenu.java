package com.yourdomain.ui;

import com.yourdomain.dao.UserDao;
import com.yourdomain.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final Scanner scanner;
    private final UserDao userDao;
    private final User user;

    public AdminMenu(Scanner scanner, UserDao userDao, User user) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.user = user;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Manage user accounts (approve / reject)");
            System.out.println("2. View reports");
            System.out.println("3. Check activity logs");
            System.out.println("4. Manage cafe rates");
            System.out.println("5. Logout");
            System.out.print("Choose: ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> manageAccounts();
                case "2", "3", "4" -> System.out.println("TODO: connect your existing feature here.");
                case "5" -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void manageAccounts() {
        List<User> pending = userDao.findByStatus(User.STATUS_PENDING);
        if (pending.isEmpty()) {
            System.out.println("No pending accounts.");
            return;
        }
        System.out.println("Pending accounts:");
        for (User u : pending) {
            System.out.printf("  ID %d | %s | %s | %s%n", u.userId(), u.name(), u.username(), u.role());
        }
        System.out.print("Enter user ID to review (or press Enter to cancel): ");
        String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (input.isEmpty()) return;

        int id;
        try {
            id = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
            return;
        }
        if (pending.stream().noneMatch(u -> u.userId() == id)) {
            System.out.println("That ID is not in the pending list.");
            return;
        }
        System.out.print("1 = Approve (Active), 2 = Reject (Inactive): ");
        String action = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        String newStatus = action.equals("1") ? User.STATUS_ACTIVE
                : action.equals("2") ? User.STATUS_INACTIVE : null;
        if (newStatus == null) {
            System.out.println("Invalid choice.");
            return;
        }
        if (userDao.updateStatus(id, newStatus)) {
            userDao.logActivity(user.userId(), "Set user " + id + " to " + newStatus);
            System.out.println("Account updated to " + newStatus + ".");
        } else {
            System.out.println("Update failed.");
        }
    }
}
