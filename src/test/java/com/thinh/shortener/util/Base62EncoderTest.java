package com.thinh.shortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - Base62Encoder Algorithm")
public class Base62EncoderTest {

    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();
    }

    @Test
    @DisplayName("Encoding ID = 0 must return the first character 'a'")
    void encode_WhenIdIsZero_ShouldReturnFirstCharacter() {
        long id = 0;

        String encoded = base62Encoder.encode(id);

        assertEquals("a", encoded);
    }

    @Test
    @DisplayName("Encoding any positive ID must return a valid short string")
    void encode_WhenIdIsPositive_ShouldReturnEncodedString() {
        long id = 125;

        String encoded = base62Encoder.encode(id);

        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Test
    @DisplayName("Decode(Encode(id)) must equal the original id")
    void encodeAndDecode_ShouldBeConsistent() {
        long originalId = 123456789L;

        String encoded = base62Encoder.encode(originalId);
        long decodedId = base62Encoder.decode(encoded);

        assertEquals(originalId, decodedId, "The ID after decryption must match the original ID");
    }

    @Test
    @DisplayName("Test with a large ID (Long.MAX_VALUE)")
    void encodeAndDecode_WithLargeId_ShouldWorkCorrectly() {
        long largeId = 987654321012345L;

        String encoded = base62Encoder.encode(largeId);
        long decodedId = base62Encoder.decode(encoded);

        assertEquals(largeId, decodedId);
    }

}
