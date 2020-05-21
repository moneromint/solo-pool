package com.moneromint.solo.stratum.message;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;

public class StratumMessageDeserializer extends StdDeserializer<StratumMessage> {
    private static final Map<String, TypeReference<? extends StratumMessage>> REQUESTS = Map.of(
            "login", new TypeReference<StratumRequest<StratumLoginParams>>() {
            },
            "submit", new TypeReference<StratumRequest<StratumSubmitParams>>() {
            }
    );

    protected StratumMessageDeserializer() {
        super(StratumMessage.class);
    }

    private static StratumMessage deserialize(ObjectMapper objectMapper,
                                              TypeReference<? extends StratumMessage> typeReference,
                                              ObjectNode object) throws IOException {
        return (StratumMessage) objectMapper.readValue(objectMapper.treeAsTokens(object),
                objectMapper.getTypeFactory().constructType(typeReference));
    }

    @Override
    public StratumMessage deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        final var objectMapper = (ObjectMapper) p.getCodec();
        final ObjectNode object = objectMapper.readTree(p);

        if (object.has("method") && object.has("params")) {
            final var method = object.get("method").asText();
            final var type = REQUESTS.get(method);
            if (type == null) {
                throw new IllegalArgumentException("Unknown object type");
            }
            return deserialize(objectMapper, type, object);
        }

        throw new IllegalArgumentException("Unknown object type");
    }
}
