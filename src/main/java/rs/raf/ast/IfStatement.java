package rs.raf.ast;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class IfStatement extends Statement{

    private Expression condition;
    private StatementList ifBlock;
    private StatementList elseBlock;

    public IfStatement(Location location, Expression condition, StatementList ifBlock, StatementList elseBlock) {
        super(location);
        this.condition = condition;
        this.ifBlock = ifBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("IfStatement", () -> {
            pp.node("Condition", () -> condition.prettyPrint(pp));
            pp.node("IfBlck", () -> ifBlock.prettyPrint(pp));
            if (elseBlock != null) pp.node("ElseBlock", () -> elseBlock.prettyPrint(pp));
        });
    }
}
