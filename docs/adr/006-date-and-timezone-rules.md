# ADR-006: Date category rules and timezone handling

- Status: Proposed (A2 and A3 need confirmation — architecture §2)

## Context
Overdue, today, next 7 days, and due-soon rules must match between task filters and notifications. Requirements say "overdue" and "due soon" but do not define inclusive boundaries precisely. Users may be in different timezones.

## Decision
- Due dates are calendar dates (`DATE`), no time component.
- `TaskDateRules` is the single source of truth. It takes `Clock` and the user's zone: `today = LocalDate.now(clock.withZone(zone))`.
- Overdue: `due < today AND status ≠ DONE`.
- Due soon: `today ≤ due ≤ today+3 AND status ≠ DONE`.
- Next 7 days: `today ≤ due ≤ today+6`.
- Today: `due = today`. All: no restriction.
- Timezone: `users.timezone` (IANA, default `UTC`), updated via `PATCH /me/preferences` when the browser zone differs.

## Alternatives
- **Separate predicates in filter and notification code**: risks divergence (rejected).
- **Timestamps with time-of-day deadlines**: not required; more complexity.
- **Server-only UTC**: wrong for users far from UTC.
- **Next 7 days = today+7**: yields 8 calendar dates; rejected as inconsistent with the name.

## Consequences
- Deterministic tests with a fixed `Clock`.
- One place to change rules.
- Invalid IANA zones rejected with 400.
