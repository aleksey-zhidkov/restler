package org.restler.testserver.springdata

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

Entity class Person(
        public Id var id: String = "",
        public Column var name: String = ""
) : Serializable
