package org.example.utils;

/**
 * Created by daqwang on 9/2/20.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
