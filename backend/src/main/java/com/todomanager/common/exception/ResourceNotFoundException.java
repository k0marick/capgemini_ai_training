package com.todomanager.common.exception;

/** Missing or not owned by the caller. Both cases map to 404 so existence is not leaked. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException() {
		super("Resource not found");
	}

}
