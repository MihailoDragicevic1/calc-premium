package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BreakStatement extends Statement {

    public BreakStatement(Location location) {
        super(location);
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.terminal("break");
    }
}
