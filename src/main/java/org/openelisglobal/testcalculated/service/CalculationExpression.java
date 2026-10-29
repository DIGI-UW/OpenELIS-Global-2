package org.openelisglobal.testcalculated.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.openelisglobal.common.util.BoundedDecimal;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * The calculation builder's bounded arithmetic and Boolean grammar. Spring is
 * used only to parse precedence and parentheses: no SpEL evaluation context or
 * script engine is invoked. Only the explicitly handled nodes can be evaluated.
 * Stored values never become expression source; operands are bound as numbers.
 */
public final class CalculationExpression {
    private static final int MAX_OPERATIONS = 256;
    private static final int MAX_DEPTH = 32;
    private static final Set<String> OPERATORS = Set.of("+", "-", "*", "/", "(", ")", "==", "!=", ">=", "<=", "&&",
            "||");
    private final SpelNode root;
    private final Map<String, Binding> bindings;

    public record Operand(double value, Double minimum, Double maximum) {
        public Operand(double value) {
            this(value, null, null);
        }
    }

    private enum Bound {
        VALUE, MINIMUM, MAXIMUM
    }

    private record Binding(Operation operation, Double constant, Bound bound) {
        Object resolve(Function<Operation, Operand> resolver) {
            if (constant != null)
                return constant;
            Operand operand = resolver.apply(operation);
            if (operand == null)
                throw invalid("A required operand is missing");
            return switch (bound) {
            case VALUE -> finite(operand.value());
            case MINIMUM -> operand.minimum() == null ? Double.NEGATIVE_INFINITY : finite(operand.minimum());
            case MAXIMUM -> operand.maximum() == null ? Double.POSITIVE_INFINITY : finite(operand.maximum());
            };
        }
    }

    private CalculationExpression(SpelNode root, Map<String, Binding> bindings) {
        this.root = root;
        this.bindings = bindings;
        type(root, 0);
    }

