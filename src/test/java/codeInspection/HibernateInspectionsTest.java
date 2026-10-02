package codeInspection;

import com.intellij.openapi.application.AccessToken;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Run with: ./gradlew test
 */
public class HibernateInspectionsTest extends LightJavaCodeInsightFixtureTestCase {

    private AccessToken ignoreUltimateModuleBug;

    @Override
    protected void setUp() throws Exception {
        // Workaround for a bug in IntelliJ IDEA 2026.2.x (not in our plugin): when a test project opens, an obfuscated
        // startup class of the `com.intellij.modules.ultimate` module fails to instantiate ("Cannot find suitable
        // constructor for class Z.Z.Z.Z.Z"), and the test framework turns that logged error into a test failure.
        // We ignore only that specific error. Remove this when a newer IntelliJ fixes it.
        ignoreUltimateModuleBug = LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
            @Override
            public @NotNull Set<Action> processError(@NotNull String category, @NotNull String message, String @NotNull [] details, @Nullable Throwable t) {
                if (isUltimateModuleBug(message, t)) return Action.NONE;
                return super.processError(category, message, details, t);
            }
        });

        super.setUp();
        myFixture.enableInspections(
                PersistedClassIsFinal_Inspection.class,
                AccessingFieldFromAFinalMethodOfPersistedClass_Inspection.class,
                EmbeddableSubclassesEmbeddable_Inspection.class);

        // Stubs of the persistence annotations, so we don't need the real libraries.
        for (String pkg : new String[]{"javax.persistence", "jakarta.persistence"}) {
            for (String annotation : new String[]{"Entity", "MappedSuperclass", "Embeddable"}) {
                myFixture.addClass("package " + pkg + "; public @interface " + annotation + " {}");
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        } finally {
            if (ignoreUltimateModuleBug != null) ignoreUltimateModuleBug.finish();
        }
    }

    private static boolean isUltimateModuleBug(@NotNull String message, @Nullable Throwable t) {
        if (!message.contains("com.intellij.modules.ultimate")) return false;
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.contains("Cannot find suitable constructor")) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Persisted class is final

    public void testFinalEntityIsReported() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public <warning descr="Persisted class cannot be final.">final</warning> class Cat {}
                """);
        myFixture.checkHighlighting();
    }

    public void testFinalJavaxEntityIsReported() {
        myFixture.configureByText("Cat.java", """
                @javax.persistence.MappedSuperclass
                public <warning descr="Persisted class cannot be final.">final</warning> class Cat {}
                """);
        myFixture.checkHighlighting();
    }

    public void testNonFinalEntityAndNonPersistedFinalClassAreNotReported() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public class Cat {}
                final class Dog {}
                """);
        myFixture.checkHighlighting();
    }

    public void testRecordEmbeddableIsNotReported() {
        myFixture.configureByText("Address.java", """
                @jakarta.persistence.Embeddable
                public record Address(int street) {}
                """);
        myFixture.checkHighlighting();
    }

    public void testRemoveFinalFromClassQuickFix() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public fi<caret>nal class Cat {}
                """);
        myFixture.launchAction(myFixture.findSingleIntention("Remove 'final' modifier"));
        myFixture.checkResult("""
                @jakarta.persistence.Entity
                public class Cat {}
                """);
    }

    // ---------------------------------------------------------------------------------------------
    // Final method of a persisted class uses direct field access

    public void testFinalMethodAccessingFieldIsReported() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public class Cat {
                    private int name;
                    public <warning descr="Method of a persisted class cannot be final if it uses direct field access.">final</warning> int getName() { return name; }
                }
                """);
        myFixture.checkHighlighting();
    }

    public void testFinalMethodAccessingSuperclassFieldIsReported() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.MappedSuperclass
                class Animal { protected long id; }

                @jakarta.persistence.Entity
                public class Cat extends Animal {
                    public <warning descr="Method of a persisted class cannot be final if it uses direct field access.">final</warning> long getId() { return this.id; }
                }
                """);
        myFixture.checkHighlighting();
    }

    public void testFinalMethodsThatDontAccessInstanceFieldsAreNotReported() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public class Cat {
                    private static final int PREFIX = 100;
                    private static int count;
                    private int name;
                    public int getName() { return name; }
                    public final int describe() { return PREFIX + getName(); }
                    public static final int getCount() { return count; }
                    public final int answer() { return 42; }
                }
                """);
        myFixture.checkHighlighting();
    }

    public void testFinalMethodOfNonPersistedClassIsNotReported() {
        myFixture.configureByText("Cat.java", """
                public class Cat {
                    private int name;
                    public final int getName() { return name; }
                }
                """);
        myFixture.checkHighlighting();
    }

    public void testRemoveFinalFromMethodQuickFix() {
        myFixture.configureByText("Cat.java", """
                @jakarta.persistence.Entity
                public class Cat {
                    private int name;
                    public fin<caret>al int getName() { return name; }
                }
                """);
        myFixture.launchAction(myFixture.findSingleIntention("Remove 'final' modifier"));
        myFixture.checkResult("""
                @jakarta.persistence.Entity
                public class Cat {
                    private int name;
                    public int getName() { return name; }
                }
                """);
    }

    // ---------------------------------------------------------------------------------------------
    // Embeddable subclasses embeddable

    public void testEmbeddableSubclassingEmbeddableIsReported() {
        myFixture.configureByText("Address.java", """
                @javax.persistence.Embeddable
                class Address { int street; }

                @javax.persistence.Embeddable
                class <warning descr="Component inheritance is not supported before Hibernate 6.6.">HomeAddress</warning> extends Address { int door; }
                """);
        myFixture.checkHighlighting();
    }

    public void testEmbeddableSubclassingEmbeddableIsNotReportedWithHibernate66() {
        // This class only exists in Hibernate 6.6 and newer.
        myFixture.addClass("package org.hibernate.metamodel.mapping; public interface EmbeddableDiscriminatorMapping {}");

        myFixture.configureByText("Address.java", """
                @jakarta.persistence.Embeddable
                class Address { int street; }

                @jakarta.persistence.Embeddable
                class HomeAddress extends Address { int door; }
                """);
        myFixture.checkHighlighting();
    }
}
