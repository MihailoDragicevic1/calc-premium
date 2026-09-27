package rs.raf.ast;

import lombok.Getter;


@Getter
public class WhileStatement extends Statement {

    private Expression condition;
    private StatementList block;

    public WhileStatement(Location location, Expression condition, StatementList block) {
        super(location);
        this.condition = condition;
        this.block = block;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("WhileStatement", () -> {
            pp.node("Condition", () -> condition.prettyPrint(pp));
            pp.node("Body", () -> block.prettyPrint(pp));
        });
    }
}
