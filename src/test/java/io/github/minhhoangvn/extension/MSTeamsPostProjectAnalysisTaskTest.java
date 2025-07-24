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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;

import io.github.minhhoangvn.config.MSTeamsConfigurationProvider;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MSTeamsPostProjectAnalysisTaskTest {

    private RequestBodyCaptureListener requestBodyCaptureListener = new RequestBodyCaptureListener();
    private WireMockServer wireMockServer;
    
    // Add these missing variables
    private MSTeamsPostProjectAnalysisTask plugin;
    private Context context;
    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;
    private ScannerContext scannerContext;
    private MSTeamsConfigurationProvider mockConfigProvider;

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
        
        // Mock the configuration provider
        mockConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(mockConfigProvider.isEnabled()).thenReturn(true);
        when(mockConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/webhook");
        when(mockConfigProvider.getAvatarUrl()).thenReturn(Constants.DEFAULT_WEBHOOK_MESSAGE_AVATAR);
        when(mockConfigProvider.getSendOnFailedOnly()).thenReturn(false);
        when(mockConfigProvider.getBaseUrl()).thenReturn("http://sonarqube.example.com");
        
        // Initialize the plugin with the mock config provider
        plugin = new MSTeamsPostProjectAnalysisTask(mockConfigProvider);
        
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
        when(scannerContext.getProperties()).thenReturn(Map.of());
    }

    @After
    public void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        // Clear system properties
        System.clearProperty("sonar.msteams.enable");
        System.clearProperty("sonar.msteams.webhook.url");
        System.clearProperty("sonar.core.serverBaseURL");
        System.clearProperty("sonar.msteams.send.on.failed");
        System.clearProperty("sonar.msteams.avatar.url");
    }

    @Test
    public void testConstructor() {
        // Create a mock config provider for this test
        MSTeamsConfigurationProvider testConfigProvider = mock(MSTeamsConfigurationProvider.class);
        MSTeamsPostProjectAnalysisTask task = new MSTeamsPostProjectAnalysisTask(testConfigProvider);
        assertEquals("MS Teams notification extension for SonarQube analysis results", task.getDescription());
    }

    @Test
    public void testFinished_WithValidConfiguration_SendsNotification() {
        // Arrange - the mock config is already set up in setUp()
        
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

        // Debug: Print what was actually received
        System.out.println("=== Received Requests ===");
        wireMockServer.getAllServeEvents().forEach(event -> {
            System.out.println("Method: " + event.getRequest().getMethod());
            System.out.println("URL: " + event.getRequest().getUrl());
            System.out.println("Headers: " + event.getRequest().getHeaders());
            System.out.println("Body: " + event.getRequest().getBodyAsString());
            System.out.println("---");
        });

        // Assert
        verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Accept", equalTo("application/json")));
    }

    @Test
    public void testFinished_WithFailedQualityGateAndSendOnFailedOnly_SendsNotification() {
        // Arrange - update the mock config to send on failed only
        when(mockConfigProvider.getSendOnFailedOnly()).thenReturn(true);
        
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
        when(mockConfigProvider.getSendOnFailedOnly()).thenReturn(true);
        
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
        when(mockConfigProvider.isEnabled()).thenReturn(false);

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
        when(mockConfigProvider.getWebhookUrl()).thenReturn("");

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
    public void testFinished_WithCustomAvatarUrl_SendsNotification() {
        // Arrange
        String customAvatarUrl = "https://example.com/custom-avatar.png";
        when(mockConfigProvider.getAvatarUrl()).thenReturn(customAvatarUrl);

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

    // Keep one backward compatibility test with the old approach
    @Test
    public void testFinishedWithSendNotify_BackwardCompatibility() {
        wireMockServer.addMockServiceRequestListener(requestBodyCaptureListener);
        
        // Create a separate test with a different config approach
        MSTeamsConfigurationProvider backwardCompatConfigProvider = mock(MSTeamsConfigurationProvider.class);
        when(backwardCompatConfigProvider.isEnabled()).thenReturn(true);
        when(backwardCompatConfigProvider.getWebhookUrl()).thenReturn("http://localhost:" + wireMockServer.port() + "/mock");
        when(backwardCompatConfigProvider.getAvatarUrl()).thenReturn("https://raw.githubusercontent.com/toilatester/logo/main/toilatester.png");
        when(backwardCompatConfigProvider.getSendOnFailedOnly()).thenReturn(true);
        when(backwardCompatConfigProvider.getBaseUrl()).thenReturn("https://toilatester.blog");
        
        MSTeamsPostProjectAnalysisTask backwardCompatPlugin = new MSTeamsPostProjectAnalysisTask(backwardCompatConfigProvider);
        
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
        when(mockScannerContext.getProperties()).thenReturn(Map.of());

        Mockito.when(mockContext.getProjectAnalysis()).thenReturn(mockProjectAnalysis);

        stubFor(post(urlEqualTo("/mock"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Mock response\"}")));

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
