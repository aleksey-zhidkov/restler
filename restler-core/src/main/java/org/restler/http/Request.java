package org.restler.http;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;

/**
 * Describes an object that contains HTTP(S) request data and can be executed to produce a response.
 */
public class Request<T> {

    private final HttpMethod httpMethod;
    private final URI url;
    private final Object body;
    private final Class<T> returnType;
    private final Type genericReturnType;
    private final MultiValueMap<String, String> headers;

    public Request(URI url, HttpMethod httpMethod, Object body, Class<T> returnType, Type genericReturnType) {
        this(url, httpMethod, new LinkedMultiValueMap<>(), body, returnType, genericReturnType);
    }

    public Request(URI url, HttpMethod httpMethod, MultiValueMap<String, String> headers, Object body, Class<T> returnType, Type genericReturnType) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.body = body;
        this.returnType = returnType;
        this.genericReturnType = genericReturnType;
        this.headers = headers;
    }


    public RequestEntity<?> toRequestEntity() {
        return new RequestEntity<>(body, headers, httpMethod, url);
    }

    public Class getReturnType() {
        return returnType;
    }

    public Type getGenericReturnType() {
        return genericReturnType;
    }

    public Request<T> setHeader(String name, String... values) {
        MultiValueMap<String, String> newHeaders = new LinkedMultiValueMap<>(headers);
        Arrays.stream(values).forEach(value -> newHeaders.set(name, value));
        return new Request<>(url, httpMethod, newHeaders, body, returnType, genericReturnType);
    }
}
