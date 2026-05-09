# UI Guide

The JavaFX UI uses centralized CSS and small reusable helpers to keep controllers maintainable.

## Theme Files

- `app/src/main/resources/styles/common.css`: shared layout, typography, cards, buttons, forms, badges, and tables.
- `app/src/main/resources/styles/apple-light.css`: light theme color tokens.
- `app/src/main/resources/styles/apple-dark.css`: dark theme color tokens.

`ThemeManager` applies `common.css` plus the selected theme stylesheet to the active scene.

## Theme Persistence

The selected theme is stored on the `Student` profile as `appTheme`. Settings can switch between light and dark themes.

## Reusable UI Helpers

`UiComponents` currently provides:

- badge creation
- empty states
- single-tone class replacement for badges/text states
- page-level scroll pane setup
- bidirectional scroll pane setup for wide content
- responsive TableView configuration
- helper methods for grow constraints

Use helpers for repeated UI patterns instead of adding inline styles in controllers.

## Style Class Conventions

Important classes:

- Layout: `app-root`, `main-shell`, `content-container`, `sidebar`, `top-bar`
- Typography: `page-title`, `page-subtitle`, `section-title`, `body-text`, `muted-text`
- Cards: `card`, `metric-card`, `prediction-card`, `priority-card`, `settings-card`, `table-card`
- Responsive layout: `page-scroll`, `responsive-flow`, `bidirectional-scroll`, `table-scroll-container`
- Buttons: `primary-button`, `secondary-button`, `danger-button`, `success-button`
- Badges: `success-badge`, `warning-badge`, `danger-badge`, `neutral-badge`, `major-badge`, `completed-badge`, `in-progress-badge`, `planned-badge`
- Prediction: `risk-low`, `risk-medium`, `risk-high`, `risk-extreme`, `confidence-high`, `confidence-medium`, `confidence-low`

## Release UI Rules

- Do not hardcode colors in controllers.
- Do not use inline FXML `style` attributes for app styling.
- Long pages should use vertical page scrolling.
- Wide tables should keep TableView virtualization and allow horizontal scrolling through unconstrained columns.
- Prediction wording must remain cautious and explainable.
- Official GPA, projected GPA, scenario GPA, cumulative target, and major target should stay visually distinct.
