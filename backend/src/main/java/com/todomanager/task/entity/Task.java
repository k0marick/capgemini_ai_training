package com.todomanager.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

	@Id
	private UUID id;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false, length = 20)
	private TaskPriority priority;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Optimistic lock. Boxed so a new entity (null) is persisted rather than merged. */
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	protected Task() {
	}

	private Task(UUID id, UUID projectId, Instant now) {
		this.id = id;
		this.projectId = projectId;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Task create(UUID id, UUID projectId, String title, String description, TaskStatus status,
			TaskPriority priority, LocalDate dueDate, Instant now) {
		Task task = new Task(id, projectId, now);
		task.applyFields(title, description, status, priority, dueDate);
		return task;
	}

	public void update(String title, String description, TaskStatus status, TaskPriority priority,
			LocalDate dueDate, Instant now) {
		applyFields(title, description, status, priority, dueDate);
		this.updatedAt = now;
	}

	public void changeStatus(TaskStatus status, Instant now) {
		this.status = status;
		this.updatedAt = now;
	}

	private void applyFields(String title, String description, TaskStatus status, TaskPriority priority,
			LocalDate dueDate) {
		this.title = title;
		this.description = description;
		this.status = status;
		this.priority = priority;
		this.dueDate = dueDate;
	}

	public UUID getId() {
		return id;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Long getVersion() {
		return version;
	}

}
