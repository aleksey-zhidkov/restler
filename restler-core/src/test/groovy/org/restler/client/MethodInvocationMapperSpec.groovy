package org.restler.client

import org.restler.Greeter
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.ReflectionUtils
import spock.lang.Specification

class MethodInvocationMapperSpec extends Specification {

    def methodInvocationMapper = new MethodInvocationMapper("any")
    def controllerClass = Greeter.class

    def "test apply"() {
        given:
        def method = ReflectionUtils.findMethod(controllerClass, "getGreeting", [String, String] as Class[])

        when:
        def serviceMethodInvocation = methodInvocationMapper.apply(method, ["lang", "name"] as Object[])

        then:
        serviceMethodInvocation.pathVariables == ["language": "lang"]
        serviceMethodInvocation.requestBody == null
        def expectedRequestParams = new LinkedMultiValueMap()
        expectedRequestParams.add("name", "name")
        serviceMethodInvocation.requestParams == expectedRequestParams
    }

    def "test Java 8 parameter names retreiving"() {
        given:
        def method = ReflectionUtils.findMethod(controllerClass, "getGreetingWithoutNamesInAnnotations", [String, String] as Class[])

        when:
        def serviceMethodInvocation = methodInvocationMapper.apply(method, ["lang", "name"] as Object[])

        then:
        serviceMethodInvocation.pathVariables == ["language": "lang"]
        serviceMethodInvocation.requestBody == null
        def expectedRequestParams = new LinkedMultiValueMap()
        expectedRequestParams.add("name", "name")
        serviceMethodInvocation.requestParams == expectedRequestParams
    }

}
