package codeInspection;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import org.jetbrains.annotations.NotNull;

/**
 * @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg ; https://github.com/java)
 */
public class PersistedClassIsFinal_Provider
        implements InspectionToolProvider {
    @NotNull
    @Override
    public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
        return new Class[]{PersistedClassIsFinal_Inspection.class};
    }
}
