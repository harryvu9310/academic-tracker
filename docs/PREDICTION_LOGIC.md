# Prediction Logic

## Official GPA

Official GPA includes only courses that are officially complete:

- `status == COMPLETED`
- credits are greater than 0
- at least one assessment exists
- total assessment weight is 100% within tolerance
- every assessment has a valid finite score

Courses marked `COMPLETED` but missing final assessment data are excluded from official GPA and completed credits.

## Projected And Scenario GPA

Projected GPA may include active courses based on currently entered assessment data and deterministic expected scores.

Scenario GPA estimates three paths:

- Conservative
- Expected
- Ambitious

These are scenario estimates, not guarantees.

## Major GPA

Major GPA only includes courses marked as major courses. If major classification is missing, prediction can still run but should warn that major GPA confidence is limited.

## Performance History

The app maps only officially completed courses into `StudentPerformanceHistory`. In-progress, planned, dropped, or incomplete-completed courses are not treated as historical completed performance.

Category profiles use:

- credit-weighted historical score
- recent performance
- variance/consistency
- confidence level

Small category samples are shrunk toward the global profile to avoid overconfidence.

## Feasibility

The engine separates:

- Mathematical feasibility: possible within remaining credits and grade bands.
- Realistic feasibility: plausible based on performance history and risk.

The app should avoid overconfident certainty wording. Preferred wording is “mathematically feasible”, “scenario estimate”, and “based on current entered data”.

## Grade Scale

The app uses discrete GPA bands:

- A+ = 4.0
- A = 3.5
- B+ = 3.0
- B = 2.5
- C = 2.0
- D+ = 1.5
- D = 1.0
- F = 0.0
