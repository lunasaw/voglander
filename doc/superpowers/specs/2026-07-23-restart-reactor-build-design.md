# Reactor-Aware Restart Design

## Problem

`restart.sh` currently falls back to `mvn spring-boot:run -pl voglander-web` when no executable Web JAR exists. That command compiles the Web module but may resolve sibling Voglander modules from stale artifacts in `~/.m2`. A newly compiled Web class can therefore reference a class that is absent from the runtime dependency JAR, causing an application startup failure such as `NoClassDefFoundError`.

## Goals

- Build `voglander-web` and all of its reactor dependencies on every restart.
- Never stop a currently healthy application when the replacement build fails.
- Start only the executable JAR produced by the successful reactor build.
- Preserve the existing process ownership checks, Lab ffmpeg cleanup, profiles, detached launch, logs, and port stability checks.

## Design

The script will use four ordered stages:

1. Build the reactor with `mvn -pl voglander-web -am package -DskipTests`.
2. Resolve the newly produced executable Web JAR and fail if it is missing.
3. Stop the current Voglander listener and clean up Lab-owned ffmpeg processes.
4. Launch the new JAR and wait for port 8181 to remain stable.

The build runs synchronously before any process termination. A non-zero Maven exit code or a missing executable JAR ends the script with a non-zero exit code while leaving the current application untouched.

After a successful build, the script will always use `java -jar`. The `spring-boot:run` fallback will be removed because it permits sibling modules to come from independently installed Maven artifacts.

## JAR Selection

The script will search only the top level of `voglander-web/target` for `voglander-web-*.jar`, excluding source and Javadoc artifacts. If no candidate exists after a successful package phase, startup will not proceed.

## Error Handling

- Maven failure: report the build failure and exit before stopping the old process.
- Missing JAR: report the missing build output and exit before stopping the old process.
- Port owned by another application: preserve the existing refusal to terminate it.
- New process exits or never stabilizes: preserve the existing recent-log output and non-zero exit.

## Verification

- Run `bash -n restart.sh`.
- Run the reactor package command and confirm the executable Web JAR exists.
- Run `./restart.sh` and confirm port 8181 remains listening for the configured stability interval.
- Confirm startup logs contain `Started ApplicationWeb` and no `ClassNotFoundException` for sibling-module classes.

## Non-Goals

- Running tests during every restart.
- Installing snapshot artifacts into `~/.m2`.
- Changing application profiles, ports, logging configuration, or shutdown behavior.
