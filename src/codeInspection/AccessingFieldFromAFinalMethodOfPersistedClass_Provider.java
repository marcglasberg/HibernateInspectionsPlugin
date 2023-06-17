package codeInspection;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import org.jetbrains.annotations.NotNull;

/**
 * @author Marcelo Glasberg (<a href="https://stackoverflow.com/users/3411681/marcg">Stack Overflow</a> ; <a href="https://https://github.com/marcglasberg">GitHub</a>)
 */
public class AccessingFieldFromAFinalMethodOfPersistedClass_Provider
        implements InspectionToolProvider {
    @Override
    @NotNull
    public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
        return new Class[]{AccessingFieldFromAFinalMethodOfPersistedClass_Inspection.class};
    }
}
