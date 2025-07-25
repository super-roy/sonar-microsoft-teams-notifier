package io.github.minhhoangvn.extension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;

import io.github.minhhoangvn.utils.Constants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.CeTask.Status;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Condition;
import org.sonar.api.ce.posttask.QualityGate.EvaluationStatus;
import org.sonar.api.ce.posttask.QualityGate.Operator;
import org.sonar.api.ce.posttask.ScannerContext;
import org.sonar.api.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MSTeamsPostProjectAnalysisTaskTest {

    private RequestBodyCaptureListener requestBodyCaptureListener = new RequestBodyCaptureListener();
    private WireMockServer wireMockServer;
    
    private MSTeamsPostProjectAnalysisTask plugin;
    private MSTeamsPreProjectAnalysisTask preAnalysisTask;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;
    private ScannerContext scannerContext;
    private Configuration mockConfiguration;

    private final Condition stubNonRatingCondition = new Condition() {
        @Override
        public EvaluationStatus getStatus() {
            return EvaluationStatus.OK;
        }

        @Override
        public String getMetricKey() {
            return "stub_metric_key_name";
        }

        @Override
        public Operator getOperator() {
            return Operator.GREATER_THAN;
        }

        @Override
        public String getErrorThreshold() {
            return "1";
        }

        @Override
        public String getValue() {
            return "1";
        }
    };

    private final Condition stubRatingCondition = new Condition() {
        @Override
        public EvaluationStatus getStatus() {
            return EvaluationStatus.OK;
        }

        @Override
        public String getMetricKey() {
            return "stub_rating_metric_key_name";
        }

        @Override
        public Operator getOperator() {
            return Operator.GREATER_THAN;
        }

        @Override
        public String getErrorThreshold() {
            return "1";
        }

        @Override
        public String getValue() {
            return "1";
        }
    };

    private final Condition stubWithNoValueCondition = new Condition() {
        @Override
        public EvaluationStatus getStatus() {
            return EvaluationStatus.NO_VALUE;
        }

        @Override
        public String getMetricKey() {
            return "stub_rating_metric_key_name";
        }

        @Override
        public Operator getOperator() {
            return Operator.GREATER_THAN;
        }

        @Override
        public String getErrorThreshold() {
            return "1";
        }

        @Override
        public String getValue() {
            return "1";
        }
    };

    @Before
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        
        // Create mock configuration
        mockConfiguration = mock(Configuration.class);
        setupMockConfiguration();
        
        // Create pre-analysis task and run it to validate configuration
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        
        // Use default constructor for post-analysis task
        plugin = new MSTeamsPostProjectAnalysisTask();
        
        // Initialize the other mock objects
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
        when(ceTask.getStatus()).thenReturn(Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);
        when(qualityGate.getName()).thenReturn("Sonar way");
        when(qualityGate.getConditions()).thenReturn(List.of());
        
        // Set up scanner properties to include configuration
        Map<String, String> scannerProps = new HashMap<>();
        scannerProps.put(Constants.ENABLE_NOTIFY, "true");
        scannerProps.put(Constants.WEBHOOK_URL, "http://localhost:" + wireMockServer.port() + "/webhook");
        scannerProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        scannerProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        scannerProps.put(Constants.SONAR_URL, "http://sonarqube.example.com");
        
        when(scannerContext.getProperties()).thenReturn(scannerProps);
        
        // Run pre-analysis to validate configuration
        preAnalysisTask.finished(context);
    }

    private void setupMockConfiguration() {
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("true"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(true));
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("http://localhost:" + wireMockServer.port() + "/webhook"));
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR));
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(false));
        when(mockConfiguration.get(Constants.SONAR_URL)).thenReturn(Optional.of("http://sonarqube.example.com"));
    }

    @After
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        // Clear system properties
        System.clearProperty(Constants.ENABLE_NOTIFY);
        System.clearProperty(Constants.WEBHOOK_URL);
        System.clearProperty(Constants.SONAR_URL);
        System.clearProperty(Constants.WEBHOOK_SEND_ON_FAILED);
        System.clearProperty(Constants.WEBHOOK_MESSAGE_AVATAR);
    }

    @Test
    public void testConstructor() {
        // Test default constructor
        MSTeamsPostProjectAnalysisTask task = new MSTeamsPostProjectAnalysisTask();
        assertEquals("MS Teams notification extension for SonarQube analysis results", task.getDescription());
    }

    @Test
    public void testPreAnalysisValidation() {
        // Test that pre-analysis validation works
        assertTrue("Configuration should be validated", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        assertEquals("true", MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.ENABLE_NOTIFY));
        assertTrue("Plugin should be enabled", MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, false));
    }

    @Test
    public void testFinished_WithValidConfiguration_SendsNotification() {
        // Arrange - the mock config is already set up in setUp() and validated by pre-analysis
        
        stubFor(post(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withRequestBody(containing("\"type\": \"AdaptiveCard\""))
                .withRequestBody(containing("\"text\": \"Test Project SonarQube Analysis Result\""))
                .withRequestBody(containing("\"value\": \"SUCCESS\""))
                .withRequestBody(containing("\"value\": \"Sonar way (OK)\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testFinished_WithFailedQualityGateAndSendOnFailedOnly_SendsNotification() {
        // Arrange - update the mock config to send on failed only
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        // Re-run pre-analysis with updated config
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);
        
        // Set up failed quality gate
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testFinished_WithPassedQualityGateAndSendOnFailedOnly_DoesNotSendNotification() {
        // Arrange - update the mock config to send on failed only
        when(mockConfiguration.getBoolean(Constants.WEBHOOK_SEND_ON_FAILED)).thenReturn(Optional.of(true));
        
        // Re-run pre-analysis with updated config
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);
        
        // Set up passed quality gate (already set in setUp)
        when(qualityGate.getStatus()).thenReturn(QualityGate.Status.OK);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert - No request should be made
        assertEquals(0, wireMockServer.getAllServeEvents().size());
    }

    @Test
    public void testFinished_WithDisabledPlugin_DoesNotSendNotification() {
        // Arrange - disable the plugin
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of("false"));
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.of(false));
        
        // Re-run pre-analysis with disabled config
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert - No request should be made
        assertEquals(0, wireMockServer.getAllServeEvents().size());
    }

    @Test
    public void testFinished_WithMissingWebhookUrl_DoesNotSendNotification() {
        // Arrange - set empty webhook URL
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of(""));
        
        // Re-run pre-analysis with missing webhook URL
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert - No request should be made (configuration should be invalid)
        assertEquals(0, wireMockServer.getAllServeEvents().size());
    }

    @Test
    public void testFinished_WithInvalidWebhookUrl_DoesNotSendNotification() {
        // Arrange - set invalid webhook URL
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.of("invalid-url"));
        
        // Re-run pre-analysis with invalid webhook URL
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);

        // Configuration should be invalid
        assertFalse("Configuration should be invalid with bad webhook URL", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());

        // Act
        plugin.finished(context);

        // Assert - Should fall back to direct configuration and still not send
        assertEquals(0, wireMockServer.getAllServeEvents().size());
    }

    @Test
    public void testFinished_WithCustomAvatarUrl_SendsNotification() {
        // Arrange
        String customAvatarUrl = "https://example.com/custom-avatar.png";
        when(mockConfiguration.get(Constants.WEBHOOK_MESSAGE_AVATAR)).thenReturn(Optional.of(customAvatarUrl));
        
        // Re-run pre-analysis with custom avatar
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);

        stubFor(post(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\""))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        verify(postRequestedFor(urlEqualTo("/webhook"))
                .withRequestBody(containing("\"url\": \"" + customAvatarUrl + "\"")));
    }

    @Test
    public void testFinished_WithServerError_HandlesGracefully() {
        // Arrange - use the default mock setup

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act - Should not throw exception
        plugin.finished(context);

        // Assert - Request was made despite error
        verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testFinished_WithoutPreValidation_UsesFallbackMode() {
        // Arrange - Clear the validated configuration to simulate no pre-validation
        // We can do this by creating a new pre-analysis task with disabled config
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        
        MSTeamsPreProjectAnalysisTask emptyPreAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        // Don't run the pre-analysis task, so configuration won't be validated
        
        // Set up scanner properties for fallback
        Map<String, String> scannerProps = new HashMap<>();
        scannerProps.put(Constants.ENABLE_NOTIFY, "true");
        scannerProps.put(Constants.WEBHOOK_URL, "http://localhost:" + wireMockServer.port() + "/webhook");
        scannerProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        scannerProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        scannerProps.put(Constants.SONAR_URL, "http://sonarqube.example.com");
        when(scannerContext.getProperties()).thenReturn(scannerProps);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert - Should use fallback mode and still send notification
        verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testFinished_WithSystemProperties_SendsNotification() {
        // Arrange - Set system properties instead of configuration
        System.setProperty(Constants.ENABLE_NOTIFY, "true");
        System.setProperty(Constants.WEBHOOK_URL, "http://localhost:" + wireMockServer.port() + "/webhook");
        System.setProperty(Constants.WEBHOOK_MESSAGE_AVATAR, Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        System.setProperty(Constants.WEBHOOK_SEND_ON_FAILED, "false");
        System.setProperty(Constants.SONAR_URL, "http://sonarqube.example.com");
        
        // Clear SonarQube configuration
        when(mockConfiguration.get(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.getBoolean(Constants.ENABLE_NOTIFY)).thenReturn(Optional.empty());
        when(mockConfiguration.get(Constants.WEBHOOK_URL)).thenReturn(Optional.empty());
        
        // Re-run pre-analysis to pick up system properties
        preAnalysisTask = new MSTeamsPreProjectAnalysisTask(mockConfiguration);
        preAnalysisTask.finished(context);

        stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("1")));

        // Act
        plugin.finished(context);

        // Assert
        verify(postRequestedFor(urlEqualTo("/webhook")));
    }

    @Test
    public void testValidatedConfigAccess() {
        // Test static methods for accessing validated configuration
        assertTrue("Configuration should be validated", MSTeamsPreProjectAnalysisTask.isConfigurationValidated());
        
        String webhookUrl = MSTeamsPreProjectAnalysisTask.getValidatedConfig(Constants.WEBHOOK_URL);
        assertTrue("Webhook URL should be set", webhookUrl != null && !webhookUrl.isEmpty());
        
        boolean enabled = MSTeamsPreProjectAnalysisTask.getValidatedBooleanConfig(Constants.ENABLE_NOTIFY, false);
        assertTrue("Plugin should be enabled", enabled);
        
        Map<String, String> allConfig = MSTeamsPreProjectAnalysisTask.getAllValidatedConfig();
        assertTrue("Config map should contain entries", allConfig.size() > 0);
        assertTrue("Config should contain enable setting", allConfig.containsKey(Constants.ENABLE_NOTIFY));
    }

    // Keep one backward compatibility test with the old approach
    @Test
    public void testFinishedWithSendNotify_BackwardCompatibility() {
        wireMockServer.addMockServiceRequestListener(requestBodyCaptureListener);
        
        Context mockContext = Mockito.mock(Context.class);
        ProjectAnalysis mockProjectAnalysis = mock(ProjectAnalysis.class);
        Project mockProject = mock(Project.class);
        CeTask mockCeTask = mock(CeTask.class);
        ScannerContext mockScannerContext = mock(ScannerContext.class);
        QualityGate mockQualityGate = mock(QualityGate.class);

        when(mockProjectAnalysis.getProject()).thenReturn(mockProject);
        when(mockProjectAnalysis.getCeTask()).thenReturn(mockCeTask);
        when(mockProjectAnalysis.getQualityGate()).thenReturn(mockQualityGate);
        when(mockProjectAnalysis.getScannerContext()).thenReturn(mockScannerContext);
        when(mockProject.getName()).thenReturn("stub-project-name");
        when(mockProject.getKey()).thenReturn("stub-project-key");
        when(mockCeTask.getStatus()).thenReturn(Status.FAILED);
        when(mockQualityGate.getStatus()).thenReturn(QualityGate.Status.ERROR); // Failed quality gate
        when(mockQualityGate.getName()).thenReturn("Sonar way");
        when(mockQualityGate.getConditions()).thenReturn(List.of(stubNonRatingCondition));
        
        // Set up scanner properties
        Map<String, String> backwardCompatProps = new HashMap<>();
        backwardCompatProps.put(Constants.ENABLE_NOTIFY, "true");
        backwardCompatProps.put(Constants.WEBHOOK_URL, "http://localhost:" + wireMockServer.port() + "/mock");
        backwardCompatProps.put(Constants.WEBHOOK_MESSAGE_AVATAR, "https://raw.githubusercontent.com/toilatester/logo/main/toilatester.png");
        backwardCompatProps.put(Constants.WEBHOOK_SEND_ON_FAILED, "true");
        backwardCompatProps.put(Constants.SONAR_URL, "https://toilatester.blog");
        when(mockScannerContext.getProperties()).thenReturn(backwardCompatProps);

        Mockito.when(mockContext.getProjectAnalysis()).thenReturn(mockProjectAnalysis);

        stubFor(post(urlEqualTo("/mock"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Mock response\"}")));

        MSTeamsPostProjectAnalysisTask backwardCompatPlugin = new MSTeamsPostProjectAnalysisTask();
        backwardCompatPlugin.finished(mockContext);

        // Just verify that a request was made
        Assert.assertTrue("Expected at least one request", requestBodyCaptureListener.getCapturedBodies().size() > 0);
    }

    static class RequestBodyCaptureListener implements RequestListener {

        private final List<String> capturedBodies = new ArrayList<>();

        public List<String> getCapturedBodies() {
            return capturedBodies;
        }

        @Override
        public void requestReceived(Request request, Response response) {
            String requestBody = request.getBodyAsString();
            capturedBodies.add(requestBody);
        }
    }
}
