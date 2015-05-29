package org.restler.client;

import org.springframework.util.MultiValueMap;

import java.util.Map;

public class ServiceMethodInvocation<T> {

    private ServiceMethodDescription<T> method;
    private Object requestBody;
    private Map<String, ?> pathVariables;
    private MultiValueMap<String, String> requestParams;

    public ServiceMethodInvocation(ServiceMethodDescription<T> method, Object requestBody, Map<String, ?> pathVariables, MultiValueMap<String, String> requestParams) {
        this.method = method;
        this.requestBody = requestBody;
        this.pathVariables = pathVariables;
        this.requestParams = requestParams;
    }

    public ServiceMethodDescription<T> getMethod() {
        return method;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public Map<String, ?> getPathVariables() {
        return pathVariables;
    }

    public MultiValueMap<String, String> getRequestParams() {
        return requestParams;
    }
}
