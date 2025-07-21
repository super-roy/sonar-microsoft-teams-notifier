package io.github.minhhoangvn;

import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import io.github.minhhoangvn.extension.MSTeamsPreProjectAnalysisTask;
import io.github.minhhoangvn.settings.MSTeamsNotifyProperties;
import org.sonar.api.Plugin;

public class MSTeamsNotifyPlugin implements Plugin {
    
    @Override
    public void define(Context context) {
        // Register the post-analysis task (main one)
        context.addExtension(MSTeamsPostProjectAnalysisTask.class);
        
        // Register the pre-analysis task if you need it
        // context.addExtension(MSTeamsPreProjectAnalysisTask.class);
        
        // Register the property definitions to make them visible in SonarQube admin
        MSTeamsNotifyProperties.getProperties().forEach(context::addExtension);
    }
}
