package com.roskart.dropwizard.jaxws.auth;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import java.util.Optional;

/**
 * BasicAuthenticator is based on ExampleAuthenticator in dropwizard-example.
 */
public class BasicAuthenticator implements Authenticator<BasicCredentials, User> {
    @Override
    public Optional<User> authenticate(BasicCredentials credentials) {
        if ("secret".equals(credentials.getPassword())) {
            return Optional.of(new User(credentials.getUsername()));
        }
        // Note that Authenticator should only throw an AuthenticationException
        // if it is unable to check the credentials.
        return Optional.empty();
    }
}
