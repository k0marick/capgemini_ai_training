package com.todomanager.project.repository;

import com.todomanager.project.dto.ProjectResponse;
import com.todomanager.project.entity.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	/** Owner-scoped lookup: a foreign or missing ID yields empty. */
	Optional<Project> findByIdAndUserId(UUID id, UUID userId);

	@Query("""
			select new com.todomanager.project.dto.ProjectResponse(p.id, p.name, count(t.id), p.createdAt, p.updatedAt)
			from Project p left join Task t on t.projectId = p.id
			where p.userId = :userId
			group by p.id, p.name, p.createdAt, p.updatedAt
			order by p.createdAt asc""")
	List<ProjectResponse> findSummariesByUserId(@Param("userId") UUID userId);

}
