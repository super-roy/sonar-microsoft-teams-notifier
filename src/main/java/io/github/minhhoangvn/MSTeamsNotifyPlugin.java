package io.github.minhhoangvn;

import io.github.minhhoangvn.extension.MSTeamsPostProjectAnalysisTask;
import org.sonar.api.Plugin;

public class MSTeamsNotifyPlugin implements Plugin {
    
    @Override
    public void define(Context context) {
        context.addExtension(MSTeamsPostProjectAnalysisTask.class);
        context.addExtension(SonarQubeMSTeamsNotifierPlugin.class);
    }
}
