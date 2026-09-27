package rs.raf.ast;

import lombok.Getter;

@Getter
public enum Type {
    INT, BOOL, VOID, CHAR, STRING, ARR;

    public String getStringRepresentation() {
        return switch (this) {
            case INT -> "Integer";
            case BOOL -> "Boolean";
            case VOID -> "Void";
            case CHAR -> "Character";
            case STRING -> "String";
            case ARR -> "Array";
            default -> throw new IllegalArgumentException("Unknown type: " + this);
        };
    }
}