    public static CalculationExpression compile(List<Operation> operations) {
        if (operations == null || operations.isEmpty() || operations.size() > MAX_OPERATIONS)
            throw invalid("A calculation needs between 1 and 256 operations");
        StringBuilder source = new StringBuilder();
        Map<String, Binding> bindings = new HashMap<>();
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (operation == null || operation.getType() == null || operation.getValue() == null)
                throw invalid("An operation is incomplete");
            String value = operation.getValue();
            Binding binding;
            switch (operation.getType()) {
            case MATH_FUNCTION:
                if (!OPERATORS.contains(value))
                    throw invalid("Unsupported mathematical operation");
                source.append(' ').append(value).append(' ');
                continue;
            case INTEGER:
                binding = new Binding(operation, number(value), Bound.VALUE);
                break;
            case PATIENT_ATTRIBUTE:
                if (!Operation.PatientAttribute.AGE.name().equals(value))
                    throw invalid("Unsupported patient attribute");
                binding = new Binding(operation, null, Bound.VALUE);
                break;
            case TEST_RESULT:
                try {
                    if (Integer.parseInt(value) <= 0)
                        throw invalid("Invalid operand test");
                } catch (NumberFormatException e) {
                    throw invalid("Invalid operand test");
                }
                String range = null;
                if (i + 1 < operations.size()) {
                    Operation next = operations.get(i + 1);
                    if (next != null && next.getType() == Operation.OperationType.MATH_FUNCTION
                            && (Operation.IN_NORMAL_RANGE.equals(next.getValue())
                                    || Operation.OUTSIDE_NORMAL_RANGE.equals(next.getValue()))) {
                        range = next.getValue();
                        i++;
                    }
                }
                binding = new Binding(operation, null, Bound.VALUE);
                if (range != null) {
                    String measured = bind(bindings, binding);
                    String minimum = bind(bindings, new Binding(operation, null, Bound.MINIMUM));
                    String maximum = bind(bindings, new Binding(operation, null, Bound.MAXIMUM));
                    // Keep the existing expansion, precedence and inclusive boundaries.
                    // Changing clinical range semantics is separate from removing scripts.
                    source.append(' ').append(measured)
                            .append(Operation.IN_NORMAL_RANGE.equals(range) ? " >= " : " <= ").append(minimum)
                            .append(Operation.IN_NORMAL_RANGE.equals(range) ? " && " : " || ").append(measured)
                            .append(Operation.IN_NORMAL_RANGE.equals(range) ? " <= " : " >= ").append(maximum)
                            .append(' ');
                    continue;
                }
                break;
            default:
                throw invalid("Unsupported operation type");
            }
            source.append(' ').append(bind(bindings, binding)).append(' ');
        }
        try {
            SpelExpression parsed = (SpelExpression) new SpelExpressionParser().parseExpression(source.toString());
            return new CalculationExpression(parsed.getAST(), bindings);
        } catch (ParseException e) {
            throw invalid("Invalid calculation syntax");
        }
    }

    private static String bind(Map<String, Binding> bindings, Binding binding) {
        String variable = "#operand" + bindings.size();
        bindings.put(variable, binding);
        return variable;
    }

    public static double number(String value) {
        if (!BoundedDecimal.isBoundedDecimal(value))
            throw invalid("An operand is not a bounded decimal");
        return finite(Double.parseDouble(value));
    }

    /**
     * Validate structure and operand types without evaluating placeholder values.
     */
    private boolean type(SpelNode node, int depth) {
        if (depth > MAX_DEPTH)
            throw invalid("Calculation nesting exceeds 32 levels");
        String kind = node.getClass().getSimpleName();
        if ("VariableReference".equals(kind)) {
            Binding binding = bindings.get(node.toStringAST());
            if (binding == null)
                throw invalid("Unknown operand");
            return false;
        }
        if (!Set.of("OpPlus", "OpMinus", "OpMultiply", "OpDivide", "OpEQ", "OpNE", "OpGE", "OpLE", "OpAnd", "OpOr")
                .contains(kind))
            throw invalid("Unsupported calculation syntax");
        int count = node.getChildCount();
        if (count != 2 && !(count == 1 && ("OpPlus".equals(kind) || "OpMinus".equals(kind))))
            throw invalid("Invalid operator arity");
        boolean left = type(node.getChild(0), depth + 1);
        boolean right = count == 2 && type(node.getChild(1), depth + 1);
        return switch (kind) {
        case "OpAnd", "OpOr" -> {
            if (!left || !right)
                throw invalid("Logical operators require conditions");
            yield true;
        }
        case "OpEQ", "OpNE" -> {
            if (left != right)
                throw invalid("Comparison operands must have the same type");
            yield true;
        }
        case "OpGE", "OpLE" -> {
            if (left || right)
                throw invalid("Ordered comparisons require numbers");
            yield true;
        }
        default -> {
            if (left || right)
                throw invalid("Arithmetic requires numbers");
            yield false;
        }
        };
    }

    public String evaluate(Function<Operation, Operand> resolver) {
        Map<Operation, Operand> values = new HashMap<>();
        Object value = evaluate(root, operation -> values.computeIfAbsent(operation, resolver));
        if (value instanceof Double)
            finite((Double) value);
        return value.toString();
    }

    private Object evaluate(SpelNode node, Function<Operation, Operand> resolver) {
        String kind = node.getClass().getSimpleName();
        if ("VariableReference".equals(kind))
            return bindings.get(node.toStringAST()).resolve(resolver);
        Object left = evaluate(node.getChild(0), resolver);
        if ("OpAnd".equals(kind))
            return (Boolean) left && (Boolean) evaluate(node.getChild(1), resolver);
        if ("OpOr".equals(kind))
            return (Boolean) left || (Boolean) evaluate(node.getChild(1), resolver);
        if (node.getChildCount() == 1)
            return "OpMinus".equals(kind) ? -(Double) left : left;
        Object right = evaluate(node.getChild(1), resolver);
        if ("OpEQ".equals(kind) || "OpNE".equals(kind)) {
            boolean equal = left instanceof Double ? ((Double) left).doubleValue() == ((Double) right).doubleValue()
                    : left.equals(right);
            return "OpEQ".equals(kind) ? equal : !equal;
        }
        double a = (Double) left, b = (Double) right;
        return switch (kind) {
        case "OpPlus" -> finite(a + b);
        case "OpMinus" -> finite(a - b);
        case "OpMultiply" -> finite(a * b);
        case "OpDivide" -> finite(a / b);
        case "OpGE" -> a >= b;
        case "OpLE" -> a <= b;
        default -> throw invalid("Unsupported calculation syntax");
        };
    }

    private static double finite(double value) {
        if (!Double.isFinite(value))
            throw invalid("A calculation produced a non-finite number");
        return value;
    }

    private static IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException(reason);
    }
}
