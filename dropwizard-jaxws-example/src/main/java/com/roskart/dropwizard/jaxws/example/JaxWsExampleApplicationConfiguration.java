package com.roskart.dropwizard.jaxws.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class JaxWsExampleApplicationConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();

    public DataSourceFactory getDatabaseConfiguration() {
        return database;
    }
}
