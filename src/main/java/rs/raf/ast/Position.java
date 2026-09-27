package rs.raf.ast;

public record Position(int line, int column) {
    /** Returns {@code true} iff the position {@code other} is before {@code
    this} in a file.  */
    public boolean lessThan(Position other) {
        if (other.line < this.line)
            return true;
        if (other.line == this.line)
            return other.column < this.column;

        /* Necessarily on a later line, hence greater.  */
        return false;
    }
}
