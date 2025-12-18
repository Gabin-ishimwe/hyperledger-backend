package com.openledger.core.util;

import com.api.jsonata4java.expressions.Expressions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Utility class for evaluating JSONata expressions programmatically for data transformation.
 */
@Component
public class JsonataTransformer {

    private final ObjectMapper objectMapper;

    public JsonataTransformer() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Transform input JSON using a JSONata expression.
     *
     * @param inputJson Input JSON string to transform
     * @param expression JSONata expression string
     * @return Transformed JSON as string
     * @throws MappingException if transformation fails
     */
    public String transform(String inputJson, String expression) {
        try {
            Expressions expr = Expressions.parse(expression);
            JsonNode source = objectMapper.readTree(inputJson);
            JsonNode result = expr.evaluate(source);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new MappingException("JSONata transformation failed for expression: " + expression, e);
        }
    }

    /**
     * Transform input JSON using a JSONata expression and return as specific type.
     *
     * @param inputJson Input JSON string to transform
     * @param expression JSONata expression string
     * @param targetClass Target class to deserialize result into
     * @param <T> Target type
     * @return Transformed object of target type
     * @throws MappingException if transformation fails
     */
    public <T> T transform(String inputJson, String expression, Class<T> targetClass) {
        try {
            String transformedJson = transform(inputJson, expression);
            return objectMapper.readValue(transformedJson, targetClass);
        } catch (Exception e) {
            throw new MappingException("JSONata transformation failed for target class: " + targetClass.getSimpleName(), e);
        }
    }

    /**
     * Transform JsonNode using a JSONata expression.
     *
     * @param source Input JsonNode to transform
     * @param expression JSONata expression string
     * @return Transformed JsonNode
     * @throws MappingException if transformation fails
     */
    public JsonNode transform(JsonNode source, String expression) {
        try {
            Expressions expr = Expressions.parse(expression);
            return expr.evaluate(source);
        } catch (Exception e) {
            throw new MappingException("JSONata transformation failed for JsonNode", e);
        }
    }
}
