/**
 * This class is copied from drowpizard-example
 */
package com.roskart.dropwizard.jaxws.example.core;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

/**
 * See dropwizard-example: com.example.helloworld.core.Person
 */
@Entity
@Table(name = "people")
@NamedQueries({
        @NamedQuery(
                name = "com.roskart.dropwizard.jaxws.example.core.Person.findAll",
                query = "SELECT p FROM Person p"
        ),
        @NamedQuery(
                name = "com.roskart.dropwizard.jaxws.example.core.Person.findById",
                query = "SELECT p FROM Person p WHERE p.id = :id"
        )
})
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotEmpty
    @Column(name = "fullname", nullable = false)
    private String fullName;

    @NotEmpty
    @Column(name = "jobtitle", nullable = false)
    private String jobTitle;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

}
