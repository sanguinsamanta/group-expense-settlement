package com.groupexpense.settlement.exception;

/**
 * Thrown when a duplicate entity or relationship is detected (e.g. member already in group).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
