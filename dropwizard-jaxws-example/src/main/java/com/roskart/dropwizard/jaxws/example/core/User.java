package com.roskart.dropwizard.jaxws.example.core;

/**
 * See dropwizard-example: com.example.helloworld.core.User
 */
public class User {
    private final String userName;

    public User(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
