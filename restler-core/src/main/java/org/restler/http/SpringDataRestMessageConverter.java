package org.restler.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.restler.client.RestlerException;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import javax.persistence.Id;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class SpringDataRestMessageConverter implements GenericHttpMessageConverter<Object> {

    private final String links = "_links";
    private final String self = "self";

    @Override
    public boolean canRead(Type type, Class<?> aClass, MediaType mediaType) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        Class<?> resultClass = ((ParameterizedTypeImpl) type).getRawType();
        return isList(resultClass);
    }

    @Override
    public Object read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode rootNode = objectMapper.readTree(httpInputMessage.getBody());
        Class<?> resultClass = ((ParameterizedTypeImpl) type).getRawType();
        Class<?> elementClass;
        try {
            elementClass = Class.forName(((ParameterizedTypeImpl) type).getActualTypeArguments()[0].getTypeName());
        } catch (ClassNotFoundException e) {
            throw new RestlerException(e);
        }
        if (isList(resultClass)) {
            String containerName = elementClass.getSimpleName().toLowerCase() + "s";
            JsonNode objects = rootNode.get("_embedded").get(containerName);
            if (objects instanceof ArrayNode) {
                ArrayNode arr = ((ArrayNode) objects);
                List<Object> res = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    res.add(mapObject(elementClass, objectMapper, arr.get(i)));
                }
                return res;
            }
        }
        throw new HttpMessageNotReadableException("Unexpected response format");
    }

    @Override
    public boolean canRead(Class<?> aClass, MediaType mediaType) {
        /*Entity entityAnnotation = aClass.getDeclaredAnnotation(Entity.class);
        return entityAnnotation != null;*/
        return true;
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(MediaType.parseMediaType("application/x-spring-data-verbose+json"));
        return supportedMediaTypes;
    }

    @Override
    public Object read(Class<?> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode rootNode = objectMapper.readTree(httpInputMessage.getBody());
        return mapObject(aClass, objectMapper, rootNode);
    }

    private Object mapObject(Class<?> aClass, ObjectMapper objectMapper, JsonNode rootNode) throws com.fasterxml.jackson.core.JsonProcessingException {
        Object entity = objectMapper.treeToValue(rootNode, aClass);
        setId(entity, aClass, getId(rootNode));
        return entity;
    }

    @Override
    public void write(Object o, MediaType mediaType, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

    }

    private Object getId(JsonNode objectNode) {
        JsonNode linksNode = objectNode.get(links);
        JsonNode selfLink = linksNode.get(self);

        String selfLinkString = selfLink.toString();

        int leftOffset = selfLinkString.lastIndexOf("/") + 1;
        int rightOffset = selfLinkString.indexOf('"', leftOffset);
        return selfLinkString.substring(leftOffset, rightOffset);
    }

    private void setId(Object object, Class<?> aClass, Object id) {
        Field[] fields = aClass.getDeclaredFields();
        String idFieldName = "";

        for (Field field : fields) {
            if (field.getDeclaredAnnotation(Id.class) != null) {
                idFieldName = field.getName();
            }
        }

        if (!idFieldName.isEmpty()) {
            try {
                BeanUtils.getPropertyDescriptor(aClass, idFieldName).getWriteMethod().invoke(object, id);
            } catch (IllegalAccessException e) {
                throw new RestlerException("Access denied to id write method", e);
            } catch (InvocationTargetException e) {
                throw new RestlerException("Can't invoke id write method", e);
            }
        } else {
            throw new RestlerException("Can't find id field");
        }
    }

    private boolean isList(Class<?> someClass) {
        if (someClass == null) {
            return false;
        }
        if (someClass.equals(List.class)) {
            return true;
        }
        for (Class<?> intrf : someClass.getInterfaces()) {
            if (isList(intrf)) {
                return true;
            }
        }

        return isList(someClass.getSuperclass());
    }
}
