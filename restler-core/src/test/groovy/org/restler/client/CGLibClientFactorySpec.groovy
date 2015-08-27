package org.restler.client

import org.restler.test.Greeter
import spock.lang.Specification

import java.util.concurrent.Executor

class CGLibClientFactorySpec extends Specification {
    def mockServiceMethodInvocationExecutor = Mock(ServiceMethodInvocationExecutor)
    def mockInvocationMapper = Mock(InvocationMapper)
    def mockThreadExecutor = Mock(Executor)

    def clientFactory = new CGLibClientFactory(mockServiceMethodInvocationExecutor, null, mockInvocationMapper, null, mockThreadExecutor)

    def "test exception CGLibClient when class not a controller"() {
        when:
        clientFactory.produceClient(CGLibClientFactory.class)
        then:
        thrown(IllegalArgumentException)
    }

    def "test CGLib produce client"() {
        when:
        def client = clientFactory.produceClient(Greeter)
        then:
        client instanceof Greeter
    }
}
