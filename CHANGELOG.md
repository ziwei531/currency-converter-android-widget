# Changelog

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
