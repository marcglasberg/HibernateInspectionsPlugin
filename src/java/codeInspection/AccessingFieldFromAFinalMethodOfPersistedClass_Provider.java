package java.codeInspection;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import org.jetbrains.annotations.NotNull;

/**
 * @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg ; https://github.com/java)
 */
public class AccessingFieldFromAFinalMethodOfPersistedClass_Provider
        implements InspectionToolProvider {
    @Override
    @NotNull
    public Class<? extends LocalInspectionTool>[] getInspectionClasses() {
        return new Class[]{AccessingFieldFromAFinalMethodOfPersistedClass_Inspection.class};
    }
}
