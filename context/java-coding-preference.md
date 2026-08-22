# Java Coding Preferences

Java-specific adaptation of the shared coding preferences. Apply these rules to Java source unless a framework, generated file, or existing public API requires otherwise.

## Formatting and Whitespace

- Use TABS for indentation. One indentation level is one tab.
- Do not use trailing whitespace.
- Use spaces around binary, assignment, comparison, logical, and conditional operators.
- Put a space after control-flow keywords and around the contents of parentheses:

```java
if ( condition ) {
	return calculateTotal( items );
}

for ( final Item item : items ) {
	processItem( item );
}
```

- Keep square-bracket contents spaced consistently for indexing and array literals:

```java
final Item first = items[ 0 ];
final int[] limits = { 10, 20, 30 };
```

- Keep opening braces on the same line as the declaration or control statement.
- Put closing braces on their own line at the matching indentation level.
- Use one variable declaration per line.

For multiline method calls and array initializers, use conventional Java comma placement:

```java
final String[] currencies = {
	"USD",
	"MYR",
};
```

## Alignment

Visually align related declarations and assignments where practical. Use spaces, not tabs, for alignment padding. Do not force alignment across unrelated statements or very long lines.

```java
final String baseCurrency   = "USD";
final String targetCurrency = "MYR";
final int refreshMinutes    = 30;
```

Align related key/value fields when it improves scanning, but prefer readable line length over excessive padding.

## Variables and Constants

- Prefer `final` for locals, parameters, and fields that are not reassigned. This is the closest Java equivalent to JavaScript `const`.
- Use a mutable local only when reassignment is required.
- Avoid Java `var` when the explicit type makes the code clearer or when Android language-level compatibility matters.
- Declare one variable per line.
- Use `static final UPPER_SNAKE_CASE` for true class constants.
- Do not add defensive null handling to values whose contract guarantees non-nullness. Validate external, user, or framework inputs at the boundary instead.

```java
private static final int DEFAULT_REFRESH_MINUTES = 30;

final String rate = response.getRate();
final String cachedRate = preferences.getString( "rate", null );
```

## Naming

- Classes, interfaces, records, and enumerations: `PascalCase`.
- Methods, parameters, and local variables: descriptive `camelCase`.
- Boolean values: use names such as `isReady`, `hasRate`, `canRefresh`, or `shouldRetry`.
- Constants: `UPPER_SNAKE_CASE`.
- Packages: lowercase, reverse-domain style.
- Avoid generic method names such as `update()`, `process()`, or `handle()` when a more precise name is possible. Prefer `refreshExchangeRate()`, `parseRateResponse()`, or `showOfflineRate()`.
- Use private helper methods instead of cryptic inline blocks. Do not prefix Java methods with `_`; visibility expresses the boundary.

## Methods and Classes

- Keep methods focused on one responsibility.
- Prefer small, descriptive methods over deeply nested control flow.
- Use constructors and methods that make required dependencies explicit.
- Prefer composition over inheritance unless inheritance is required by an Android or framework contract.
- Use `@Override` whenever overriding a superclass or interface method.
- Do not expose mutable internal collections directly; return an immutable or defensive copy when the API boundary requires it.
- Avoid utility classes with unexplained static state.

## Imports and Packages

- Use explicit imports. Do not use wildcard imports.
- Keep package declarations first, followed by imports, then the class declaration.
- Group imports consistently: project imports after platform and third-party imports, with no unused imports.
- Keep package names lowercase and stable; changing them is an API and application-identity change.

## Control Flow

- Always use braces, including for single-statement bodies.
- Use a guard clause and omit `else` after an `if` that returns or throws.

```java
if ( !isValidRate( rate ) ) {
	return showUnavailableState();
}

return showRate( rate );
```

- Do not put `break` after a `return` or `throw` in a `switch`.
- Put `default` last in a `switch` unless there is a documented reason not to.
- Prefer enhanced `for` loops when the index is not needed. Use an indexed loop when the index is part of the logic or when performance requires it.
- Use streams for clear transformations, not to hide control flow or exception handling.
- Do not swallow interruptions. Restore the interrupt flag when catching `InterruptedException`.

```java
try {
	worker.join();
} catch ( final InterruptedException error ) {
	Thread.currentThread().interrupt();
	showError( "The refresh was interrupted" );
}
```

## Collections and Arrays

- Prefer collection interfaces in declarations: `List`, `Map`, and `Set`.
- Use `List.of`, `Map.of`, or immutable copies when the project’s Android language level supports them; otherwise use an explicitly unmodifiable collection.
- Do not use mutable static collections unless their lifecycle and synchronization are documented.
- Use array and collection literals/factories instead of obscure constructor patterns where available.
- Do not expose mutable arrays or collections from public APIs without a clear ownership contract.

## Nulls and Errors

- Treat network responses, intents, preferences, files, and other external data as untrusted.
- Validate at the boundary and fail with an actionable state.
- Do not catch `Exception` merely to continue silently.
- Catch only errors the code can recover from; let programming errors and impossible states surface during development.
- Report failures to the user-facing surface where appropriate, and log enough context to diagnose the failure without logging secrets.
- Never log API keys, bearer tokens, personal data, or complete untrusted responses unnecessarily.

```java
try {
	final Rate rate = rateClient.fetchUsdMyr();
	showRate( rate );
} catch ( final IOException error ) {
	logger.warn( "USD/MYR refresh failed", error );
	showCachedOrOfflineState();
}
```

## Background Work and Android

- Never perform network or disk work on the Android main thread.
- Prefer lifecycle-aware or platform-sanctioned work such as WorkManager for deferred refreshes.
- Use an executor or structured task boundary for immediate work; do not create unmanaged thread forests.
- Make cancellation, retry, timeout, and offline behavior explicit.
- Do not assume a background process or service will remain alive indefinitely.
- Update widgets through the Android widget APIs and keep the widget state small and cacheable.

## Comments and Documentation

- Comments explain **why**, not what the code mechanically does.
- Prefer single-line `//` comments for short rationale. Use Javadoc for public APIs, lifecycle contracts, and non-obvious invariants.
- Start comments with a capital letter and avoid stale comments that merely repeat the code.
- Document Android lifecycle assumptions, thread ownership, cache semantics, and network/provider behavior when they are not obvious.

```java
// Keep the cached value visible because Android may kill the background process
// before the next scheduled widget refresh.
showCachedRate();
```

## Generated and Framework-Mandated Code

Generated files, Android resource identifiers, manifest declarations, and framework callback signatures may not follow every preference. Do not manually reformat generated output. Keep handwritten adapters around generated or framework code clean and consistent.

## Pre-Commit Checklist

- [ ] Tabs used for indentation; no trailing whitespace
- [ ] Spaces around operators and inside parentheses/brackets follow project style
- [ ] Braces used for every control-flow body
- [ ] Related declarations visually aligned without excessive padding
- [ ] One variable declaration per line
- [ ] `final` used for values that are not reassigned
- [ ] Constants use `static final UPPER_SNAKE_CASE`
- [ ] Names are descriptive and follow Java casing conventions
- [ ] No wildcard or unused imports
- [ ] No `else` after `return` or `throw`
- [ ] No unreachable `break` after `return` or `throw`
- [ ] Enhanced loops or clear collection operations used where appropriate
- [ ] Network and disk work kept off the main thread
- [ ] Cancellation, timeout, retry, and offline behavior are explicit
- [ ] Errors are surfaced without leaking secrets
- [ ] Comments explain WHY, not WHAT
- [ ] Framework and generated files were not hand-edited unnecessarily
- [ ] Build, tests, and static checks pass
