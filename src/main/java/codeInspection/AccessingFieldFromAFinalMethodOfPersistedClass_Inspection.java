package codeInspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static codeInspection.UtilHibernateInspections.*;

/**
 * The inspection's name, group, short name (used in @SuppressWarnings) and default level are in plugin.xml.
 *
 * @author Marcelo Glasberg (<a href="https://stackoverflow.com/users/3411681/marcg">Stack Overflow</a> ; <a href="https://github.com/marcglasberg">GitHub</a>)
 */
public class AccessingFieldFromAFinalMethodOfPersistedClass_Inspection
        extends AbstractBaseJavaLocalInspectionTool {

    private final LocalQuickFix quickFix = new RemoveFinalModifierQuickFix();

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Method of a persisted class cannot be final if it uses direct field access.";

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
        public void visitMethod(@NotNull PsiMethod method) {
            super.visitMethod(method);

            // Note: Keep the order, faster checks first.
            if (!ifMethodIsFinal(method)) return;
            if (ifMethodIsStatic(method)) return; // Static methods are not proxied.
            if (!ifClassOfTheMethodIsPersisted(method)) return;
            if (!ifMethodUsesDirectFieldAccess(method)) return;

            PsiElement finalKeyword = findFinalKeyword(method);
            if (finalKeyword == null) return;

            holder.registerProblem(finalKeyword, DESCRIPTION_TEMPLATE, quickFix);
        }

        private boolean ifMethodUsesDirectFieldAccess(@NotNull PsiMethod method) {
            PsiClass clazz = method.getContainingClass();
            PsiCodeBlock body = method.getBody();
            if (clazz == null || body == null) return false;

            List<PsiClass> classAndItsSuperclasses = getClassAndItsSuperclasses(clazz);

            for (PsiReferenceExpression reference : PsiTreeUtil.findChildrenOfType(body, PsiReferenceExpression.class)) {
                if (ifIsAReferenceToAnInstanceFieldOf(reference, classAndItsSuperclasses)) return true;
            }
            return false;
        }

        private boolean ifIsAReferenceToAnInstanceFieldOf(@NotNull PsiReferenceExpression reference, @NotNull List<PsiClass> classes) {
            // If the referenced element is a field,
            if (reference.resolve() instanceof PsiField field) {
                // Static fields are not part of the persisted state, and are not affected by proxies.
                if (field.hasModifierProperty(PsiModifier.STATIC)) return false;

                // Then check if it is a field of the class or its superclasses (it may be a field of some other unrelated class).
                return classes.contains(field.getContainingClass());
            }
            return false;
        }
    }
}
