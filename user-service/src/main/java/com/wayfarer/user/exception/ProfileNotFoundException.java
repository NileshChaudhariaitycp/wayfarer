package com.wayfarer.user.exception;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(Long id) {
        super("No profile found for user id: " + id);
    }
}
