package center.oneapi.mobile.ui.composer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComposerState {
    public final String sectionId;
    public final String projectLabel;
    public final String assistantLabel;
    public final String modelLabel;
    public final String reasoningLabel;
    public final String contextLabel;
    public final String sizeLabel;
    public final String qualityLabel;
    public final List<String> selectedSkills;
    public final boolean fullPermission;
    public final boolean desktopMode;
    public final boolean enabled;
    public final String hint;

    public ComposerState(
            String sectionId,
            String projectLabel,
            String assistantLabel,
            String modelLabel,
            String reasoningLabel,
            String contextLabel,
            String sizeLabel,
            String qualityLabel,
            List<String> selectedSkills,
            boolean fullPermission,
            boolean desktopMode,
            boolean enabled,
            String hint
    ) {
        this.sectionId = clean(sectionId);
        this.projectLabel = clean(projectLabel);
        this.assistantLabel = clean(assistantLabel);
        this.modelLabel = clean(modelLabel);
        this.reasoningLabel = clean(reasoningLabel);
        this.contextLabel = clean(contextLabel);
        this.sizeLabel = clean(sizeLabel);
        this.qualityLabel = clean(qualityLabel);
        this.selectedSkills = selectedSkills == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(selectedSkills));
        this.fullPermission = fullPermission;
        this.desktopMode = desktopMode;
        this.enabled = enabled;
        this.hint = clean(hint);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
