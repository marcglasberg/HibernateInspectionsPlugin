package codeInspection;

import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.PsiUpdateModCommandQuickFix;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static codeInspection.UtilHibernateInspections.removeFinalModifier;

/**
 * Removes the `final` modifier of the class or method that owns the highlighted `final` keyword.
 * <p>
 * Uses the ModCommand API, so the IDE can preview the fix, and apply it to many problems at once.
 *
 * @author Marcelo Glasberg (<a href="https://stackoverflow.com/users/3411681/marcg">Stack Overflow</a> ; <a href="https://github.com/marcglasberg">GitHub</a>)
 */
class RemoveFinalModifierQuickFix
        extends PsiUpdateModCommandQuickFix {

    @Override
    @NotNull
    public String getFamilyName() {
        return "Remove 'final' modifier";
    }

    @Override
    protected void applyFix(@NotNull Project project, @NotNull PsiElement element, @NotNull ModPsiUpdater updater) {
        removeFinalModifier(PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class));
    }
}
