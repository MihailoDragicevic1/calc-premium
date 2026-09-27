package rs.raf.vm;

import rs.raf.ast.FunctionDeclaration;
import rs.raf.ast.FunctionDeclarationList;
import rs.raf.gen.Instruction;

import java.util.*;

import static rs.raf.gen.Instruction.Code.*;

public class VM {
    private final Deque<Value> invocations; // Stek poziva funkcija
    private final Stack<Object> globalStack; // Globalni stek
    private boolean running; // Status vm
    private int ip; //pointer
    private final FunctionDeclarationList functionList;

    public VM(FunctionDeclarationList functionList) {
        this.functionList = functionList;
        this.invocations = new ArrayDeque<>();
        this.globalStack = new Stack<>();
        this.running = true;
    }

    public void run() {
        if (shouldStopCompilation()) {
            return;
        }

        while (running) {
            var currentFrame = currentFrame();
            var localStack = currentFrame.getLocalStack();
            var compiledCode = currentFrame.getCompiledBody();

            if (ip < 0 || ip >= compiledCode.size()) {
                throw new IllegalStateException("Instruction pointer (IP) is out of scope");
            }

            var instruction = compiledCode.get(ip++);

            switch (instruction.getOpcode()) {
                case EXIT -> {
                    if (localStack.isEmpty()) {
                        System.exit(0);
                    }

                    var result = localStack.remove(localStack.size() - 1);

                    if (result instanceof Integer intValue) {
                        System.exit(intValue);
                    } else if (result instanceof Double doubleValue) {
                        System.exit(doubleValue.intValue());
                    } else {
                        throw new IllegalStateException("Unsupported value for EXIT: " + result);
                    }
                }
                default -> executeInstruction(instruction);
            }
        }
    }
    private void executeInstruction(Instruction instruction) {
        validateStackSize(1);
        switch (instruction.getOpcode()) {
            case ADD -> performBinaryOperation((a, b) -> (int) a + (int) b);
            case SUBTRACT -> performBinaryOperation((a, b) -> (int) a - (int) b);
            case MULTIPLY -> performBinaryOperation((a, b) -> (int) a * (double) b);
            case DIVIDE -> performBinaryOperation((a, b) -> (int) a / (int) b);
            case MODULO -> performBinaryOperation((a, b) -> (int) a % (int) b);

            case LOGICAL_AND -> performBinaryOperation((a, b) -> (boolean) a && (boolean) b);
            case LOGICAL_OR -> performBinaryOperation((a, b) -> (boolean) a || (boolean) b);
            case LOGICAL_NOT -> {
                var value = globalStack.pop();
                globalStack.push(!(boolean) value);
            }

            case EQUALS -> performBinaryOperation(Object::equals);
            case NOT_EQUALS -> performBinaryOperation((a, b) -> !a.equals(b));
            case LESS_THAN -> performBinaryOperation((a, b) -> (int) a < (int) b);
            case LESS_THAN_EQUAL -> performBinaryOperation((a, b) -> (int) a <= (int) b);
            case GREATER_THAN -> performBinaryOperation((a, b) -> (int) a > (int) b);
            case GREATER_THAN_EQUAL -> performBinaryOperation((a, b) -> (int) a >= (int) b);

            case PUSH_CONSTANT -> globalStack.push(instruction.getArg1());
            case POP -> globalStack.pop();

            case SET_LOCAL -> currentFrame().setLocal(instruction.getArg1(), globalStack.pop());
            case GET_LOCAL -> globalStack.push(currentFrame().getLocal(instruction.getArg1()));

            //case SET_GLOBAL -> setGlobal(instruction.getArg1(), globalStack.pop());
            //case GET_GLOBAL -> globalStack.push(getGlobal(instruction.getArg1()));

            case VECTOR_CONSTRUCT -> {
                int size = instruction.getArg1();
                List<Object> vector = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    vector.add(globalStack.pop());
                }
                globalStack.push(vector);
            }

            case VECTOR_ACCESS -> {
                int index = (int) globalStack.pop();
                List<Object> vector = (List<Object>) globalStack.pop();
                globalStack.push(vector.get(index));
            }

            case ARRAY_PUSH -> {
                Object value = globalStack.pop();
                List<Object> vector = (List<Object>) globalStack.pop();
                vector.add(value);
            }

            case ARRAY_LENGTH -> {
                List<Object> vector = (List<Object>) globalStack.pop();
                globalStack.push(vector.size());
            }

            case RETURN -> {
                Object returnValue = globalStack.pop();
                invocations.pop(); // Uklanjanje trenutnog okvira sa steka
                if (invocations.isEmpty()) {
                    running = false;
                } else {
                    globalStack.push(returnValue); // Vraćanje povratne vrednosti
                }
            }

            case RETURN_VOID -> {
                invocations.pop();
                if (invocations.isEmpty()) {
                    running = false;
                }
            }

            case JUMP -> ip = instruction.getArg1();
            case JUMP_IF_FALSE -> {
                boolean condition = (boolean) globalStack.pop();
                if (!condition) {
                    ip = instruction.getArg1();
                }
            }

            case PRINT -> {
                Object value = globalStack.pop();
                System.out.println(value);
            }

            default -> throw new UnsupportedOperationException("Unsupported Instruction" + instruction.getOpcode());
        }
    }

    private Value currentFrame() {
        return invocations.peek();
    }


    private FunctionDeclaration currentFunction(int index) {
        if (index < 0 || index >= functionList.getFunDeclarations().size()) {
            throw new IllegalStateException("Invalid function index: " + index);
        }
        return functionList.getFunDeclarations().get(index);
    }

    private boolean shouldStopCompilation() {
        return false;
    }
    private void validateStackSize(int requiredSize){
        if (globalStack.size() < requiredSize) {
            throw new IllegalStateException("Not enough elements on the stack for the operation!");
        }
    }

    private void performBinaryOperation(BinaryOperation operation) {
        validateStackSize(2);
        var rhs = globalStack.pop();
        var lhs = globalStack.pop();
        globalStack.push(operation.apply(lhs, rhs));
    }

    @FunctionalInterface
    private interface BinaryOperation {
        Object apply(Object lhs, Object rhs);
    }
}
