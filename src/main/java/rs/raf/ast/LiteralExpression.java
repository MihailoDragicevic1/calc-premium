package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LiteralExpression extends Expression {
    private final Object value;

    public LiteralExpression(Location location, Object value) {
        super(location);
        this.value = value;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("Literal", () -> {
            pp.terminal("Value: " + value);
        });
    }
}
