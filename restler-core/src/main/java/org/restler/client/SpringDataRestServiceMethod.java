package org.restler.client;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Type;

public class SpringDataRestServiceMethod<T> extends ServiceMethod<T> {

    private final Type idType;
    private final Type entityType;

    public SpringDataRestServiceMethod(String uriTemplate, Class<T> returnType, Type genericReturnType, HttpMethod httpMethod, HttpStatus expectedHttpResponseStatus, Type idType, Type entityType) {
        super(uriTemplate, returnType, genericReturnType, httpMethod, expectedHttpResponseStatus);
        this.idType = idType;
        this.entityType = entityType;
    }

    public Type getIdType() {
        return idType;
    }

    public Type getEntityType() {
        return entityType;
    }

    public Class<?> getIdClass() {
        // TODO: will it work generic classes?
        try {
            return Class.forName(idType.getTypeName());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Should never happen because otherwise user will not able to create argument objects for call" + e);
        }
    }
}
