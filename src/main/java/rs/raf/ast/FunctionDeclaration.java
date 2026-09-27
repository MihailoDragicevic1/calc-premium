package rs.raf.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class FunctionDeclaration extends Statement {

    private ArgumentList args;
    private PrimitiveType returnType;
    private String name;
    private StatementList body;

    public FunctionDeclaration (Location location, ArgumentList args, String name, PrimitiveType returnType, StatementList body) {
        super (location);
        this.args = args;
        this.returnType = returnType;
        this.name = name;
        this.body = body;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("FunctionDeclaration", () -> {
            pp.terminal(returnType.getType().getStringRepresentation() + " " + name);

            pp.node("Arguments", () -> args.prettyPrint(pp));

            pp.node("Body", () -> body.prettyPrint(pp));
        });
    }

}
