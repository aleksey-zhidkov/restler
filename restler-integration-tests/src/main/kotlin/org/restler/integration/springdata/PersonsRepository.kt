package org.restler.testserver.springdata

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource

// java.lang.String - it is workaround for https://youtrack.jetbrains.com/issue/KT-5821
RepositoryRestResource(exported = true)
interface PersonsRepository : CrudRepository<Person, java.lang.String> {

    fun findByName(@Param("name") name: String): List<Person>
    fun findById(@Param("id") id: java.lang.String): Person
}

