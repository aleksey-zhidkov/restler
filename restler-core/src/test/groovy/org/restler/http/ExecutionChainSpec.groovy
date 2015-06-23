package org.restler.http

import spock.lang.Specification

class ExecutionChainSpec extends Specification{
    def mockExecutor = Mock(RequestExecutor)
    def mockExecutionAdvice = Mock(RequestExecutionAdvice)

    List<RequestExecutionAdvice> advices = new ArrayList<RequestExecutionAdvice>()
    List<RequestExecutionAdvice> nullAdvices = new ArrayList<RequestExecutionAdvice>()

    def executionChain;

    def setup() {
        advices.add(mockExecutionAdvice)
        nullAdvices.add(null)

        executionChain = new RequestExecutionChain(mockExecutor, advices)
    }

    def "test null advices"() {
        when:
        new RequestExecutionChain(mockExecutor, nullAdvices)
        then:
        thrown(NullPointerException)
    }

    def "test chain execute"() {
        when:
        def request = new Request(null, null, null, null, null)
        executionChain.execute(request)
        then:
        1 * mockExecutionAdvice.advice(_, _)
    }
}
