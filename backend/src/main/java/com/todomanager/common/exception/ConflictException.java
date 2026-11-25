package com.todomanager.common.exception;

/** State conflict: stale version or delete blocked by existing children. Maps to 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
