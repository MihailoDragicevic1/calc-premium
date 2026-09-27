package rs.raf.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NullStatement extends Statement {

    public NullStatement(Location location) {
        super(location);
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("NullStatement", () -> {
            pp.terminal("This is a null statement.");
        });
    }

}
