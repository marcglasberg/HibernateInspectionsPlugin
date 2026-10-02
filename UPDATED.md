# UPDATED.md — Maintenance notes for the Hibernate Inspections plugin

This file explains what was done for version **1.7** (October 2026), why it was done,
and **how to do the next update** when JetBrains sends another "plugin verification" email.

Plugin page: https://plugins.jetbrains.com/plugin/7866-hibernate-inspections
Plugin id: `marcglasberg.HibernateInspectionsPlugin` (never change it; it identifies the plugin on the Marketplace).

---

## 1. What JetBrains was complaining about

The email said:

> IntelliJ IDEA 2026.3 eap (263.5701.42): Compatible. 1 plugin configuration defect

"Compatible" means the plugin still loads and runs. The "plugin configuration defect" is a problem
in the plugin's **descriptor** (`plugin.xml`) or packaging, not in the Java code. JetBrains runs
the [IntelliJ Plugin Verifier](https://github.com/JetBrains/intellij-plugin-verifier) against every
new IDE build (including EAPs) and emails the result.

I downloaded the published 1.6 jar and ran the Plugin Verifier on it locally (see section 5 for how). The exact defect was:

```
Plugin marcglasberg.HibernateInspectionsPlugin:1.6 against IU-253.33813.55: Compatible. 1 plugin configuration defect
Plugin structure warnings (1):
    Invalid plugin descriptor 'description'. All the links in the plugin description must be HTTPS:
    http://stackoverflow.com/questions/6608222/does-a-final-method-prevent-hibernate-from-creating-a-proxy-for-such-an-entity.
```

So the email's "defect" was just one `http://` link in `<description>`. The repo had already changed the links to
`https://` (commits from 2023), but **that was never published**: the Marketplace still had the 2022 jar.

Other things found inside the published 1.6 jar:

* Its `plugin.xml` says `since-build="192.0"`. The Marketplace page shows "231.0+" because JetBrains overrode
  the range on their side, since the plugin was not compatible with older builds.
* Every class was in the jar **twice**, once in `codeInspection/` (correct) and once in `java/codeInspection/`
  (junk from the old manual packaging). The Gradle build doesn't have this problem.

After the changes below, `./gradlew verifyPlugin` reports for version 1.7:

```
Plugin marcglasberg.HibernateInspectionsPlugin:1.7 against IU-262.10968.63: Compatible   (IntelliJ IDEA 2026.2.3)
    Plugin can probably be enabled or disabled without IDE restart
Plugin marcglasberg.HibernateInspectionsPlugin:1.7 against IU-263.6259.32: Compatible    (IntelliJ IDEA 2026.3 EAP)
    Plugin can probably be enabled or disabled without IDE restart
```

That's zero defects, zero structure warnings, and zero deprecated, experimental, internal or override-only API usages
(the build is configured to fail on any of those). The plugin is also a "dynamic plugin" now: it can be
installed, updated and disabled without restarting the IDE.

---

## 2. What was outdated, and what was changed

### Build system: old DevKit module → Gradle (the biggest change)

Before, the project was an IntelliJ **DevKit plugin module** (`HibernateInspectionsPlugin.iml` with
`type="PLUGIN_MODULE"`). It was built with *Build > Prepare Plugin Module for Deployment*, and the
`.jar` was uploaded by hand. JetBrains no longer recommends this. It can't run the Plugin Verifier,
it can't run tests easily, and it compiles against whatever IDE you happen to have installed.

Now the project uses the setup JetBrains currently recommends (the same as their official
[IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)):

| Thing | Version | Where it's set |
|---|---|---|
| Gradle (via wrapper, no install needed) | 9.8.0 | `gradle/wrapper/gradle-wrapper.properties` |
| [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html) | 2.19.0 | `settings.gradle.kts` |
| Java (toolchain, downloaded automatically) | 25 | `build.gradle.kts` (`languageVersion`) |
| IntelliJ Platform compiled against (= oldest supported IDE) | IntelliJ IDEA 2026.2.3 | `build.gradle.kts` (`intellijIdea(...)`) |
| `since-build` | 262 (= 2026.2) | `build.gradle.kts` (`sinceBuild`) |
| `until-build` | none (open-ended) | `build.gradle.kts` (`untilBuild = provider { null }`) |
| Plugin version | 1.7 | `gradle.properties` (`version`) |

Files layout changed to the standard Gradle layout:

```
build.gradle.kts, settings.gradle.kts, gradle.properties, gradlew, gradlew.bat, gradle/wrapper/
src/main/java/codeInspection/...                 (was src/codeInspection/)
src/main/resources/META-INF/plugin.xml           (was resources/META-INF/plugin.xml)
src/main/resources/META-INF/pluginIcon.svg
src/main/resources/inspectionDescriptions/...    (was src/inspectionDescriptions/)
src/test/java/codeInspection/HibernateInspectionsTest.java   (new: automated tests)
```

Removed: `HibernateInspectionsPlugin.iml`, `.idea/modules.xml`, `.idea/compiler.xml`,
`.idea/misc.xml` (IntelliJ regenerates what it needs from Gradle), and the committed `out/` folder.

**Why the minimum IDE is now 2026.2:** the decision was to support only the latest IntelliJ IDEA.
The build compiles against the minimum supported IDE, which guarantees we don't call an API that doesn't exist there.
Users on older IDEs are not broken: the Marketplace automatically keeps offering them version 1.6, which
declares `since-build="231.0"`.

**There is no "IntelliJ IDEA Community" anymore.** JetBrains' last separate Community release was 2025.3
(December 2025). From 2026.1 on there's a single "IntelliJ IDEA" product (product code `IU`, called
`IntelliJPlatformType.IntellijIdea` in Gradle), and that's what this plugin targets. The old
`IntellijIdeaCommunity` type / `intellijIdeaCommunity(...)` helper only exists for versions up to 2025.3.

