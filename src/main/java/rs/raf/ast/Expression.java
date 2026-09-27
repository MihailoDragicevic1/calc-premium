package rs.raf.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public class Expression extends Tree {

    public enum Operation {
        ADD("+", 2),
        SUB("-", 2),

        MUL("*", 2),
        DIV("/", 2),

        POW("^", 2),

        /** A vector or a number or a variable.  */
        VALUE(null, 0),
        LOGICAL_OR("||", 2),
        LOGICAL_AND("&&", 2),
        EQUALS("==", 2),
        NOT_EQUALS("!=", 2),
        LESS_THAN("<", 2),
        LESS_THAN_EQUAL("<=", 2),
        GREATER_THAN(">", 2),
        GREATER_THAN_EQUAL(">=", 2),
        MOD("%", 2),
        NEGATE("-", 1),
        LOGICAL_NOT("!", 1),
        ASSIGN("=", 2),

        INDEX ("!!", 2),
        PUSH ("++", 1),
        LENGTH ("length", 1),
        COLLECT ("list", 0),

        FUNCALL ("apply", -1),
        LITERAL ("lit", 0),
        DECL_USE ("d", 0);

        @Getter
        public final String label;
        @Getter
        private final int opCount;

        Operation(String label, int operationCount) {
            this.label = label;
            this.opCount = operationCount;
        }
    }

    private Operation operation;
    private List<Expression> operands;
    private Expression rhs;

    public Expression(Location location, Operation operation, List<Expression> operands) {
        super(location);
        operands.forEach (Objects::requireNonNull);
        assert ((operands.size () == operation.getOpCount ())
                || (operation.getOpCount () <= 0
                && operands.size () >= -operation.getOpCount ()))
                : "Wrong operand count";

        this.operation = operation;
        this.operands = new ArrayList<>(operands);
    }

    protected Expression(Location location)
    {
        super(location);
        this.operation = Operation.VALUE;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node(operation.label,
                () -> {
                    operands.forEach (x -> x.prettyPrint (pp));
                });
    }

}