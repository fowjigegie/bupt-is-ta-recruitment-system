package com.bupt.tarecruitment.auth;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * 提供登录注册相关的控制台交互流程。
 */
public final class AuthConsoleWorkflow {
    private final AuthService service;
    private final Scanner scanner;
    private final PrintStream output;

    public AuthConsoleWorkflow(AuthService service, InputStream input, PrintStream output) {
        this.service = Objects.requireNonNull(service);
        this.scanner = new Scanner(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        // US00: 控制台版注册/登录流程，便于快速演示账号规则
        output.println("US-00 User Registration and Login Demo");
        output.println("Register with a project userId and role, then log in to verify role recognition.");
        output.println("ID format rules: APPLICANT -> ta..., MO -> mo..., ADMIN -> admin...");

        boolean running = true;
        while (running) {
            output.println();
            output.println("Choose an option:");
            output.println("1. Register");
            output.println("2. Login");
            output.println("3. View current user");
            output.println("4. Logout");
            output.println("5. Exit");
            output.print("> ");

            String command = scanner.nextLine().trim();
            switch (command) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> viewCurrentUser();
                case "4" -> logout();
                case "5" -> running = false;
                default -> output.println("Unknown option. Please choose 1, 2, 3, 4, or 5.");
            }
        }
    }

    private void register() {
        // US00: 注册只负责"创建账号"，不做自动登录
        output.println();
        output.println("Register User");

        String userId = promptNonBlank("User ID");
        String password = promptNonBlank("Password");
        UserRole role = promptRole();

        try {
            UserAccount account = service.register(userId, password, role);
            output.println("Registration successful.");
            printUser(account);
        } catch (IllegalArgumentException exception) {
            output.println("Failed to register user: " + exception.getMessage());
        }
    }

    private void login() {
        // US00: 登录用于验证账号是否可用、角色是否正确识别
        output.println();
        output.println("Login");

        String userId = promptNonBlank("User ID");
        String password = promptNonBlank("Password");

        try {
            UserAccount account = service.login(userId, password);
            output.println("Login successful.");
            output.println("Role recognized: " + account.role());
            output.println("Entry point: " + describeEntryPoint(account.role()));
        } catch (IllegalArgumentException exception) {
            output.println("Failed to log in: " + exception.getMessage());
        }
    }

    private void viewCurrentUser() {
        // 查看当前会话用户（如果还没登录会提示为空）
        output.println();
        output.println("Current User");

        Optional<UserAccount> currentUser = service.getCurrentUser();
        if (currentUser.isEmpty()) {
            output.println("No user is currently logged in.");
            return;
        }

        printUser(currentUser.get());
    }

    private void logout() {
        // 清理当前会话
        service.logout();
        output.println("Logged out.");
    }

    private void printUser(UserAccount account) {
        output.println("User summary:");
        output.println(" - userId: " + account.userId());
        output.println(" - role: " + account.role());
        output.println(" - displayName: " + account.displayName());
        output.println(" - status: " + account.status());
    }

    private String promptNonBlank(String label) {
        while (true) {
            output.print(label + ": ");
            String value = scanner.nextLine().trim();
            if (!value.isBlank()) {
                return value;
            }
            output.println(label + " cannot be blank.");
        }
    }

    private UserRole promptRole() {
        while (true) {
            output.print("Role (APPLICANT, MO, ADMIN): ");
            String value = scanner.nextLine().trim();
            try {
                return UserRole.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                output.println("Role must be APPLICANT, MO, or ADMIN.");
            }
        }
    }

    private String describeEntryPoint(UserRole role) {
        switch (role) {
            case APPLICANT:
                return "applicant features";
            case MO:
                return "module organiser features";
            case ADMIN:
                return "admin features";
            default:
                throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }
}
