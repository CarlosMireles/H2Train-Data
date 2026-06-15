package com.h2traindata.dataapp.dataset.filter;

import com.h2traindata.dataapp.dataset.exception.UnsupportedOperatorException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DatasetOperator {
    GT("gt") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) > 0;
        }
    },
    GTE("gte") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) >= 0;
        }
    },
    LT("lt") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) < 0;
        }
    },
    LTE("lte") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) <= 0;
        }
    },
    EQ("eq") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) == 0;
        }
    },
    BETWEEN("between") {
        @Override
        public boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue) {
            return actual.compareTo(value) >= 0 && actual.compareTo(maxValue) <= 0;
        }
    };

    private final String value;

    DatasetOperator(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public abstract boolean matches(BigDecimal actual, BigDecimal value, BigDecimal maxValue);

    public static DatasetOperator parse(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (DatasetOperator operator : values()) {
                if (operator.value.equals(normalized)) {
                    return operator;
                }
            }
        }
        throw new UnsupportedOperatorException(value);
    }

    public static List<String> supportedValues() {
        return Arrays.stream(values()).map(DatasetOperator::value).toList();
    }
}
