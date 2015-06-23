package org.restler.testserver.springdata

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.Repository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource

// java.lang.String - it is workaround for https://youtrack.jetbrains.com/issue/KT-5821
RepositoryRestResource(exported = true)
interface PersonsRepository : CrudRepository<Person, java.lang.String>

