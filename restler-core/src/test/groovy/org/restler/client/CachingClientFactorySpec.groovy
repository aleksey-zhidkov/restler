package org.restler.client

import org.springframework.stereotype.Controller

import spock.lang.Specification

class CachingClientFactorySpec extends Specification {
    def mockClientFactory = Mock(ClientFactory)
    def cachingClientFactory = new CachingClientFactory(mockClientFactory)

    def "test caching clients"() {
        when:
        cachingClientFactory.produceClient(Controller)
        cachingClientFactory.produceClient(Controller)
        then:
        1 * mockClientFactory.produceClient(_)
    }
}
