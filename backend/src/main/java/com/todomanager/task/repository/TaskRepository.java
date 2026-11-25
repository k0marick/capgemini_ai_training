package com.todomanager.task.repository;

import com.todomanager.task.entity.Task;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

	/** Owner-scoped lookup through the project: a foreign or missing ID yields empty. */
	@Query("""
			select t from Task t
			where t.id = :id
			and t.projectId in (select p.id from Project p where p.userId = :userId)""")
	Optional<Task> findOwnedById(@Param("id") UUID id, @Param("userId") UUID userId);

	long countByProjectId(UUID projectId);

	boolean existsByProjectId(UUID projectId);

}