### plugin.xml

| Before | After | Why |
|---|---|---|
| `<inspectionToolProvider implementation="..._Provider"/>` (3 of them), plus 3 `*_Provider.java` classes | `<localInspection language="JAVA" shortName=... displayName=... groupName=... enabledByDefault=... level=... implementationClass=.../>` | `InspectionToolProvider` is the legacy way to register inspections. JetBrains docs say to use `localInspection`. It also lets the IDE show the inspections in Settings without loading/instantiating the classes. |
| `getDisplayName()`, `getShortName()`, `getGroupDisplayName()`, `isEnabledByDefault()` overridden in each inspection | Removed; the values live in `plugin.xml` | Docs: overriding these is "not recommended in general". |
| `<version>1.8</version>` and `<idea-version since-build="231.0"/>` hard-coded | Removed from the file; Gradle injects them at build time (`patchPluginXml` task) | One source of truth. Note the file said 1.8 but the Marketplace only ever had 1.6, so 1.7 is the next version. |
| `<depends>com.intellij.modules.lang</depends>` | Removed | Redundant. `com.intellij.java` already implies it. `com.intellij.modules.platform` + `com.intellij.java` is what the [compatibility guide](https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html) prescribes for a Java-only plugin. |
| Empty `<actions>` block | Removed | Useless. |
| Description used Markdown (`*` bullets, backticks, ``` fences) inside HTML | Pure HTML | The Marketplace renders the description as HTML; Markdown showed up as raw characters. |
| `<change-notes>` didn't describe changes | Lists 1.7 changes | Shown to users in the IDE's update dialog. |

**The `shortName`s were NOT changed** (`PersistedClassIsFinal`,
`AccessingFieldFromAFinalMethodOfPersistedClass`, `EmbeddableSubclassesEmbeddable`). They are what
users write in `@SuppressWarnings("...")` and must match the file names in `inspectionDescriptions/`.

### Java code

* **Quick-fix moved to the ModCommand API.** The old quick-fix implemented `LocalQuickFix.applyFix(Project, ProblemDescriptor)`
  and edited the PSI directly. The modern way is `PsiUpdateModCommandQuickFix`
  (see `RemoveFinalModifierQuickFix.java`). This gives the "preview" of the fix in the Alt+Enter popup,
  works with "fix all", and is thread-safe. The two identical inner `MyQuickFix` classes became one shared class.
* **Problems are now highlighted on the `final` keyword** (or on the class name for the embeddable inspection),
  instead of on the whole modifier list (which included all the annotations).
* Removed the `registerProblem(element, text, null)` call that passed a `null` quick-fix.
* Uses modern Java (`instanceof` pattern matching, `List.of`, text blocks in tests), since the target is now Java 25.

### Hibernate changed too (functional fixes)

* **Jakarta Persistence.** Hibernate 6+ uses `jakarta.persistence.*`, not `javax.persistence.*`.
  The plugin only knew `javax.persistence`, so **it silently did nothing on any modern project.**
  It now recognizes both.
* **Hibernate 6.6 supports `@Embeddable` inheritance** ([announcement](https://in.relation.to/2024/07/12/embeddable-inheritance/)).
  So "Embeddable subclasses embeddable" was producing false warnings. It now checks if the class
  `org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping` (which first appeared in Hibernate 6.6)
  is in the module's classpath, and if so, it reports nothing.
* **Records** can be `@Embeddable` in Hibernate 6.2+. They are implicitly final, and were being reported as
  "Persisted class is final" (with a quick-fix that couldn't work). Now only an explicit `final` keyword is reported.
* "Final method uses direct field access" no longer reports **static** methods, or uses of **static** fields
  (they are not part of the persisted state and are not affected by proxies).

---

## 3. How to build, test and verify (do this before every release)

### One-time setup

* No Gradle install is needed (`gradlew` downloads it).
* **Java:** point `JAVA_HOME` to the JetBrains Runtime bundled with IntelliJ (it's Java 25, exactly what the
  build needs to compile, so Gradle uses it and downloads nothing):
  ```
  # Git Bash (on this machine the IntelliJ 2026.2.2 install lives in a folder still named "IntelliJ IDEA 2025.2.2")
  export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.2.2/jbr"
  # PowerShell
  $env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\jbr"
  ```
  Check the folder name after upgrading IntelliJ. If `JAVA_HOME` points to an older JDK (like the system's Oracle
  JDK 17.0.1 from 2021), Gradle still runs, but downloads a Java 25 for compiling (foojay toolchain resolver),
  and that old JDK also fails the HTTPS checks described below.
  When running from inside IntelliJ, it uses its own Gradle JVM setting (*Settings > Build Tools > Gradle*).
* **Disk space:** each IDE that Gradle downloads for compiling or verifying takes ~1.5 GB (zip) + ~4 GB (unpacked),
  in `~/.gradle/caches`. Plan for ~15 GB. If C: is short on space, set `GRADLE_USER_HOME` to a folder on another drive.
* **Avast antivirus gotcha (this machine):** Avast intercepts HTTPS, and Java doesn't trust Avast's certificate,
  so Gradle fails with `Plugin [id: '...'] was not found` / `PKIX path building failed`. Fix: make Java use the
  Windows certificate store:
  ```
  # Git Bash
  export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  # PowerShell
  $env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
  ```
  (Or set it permanently as a Windows user environment variable.)

### Open the project in IntelliJ

*File > Open* and pick the project folder (or `build.gradle.kts`). IntelliJ imports it as a Gradle project.
If it had the old DevKit module open, close the project and delete the `.idea` folder first.

### Commands

Run from the project folder (Git Bash: `./gradlew`, PowerShell/cmd: `.\gradlew`):

| Command | What it does |
|---|---|
| `./gradlew test` | Runs the automated tests in `src/test` (inspections highlight the right code, quick-fixes work). |
| `./gradlew runIde` | Starts a sandbox IntelliJ with the plugin installed, to try it by hand. |
| `./gradlew buildPlugin` | Creates the distributable zip in `build/distributions/HibernateInspectionsPlugin-<version>.zip`. |
| `./gradlew verifyPlugin` | Runs the **same Plugin Verifier JetBrains runs** against the IDEs listed in `build.gradle.kts` (`pluginVerification { ides { ... } }`). Reports go to `build/reports/pluginVerifier/`. The build fails on deprecated/internal/experimental API usage and on plugin.xml structure warnings. |
| `./gradlew publishPlugin` | Builds, verifies the descriptor, and uploads to the Marketplace (see below). |

---

## 4. How to publish to JetBrains Marketplace

### Option A: from Gradle (recommended)

1. Create a token (once): https://plugins.jetbrains.com/author/me/tokens. Keep it secret; never commit it.
2. Bump `version` in `gradle.properties` (the Marketplace rejects a version that already exists),
   and update `<change-notes>` in `plugin.xml`.
3. Run `./gradlew test verifyPlugin` and make sure both pass.
4. Publish:
   ```
   # Git Bash
   export PUBLISH_TOKEN="perm:...."
   ./gradlew publishPlugin
   # PowerShell
   $env:PUBLISH_TOKEN = "perm:...."
   .\gradlew publishPlugin
   ```
5. JetBrains moderates every upload; it shows on the plugin page as "pending approval" and usually goes live
   within 1–2 business days. You get an email.

### Option B: manual upload

1. `./gradlew buildPlugin`
2. Go to https://plugins.jetbrains.com/plugin/7866-hibernate-inspections/edit, *Versions*, *Upload Update*,
   and pick `build/distributions/HibernateInspectionsPlugin-<version>.zip`.

Note: since this plugin has no library dependencies, the build produces a zip containing a single jar.
Both the zip and the jar are accepted by the Marketplace.

### Plugin signing (optional, recommended by JetBrains)

The Marketplace signs every plugin with its own certificate anyway, so this isn't required.
To also sign it yourself, follow https://plugins.jetbrains.com/docs/intellij/plugin-signing.html,
then add `signing { certificateChain = ...; privateKey = ...; password = ... }` to `intellijPlatform { }` in
`build.gradle.kts`, reading the values from environment variables (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`,
`PRIVATE_KEY_PASSWORD`). `publishPlugin` then signs automatically.

