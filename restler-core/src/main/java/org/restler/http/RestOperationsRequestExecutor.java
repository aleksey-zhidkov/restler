package org.restler.http;

import org.restler.util.Util;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RestOperationsRequestExecutor implements RequestExecutor {

    private final RestTemplate restTemplate;

    public RestOperationsRequestExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        restTemplate.getMessageConverters().add(new BodySavingMessageConverter());
    }

    public <T> ResponseEntity<T> execute(Request<T> request) {
        RequestEntity<?> requestEntity = request.toRequestEntity();
        return restTemplate.exchange(requestEntity, new ParameterizedTypeReference<T>() {
            @Override
            public Type getType() {
                return request.getGenericReturnType();
            }
        });
    }

    private class BodySavingMessageConverter implements GenericHttpMessageConverter<Object> {
        @Override
        public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return true;
        }

        @Override
        public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
            return throwException(inputMessage);
        }

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return true;
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return true;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return new ArrayList<>();
        }

        @Override
        public Object read(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
            return throwException(inputMessage);
        }

        private Object throwException(HttpInputMessage inputMessage) throws IOException {
            String responseBody = Util.toString(inputMessage.getBody());
            inputMessage.getBody().close();
            throw new HttpExecutionException(responseBody);
        }

        @Override
        public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

        }
    }
}
