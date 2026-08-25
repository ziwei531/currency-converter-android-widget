# Changelog

## [2.2.0] - 2026-08-25

### Added

- Added a per-pair graph button that opens Google's search for the selected currency pair.
- Added a chart-style graph icon button to each widget conversion row.

### Changed

- Routed row actions independently so row taps still open editing while graph-button taps open Google.
- Updated the widget screenshot and usage documentation for graph actions.

## [2.1.0] - 2026-08-24

### Added

- Independent base-and-target conversion pairs, with up to fifteen pairs per widget.
- Add, edit, remove, and drag-and-drop reorder controls for conversion pairs.
- Grouped rate requests by base currency when multiple pairs share a base.
- Scrollable widget conversion list so all fifteen configured pairs remain accessible.
- Direct row-to-edit navigation for individual conversion pairs.

### Changed

- The configuration flow now chooses a base currency before the target currency for each new conversion.
- Widget rows display each pair's base and target instead of assuming one shared base.
- Cache keys now include both base and target currencies.
- Existing one-base/multiple-target widget settings migrate to pairs sharing the old base.
- Currency display uses familiar symbols such as `RM`, `$`, `£`, and `€`.
- Increased conversion-row typography for the scrollable widget presentation.
- Replaced the redundant widget configuration gear with whole-widget tapping.
- Updated the README and widget screenshot for the one-widget multi-pair experience.

## [2.0.0] - 2026-08-24

### Added

- Dynamic target-currency list that renders only selected targets.
- Searchable currency picker with name and ISO-code matching.
- Duplicate-target prevention and base-currency exclusion in the picker.
- Long-press drag-and-drop target reordering.
- Target count indicator and a clear five-currency limit.
- Dedicated rounded back navigation control and refreshed configuration-screen hierarchy.

### Changed

- Redesigned the configuration screen around compact currency rows and clearer section labels.
- Added remove controls for selected target currencies.
- Added an accent-colored `Save changes` action and refresh helper text.
- Preserved the existing five-slot preference format for saved-configuration compatibility.
- Preserved the existing widget refresh mechanism after saving.

### Fixed

- Removed permanent empty target selectors from the configuration screen.
- Prevented malformed external drag payloads from crashing the configuration activity.
- Kept long currency names readable with multiline row content and separately scannable ISO codes.

## [1.0.3] - 2026-08-23

### Changed

- Aligned the automatic refresh cadence with the provider's daily rate dataset.
- Documented that the widget checks every 30 minutes while Android may defer background work.

## [1.0.2] - 2026-08-23

### Added

- Added visible refresh-cadence guidance to the widget documentation and display.
- Documented the ExchangeRate-API Open Access endpoint and its daily dataset behavior.

### Changed

- Removed internal agent-build instructions from the public installation documentation.

## [1.0.1] - 2026-08-22

### Added

- Added system light and dark theme behavior for the widget and configuration experience.
- Improved public installation and widget-usage documentation, including a widget screenshot.
- Documented the native Termux build provenance.

### Fixed

- Configuration and widget surfaces now follow the device's system theme instead of forcing one palette.

## [1.0.0] - 2026-08-22

### Added

- First stable release of the Currency Converter Widget.
- Per-widget base currency and up to five target currencies.
- Search by currency name or ISO code.
- Independent configurations and cached rates for multiple widget instances.
- Single, compact, standard, and expanded widget layouts with resize support.
- Visible refreshing state and cached offline values.
- Automatic inexact 30-minute refresh scheduling with reboot restoration.
- Light and dark widget palettes.
- No-key ExchangeRate-API Open Access integration.
- Android API 26+ support targeting and compiling against API 33.

### Changed

- Prepared the project and release build for stable 1.0.0 distribution.

### Security

- The application requests only `INTERNET` and `RECEIVE_BOOT_COMPLETED` permissions.
