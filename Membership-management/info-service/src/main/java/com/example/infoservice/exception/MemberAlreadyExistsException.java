package com.example.infoservice.exception;

public class MemberAlreadyExistsException extends RuntimeException {
    public MemberAlreadyExistsException(String name) {
        super("Member already exists: " + name);
    }
}
