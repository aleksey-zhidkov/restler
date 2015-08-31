package org.restler.integration.springdata

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "pets") class Pet(
        public @Id var id: String = "",
        public @Column var name: String = ""
) : Serializable
