package marcglasberg.codeInspection;

import com.intellij.codeInspection.*;

/**
 @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg) */
public class AccessingFieldFromAFinalMethodOfPersistedClass_Provider
      implements InspectionToolProvider
    {
    @Override
    public Class[] getInspectionClasses()
        {
        return new Class[]{AccessingFieldFromAFinalMethodOfPersistedClass_Inspection.class};
        }
    }
