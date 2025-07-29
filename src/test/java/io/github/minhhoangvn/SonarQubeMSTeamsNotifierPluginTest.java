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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        
        // Create both tasks
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        postAnalysisTask = new MSTeamsPostProjectAnalysisTask();
        
        setupMockObjects();
        setupMockConfiguration();
    }

    private void setupMockObjects() {
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
        MSTeamsPreProjectAnalysisTask.clearValidatedConfig();
        clearSystemProperties();
    }

    private void clearSystemProperties() {
        System.clearProperty(Constants.ENABLE_NOTIFY);
        System.clearProperty(Constants.WEBHOOK_URL);
        System.clearProperty(Constants.WEBHOOK_MESSAGE_AVATAR);
        System.clearProperty(Constants.WEBHOOK_SEND_ON_FAILED);
        System.clearProperty(Constants.SONAR_URL);
    }

    @Test
    public void testFullWorkflow_PreAndPostAnalysis_SendsNotification() {
        // Arrange - Updated to match actual payload structure
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"attachments\""))
                .withRequestBody(containing("\"contentType\": \"application/vnd.microsoft.card.adaptive\""))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("Test Project SonarQube Analysis Result"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        preAnalysisTask.finished(context);
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testPreAnalysisConfigurationValidation() {
        // Act
        preAnalysisTask.finished(context);
        
        // Assert
        if (!MSTeamsPreProjectAnalysisTask.isConfigurationValidated()) {
            throw new AssertionError("Configuration should be validated");
        }
        
        if (!"true".equals(MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.ENABLE_NOTIFY))) {
            throw new AssertionError("Plugin should be enabled");
        }
        
        if (!MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, false)) {
            throw new AssertionError("Plugin should be enabled (boolean)");
        }
    }

    @Test
    public void testWorkflow_WithNoValueConditions_SendsNotification() {
        // Arrange - Create a quality gate with NO_VALUE conditions
        QualityGate.Condition noValueCondition = mock(QualityGate.Condition.class);
        when(noValueCondition.getStatus()).thenReturn(QualityGate.EvaluationStatus.NO_VALUE);
        when(noValueCondition.getMetricKey()).thenReturn("new_coverage");
        
        List<QualityGate.Condition> conditions = Arrays.asList(noValueCondition);
        when(qualityGate.getConditions()).thenReturn(conditions);
        
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"New Coverage\""))
                .withRequestBody(containing("\"N/A\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testWorkflow_WithDisabledPlugin_DoesNotSendNotification() {
        // Arrange
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("false"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(false));
        
        preAnalysisTask.finished(context);

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
    }

    @Test
    public void testWorkflow_WithInvalidWebhookUrl_DoesNotSendNotification() {
        // Arrange - Ensure ALL configuration sources have invalid URLs
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("invalid-url"));
        
        Map<String, String> invalidScannerProps = new HashMap<>();
        invalidScannerProps.put(Constants.ENABLE_NOTIFY, "true");
        invalidScannerProps.put(Constants.WEBHOOK_URL, "invalid-url");
        invalidScannerProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        invalidScannerProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        invalidScannerProps.put(Constants.SONAR_URL, "http://sonarqube.example.com");
        when(scannerContext.getProperties()).thenReturn(invalidScannerProps);
        
        System.setProperty(Constants.WEBHOOK_URL, "invalid-url");
        System.setProperty("SONAR_MSTEAMS_WEBHOOK_URL", "invalid-url");
        
        preAnalysisTask.finished(context);
        
        assert !MSTeamsPreProjectAnalysisTask.isConfigurationValidated() : "Configuration should not be validated with invalid URL";

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(0, postRequestedFor(urlMatching(".*")));
        
        // Clean up
        System.clearProperty(Constants.WEBHOOK_URL);
        System.clearProperty("SONAR_MSTEAMS_WEBHOOK_URL");
    }

    @Test
    public void testWorkflow_WithFailedQualityGateAndSendOnFailedOnly() {
        // Arrange
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        preAnalysisTask.finished(context);
        
        when(ceTask.getStatus()).thenReturn(CeTask.Status.FAILED);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
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
        // Arrange
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        // Run pre-analysis
        preAnalysisTask.finished(context);
        
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
        if (!"MS Teams configuration validator for SonarQube analysis".equals(preAnalysisTask.getDescription())) {
            throw new AssertionError("Pre-analysis task description mismatch");
        }
        if (!"MS Teams notification extension for SonarQube analysis results".equals(postAnalysisTask.getDescription())) {
            throw new AssertionError("Post-analysis task description mismatch");
        }
    }

    @Test
    public void testWorkflow_WithServerError_HandlesGracefully() {
        // Arrange
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testWorkflow_WithCustomAvatarUrl_SendsNotification() {
        // Arrange
        String customAvatarUrl = "https://example.com/custom-avatar.png";
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(customAvatarUrl));
        
        preAnalysisTask.finished(context);

        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\"")));
        
        String cachedAvatarUrl = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_MESSAGE_AVATAR);
        if (!customAvatarUrl.equals(cachedAvatarUrl)) {
            throw new AssertionError("Custom avatar URL not cached properly. Expected: " + customAvatarUrl + ", Got: " + cachedAvatarUrl);
        }
    }

    @Test
    public void testPayloadStructure_NoValueAware() {
        // Arrange - Set up mixed conditions (some NO_VALUE, some with values)
        QualityGate.Condition coverageCondition = mock(QualityGate.Condition.class);
        when(coverageCondition.getStatus()).thenReturn(QualityGate.EvaluationStatus.NO_VALUE);
        when(coverageCondition.getMetricKey()).thenReturn("new_coverage");

        QualityGate.Condition violationsCondition = mock(QualityGate.Condition.class);
        when(violationsCondition.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);
        when(violationsCondition.getMetricKey()).thenReturn("new_violations");
        when(violationsCondition.getValue()).thenReturn("0");

        when(qualityGate.getConditions()).thenReturn(Arrays.asList(coverageCondition, violationsCondition));

        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"attachments\""))
                .withRequestBody(containing("\"contentType\": \"application/vnd.microsoft.card.adaptive\""))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"New Coverage\""))
                .withRequestBody(containing("\"N/A\""))
                .withRequestBody(containing("\"New Violations\""))
                .withRequestBody(containing("\"0\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert - Check for the actual structure that's being sent
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"attachments\""))
                .withRequestBody(containing("\"New Coverage\""))
                .withRequestBody(containing("\"N/A\""))
                .withRequestBody(containing("\"New Violations\""))
                .withRequestBody(containing("\"0\"")));
    }

    @Test
    public void testDirectNoValuePayloadCreation() {
        // Test that NO_VALUE conditions are handled properly
        
        preAnalysisTask.finished(context);
        
        // Create condition that should show N/A
        QualityGate.Condition condition = mock(QualityGate.Condition.class);
        when(condition.getStatus()).thenReturn(QualityGate.EvaluationStatus.NO_VALUE);
        when(condition.getMetricKey()).thenReturn("new_coverage");
        when(qualityGate.getConditions()).thenReturn(Arrays.asList(condition));
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"attachments\""))
                .withRequestBody(containing("\"contentType\": \"application/vnd.microsoft.card.adaptive\""))
                .withRequestBody(containing("\"New Coverage\""))
                .withRequestBody(containing("\"N/A\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));
        
        // Act
        postAnalysisTask.finished(context);
        
        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testSimplifiedPayloadStructure() {
        // Test the basic payload structure 
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"type\": \"message\""))
                .withRequestBody(containing("\"attachments\""))
                .withRequestBody(containing("\"contentType\": \"application/vnd.microsoft.card.adaptive\""))
                .withRequestBody(containing("\"content\""))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"SonarQube Analysis Result\""))
                .withRequestBody(containing("Test Project SonarQube Analysis Result"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    // Add a test to verify the specific payload structure elements
    @Test
    public void testPayloadStructure_VerifyElements() {
        // Test specific elements of the payload structure
        
        preAnalysisTask.finished(context);
        
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        postAnalysisTask.finished(context);

        // Assert
        wireMockServer.verify(postRequestedFor(urlEqualTo("/webhook")));
        
        // Verify specific payload elements
        wireMockServer.getAllServeEvents().forEach(event -> {
            String body = event.getRequest().getBodyAsString();
            
            // Verify basic structure
            assert body.contains("\"type\": \"message\"") : "Should contain message type";
            assert body.contains("\"attachments\"") : "Should contain attachments";
            assert body.contains("\"contentType\": \"application/vnd.microsoft.card.adaptive\"") : "Should contain adaptive card content type";
            assert body.contains("\"type\": \"AdaptiveCard\"") : "Should contain AdaptiveCard type";
            
            // Verify content
            assert body.contains("\"SonarQube Analysis Result\"") : "Should contain analysis result title";
            assert body.contains("Test Project SonarQube Analysis Result") : "Should contain project name";
            assert body.contains("\"Status\"") : "Should contain status field";
            assert body.contains("\"SUCCESS\"") : "Should contain success status";
            assert body.contains("\"Quality Gate\"") : "Should contain quality gate field";
            assert body.contains("\"Sonar way (OK)\"") : "Should contain quality gate status";
            
            // Verify action
            assert body.contains("\"Action.OpenUrl\"") : "Should contain open URL action";
            assert body.contains("\"View Analysis\"") : "Should contain view analysis button";
            assert body.contains("http://sonarqube.example.com/dashboard?id=test-project-key") : "Should contain project URL";
        });
    }
}