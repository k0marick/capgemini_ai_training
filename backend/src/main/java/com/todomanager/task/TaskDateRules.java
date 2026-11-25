package com.todomanager.task;

import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.data.jpa.domain.Specification;

/** Single source for date predicates (architecture §9). "today" is evaluated in the user's timezone. */
public final class TaskDateRules {

	private static final int DUE_SOON_DAYS = 3;
	private static final int NEXT_DAYS = 6;

	private TaskDateRules() {
	}

	/** Filter values accepted by GET /api/v1/tasks?due=. */
	public enum DueFilter {
		OVERDUE,
		TODAY,
		NEXT_7_DAYS,
		ALL
	}

	public static LocalDate today(Clock clock, ZoneId userZone) {
		return LocalDate.now(clock.withZone(userZone));
	}

	public static Specification<Task> dueFilter(DueFilter filter, LocalDate today) {
		return switch (filter) {
			case OVERDUE -> overdue(today);
			case TODAY -> dueOn(today);
			case NEXT_7_DAYS -> dueBetween(today, today.plusDays(NEXT_DAYS));
			case ALL -> (root, query, cb) -> cb.conjunction();
		};
	}

	/** Notification window (architecture §10.3): open tasks due on or before today+3, overdue included. */
	public static Specification<Task> openDueBy(LocalDate horizon) {
		return (root, query, cb) -> cb.and(
				cb.lessThanOrEqualTo(root.<LocalDate>get("dueDate"), horizon),
				cb.notEqual(root.get("status"), TaskStatus.DONE));
	}

	public static LocalDate dueSoonHorizon(LocalDate today) {
		return today.plusDays(DUE_SOON_DAYS);
	}

	public static boolean isOverdue(LocalDate dueDate, LocalDate today) {
		return dueDate.isBefore(today);
	}

	private static Specification<Task> overdue(LocalDate today) {
		return (root, query, cb) -> cb.and(
				cb.lessThan(root.<LocalDate>get("dueDate"), today),
				cb.notEqual(root.get("status"), TaskStatus.DONE));
	}

	private static Specification<Task> dueOn(LocalDate today) {
		return (root, query, cb) -> cb.equal(root.get("dueDate"), today);
	}

	private static Specification<Task> dueBetween(LocalDate from, LocalDate to) {
		return (root, query, cb) -> cb.between(root.<LocalDate>get("dueDate"), from, to);
	}

}
