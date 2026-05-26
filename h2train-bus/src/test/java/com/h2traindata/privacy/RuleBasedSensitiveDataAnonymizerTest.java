package com.h2traindata.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleBasedSensitiveDataAnonymizerTest {

    private final RuleBasedSensitiveDataAnonymizer anonymizer = new RuleBasedSensitiveDataAnonymizer();

    @Test
    void anonymizesPersonalFieldsRecursively() {
        Map<String, Object> anonymized = anonymizer.anonymizeFields(Map.of(
                "email", "runner@example.com",
                "displayName", "Carlos Runner",
                "nested", Map.of("firstName", "Carlos"),
                "items", List.of(Map.of("lastName", "Runner")),
                "distanceMeters", 5000
        ));

        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, anonymized.get("email"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, anonymized.get("displayName"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, ((Map<?, ?>) anonymized.get("nested")).get("firstName"));
        assertEquals(RuleBasedSensitiveDataAnonymizer.ANONYMIZED_VALUE, ((Map<?, ?>) ((List<?>) anonymized.get("items")).get(0)).get("lastName"));
        assertEquals(5000, anonymized.get("distanceMeters"));
    }
}
