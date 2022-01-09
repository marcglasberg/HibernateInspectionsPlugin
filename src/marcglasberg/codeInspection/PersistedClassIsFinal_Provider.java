package marcglasberg.codeInspection;

import com.intellij.codeInspection.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg ; https://github.com/marcglasberg)
 */
public class PersistedClassIsFinal_Provider
        implements InspectionToolProvider {
    @NotNull
    @Override
    public Class<? extends com.intellij.codeInspection.LocalInspectionTool>[] getInspectionClasses() {
        return new Class[]{PersistedClassIsFinal_Inspection.class};
    }
}
