package io.github.minhhoangvn.utils;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.CeTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.ce.posttask.QualityGate.Status;
import org.sonar.api.ce.posttask.QualityGate.Condition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class AdaptiveCardsFormatTest {

    private ProjectAnalysis projectAnalysis;
    private Project project;
    private CeTask ceTask;
    private QualityGate qualityGate;

    @BeforeMethod
    public void setUp() {
        projectAnalysis = mock(ProjectAnalysis.class);
        project = mock(Project.class);
        ceTask = mock(CeTask.class);
        qualityGate = mock(QualityGate.class);

        when(projectAnalysis.getProject()).thenReturn(project);
        when(projectAnalysis.getCeTask()).thenReturn(ceTask);
        when(projectAnalysis.getQualityGate()).thenReturn(qualityGate);
        
        when(project.getName()).thenReturn("Test Project");
        when(project.getKey()).thenReturn("test-project-key");
        when(ceTask.getStatus()).thenReturn(CeTask.Status.SUCCESS);
        when(qualityGate.getStatus()).thenReturn(Status.OK);
        when(qualityGate.getName()).thenReturn("Sonar way");
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithBasicData() {
        // Arrange
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"type\": \"AdaptiveCard\""));
        Assert.assertTrue(result.contains("\"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\""));
        Assert.assertTrue(result.contains("\"version\": \"1.5\""));
        Assert.assertTrue(result.contains("\"text\": \"SonarQube Analysis Result\""));
        Assert.assertTrue(result.contains("\"text\": \"Test Project SonarQube Analysis Result\""));
        Assert.assertTrue(result.contains("\"title\": \"Status\""));
        Assert.assertTrue(result.contains("\"value\": \"SUCCESS\""));
        Assert.assertTrue(result.contains("\"title\": \"Quality Gate\""));
        Assert.assertTrue(result.contains("\"value\": \"Sonar way (OK)\""));
        Assert.assertTrue(result.contains("\"url\": \"" + projectUrl + "\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithMetrics() {
        // Arrange
        Condition newViolationsCondition = mock(Condition.class);
        when(newViolationsCondition.getMetricKey()).thenReturn("new_violations");
        when(newViolationsCondition.getValue()).thenReturn("5");
        when(newViolationsCondition.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);

        Condition newCoverageCondition = mock(Condition.class);
        when(newCoverageCondition.getMetricKey()).thenReturn("new_coverage");
        when(newCoverageCondition.getValue()).thenReturn("80.5");
        when(newCoverageCondition.getStatus()).thenReturn(QualityGate.EvaluationStatus.OK);

        List<Condition> conditions = Arrays.asList(newViolationsCondition, newCoverageCondition);
        when(qualityGate.getConditions()).thenReturn(conditions);
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert
        Assert.assertTrue(result.contains("\"title\": \"New Violations\""));
        Assert.assertTrue(result.contains("\"value\": \"5\""));
        Assert.assertTrue(result.contains("\"title\": \"New Coverage\""));
        Assert.assertTrue(result.contains("\"value\": \"80.5\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithFailedQualityGate() {
        // Arrange
        when(qualityGate.getStatus()).thenReturn(Status.ERROR);
        when(ceTask.getStatus()).thenReturn(CeTask.Status.FAILED);
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert
        Assert.assertTrue(result.contains("\"value\": \"FAILED\""));
        Assert.assertTrue(result.contains("\"value\": \"Sonar way (ERROR)\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_ExactFormatting() {
        // Arrange
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert - Check exact formatting matches template
        Assert.assertTrue(result.contains("    \"type\": \"AdaptiveCard\","));
        Assert.assertTrue(result.contains("    \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\","));
        Assert.assertTrue(result.contains("    \"version\": \"1.5\","));
        Assert.assertTrue(result.contains("    \"body\": ["));
        Assert.assertTrue(result.contains("        {"));
        Assert.assertTrue(result.contains("            \"type\": \"TextBlock\","));
        Assert.assertTrue(result.contains("            \"size\": \"Medium\","));
        Assert.assertTrue(result.contains("            \"weight\": \"Bolder\","));
        Assert.assertTrue(result.contains("            \"text\": \"SonarQube Analysis Result\""));
        Assert.assertTrue(result.contains("        },"));
        Assert.assertTrue(result.contains("    ],"));
        Assert.assertTrue(result.contains("    \"actions\": ["));
        Assert.assertTrue(result.contains("            \"type\": \"Action.OpenUrl\","));
        Assert.assertTrue(result.contains("            \"title\": \"View Analysis\","));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithSupermicroLogo() {
        // Arrange
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert
        Assert.assertTrue(result.contains("\"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Super_Micro_Computer_Logo.svg/330px-Super_Micro_Computer_Logo.svg.png\""));
        Assert.assertTrue(result.contains("\"altText\": \"Supermicro IT2 DevOps Team\""));
        Assert.assertTrue(result.contains("\"text\": \"Supermicro IT2 DevOps Team\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithAllFactsPresent() {
        // Arrange
        when(qualityGate.getConditions()).thenReturn(Collections.emptyList());
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert - Verify all 6 required facts are present
        Assert.assertTrue(result.contains("\"title\": \"Status\""));
        Assert.assertTrue(result.contains("\"title\": \"Quality Gate\""));
        Assert.assertTrue(result.contains("\"title\": \"New Violations\""));
        Assert.assertTrue(result.contains("\"title\": \"New Coverage\""));
        Assert.assertTrue(result.contains("\"title\": \"New Duplicated Lines Density\""));
        Assert.assertTrue(result.contains("\"title\": \"New Security Hotspots Reviewed\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithNullQualityGate() {
        // Arrange
        when(projectAnalysis.getQualityGate()).thenReturn(null);
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act
        String result = AdaptiveCardsFormat.createMessageCardJSONPayload(projectAnalysis, projectUrl);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"value\": \"N/A\""));
    }

    @Test
    public void testCreateMessageCardJSONPayload_WithNullProjectAnalysis() {
        // Arrange
        String projectUrl = "http://sonarqube.example.com/dashboard?id=test-project-key";

        // Act & Assert - Should handle null gracefully
        try {
            String result = AdaptiveCardsFormat.createMessageCardJSONPayload(null, projectUrl);
            Assert.fail("Should throw exception for null project analysis");
        } catch (Exception e) {
            // Expected behavior
            Assert.assertNotNull(e);
        }
    }
}
