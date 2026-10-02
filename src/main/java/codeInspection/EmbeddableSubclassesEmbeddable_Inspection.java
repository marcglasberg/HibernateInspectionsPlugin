package codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.NotNull;

import static codeInspection.UtilHibernateInspections.*;

/**
 * The inspection's name, group, short name (used in @SuppressWarnings) and default level are in plugin.xml.
 *
 * @author Marcelo Glasberg (<a href="https://stackoverflow.com/users/3411681/marcg">Stack Overflow</a> ; <a href="https://github.com/marcglasberg">GitHub</a>)
 */
public class EmbeddableSubclassesEmbeddable_Inspection
        extends AbstractBaseJavaLocalInspectionTool {

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Component inheritance is not supported before Hibernate 6.6.";

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
            if (!ifClassIsEmbeddable(clazz)) return;
            if (!ifAnySuperclassIsEmbeddable(clazz)) return;
            if (ifHibernateSupportsEmbeddableInheritance(clazz)) return;

            PsiIdentifier nameIdentifier = clazz.getNameIdentifier();
            if (nameIdentifier == null) return;

            holder.registerProblem(nameIdentifier, DESCRIPTION_TEMPLATE);
        }
    }
}
