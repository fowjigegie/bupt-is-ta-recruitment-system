package com.bupt.tarecruitment.bootstrap;

import java.nio.file.Path;
import java.util.List;

public record StartupReport(Path dataDirectory, List<Path> createdFiles) {
}
