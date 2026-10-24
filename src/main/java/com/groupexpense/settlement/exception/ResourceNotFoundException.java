package com.groupexpense.settlement.exception;

/**
 * Thrown when a requested resource (e.g. Group, Member) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
