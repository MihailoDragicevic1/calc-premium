package rs.raf.gen;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Instruction {

    public enum Code {
        // Arithmetic Operations
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO,
        NEGATIVE,
        RAISE,

        // Logical Operations
        LOGICAL_AND,
        LOGICAL_OR,
        LOGICAL_NOT,

        // Comparison Operations
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        LESS_THAN_EQUAL,
        GREATER_THAN,
        GREATER_THAN_EQUAL,

        PUSH_CONSTANT(1),
        POP,

        // Variable Handling
        SET_LOCAL(1),
        GET_LOCAL(1),
        SET_GLOBAL(1),
        GET_GLOBAL(1),

        // Array and Vector Operations
        VECTOR_CONSTRUCT(1),
        VECTOR_ACCESS,
        ARRAY_PUSH,
        ARRAY_LENGTH,

        // Function Operations
        FUNCTION_CALL(1),
        RETURN,
        RETURN_VOID,
        BUILD_CLOSURE(1),

        // Control Flow
        JUMP(1),
        JUMP_IF_FALSE(1),

        // Printing and Termination
        PRINT,
        EXIT;

        public final int argCount;
        Code() {
            this(0);
        }

        Code(int argCount) {
            this.argCount = argCount;
        }
    }

    private final Code opcode;

    private int arg1 = -1;
    public Instruction(Code opcode) {
        assert opcode.argCount == 0 : "This instruction expects no arguments.";
        this.opcode = opcode;
    }

    public Instruction(Code opcode, int arg1) {
        assert opcode.argCount == 1 : "This instruction expects one argument.";
        this.opcode = opcode;
        this.arg1 = arg1;
    }
    public void setArg1(int arg1) {
        assert opcode.argCount >= 1 : "This instruction does not support arguments.";
        this.arg1 = arg1;
    }

    @Override
    public String toString() {
        var s = new StringBuilder();
        s.append(opcode);
        if (opcode.argCount >= 1) {
            s.append(" ").append(arg1);
        }
        return s.toString();
    }
}
