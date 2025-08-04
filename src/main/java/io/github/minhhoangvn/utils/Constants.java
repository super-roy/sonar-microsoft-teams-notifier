package io.github.minhhoangvn.utils;

public class Constants {
    public static final String CATEGORY = "Microsoft Teams";
    public static final String ENABLE_NOTIFY = "sonar.msteams.enable";
    public static final String WEBHOOK_URL = "sonar.msteams.webhook.url";
    public static final String WEBHOOK_MESSAGE_AVATAR = "sonar.msteams.avatar.url";
    public static final String WEBHOOK_SEND_ON_FAILED = "sonar.msteams.send.on.failed";
    public static final String WEBHOOK_TEAM_NAME = "sonar.msteams.teamName";
    public static final String SONAR_URL = "sonar.core.serverBaseURL";
    
    // Default webhook message avatar (generic SonarQube logo)
    public static final String DEFAULT_WEBHOOK_MESSAGE_AVATAR = "https://docs.sonarqube.org/latest/images/sonarqube-logo.svg";
    
    // Default values
    public static final boolean DEFAULT_ENABLE_NOTIFY = true;
    public static final boolean DEFAULT_WEBHOOK_SEND_ON_FAILED = false;
    public static final String DEFAULT_WEBHOOK_TEAM_NAME = "DevOps Team";
    
    private Constants() {
        // Utility class - prevent instantiation
    }
}
