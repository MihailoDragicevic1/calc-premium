package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpressionStatement extends Statement {

    private Expression lhs;
    private Expression rhs;

    public ExpressionStatement (Location location, Expression lhs, Expression rhs)
    {
        super (location);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        // Start a new node for the expression statement
        pp.node("ExpressionStatement", () -> {
            // Pretty print the contained expression
            lhs.prettyPrint(pp);
            if (rhs != null) {
                rhs.prettyPrint(pp);
            }
        });
    }

}
