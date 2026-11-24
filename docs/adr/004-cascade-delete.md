# ADR-004: Block deletion of non-empty projects

- Status: Accepted

## Context
Requirements say deleting a project must cascade to its tasks, and also "block deletion until tasks are removed". One approach must be chosen and documented in README.

## Decision
Deleting a project is allowed only when it has no tasks. `DELETE /api/v1/projects/{id}` returns 409 (ProblemDetail, with task count) if tasks exist; the user deletes tasks first. The FK is `ON DELETE RESTRICT` as a backstop. The UI disables project delete when `taskCount > 0` and explains why. Only the owner can delete (owner-scoped lookup → 404 otherwise).

## Alternatives
- **Cascade delete with confirmation**: one-step deletion, but a single click can remove many tasks irreversibly.
- **Soft delete / trash**: adds retention, restore, and query filters; out of MVP scope.

## Consequences
- No accidental data loss of tasks via project deletion.
- Users need extra steps to remove a populated project.
- Covered by `ProjectDeleteBlockedTest` (409 while tasks exist; 204 after removal).
- README must document this behavior.
