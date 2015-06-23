package org.restler.http.security

import org.restler.http.security.authentication.AuthenticationStrategy
import org.restler.http.security.authorization.AuthorizationStrategy
import spock.lang.Specification

class SecuritySessionSpec extends Specification {
    def mockAuthorizationStrategy = Mock(AuthorizationStrategy)
    def mockAuthenticationStrategy = Mock(AuthenticationStrategy)

    def securitySession = new SecuritySession(mockAuthorizationStrategy, mockAuthenticationStrategy, false)
    def securitySessionWithAutoAuthorize = new SecuritySession(mockAuthorizationStrategy, mockAuthenticationStrategy, true)

    def "test auto authorize"() {
        when:
        securitySessionWithAutoAuthorize.getAuthenticationToken()
        then:
        1 * mockAuthorizationStrategy.authorize()
    }

    def "test authorize"() {
        when:
        securitySession.authorize()
        then:
        1 * mockAuthorizationStrategy.authorize()
    }

    def "test getAuthenticationStrategy"() {
        when:
        def authenticationStrategy = securitySession.getAuthenticationStrategy()
        then:
        authenticationStrategy == mockAuthenticationStrategy
    }
}
