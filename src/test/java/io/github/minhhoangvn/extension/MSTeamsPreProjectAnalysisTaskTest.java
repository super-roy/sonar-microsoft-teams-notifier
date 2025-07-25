package io.github.minhhoangvn.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.minhhoangvn.utils.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MSTeamsPreProjectAnalysisTaskTest {

    private MSTeamsPreProjectAnalysisTask preAnalysisTask;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;
    private ScannerContext scannerContext;
    private Configuration mockConfiguration;

    @Before
    public void setUp() {
        // Create mock configuration
        mockConfiguration = mock(Configuration.class);
        
        // Create pre-analysis task
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        
        // Initialize mock objects
        context = mock(Context.class);
        projectAnalysis = mock(ProjectAnalysis.class);
        project = mock(Project.class);
        ceTask = mock(CeTask.class);
        qualityGate = mock(QualityGate.class);
        scannerContext = mock(ScannerContext.class);

        // Set up common mock behavior
        when(context.getProjectAnalysis()).thenReturn(projectAnalysis);
        when(projectAnalysis.getProject()).thenReturn(project);
        when(projectAnalysis.getCeTask()).thenReturn(ceTask);
        when(projectAnalysis.getQualityGate()).thenReturn(qualityGate);
        when(projectAnalysis.getScannerContext()).thenReturn(scannerContext);
        when(project.getName()).thenReturn("Test Project");
        when(project.getKey()).thenReturn("test-project-key");
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);
        when(qualityGate.getName()).thenReturn("Sonar way");
        
        // Set up scanner properties
        Map<String, String> scannerProps = new HashMap<>();
        when(scannerContext.getProperties()).thenReturn(scannerProps);
    }

    @After
    public void tearDown() {
        // Clear system properties
        System.clearProperty(Constants.ENABLE_NOTIFY);
        System.clearProperty(Constants.WEBHOOK_URL);
        System.clearProperty(Constants.SONAR_URL);
        System.clearProperty(Constants.WEBHOOK_SEND_ON_FAILED);
        System.clearProperty(Constants.WEBHOOK_MESSAGE_AVATAR);
    }

    @Test
    public void testConstructor() {
        // Test constructor with configuration
        MSTeamsPreProjectAnalysisTask task = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        assertEquals("MS Teams configuration validator for SonarQube analysis", task.getDescription());
    }

    @Test
    public void testFinished_WithValidConfiguration_ValidatesSuccessfully() {
        // Arrange
        setupValidConfiguration();

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertTrue("Configuration should be validated", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertEquals("true", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.ENABLE_NOTIFY));
        assertTrue("Plugin should be enabled", MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, false));
        assertEquals("https://outlook.office.com/webhook/test", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL));
    }

    @Test
    public void testFinished_WithDisabledPlugin_ValidatesButSkipsOtherChecks() {
        // Arrange
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("false"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(false));

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertTrue("Configuration should be considered validated even when disabled", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertFalse("Plugin should be disabled", MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, true));
    }

    @Test
    public void testFinished_WithMissingWebhookUrl_FailsValidation() {
        // Arrange
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of(""));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://localhost:9000"));

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertFalse("Configuration should fail validation with missing webhook URL", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
    }

    @Test
    public void testFinished_WithInvalidWebhookUrl_FailsValidation() {
        // Arrange
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("invalid-url"));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://localhost:9000"));

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertFalse("Configuration should fail validation with invalid webhook URL", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
    }

    @Test
    public void testFinished_WithSystemProperties_ValidatesSuccessfully() {
        // Arrange - Set system properties instead of SonarQube configuration
        System.setProperty(Constants.ENABLE_NOTIFY, "true");
        System.setProperty(Constants.WEBHOOK_URL, "https://outlook.office.com/webhook/test-system");
        System.setProperty(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        System.setProperty(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        System.setProperty(Constants.SONAR_URL, "http://localhost:9000");
        
        // Mock configuration returns empty
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.empty());

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertTrue("Configuration should be validated from system properties", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertEquals("https://outlook.office.com/webhook/test-system", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL));
    }

    @Test
    public void testFinished_WithEnvironmentVariables_ValidatesSuccessfully() {
        // This test simulates environment variables through system properties with uppercase names
        System.setProperty("SONAR_MSTEAMS_ENABLE", "true");
        System.setProperty("SONAR_MSTEAMS_WEBHOOK_URL", "https://outlook.office.com/webhook/test-env");
        System.setProperty("SONAR_MSTEAMS_AVATAR_URL", Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        System.setProperty("SONAR_MSTEAMS_SEND_ON_FAILED", "false");
        System.setProperty("SONAR_CORE_SERVERBASEURL", "http://localhost:9000");
        
        // Mock configuration and system properties return empty
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.empty());

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertTrue("Configuration should be validated from environment variables", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertEquals("https://outlook.office.com/webhook/test-env", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL));
        
        // Clean up environment simulation
        System.clearProperty("SONAR_MSTEAMS_ENABLE");
        System.clearProperty("SONAR_MSTEAMS_WEBHOOK_URL");
        System.clearProperty("SONAR_MSTEAMS_AVATAR_URL");
        System.clearProperty("SONAR_MSTEAMS_SEND_ON_FAILED");
        System.clearProperty("SONAR_CORE_SERVERBASEURL");
    }

    @Test
    public void testFinished_ConfigurationPriority_SonarQubeConfigWins() {
        // Arrange - Set system properties and SonarQube config, SonarQube should win
        System.setProperty(Constants.WEBHOOK_URL, "https://system.property.url");
        
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("https://sonarqube.config.url"));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://localhost:9000"));

        // Act
        preAnalysisTask.finished(context);

        // Assert
        assertTrue("Configuration should be validated", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertEquals("SonarQube config should take priority", "https://sonarqube.config.url", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL));
    }

    @Test
    public void testValidWebhookUrls() {
        // Test various valid webhook URL formats
        String[] validUrls = {
            "https://outlook.office.com/webhook/abc123",
            "https://webhook.office.com/webhook/test",
            "https://teams.microsoft.com/webhook/test",
            "https://localhost:8080/webhook", // For testing
            "https://127.0.0.1:9000/webhook"  // For testing
        };

        for (String url : validUrls) {
            // Arrange
            when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
            when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
            when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of(url));
            when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
            when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
            when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://localhost:9000"));

            // Act
            preAnalysisTask.finished(context);

            // Assert
            assertTrue("URL should be valid: " + url, MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        }
    }

    @Test
    public void testGetAllValidatedConfig() {
        // Arrange
        setupValidConfiguration();

        // Act
        preAnalysisTask.finished(context);
        Map<String, String> allConfig = MSTeamsPreProjectAnalysisTask.getAllValidatedConfig();

        // Assert
        assertTrue("Config map should contain entries", allConfig.size() > 0);
        assertTrue("Should contain enable setting", allConfig.containsKey(Constants.ENABLE_NOTIFY));
        assertTrue("Should contain webhook URL", allConfig.containsKey(Constants.WEBHOOK_URL));
        assertTrue("Should contain avatar URL", allConfig.containsKey(Constants.WEBHOOK_MESSAGE_AVATAR));
        assertTrue("Should contain send on failed setting", allConfig.containsKey(Constants.WEBHOOK_SEND_ON_FAILED));
        assertTrue("Should contain base URL", allConfig.containsKey(Constants.SONAR_URL));
    }

    @Test
    public void testConfigurationErrorHandling() {
        // Arrange - Mock configuration that throws exceptions
        Configuration faultyConfig = mock(Configuration.class);
        when(faultyConfig.get(Constants.ENABLE_NOTIFY)).thenThrow(new RuntimeException("Config error"));
        when(faultyConfig.getBoolean(Constants.ENABLE_NOTIFY)).thenThrow(new RuntimeException("Config error"));
        
        MSTeamsPreProjectAnalysisTask faultyTask = new MSTeamsPreProjectAnalysisTask(faultyConfig);

        // Act - Should not throw exception
        faultyTask.finished(context);

        // Assert - Should fail validation gracefully
        assertFalse("Configuration should fail validation on errors", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
    }

    private void setupValidConfiguration() {
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("https://outlook.office.com/webhook/test"));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://localhost:9000"));
    }
}