### Commit and tag

```
git add -A
git commit -m "Version 1.7"
git tag v1.7
git push && git push --tags
```

---

## 5. Next time JetBrains emails about deprecated APIs / defects: checklist

1. **Read the exact problem.** Open the link in the email (plugin page, *Versions*, the version, *Verification results*),
   and expand the IDE row. It names the deprecated class/method or the plugin.xml problem.
2. **Reproduce locally:** in `build.gradle.kts`, `pluginVerification { ides { ... } }`, set the EAP build number
   from the email (e.g. `create(IntelliJPlatformType.IntellijIdea, "263.5701.42")`), then run `./gradlew verifyPlugin`.
   Latest builds: https://data.services.jetbrains.com/products/releases?code=IIU&type=eap,release&latest=true
3. **Update the tooling first**, since new IDEs often need a newer Gradle plugin:
   * IntelliJ Platform Gradle Plugin: newest version at
     https://plugins.gradle.org/plugin/org.jetbrains.intellij.platform (change it in `settings.gradle.kts`).
   * Gradle: `./gradlew wrapper --gradle-version <latest>` (latest at https://gradle.org/releases/).
   * Compare with JetBrains' template, which they keep current:
     https://github.com/JetBrains/intellij-platform-plugin-template (look at `build.gradle.kts`,
     `settings.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`).
