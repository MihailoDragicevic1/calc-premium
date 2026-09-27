package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ContinueStatement extends Statement{

    public ContinueStatement(Location location) {
        super(location);
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.terminal("continue");
    }

}
