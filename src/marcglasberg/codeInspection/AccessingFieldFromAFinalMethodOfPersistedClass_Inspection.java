package marcglasberg.codeInspection;

import org.jetbrains.annotations.*;
import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.*;
import com.intellij.openapi.project.*;
import com.intellij.psi.*;
import com.intellij.util.*;
import static marcglasberg.codeInspection.UtilHibernateInspections.*;

/**
 @author Marcelo Glasberg (http://stackoverflow.com/users/3411681/marcg) */
public class AccessingFieldFromAFinalMethodOfPersistedClass_Inspection
      extends BaseJavaLocalInspectionTool
    {
    private static final Logger LOG = Logger.getInstance("#marcglasberg.codeInspection.AccessingFieldFromAFinalMethodOfPersistedClass_Inspection");

    private final LocalQuickFix quickFix = new MyQuickFix();

    // ---

    // Appears under Settings > Inspections > Hibernate inspections.
    private static final String DISPLAY_NAME = "Final method of a persisted class uses direct field access";

    // Error tooltip that appears in the editor.
    private static final String DESCRIPTION_TEMPLATE = "Method of a persisted class cannot be final if it uses direct field access.";

    private static final String SHORT_NAME = "AccessingFieldFromAFinalMethodOfPersistedClass";

    private static final String QUICK_FIX__REMOVE_FINAL_MODIFIER = "Remove 'final' modifier";

    @Override
    @NotNull
    public String getDisplayName()
        {
        return DISPLAY_NAME;
        }

    @Override
    @NotNull
    public String getShortName()
        {
        return SHORT_NAME;
        }

    @Override
    @NotNull
    public String getGroupDisplayName()
        {
        return HIBERNATE_CHECKS__GROUP_DISPLAY_NAME;
        }

    @Override
    public boolean isEnabledByDefault()
        {
        return true;
        }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly)
        {
        return new MyJavaElementVisitor(holder);
        }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class MyJavaElementVisitor
          extends JavaElementVisitor
        {
        private final ProblemsHolder holder;

        public MyJavaElementVisitor(ProblemsHolder holder)
            {
            super();
            this.holder = holder;
            }

        /** This is the core of the inspection. */
        @Override
        public void visitMethod(PsiMethod method)
            {
            super.visitMethod(method);

            // Note: Keep the order, faster checks first.
            if (!ifMethodIsFinal(method)) return;
            if (!ifClassOfTheMehodIsPersisted(method)) return;
            if (!ifMethodUsesDirectFieldAccess(method)) return;

            PsiModifierList modifierList = method.getModifierList();
            holder.registerProblem(modifierList, DESCRIPTION_TEMPLATE, quickFix);
            }

        private boolean ifMethodUsesDirectFieldAccess(@NotNull PsiMethod method)
            {
            PsiClass clazz = method.getContainingClass();
            if (clazz == null) return false;

            return ifPsiElementIsOrContainsAReferenceToAFieldOfItsClass(method, clazz);
            }

        /** Recursive. */
        private boolean ifPsiElementIsOrContainsAReferenceToAFieldOfItsClass(@NotNull PsiElement psiElement, @NotNull PsiClass clazz)
            {
            if (ifElementIsAReferenceToAFieldOfItsClass(psiElement, clazz)) return true;
            else if (ifElementContainsReferenceToField(psiElement, clazz)) return true;
            else return false;
            }

        /** Recursive. */
        private boolean ifElementContainsReferenceToField(@NotNull PsiElement psiElement, @NotNull PsiClass clazz)
            {
            PsiElement[] innerPsiElements = psiElement.getChildren();
            for (PsiElement innerPsiElement : innerPsiElements)
                {
                if (ifPsiElementIsOrContainsAReferenceToAFieldOfItsClass(innerPsiElement, clazz)) return true;
                }
            return false;
            }

        private boolean ifElementIsAReferenceToAFieldOfItsClass(@NotNull PsiElement psiElement, @NotNull PsiClass clazz)
            {
            if (psiElement instanceof PsiReferenceExpression)
                {
                // Gets the reference element.
                PsiElement referencedElement = ((PsiReferenceExpression)psiElement).resolve();

                // If the element is a field,
                if (referencedElement instanceof PsiField)
                    {
                    // Then check if it is a field of the class or its superclasses (it may be a field of some other unrelated class).
                    PsiField field = (PsiField)referencedElement;
                    PsiClass classOfField = field.getContainingClass();

                    if (getClassAndItsSuperclasses(clazz).contains(classOfField)) return true;
                    }
                }
            return false;
            }
        }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class MyQuickFix
          implements LocalQuickFix
        {
        @Override
        @NotNull
        public String getName()
            {
            return QUICK_FIX__REMOVE_FINAL_MODIFIER;
            }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor)
            {
            try
                {
                PsiModifierList psiModifierList = (PsiModifierList)descriptor.getPsiElement();
                PsiMethod method = (PsiMethod)psiModifierList.getParent();
                removeFinalModifier(method);
                }

            catch (IncorrectOperationException exception)
                {
                LOG.error(exception);
                }
            }

        @Override
        @NotNull
        public String getFamilyName()
            {
            return getName();
            }
        }
    }
