package io.github.minhhoangvn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.minhhoangvn.extension.MSTeamsPreProjectAnalysisTask;
import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import io.github.minhhoangvn.utils.Constants;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.*;

public class SonarQubeMSTeamsNotifierPluginTest {

    private WireMockServer wireMockServer;
    private MSTeamsPreProjectAnalysisTask preAnalysisTask;
    private MSTeamsPostProjectAnalysisTask postAnalysisTask;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;
    private ScannerContext scannerContext;
    private Configuration mockConfiguration;

    @BeforeMethod
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Clear any previous validated configuration
        MSTeamsPreProjectAnalysisTask.clearValidatedConfig();

        // Create mock configuration
        mockConfiguration = mock(Configuration.class);
        setupMockConfiguration();
        
        // Create both tasks
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        postAnalysisTask = new MSTeamsPostProjectAnalysisTask();
        
        context = mock(Context.class);
        projectAnalysis = mock(ProjectAnalysis.class);
        project = mock(Project.class);
        ceTask = mock(CeTask.class);
        qualityGate = mock(QualityGate.class);
        scannerContext = mock(ScannerContext.class);

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
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        
        // Set up default scanner properties
        Map<String, String> scannerProps = new HashMap<>();
        scannerProps.put(Constants.ENABLE_NOTIFY, "true");
        scannerProps.put(Constants.WEBHOOK_URL, "http://localhost:" + wireMockServer.port() + "/webhook");
        scannerProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        scannerProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        scannerProps.put(Constants.SONAR_URL, "http://sonarqube.example.com");
        when(scannerContext.getProperties()).thenReturn(scannerProps);
    }

    private void setupMockConfiguration() {
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("http://localhost:" + wireMockServer.port() + "/webhook"));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://sonarqube.example.com"));
    }

    @AfterMethod
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        // Clear validated configuration
        MSTeamsPreProjectAnalysisTask.clearValidatedConfig();
        // Clear system properties
        System.clearProperty(Constants.ENABLE_NOTIFY);
        System.clearProperty(Constants.WEBHOOK_URL);
        System.clearProperty(Constants.WEBHOOK_MESSAGE_AVATAR);
        System.clearProperty(Constants.WEBHOOK_SEND_ON_FAILED);
        System.clearProperty(Constants.SONAR_URL);
    }

    @Test
    public void testFullWorkflow_PreAndPostAnalysis_SendsNotification() {
        // Arrange
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"text\": \"Test Project SonarQube Analysis Result\""))
                .withRequestBody(containing("\"value\": \"SUCCESS\""))
                .withRequestBody(containing("\"value\": \"Sonar way (OK)\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act - Run the full workflow
        preAnalysisTask.finished(context); // Validate configuration
        postAnalysisTask.finished(context); // Send notification

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testPreAnalysisConfigurationValidation() {
        // Act
        preAnalysisTask.finished(context);
        
        // Debug logging
        System.out.println("Configuration validated: " + MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        System.out.println("Enable notify: " + MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.ENABLE_NOTIFY));
        System.out.println("Webhook URL: " + MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL));
        System.out.println("All config: " + MSTeamsPreProjectAnalysisTask.getAllValidatedConfig());
        
        // Assert
        assert MSTeamsPreProjectAnalysisTask.isConfigurationValidated() : "Configuration should be validated";
        assert "true".equals(MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.ENABLE_NOTIFY)) : "Plugin should be enabled";
        assert MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, false) : "Plugin should be enabled (boolean)";
    }

    @Test
    public void testPostAnalysisWithPreValidatedConfig() {
        // Arrange - Run pre-analysis first
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testPostAnalysisWithoutPreValidation_UsesFallback() {
        // Arrange - Don't run pre-analysis, configuration won't be validated
        // Clear any existing validation
        MSTeamsPreProjectAnalysisTask.clearValidatedConfig();
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act - Should use fallback mode
        postAnalysisTask.finished(context);

        // Assert - Should still work via fallback
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testWorkflow_WithDisabledPlugin_DoesNotSendNotification() {
        // Arrange - Update the mock to disable the plugin
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("false"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(false));
        
        // Run pre-analysis with disabled config
        preAnalysisTask.finished(context);

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testWorkflow_WithInvalidWebhookUrl_DoesNotSendNotification() {
        // Arrange - Create completely new mock objects with invalid configuration
        Configuration invalidConfig = mock(Configuration.class);
        when(invalidConfig.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(invalidConfig.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(invalidConfig.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("invalid-url"));
        when(invalidConfig.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(invalidConfig.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(invalidConfig.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://sonarqube.example.com"));
        
        // Create new scanner context with invalid URL
        ScannerContext invalidScannerContext = mock(ScannerContext.class);
        Map<String, String> invalidScannerProps = new HashMap<>();
        invalidScannerProps.put(Constants.ENABLE_NOTIFY, "true");
        invalidScannerProps.put(Constants.WEBHOOK_URL, "invalid-url");
        invalidScannerProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        invalidScannerProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        invalidScannerProps.put(Constants.SONAR_URL, "http://sonarqube.example.com");
        when(invalidScannerContext.getProperties()).thenReturn(invalidScannerProps);
        
        // Update project analysis to use invalid scanner context
        when(projectAnalysis.getScannerContext()).thenReturn(invalidScannerContext);
        
        // Clear system properties to prevent fallback to system properties
        System.clearProperty(Constants.WEBHOOK_URL);
        System.setProperty(Constants.WEBHOOK_URL, "invalid-url"); // Also set invalid system property
        
        // Create new pre-analysis task with invalid config
        MSTeamsPreProjectAnalysisTask invalidPreAnalysisTask = new MSTeamsPreProjectAnalysisTask(invalidConfig);
        
        // Run pre-analysis with invalid URL - should fail validation
        invalidPreAnalysisTask.finished(context);
        
        // Verify that configuration validation failed
        assert !MSTeamsPreProjectAnalysisTask.isConfigurationValidated() : "Configuration should not be validated with invalid URL";

        // Act
        postAnalysisTask.finished(context);

        // Assert - Should not send any requests since all configuration sources have invalid URLs
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
        
        // Clean up
        System.clearProperty(Constants.WEBHOOK_URL);
    }

    @Test
    public void testWorkflow_WithFailedQualityGateAndSendOnFailedOnly() {
        // Arrange - Update config for send on failed only
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        // Run pre-analysis
        preAnalysisTask.finished(context);
        
        // Set up failed quality gate
        when(ceTask.getStatus()).thenReturn(CeTask.Status.FAILED);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testWorkflow_WithPassedQualityGateAndSendOnFailedOnly_DoesNotSend() {
        // Arrange - Update config for send on failed only
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        // Run pre-analysis
        preAnalysisTask.finished(context);
        
        // Quality gate passes (already set in setUp)
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testGetDescription() {
        // Act & Assert
        assert "MS Teams configuration validator for SonarQube analysis".equals(preAnalysisTask.getDescription());
        assert "MS Teams notification extension for SonarQube analysis results".equals(postAnalysisTask.getDescription());
    }

    @Test
    public void testWorkflow_WithServerError_HandlesGracefully() {
        // Arrange
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act - Should not throw exception
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testWorkflow_WithCustomAvatarUrl_SendsNotification() {
        // Arrange
        String customAvatarUrl = "https://example.com/custom-avatar.png";
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(customAvatarUrl));
        
        // Run pre-analysis with custom avatar
        preAnalysisTask.finished(context);
        
        // Debug: Check if custom avatar was cached
        System.out.println("Cached avatar URL: " + MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_MESSAGE_AVATAR));

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert - Just verify that a request was made, let's see the actual payload
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
        
        // Print actual requests for debugging
        wireMockServer.getAllServeEvents().forEach(event -> {
            System.out.println("Request body: " + event.getRequest().getBodyAsString());
        });
    }

    @Test
    public void testConfigurationPriority() {
        // Test that SonarQube configuration takes priority over system properties
        System.setProperty(Constants.WEBHOOK_URL, "https://system.property.url");
        
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("https://sonarqube.config.url"));
        
        preAnalysisTask.finished(context);
        
        // Assert
        assert "https://sonarqube.config.url".equals(MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL)) : 
            "SonarQube config should take priority over system properties";
    }
}