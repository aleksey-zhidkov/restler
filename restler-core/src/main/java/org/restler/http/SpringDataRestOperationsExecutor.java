package org.restler.http;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.restler.client.RestlerException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SpringDataRestOperationsExecutor implements RequestExecutor {

    private RequestExecutor executor;
    private RestTemplate restTemplate;
    private Class<?> entityClass;

    public SpringDataRestOperationsExecutor(RequestExecutor executor) {
        this.executor = executor;

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new RestDataMessageConverter());
        this.restTemplate = new RestTemplate(messageConverters);

        restTemplate.getMessageConverters().add(0, new RestDataMessageConverter());
    }

    @Override
    public <T> ResponseEntity<T> execute(Request<T> request) {

        Class<?> returnType = request.getReturnType();
        Entity entityAnnotation = returnType.getDeclaredAnnotation(Entity.class);

        entityClass = request.getReturnType();

        if(entityAnnotation == null && isList(returnType)) {
            Type genericType = request.getGenericReturnType();
            Type[] actualTypeArguments = ((ParameterizedType)genericType).getActualTypeArguments();

            Class<?> genericClass;

            try {
                genericClass = Class.forName(actualTypeArguments[0].getTypeName());
            } catch (ClassNotFoundException e) {
                throw new RestlerException("Can't create class from type name " + actualTypeArguments[0].getTypeName(), e);
            }

            entityAnnotation = genericClass.getDeclaredAnnotation(Entity.class);

            if(entityAnnotation != null) {
                entityClass = genericClass;
            }
        }

        if(entityAnnotation != null) {
            RequestEntity<?> requestEntity = request.toRequestEntity();
            return restTemplate.exchange(requestEntity, new ParameterizedTypeReference<T>() {
                @Override public Type getType() {
                    return request.getGenericReturnType();
                }
            });
        } else {
            return executor.execute(request);
        }
    }

    private boolean isList(Class<?> someClass) {
        if(someClass == null) {
            return false;
        }
        if(someClass.equals(List.class)) {
            return true;
        }
        for(Class<?> intrf : someClass.getInterfaces()) {
            if(isList(intrf)) {
                return true;
            }
        }

        return isList(someClass.getSuperclass());
    }

    private class RestDataMessageConverter implements GenericHttpMessageConverter<Object> {

        private final String embedded = "_embedded";
        private final String links = "_links";
        private final String self = "self";

        @Override
        public boolean canRead(Type type, Class<?> aClass, MediaType mediaType) {
            return false;
        }

        @Override
        public Object read(Type type, Class<?> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
            return null;
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

            String containerName = entityClass.getSimpleName().toLowerCase() + "s";

            JsonNode rootNode = objectMapper.readTree(httpInputMessage.getBody());
            JsonNode embeddedNode = rootNode.path(embedded);
            JsonNode containerNode = embeddedNode.path(containerName);

            ArrayList<Object> objectsList = new ArrayList<>();

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            if(containerNode.isArray()) {
                ArrayNode arrayNode = (ArrayNode)containerNode;
                JsonNode node;
                Object object;
                for(int i = 0; i < containerNode.size(); ++i) {
                    node = arrayNode.get(i);

                    object = objectMapper.readValue(node.toString(), entityClass);
                    objectsList.add(object);

                    setId(object, entityClass, getId(node));
                }
            }

            if(isList(aClass)) {
                return objectsList;
            }

            return objectsList.get(0);
        }

        @Override
        public void write(Object o, MediaType mediaType, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {

        }

        private Object getId(JsonNode objectNode) {
            JsonNode linksNode = objectNode.get(links);
            JsonNode selfLink = linksNode.get(self);

            String selfLinkString = selfLink.toString();

            int leftOffset = selfLink.toString().lastIndexOf("/") + 1;
            int rightOffset = 2;
            return new String(selfLinkString.toCharArray(), leftOffset, selfLinkString.length() - leftOffset - rightOffset);
        }

        private void setId(Object object, Class<?> aClass, Object id) {
            Field[] fields = aClass.getDeclaredFields();
            String idFieldName = "";

            for(Field field : fields) {
                if(field.getDeclaredAnnotation(Id.class) != null) {
                    idFieldName = field.getName();
                }
            }

            if(!idFieldName.isEmpty()) {
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
    }
}
