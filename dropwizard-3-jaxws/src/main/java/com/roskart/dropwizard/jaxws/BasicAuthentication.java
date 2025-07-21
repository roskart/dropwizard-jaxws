package com.roskart.dropwizard.jaxws;

import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

public class BasicAuthentication {

    private final Authenticator<BasicCredentials, ?> authenticator;
    private final String realm;

    public BasicAuthentication(Authenticator<BasicCredentials, ?> authenticator, String realm) {
        this.authenticator = authenticator;
        this.realm = realm;
    }

    public Authenticator<BasicCredentials, ?> getAuthenticator() {
        return this.authenticator;
    }

    public String getRealm() {
        return realm;
    }

}