4. **Find the replacement for a deprecated API:**
   * The deprecated method's Javadoc usually says what to use (Ctrl+click it in IntelliJ, since sources are attached by Gradle).
   * List of incompatible API changes per release: https://plugins.jetbrains.com/docs/intellij/api-changes-list.html
   * Notable changes per release: https://plugins.jetbrains.com/docs/intellij/api-notable.html
   * Search usages in the IntelliJ source: https://github.com/JetBrains/intellij-community
   * Ask on the forum: https://platform.jetbrains.com/
5. **If the replacement doesn't exist in the oldest supported IDE**, raise the minimum:
   change `intellijIdea("...")` and `sinceBuild` in `build.gradle.kts` together, and the Java
   `languageVersion` if the new platform requires a newer Java. Table of branch numbers ↔ versions ↔ Java:
   https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
   (e.g. 2025.3 = 253 = Java 21; 2026.2 = 262 = Java 25.)
   Since we only support the latest IDE, normally you just move both to the newest stable release.
6. Run `./gradlew test verifyPlugin` until both pass, bump the version, publish (section 4).

### Writing the Marketplace description

The text on the [plugin page](https://plugins.jetbrains.com/plugin/7866-hibernate-inspections) comes from
`<description>` in `plugin.xml`, **not** from `README.md`. Rules
([JetBrains guidelines](https://plugins.jetbrains.com/docs/marketplace/best-practices-for-listing.html)):

* **HTML only**, inside `<![CDATA[ ... ]]>`. Markdown is NOT rendered: in 1.6, `## Do I need this plugin?` showed up
  on the page with the `#` characters. Use `<h2>`, `<p>`, `<ul><li>`, `<b>`, `<code>`, `<pre>`, `<a href="...">`.
* **All links must be `https://`.** One `http://` link was the "plugin configuration defect" in JetBrains' email.
* **The first 40 characters are the summary on the plugin's preview card.** Start with what the plugin does
  (now: "Finds Hibernate bugs that fail silently" = 39 characters), not with a question or a title.
* English first, direct wording, no marketing words ("simple", "powerful", "lightweight"...), bullet points for the main features.
* Images go in the Media section of the plugin page, not in the description.
* The description can also be edited on the Marketplace website. If someone does that, the website text **replaces**
  the `plugin.xml` text, so either keep editing it there, or reset it to use the one from the plugin.
* `./gradlew verifyPluginStructure` checks the descriptor quickly (no IDE download), including the `https` rule.

`README.md` (shown on GitHub) has the same content in Markdown, with more detail and examples.
Keep both in sync when you change an inspection.

### Known test workaround (IntelliJ 2026.2.x bug)

With IntelliJ IDEA 2026.2.x, *every* light test failed with
`Cannot create extension (class=Z.Z.Z.Z.Z) [Plugin: com.intellij.modules.ultimate]` /
`Cannot find suitable constructor for class Z.Z.Z.Z.Z`. This is a JetBrains bug (an obfuscated class of the IDE's
"Ultimate" module clashes with another class in headless tests); our code never even ran
([another plugin hitting the same bug](https://github.com/nise-nabe/typespec-intellij-plugin/pull/107)).
`HibernateInspectionsTest.setUp()` installs a `LoggedErrorProcessor` that ignores **only** that error.
When moving to a newer IntelliJ, try deleting the workaround; if the tests still pass, leave it deleted.

### Verifying an already-published version (without building it)

This is how the 1.6 defect was found. Download the published file from
`https://plugins.jetbrains.com/plugin/download?updateId=<id>` (the id is in the version's URL on the plugin page, e.g.
`.../versions/stable/155527`), and run the verifier CLI that Gradle already downloaded, against any IDE folder:

```
java -jar ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.intellij.plugins/verifier-cli/<ver>/<hash>/verifier-cli-<ver>-all.jar \
     check-plugin old.jar "<path to an unpacked IDE, e.g. C:\Program Files\JetBrains\IntelliJ IDEA 2026.2>"
```

### Useful links

* Plugin compatibility guide (module dependencies): https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html
* plugin.xml reference: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
* Inspections: https://plugins.jetbrains.com/docs/intellij/code-inspections.html
* Verifier on the Marketplace: https://plugins.jetbrains.com/docs/marketplace/plugin-verifier.html
* Gradle plugin tasks: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html
