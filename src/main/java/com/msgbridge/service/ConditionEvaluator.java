package com.msgbridge.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ConditionEvaluator {
    private static final Pattern CONDITION = Pattern.compile(
            "^\\s*([a-zA-Z0-9_.-]+)\\s*(>=|<=|==|=|!=|>|<|contains)\\s*'?(.*?)'?\\s*$");

    public boolean matches(String expression, Map<String, Object> data) {
        if (!StringUtils.hasText(expression)) {
            return true;
        }
        String[] orParts = expression.split("\\s+\\|\\|\\s+");
        for (String orPart : orParts) {
            if (matchesAnd(orPart, data)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAnd(String expression, Map<String, Object> data) {
        String[] andParts = expression.split("\\s+&&\\s+");
        for (String condition : andParts) {
            if (!matchesOne(condition, data)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesOne(String expression, Map<String, Object> data) {
        Matcher matcher = CONDITION.matcher(expression);
        if (!matcher.matches()) {
            return false;
        }
        Object actual = data == null ? null : data.get(matcher.group(1));
        String operator = matcher.group(2);
        String expected = matcher.group(3);
        if (actual == null) {
            return "!=".equals(operator);
        }
        String actualText = String.valueOf(actual);
        return switch (operator) {
            case "=", "==" -> actualText.equals(expected);
            case "!=" -> !actualText.equals(expected);
            case "contains" -> actualText.contains(expected);
            case ">", ">=", "<", "<=" -> compareNumber(actualText, expected, operator);
            default -> false;
        };
    }

    private boolean compareNumber(String actual, String expected, String operator) {
        try {
            int result = new BigDecimal(actual).compareTo(new BigDecimal(expected));
            return switch (operator) {
                case ">" -> result > 0;
                case ">=" -> result >= 0;
                case "<" -> result < 0;
                case "<=" -> result <= 0;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
