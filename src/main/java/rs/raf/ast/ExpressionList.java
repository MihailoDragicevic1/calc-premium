package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ExpressionList extends Expression {
    private final List<Expression> expressions;

    public ExpressionList(Location location, List<Expression> expressions) {
        super(location);
        this.expressions = expressions;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("ExpressionList", () -> {
            for (int i = 0; i < expressions.size(); i++) {
                expressions.get(i).prettyPrint(pp);
                if (i < expressions.size() - 1) {
                    pp.terminal(","); // Add comma separator for expressions
                }
            }
        });
    }
}