package org.restler

import org.restler.integration.springdata.Person
import org.restler.integration.springdata.PersonsRepository
import org.restler.util.IntegrationSpec
import spock.lang.Specification

class SpringDataRestIntegrationTest extends Specification implements IntegrationSpec {

    Service serviceWithBasicAuth = new ServiceBuilder("http://localhost:8080").
            httpBasicAuthentication("user", "password").
            build();

    PersonsRepository personRepository = serviceWithBasicAuth.produceClient(PersonsRepository.class)

    def "test PersonRepository findOne"() {
        expect:
        Person person = personRepository.findOne("0")
        person.getId() == "0"
        person.getName() == "test name"
    }

    def "test query method PersonRepository findById"() {
        expect:
        Person person = personRepository.findById("0")
        person.getId() == "0"
        person.getName() == "test name"
    }

    def "test query method PersonRepository findByName"() {
        expect:
        List<Person> persons = personRepository.findByName("test name")
        persons[0].getId() == "0"
        persons[0].getName() == "test name"
    }

}
