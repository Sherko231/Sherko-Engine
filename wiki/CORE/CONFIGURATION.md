# Configuration

The current startup-configuration API lives in `com.samo.engine.core.api`.

## Main types

- `EngineConfigLoader` — resolves supported configuration layers/sources.
- `EngineConfigSchema` — validates raw entries and produces typed values.
- `ConfigKey<T>` — typed schema key.
- `ConfigEntry` — raw string value plus source.
- `ConfigSource` — source description used in diagnostics.
- `ConfigError` — one validation error.
- `ConfigValidationException` — aggregates validation failures.

## Current schema keys

`EngineConfigSchema` currently defines:

```text
fullscreen.width   default 1920
fullscreen.height  default 1080
simulation.tickRate default 60
```

Current bounds:

- fullscreen width: 320..16384 inclusive;
- fullscreen height: 200..16384 inclusive;
- simulation tick rate: must equal the engine's locked 60 Hz fixed rate.

Unknown keys are validation errors.

## Minimal direct validation example

```java
import com.samo.engine.core.api.ConfigEntry;
import com.samo.engine.core.api.ConfigKey;
import com.samo.engine.core.api.ConfigSource;
import com.samo.engine.core.api.EngineConfigSchema;
import java.util.Map;

EngineConfigSchema schema = new EngineConfigSchema();

Map<String, ConfigEntry> raw = Map.of(
        "fullscreen.width",
        new ConfigEntry("2560", new ConfigSource("example")),
        "fullscreen.height",
        new ConfigEntry("1440", new ConfigSource("example")));

Map<ConfigKey<?>, Object> config = schema.validate(raw);

int width = (Integer) config.get(EngineConfigSchema.FULLSCREEN_WIDTH);
int height = (Integer) config.get(EngineConfigSchema.FULLSCREEN_HEIGHT);
```

If validation fails, `ConfigValidationException` contains source-aware errors rather than silently accepting invalid/unknown values.

## Guidance

- Treat configuration as startup input, not mutable gameplay state.
- Preserve source information so validation errors can explain where a value came from.
- Do not add ad-hoc keys outside the schema and assume they are accepted.
- Do not use configuration to bypass locked architectural constants such as the current 60 Hz simulation rate.

For exact layer precedence and file/environment/CLI behavior, use `EngineConfigLoader` source and the accepted configuration decision in `docs/DECISIONS.md`; this wiki must follow those contracts rather than redefining them.
