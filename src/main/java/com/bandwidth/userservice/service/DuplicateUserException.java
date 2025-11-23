package com.bandwidth.userservice.service;

/**
 * Custom exception thrown when a user attempts to register with an email or username
 * that already exists in the database.
 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String field, String value) {
        super(String.format("User registration failed: The %s '%s' is already in use.", field, value));
    }
}