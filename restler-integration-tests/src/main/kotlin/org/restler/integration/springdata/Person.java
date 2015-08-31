package org.restler.integration.springdata;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity(name = "persons")
public class Person implements Serializable {

    @Id private String id;

    @Column private String name;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner1_id")
    private Set<Pet> pets;

    public Person(String id, String name, Set<Pet> pets) {
        this.id = id;
        this.name = name;
        this.pets = pets;
    }

    // for JPA
    Person() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Pet> getPets() {
        return pets;
    }
}
