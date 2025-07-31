package io.github.minhhoangvn.utils;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class LoggingUtils {
    
    private static final Logger LOGGER = Loggers.get(LoggingUtils.class);
    
    public static void logPayload(String payload) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generated Adaptive Card payload:");
            LOGGER.debug(payload);
        }
    }
    
    public static void logFormattingCheck(String payload) {
        // Validate that the payload matches expected format
        if (!payload.contains("\"type\": \"AdaptiveCard\"") ||
            !payload.contains("\"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\"") ||
            !payload.contains("\"version\": \"1.5\"")) {
            LOGGER.warn("Generated payload may not match expected Adaptive Card format");
        }
    }
}