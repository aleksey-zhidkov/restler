package org.restler.testserver.springdata

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

Entity class Person(
        Id val id: String = "",
        Column val name: String = ""
) : Serializable
