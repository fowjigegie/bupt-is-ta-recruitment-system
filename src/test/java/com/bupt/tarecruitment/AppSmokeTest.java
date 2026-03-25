package com.bupt.tarecruitment;

import com.bupt.tarecruitment.bootstrap.ProjectBootstrap;
import com.bupt.tarecruitment.bootstrap.StartupReport;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.file.Files;

public final class AppSmokeTest {
    private AppSmokeTest() {
    }

    public static void main(String[] args) {
        StartupReport report = new ProjectBootstrap().initialize();

        for (DataFile dataFile : DataFile.values()) {
            if (Files.notExists(report.dataDirectory().resolve(dataFile.fileName()))) {
                throw new IllegalStateException("Missing required data file: " + dataFile.fileName());
            }
        }

        System.out.println("Smoke test passed.");
    }
}
