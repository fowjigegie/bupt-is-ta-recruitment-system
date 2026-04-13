package com.bupt.tarecruitment.bootstrap;

import java.nio.file.Path;
import java.util.List;

/**
 * 记录系统启动后的路径和环境信息。
 */
public record StartupReport(Path dataDirectory, List<Path> createdFiles) {
}
