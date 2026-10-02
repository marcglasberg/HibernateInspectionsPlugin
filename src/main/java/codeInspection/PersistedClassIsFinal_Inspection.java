package codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import static codeInspection.UtilHibernateInspections.*;

/**
 * The inspection's name, group, short name (used in @SuppressWarnings) and default level are in plugin.xml.
 *
 * @author Marcelo Glasberg (<a href="https://stackoverflow.com/users/3411681/marcg">Stack Overflow</a> ; <a href="https://github.com/marcglasberg">GitHub</a>)
 */
public class PersistedClassIsFinal_Inspection
        extends AbstractBaseJavaLocalInspectionTool {

    private final LocalQuickFix quickFix = new RemoveFinalModifierQuickFix();

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Persisted class cannot be final.";

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
        public void visitClass(@NotNull PsiClass clazz) {
            super.visitClass(clazz);

            // Note: Keep the order, faster checks first.
            if (!ifClassIsFinal(clazz)) return;
            if (!ifClassIsPersisted(clazz)) return;

            // Implicitly final classes (like records, which can be embeddables) are not reported.
            PsiElement finalKeyword = findFinalKeyword(clazz);
            if (finalKeyword == null) return;

            holder.registerProblem(finalKeyword, DESCRIPTION_TEMPLATE, quickFix);
        }
    }
}
