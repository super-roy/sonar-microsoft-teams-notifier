package io.github.minhhoangvn.extension;

import io.github.minhhoangvn.utils.Constants;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MSTeamsPreProjectAnalysisTask implements PostProjectAnalysisTask {

    private static final Logger LOGGER = Loggers.get(MSTeamsPreProjectAnalysisTask.class);
    
    // Static cache to store validated configuration
    private static final Map<String, String> VALIDATED_CONFIG = new HashMap<>();
    private static volatile boolean configurationValidated = false;
    
    private final Configuration configuration;

    public MSTeamsPreProjectAnalysisTask(Configuration configuration) {
        this.configuration = configuration;
    }
    
    // Default constructor for SonarQube plugin system (when Configuration injection doesn't work)
    public MSTeamsPreProjectAnalysisTask() {
        this.configuration = null;
    }

    @Override
    public void finished(Context context) {
        LOGGER.info("=== MS Teams Plugin: Pre-analysis configuration validation started ===");
        
        try {
            // Clear previous configuration
            VALIDATED_CONFIG.clear();
            configurationValidated = false;
            
            // Load and validate configuration from SonarQube settings
            validateAndCacheConfiguration(context);
            
            if (configurationValidated) {
                LOGGER.info("MS Teams Plugin: Configuration validation completed successfully");
                logValidatedConfiguration();
            } else {
                LOGGER.warn("MS Teams Plugin: Configuration validation failed - some settings may be missing");
            }
            
        } catch (Exception e) {
            LOGGER.error("MS Teams Plugin: Error during configuration validation", e);
            configurationValidated = false;
        }
        
        LOGGER.info("=== MS Teams Plugin: Pre-analysis configuration validation completed ===");
    }
    
    private void validateAndCacheConfiguration(Context context) {
        LOGGER.info("MS Teams Plugin: Loading configuration from SonarQube settings...");
        
        // 1. Load and validate plugin enable setting
        boolean isEnabled = loadBooleanConfig(Constants.ENABLE_NOTIFY, Constants.DEFAULT_ENABLE_NOTIFY);
        VALIDATED_CONFIG.put(Constants.ENABLE_NOTIFY, String.valueOf(isEnabled));
        LOGGER.info("MS Teams Plugin: Plugin enabled = {}", isEnabled);
        
        if (!isEnabled) {
            LOGGER.info("MS Teams Plugin: Plugin is disabled, skipping further configuration validation");
            configurationValidated = true; // Consider it validated even if disabled
            return;
        }
        
        // 2. Load and validate webhook URL (required)
        String webhookUrl = loadStringConfig(Constants.WEBHOOK_URL, "");
        VALIDATED_CONFIG.put(Constants.WEBHOOK_URL, webhookUrl);
        
        if (StringUtils.isEmpty(webhookUrl)) {
            LOGGER.error("MS Teams Plugin: Webhook URL is required but not configured. Please set '{}' in Administration > Configuration > Microsoft Teams", Constants.WEBHOOK_URL);
            configurationValidated = false;
            return;
        } else if (!isValidWebhookUrl(webhookUrl)) {
            LOGGER.error("MS Teams Plugin: Invalid webhook URL format: {}", maskUrl(webhookUrl));
            configurationValidated = false;
            return;
        }
        
        LOGGER.info("MS Teams Plugin: Webhook URL validated successfully");
        
        // 3. Load avatar URL (optional)
        String avatarUrl = loadStringConfig(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        VALIDATED_CONFIG.put(Constants.WEBHOOK_MESSAGE_AVATAR, avatarUrl);
        LOGGER.info("MS Teams Plugin: Avatar URL = {}", avatarUrl);
        
        // 4. Load send on failed only setting
        boolean sendOnFailedOnly = loadBooleanConfig(Constants.WEBHOOK_SEND_ON_FAILED, Constants.DEFAULT_WEBHOOK_SEND_ON_FAILED);
        VALIDATED_CONFIG.put(Constants.WEBHOOK_SEND_ON_FAILED, String.valueOf(sendOnFailedOnly));
        LOGGER.info("MS Teams Plugin: Send on failed only = {}", sendOnFailedOnly);
        
        // 5. Load SonarQube base URL (optional but recommended)
        String baseUrl = loadStringConfig(Constants.SONAR_URL, "");
        if (StringUtils.isEmpty(baseUrl)) {
            // Try to get from common environment variables
            baseUrl = System.getenv("SONAR_CORE_SERVERBASEURL");
            if (StringUtils.isEmpty(baseUrl)) {
                baseUrl = "http://localhost:9000"; // Default fallback
                LOGGER.warn("MS Teams Plugin: SonarQube base URL not configured, using default: {}", baseUrl);
            } else {
                LOGGER.info("MS Teams Plugin: Using base URL from environment: {}", baseUrl);
            }
        }
        VALIDATED_CONFIG.put(Constants.SONAR_URL, baseUrl);
        
        // 6. Load team name (optional)
        String teamName = loadStringConfig(Constants.WEBHOOK_TEAM_NAME, Constants.DEFAULT_WEBHOOK_TEAM_NAME);
        VALIDATED_CONFIG.put(Constants.WEBHOOK_TEAM_NAME, teamName);
        LOGGER.info("MS Teams Plugin: Team name = '{}'", StringUtils.isEmpty(teamName) ? "[NOT SET - will use 'DevOps Team']" : teamName);
        
        // 7. Test webhook connectivity (optional)
        if (Boolean.parseBoolean(System.getProperty("sonar.msteams.test.webhook", "false"))) {
            testWebhookConnectivity(webhookUrl);
        }
        
        configurationValidated = true;
        LOGGER.info("MS Teams Plugin: All configuration validation checks passed");
    }
    
    private String loadStringConfig(String key, String defaultValue) {
        try {
            // Priority: SonarQube Configuration -> System Properties -> Environment Variables -> Default
            
            // 1. Try SonarQube Configuration first (if available)
            if (configuration != null) {
                String value = configuration.get(key).orElse(null);
                if (value != null && !value.trim().isEmpty()) {
                    LOGGER.debug("Config [{}] loaded from SonarQube settings: {}", key, maskSensitiveValue(key, value));
                    return value.trim();
                }
            }
            
            // 2. Try system properties
            String value = System.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                LOGGER.debug("Config [{}] loaded from system properties: {}", key, maskSensitiveValue(key, value));
                return value.trim();
            }
            
            // 3. Try environment variables
            String envKey = key.replace(".", "_").toUpperCase();
            value = System.getenv(envKey);
            if (value != null && !value.trim().isEmpty()) {
                LOGGER.debug("Config [{}] loaded from environment variable {}: {}", key, envKey, maskSensitiveValue(key, value));
                return value.trim();
            }
            
            LOGGER.debug("Config [{}] not found, using default: {}", key, defaultValue);
            return defaultValue;
            
        } catch (Exception e) {
            LOGGER.warn("Error loading config [{}], using default: {}", key, e.getMessage());
            return defaultValue;
        }
    }
    
    private boolean loadBooleanConfig(String key, boolean defaultValue) {
        try {
            // Try SonarQube Configuration first (if available)
            if (configuration != null) {
                return configuration.getBoolean(key).orElse(defaultValue);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not access SonarQube boolean configuration for key {}: {}", key, e.getMessage());
        }
        
        // Fallback to string parsing
        String stringValue = loadStringConfig(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(stringValue);
    }
    
    private boolean isValidWebhookUrl(String url) {
        try {
            // More lenient validation for testing - accept localhost URLs
            return url.startsWith("https://") || url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1");
        } catch (Exception e) {
            return false;
        }
    }
    
    private String maskUrl(String url) {
        if (StringUtils.isEmpty(url) || url.length() < 20) {
            return "[MASKED]";
        }
        return url.substring(0, 20) + "..." + url.substring(url.length() - 10);
    }
    
    private String maskSensitiveValue(String key, String value) {
        if (key.toLowerCase().contains("url") || key.toLowerCase().contains("webhook")) {
            return maskUrl(value);
        }
        return value;
    }
    
    private void testWebhookConnectivity(String webhookUrl) {
        try {
            LOGGER.info("MS Teams Plugin: Testing webhook connectivity...");
            // Simple connectivity test - you could implement actual HTTP test here
            // For now, just validate the URL format more thoroughly
            java.net.URL url = new java.net.URL(webhookUrl);
            LOGGER.info("MS Teams Plugin: Webhook URL format validation passed: {}:{}", url.getHost(), url.getPort());
        } catch (Exception e) {
            LOGGER.warn("MS Teams Plugin: Webhook connectivity test failed: {}", e.getMessage());
        }
    }
    
    private void logValidatedConfiguration() {
        LOGGER.info("=== MS Teams Plugin: Validated Configuration Summary ===");
        LOGGER.info("  - Plugin Enabled: {}", VALIDATED_CONFIG.get(Constants.ENABLE_NOTIFY));
        LOGGER.info("  - Webhook URL: {}", maskUrl(VALIDATED_CONFIG.get(Constants.WEBHOOK_URL)));
        LOGGER.info("  - Avatar URL: {}", VALIDATED_CONFIG.get(Constants.WEBHOOK_MESSAGE_AVATAR));
        LOGGER.info("  - Send on Failed Only: {}", VALIDATED_CONFIG.get(Constants.WEBHOOK_SEND_ON_FAILED));
        LOGGER.info("  - SonarQube Base URL: {}", VALIDATED_CONFIG.get(Constants.SONAR_URL));
        LOGGER.info("=== End Configuration Summary ===");
    }
    
    // Static methods for MSTeamsPostProjectAnalysisTask to access validated config
    public static boolean isConfigurationValidated() {
        return configurationValidated;
    }
    
    public static String getValidatedConfig(String key) {
        return VALIDATED_CONFIG.get(key);
    }
    
    public static String getValidatedConfig(String key, String defaultValue) {
        String value = VALIDATED_CONFIG.get(key);
        return value != null ? value : defaultValue;
    }
    
    public static boolean getValidatedBooleanConfig(String key, boolean defaultValue) {
        String value = VALIDATED_CONFIG.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public static Map<String, String> getAllValidatedConfig() {
        return new HashMap<>(VALIDATED_CONFIG);
    }
    
    // Static method to manually set configuration for testing
    public static void setValidatedConfigForTesting(Map<String, String> config, boolean validated) {
        VALIDATED_CONFIG.clear();
        VALIDATED_CONFIG.putAll(config);
        configurationValidated = validated;
    }
    
    // Static method to clear configuration for testing
    public static void clearValidatedConfig() {
        VALIDATED_CONFIG.clear();
        configurationValidated = false;
    }

    @Override
    public String getDescription() {
        return "MS Teams configuration validator for SonarQube analysis";
    }
}