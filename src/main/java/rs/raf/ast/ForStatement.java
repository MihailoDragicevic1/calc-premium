package rs.raf.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ForStatement extends Statement {
    private DeclarationStatement declaration;
    private Expression condition;
    private ExpressionStatement incrementExpression;
    private StatementList body;

    public ForStatement(Location location, DeclarationStatement declaration, Expression condition, ExpressionStatement incrementExpr, StatementList block) {
        super(location);
        this.declaration = declaration;
        this.condition = condition;
        this.incrementExpression = incrementExpr;
        this.body = block;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("ForStatement", () -> {
            if (declaration != null) {
                pp.node("Declaration", () -> declaration.prettyPrint(pp));
            }

            pp.node("Condition", () -> condition.prettyPrint(pp));

            if (incrementExpression != null) {
                pp.node("Increment", () -> incrementExpression.prettyPrint(pp));
            }

            pp.node("Body", () -> body.prettyPrint(pp));
        });
    }

}
