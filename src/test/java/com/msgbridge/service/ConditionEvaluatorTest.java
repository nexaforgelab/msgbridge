package com.msgbridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ConditionEvaluatorTest {

    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    @Test
    void evaluatesComparisonsAndBooleanOperators() {
        Map<String, Object> data = Map.of("amount", "12800", "level", "P1", "service", "payment-core");

        assertThat(evaluator.matches("amount >= 10000 && level = P1", data)).isTrue();
        assertThat(evaluator.matches("amount < 10000 || service contains payment", data)).isTrue();
        assertThat(evaluator.matches("amount < 10000 && level = P1", data)).isFalse();
    }
}
