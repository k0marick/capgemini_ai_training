package com.todomanager.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Project() {
	}

	private Project(UUID id, UUID userId, String name, Instant now) {
		this.id = id;
		this.userId = userId;
		this.name = name;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public static Project create(UUID id, UUID userId, String name, Instant now) {
		return new Project(id, userId, name, now);
	}

	public void rename(String name, Instant now) {
		this.name = name;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
