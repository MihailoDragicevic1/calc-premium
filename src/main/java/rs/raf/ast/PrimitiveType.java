package rs.raf.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class PrimitiveType extends Expression {

    private Type type;

    public PrimitiveType(Location location, Type type) {
        super(location);
        this.type = type;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("PrimitiveType", () -> pp.terminal(type.getStringRepresentation()));
    }

}
