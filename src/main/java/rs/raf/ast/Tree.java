package rs.raf.ast;

import lombok.*;

/** Base class for AST nodes.  */
@Getter
@Setter
@EqualsAndHashCode
public abstract class Tree{
    private Location location;

    public Tree(Location location) {
        this.location = location;
    }

    public abstract void prettyPrint(ASTPrettyPrinter pp);

}