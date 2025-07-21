package com.roskart.dropwizard.jaxws.auth;

import java.security.Principal;

/**
 * See dropwizard-example: com.example.helloworld.core.User
 */
public class User implements Principal {
    private final String userName;

    public User(String userName) {
        this.userName = userName;
    }

    @Override
    public String getName() {
        return this.userName;
    }
}
