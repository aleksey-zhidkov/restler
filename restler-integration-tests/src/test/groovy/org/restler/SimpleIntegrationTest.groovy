package org.restler

import org.restler.client.CGLibClientFactory
import org.restler.http.RestOperationsRequestExecutor
import org.restler.http.security.authentication.CookieAuthenticationStrategy
import org.restler.http.security.authorization.FormAuthorizationStrategy
import org.restler.integration.Controller
import org.restler.integration.IntegrationPackage
import org.restler.util.IntegrationSpec
import org.restler.integration.springdata.*
import org.restler.integration.springdata.*
import org.restler.testserver.springdata.Person
import org.restler.testserver.springdata.PersonsRepository
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions
import static org.restler.testserver.springdata.PersonsRepository.*

class SimpleIntegrationTest extends Specification implements IntegrationSpec {

    def login = "user";
    def password = "password";

    def formAuth = new FormAuthorizationStrategy("http://localhost:8080/login", login, "username", password, "password");

    def spySimpleHttpRequestExecutor = Spy(RestOperationsRequestExecutor, constructorArgs: [new RestTemplate()])

    Service serviceWithFormAuth = new ServiceBuilder("http://localhost:8080").
            authorizationStrategy(formAuth).
            cookieBasedAuthentication().
            classNameExceptionMapper().
            requestExecutor(spySimpleHttpRequestExecutor).
            build();

    Service serviceWithFormReAuth = new ServiceBuilder("http://localhost:8080").
            authorizationStrategy(formAuth).
            reauthorizeRequestsOnForbidden(true).
            cookieBasedAuthentication().
            build();

    Service serviceWithBasicAuth = new ServiceBuilder("http://localhost:8080").
            httpBasicAuthentication(login, password).
            build();

    def controller = serviceWithFormAuth.produceClient(Controller.class);
    def controllerWithBasicAuth = serviceWithBasicAuth.produceClient(Controller.class);

    def "test unsecured get"() {
        expect:
        "OK" == controller.publicGet()
    }

    def "test deferred get"() {
        def deferredResult = controller.deferredGet()
        def asyncCondition = new AsyncConditions();

        Thread.start {
            while (!deferredResult.hasResult());
            asyncCondition.evaluate {
                assert deferredResult.getResult() == "Deferred OK"
            }
        }

        expect:
        asyncCondition.await(5)
    }

    def "test callable get"() {
        when:
        def result = controller.callableGet()
        def asyncCondition = new AsyncConditions();
        then:
        0 * spySimpleHttpRequestExecutor.execute(_)
        and:
        when:
        Thread.start {
            asyncCondition.evaluate {
                assert result.call() == "Callable OK"
            }
        }
        then:
        asyncCondition.await(5)
    }

    def "test get with variable"() {
        expect:
        "Variable OK" == controller.getWithVariable("test", "Variable OK")
    }

    def "test secured get authorized with form auth"() {
        expect:
        "Secure OK" == controller.securedGet()
    }

    def "test secured get authorized with basic auth"() {
        expect:
        "Secure OK" == controllerWithBasicAuth.securedGet()
    }

    def "test reauthorization"() {
        given:
        def ctrl = serviceWithFormReAuth.produceClient(Controller)
        ctrl.logout()
        when:
        def response = ctrl.securedGet()
        then:
        response == "Secure OK"
    }

    def "test exception CookieAuthenticationRequestExecutor when cookie name is empty"() {
        when:
        new CookieAuthenticationStrategy("");
        then:
        thrown(IllegalArgumentException)
    }

    def "test PersonRepository findOne"() {
        expect:
        PersonsRepository personRepository = serviceWithFormAuth.produceClient(PersonsRepository.class)
        Person person = personRepository.findOne("0")
        person.getId() == "0"
        person.getName() == "test name"
    }

    def "test query method PersonRepository findById"() {
        expect:
        PersonsRepository personRepository = serviceWithFormAuth.produceClient(PersonsRepository.class)
        Person person = personRepository.findById("0")
        person.getId() == "0"
        person.getName() == "test name"
    }

    def "test query method PersonRepository findByName"() {
        expect:
        PersonsRepository personRepository = serviceWithFormAuth.produceClient(PersonsRepository.class)
        List<Person> persons = personRepository.findByName("test name")
        persons[0].getId() == "0"
        persons[0].getName() == "test name"
    }

}
