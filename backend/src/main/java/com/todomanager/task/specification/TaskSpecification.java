package com.todomanager.task.specification;

import com.todomanager.project.entity.Project;
import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Composable task predicates. belongsToUser must always be the first predicate of a user-facing query. */
public final class TaskSpecification {

	private static final char LIKE_ESCAPE = '!';

	private TaskSpecification() {
	}

	public static Specification<Task> belongsToUser(UUID userId) {
		return (root, query, cb) -> {
			Subquery<UUID> owned = query.subquery(UUID.class);
			Root<Project> project = owned.from(Project.class);
			owned.select(project.get("id")).where(cb.equal(project.get("userId"), userId));
			return root.get("projectId").in(owned);
		};
	}

	public static Specification<Task> inProject(UUID projectId) {
		return (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
	}

	public static Specification<Task> hasStatus(TaskStatus status) {
		return (root, query, cb) -> cb.equal(root.get("status"), status);
	}

	public static Specification<Task> hasPriority(TaskPriority priority) {
		return (root, query, cb) -> cb.equal(root.get("priority"), priority);
	}

	/** Case-insensitive substring match on title or description; wildcards in the term are literal. */
	public static Specification<Task> matchesText(String term) {
		String pattern = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
				cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE));
	}

	/** Escapes the LIKE metacharacters using LIKE_ESCAPE as the escape character. */
	static String escapeLike(String term) {
		return term.replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
				.replace("%", LIKE_ESCAPE + "%")
				.replace("_", LIKE_ESCAPE + "_");
	}

}
