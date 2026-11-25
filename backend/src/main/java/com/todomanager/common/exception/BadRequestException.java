package com.todomanager.common.exception;

/** Invalid query or path input that is not covered by bean validation. Maps to 400 with field errors. */
public class BadRequestException extends RuntimeException {

	private final String field;

	public BadRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
