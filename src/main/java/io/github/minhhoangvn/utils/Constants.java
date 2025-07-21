package io.github.minhhoangvn.utils;

public class Constants {
    public static final String CATEGORY = "Microsoft Teams";
    public static final String ENABLE_NOTIFY = "sonar.msteams.enable";
    public static final String WEBHOOK_URL = "sonar.msteams.webhook.url";
    public static final String WEBHOOK_MESSAGE_AVATAR = "sonar.msteams.avatar.url";
    public static final String WEBHOOK_SEND_ON_FAILED = "sonar.msteams.send.on.failed";
    public static final String SONAR_URL = "sonar.core.serverBaseURL";
    
    // Update this to match AdaptiveCardsFormat.DEFAULT_IMAGE_URL
    public static final String DEFAULT_WEBHOOK_MESSAGE_AVATAR = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png";
    
    // Default values
    public static final boolean DEFAULT_ENABLE_NOTIFY = false;
    public static final boolean DEFAULT_WEBHOOK_SEND_ON_FAILED = true;
    
    private Constants() {
        // Utility class - prevent instantiation
    }
}
