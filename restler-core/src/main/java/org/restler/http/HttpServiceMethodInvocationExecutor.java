package org.restler.http;

import org.restler.client.ServiceMethod;
import org.restler.client.ServiceMethodInvocation;
import org.restler.client.ServiceMethodInvocationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class HttpServiceMethodInvocationExecutor implements ServiceMethodInvocationExecutor {

    private final RequestExecutionChain executors;

    public HttpServiceMethodInvocationExecutor(RequestExecutionChain executors) {
        this.executors = executors;
    }

    @Override
    public <T> T execute(ServiceMethodInvocation<T> invocation) {

        Request<T> request = toRequest(invocation);
        ResponseEntity<T> responseEntity = executors.execute(request);
        return responseEntity.getBody();
    }

    private <T> Request<T> toRequest(ServiceMethodInvocation<T> invocation) {
        ServiceMethod<T> method = invocation.getMethod();

        URI target = UriComponentsBuilder.fromUri(invocation.getBaseUrl()).
                path(method.getUriTemplate()).
                queryParams(invocation.getRequestParams()).
                buildAndExpand(invocation.getPathVariables()).toUri();

        return new Request<T>(target, method.getHttpMethod(), invocation.getRequestBody(), method.getReturnType(), method.getGenericReturnType());
    }
}
