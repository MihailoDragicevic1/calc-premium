package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ArrayType extends Expression {
    private final PrimitiveType baseType;

    public ArrayType(Location location, PrimitiveType baseType) {
        super(location);
        this.baseType = baseType;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("ArrayType", () -> {
            baseType.prettyPrint(pp);
        });
    }

}