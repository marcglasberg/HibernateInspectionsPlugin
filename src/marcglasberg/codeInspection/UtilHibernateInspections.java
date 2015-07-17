package marcglasberg.codeInspection;

import org.jetbrains.annotations.*;
import java.util.*;
import com.intellij.psi.*;

final class UtilHibernateInspections
    {

    static final String HIBERNATE_CHECKS__GROUP_DISPLAY_NAME = "Hibernate inspections";

    private static final List<String> PERSISTENCE_ANNOTATIONS = Arrays.asList("javax.persistence.Entity",
                                                                              "javax.persistence.MappedSuperclass",
                                                                              "javax.persistence.Embeddable");

    private UtilHibernateInspections()
        {
        }

    public static boolean ifClassIsPersisted(@NotNull PsiClass clazz)
        {
        PsiModifierList classModifierList = clazz.getModifierList();
        if (classModifierList == null) return false;

        for (PsiAnnotation annotation : classModifierList.getAnnotations())
            {
            String qualifiedName = annotation.getQualifiedName();
            if (PERSISTENCE_ANNOTATIONS.contains(qualifiedName)) return true;
            }

        return false;
        }

    public static boolean ifClassOfTheMehodIsPersisted(@NotNull PsiMethod method)
        {
        PsiClass clazz = method.getContainingClass();
        if (clazz == null) return false;

        return UtilHibernateInspections.ifClassIsPersisted(clazz);
        }

    public static boolean ifClassIsFinal(@NotNull PsiClass clazz)
        {
        PsiModifierList methodModifierList = clazz.getModifierList();

        if (methodModifierList == null) return false;
        else return methodModifierList.hasModifierProperty(PsiModifier.FINAL);
        }

    public static boolean ifMethodIsFinal(@NotNull PsiMethod method)
        {
        PsiModifierList methodModifierList = method.getModifierList();
        return methodModifierList.hasModifierProperty(PsiModifier.FINAL);
        }

    public static void removeFinalModifier(@Nullable PsiModifierListOwner clazz)
        {
        if (clazz != null)
            {
            PsiModifierList modifierList = clazz.getModifierList();
            if (modifierList != null) modifierList.setModifierProperty(PsiModifier.FINAL, false);
            }
        }

    /** Given a class, returns a list of all its superclasses. */
    @NotNull
    public static List<PsiClass> getClassAndItsSuperclasses(@NotNull PsiClass clazz)
        {
        List<PsiClass> superclasses = new ArrayList<>();

        PsiClass superclass = clazz;

        while (superclass != null)
            {
            superclasses.add(superclass);
            superclass = superclass.getSuperClass();
            }

        return superclasses;
        }
    }