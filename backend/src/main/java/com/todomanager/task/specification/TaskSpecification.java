package com.todomanager.task.specification;

import com.todomanager.project.entity.Project;
import com.todomanager.task.entity.Task;
import com.todomanager.task.entity.TaskPriority;
import com.todomanager.task.entity.TaskStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** Composable task predicates. belongsToUser must always be the first predicate of a user-facing query. */
public final class TaskSpecification {

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

}
