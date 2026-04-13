package com.bupt.tarecruitment;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;
import com.bupt.tarecruitment.ui.LoginPage;
import javafx.application.Application;
import java.util.Locale;

/**
 * 项目主入口，默认启动 JavaFX 桌面端。
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && isHelpCommand(args[0])) {
            printHelp(new ProjectBootstrap().initialize());
            return;
        }

        Application.launch(LoginPage.class, args);
    }

    private static boolean isHelpCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        return normalized.equals("help")
            || normalized.equals("--help")
            || normalized.equals("-h");
    }

    private static void printHelp(StartupReport report) {
        System.out.println("BUPT International School TA Recruitment System");
        System.out.println("Default startup launches the JavaFX desktop UI.");
        System.out.println("Data directory: " + report.dataDirectory().toAbsolutePath());
        System.out.println("JSP web resources are still kept in src/main/webapp for later work.");
        System.out.println("Examples:");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run.ps1 help");
        System.out.println(" - powershell -ExecutionPolicy Bypass -File scripts\\run-javafx.ps1");
    }
}
