# Sherko Engine Java Style

This repository uses Spotless with a pinned Eclipse JDT formatter profile as the authoritative Java formatter.

## Commands

Apply formatting:

```powershell
.\gradlew.bat spotlessApply
```

Verify formatting without changing files:

```powershell
.\gradlew.bat spotlessCheck
```

The root `check` task depends on `spotlessCheck`, so ordinary CI fails when committed Java does not match the repository style.

## Style contract

The committed formatter profile is `config/formatter/sherko-eclipse-java.xml`.

Current style:

- 4-space indentation;
- 180-column page width;
- method and constructor declarations keep parameters on one line whenever they fit;
- method and constructor invocations keep arguments on one line whenever they fit;
- hand-wrapped declarations/calls are rejoined when the formatter can fit them inside the page width;
- opening braces remain at the end of the declaration/control line;
- method/constructor bodies use a visual blank line immediately inside the opening and closing brace;
- ordinary nested control-flow blocks do not receive method-body padding;
- Java text-block indentation is preserved so formatter application cannot silently alter runtime string contents;
- trailing whitespace is removed and files end with a newline.

Representative shape:

```java
private static WindowSizeListener createSizeListener(SandboxFramebufferSize framebufferSize, EngineLogger logger) {

    return new WindowSizeListener() {
        @Override
        public void onLogicalWindowSizeChanged(int width, int height) {

            log(logger, EngineLogger.Level.INFO, "Logical window size changed to %dx%d".formatted(width, height));

        }

        @Override
        public void onFramebufferSizeChanged(int width, int height) {

            framebufferSize.update(width, height);
            log(logger, EngineLogger.Level.INFO, "Framebuffer size changed to %dx%d".formatted(width, height));

        }
    };

}
```

## Scope

Spotless targets all authored `.java` files in the repository, including production, tests, renderer visual-demo sources, public-API tests, feasibility spikes, and committed Java fixtures. Generated `build/` output and Gradle working directories are excluded.

Checkstyle remains enabled for semantic/static style rules such as wildcard imports, empty catches, and ignored pure-return calls. Spotless does not replace Checkstyle.

## Formatting-only maintenance rule

A repository-wide formatting change must not be mixed with behavior changes. When changing Java logic, apply the formatter after the logic change and review semantic changes separately from mechanical formatting.

The formatter is intentionally enforced before Phase 6 so future asset-pipeline work starts from one deterministic Java layout.
