package kh.edu.paragoniu.SpringBoot.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.memory-log.enabled", havingValue = "true")
public class MemoryUsageLogger {

    private static final Logger log = LoggerFactory.getLogger(MemoryUsageLogger.class);

    @Scheduled(fixedRate = 30000)
    public void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMb = runtime.totalMemory() / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);

        log.info("JVM memory used={}MB total={}MB max={}MB", usedMb, totalMb, maxMb);
    }
}
