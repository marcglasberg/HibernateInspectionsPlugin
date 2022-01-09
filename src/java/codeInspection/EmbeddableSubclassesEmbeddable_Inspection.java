package java.codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;

import static java.codeInspection.UtilHibernateInspections.*;

/**
 * @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg ; https://github.com/java)
 */
public class EmbeddableSubclassesEmbeddable_Inspection
        extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#java.codeInspection.EmbeddableSubclassesEmbeddable_Inspection");

    // ---

    // Appears under Settings > Inspections > Hibernate inspections.
    private static final String DISPLAY_NAME = "Embeddable subclasses embeddable";

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Component inheritance is not supported.";

    private static final String SHORT_NAME = "EmbeddableSubclassesEmbeddable";

    @NotNull
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @NotNull
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    @NotNull
    @Override
    public String getGroupDisplayName() {
        return HIBERNATE_CHECKS__GROUP_DISPLAY_NAME;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new MyJavaElementVisitor(holder);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class MyJavaElementVisitor
            extends JavaElementVisitor {
        private final ProblemsHolder holder;

        public MyJavaElementVisitor(ProblemsHolder holder) {
            super();
            this.holder = holder;
        }

        /**
         * This is the core of the inspection.
         */
        @Override
        public void visitClass(PsiClass clazz) {
            super.visitClass(clazz);

            // Note: Keep the order, faster checks first.
            if (!ifClassIsEmbeddable(clazz)) return;
            if (!ifAnySuperclassIsEmbeddable(clazz)) return;

            PsiModifierList modifierList = clazz.getModifierList();
            if (modifierList == null) return;

            holder.registerProblem(modifierList, DESCRIPTION_TEMPLATE, null);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
