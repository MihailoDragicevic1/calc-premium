package rs.raf.ast;

import java.util.List;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public final class PrintStatement extends Statement {
    private List<Expression> args;

    public PrintStatement(Location location, List<Expression> args) {
        super(location);
        this.args = args;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("print",
                () -> {
                    args.forEach(x -> x.prettyPrint(pp));
                });
    }

}
