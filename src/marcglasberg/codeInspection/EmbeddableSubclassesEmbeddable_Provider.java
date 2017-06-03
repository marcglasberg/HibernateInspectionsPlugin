package marcglasberg.codeInspection;

import com.intellij.codeInspection.*;

/**
 @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg) */
public class EmbeddableSubclassesEmbeddable_Provider
      implements InspectionToolProvider
    {
    @Override
    public Class[] getInspectionClasses()
        {
        return new Class[]{EmbeddableSubclassesEmbeddable_Inspection.class};
        }
    }
