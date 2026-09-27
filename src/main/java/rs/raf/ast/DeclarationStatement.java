package rs.raf.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class DeclarationStatement extends Statement {

    private Type type;
    private String name;
    private Expression initializer;

    public DeclarationStatement (Location location, Type type, String name, Expression initializer)
    {
        super (location);
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("declaration of %s %s".formatted(name, type),
                () -> {
                    initializer.prettyPrint(pp);
                });
    }

}
