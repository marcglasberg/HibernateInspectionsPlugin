package codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiModifierList;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import static codeInspection.UtilHibernateInspections.*;

/**
 * @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg ; https://github.com/java)
 */
public class PersistedClassIsFinal_Inspection
        extends AbstractBaseJavaLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#PersistedClassIsFinal_Inspection");

    private final LocalQuickFix quickFix = new MyQuickFix();

    // ---

    // Appears under Settings > Inspections > Hibernate inspections.
    private static final String DISPLAY_NAME = "Persisted class is final";

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Persisted class cannot be final.";

    private static final String SHORT_NAME = "PersistedClassIsFinal";

    private static final String QUICK_FIX__REMOVE_FINAL_MODIFIER = "Remove 'final' modifier";

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
            if (!ifClassIsFinal(clazz)) return;
            if (!ifClassIsPersisted(clazz)) return;

            PsiModifierList modifierList = clazz.getModifierList();
            if (modifierList == null) return;

            holder.registerProblem(modifierList, DESCRIPTION_TEMPLATE, quickFix);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class MyQuickFix
            implements LocalQuickFix {
        @Override
        @NotNull
        public String getName() {
            return QUICK_FIX__REMOVE_FINAL_MODIFIER;
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                PsiModifierList psiModifierList = (PsiModifierList) descriptor.getPsiElement();
                PsiClass clazz = (PsiClass) psiModifierList.getParent();
                removeFinalModifier(clazz);
            } catch (IncorrectOperationException exception) {
                LOG.error(exception);
            }
        }

        @Override
        @NotNull
        public String getFamilyName() {
            return getName();
        }
    }
}
