package rs.raf.gen;


import rs.raf.ast.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class NotQuiteByteCodeCompiler {
    private final List<Instruction> instructions;
    private final Deque<Integer> loopStartStack = new ArrayDeque<>();
    private final Deque<List<Instruction>> loopEndStack = new ArrayDeque<>();

    public NotQuiteByteCodeCompiler() {
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> compile(Tree tree) {
        if (tree instanceof FunctionDeclarationList) {
            for (Tree function : ((FunctionDeclarationList) tree).getFunDeclarations()) {
                compile(function);
            }
        } else if (tree instanceof FunctionDeclaration) {
            compileFunctionDeclaration((FunctionDeclaration) tree);
        } else {
            throw new UnsupportedOperationException("Unsupported top-level construct: " + tree.getClass().getSimpleName());
        }
        return instructions;
    }

    private void compileFunctionDeclaration(FunctionDeclaration func) {
        emit(Instruction.Code.BUILD_CLOSURE, instructions.size());

        compileStatementList(func.getBody());
    }

    private void compileStatementList(StatementList stmtList) {
        for (Statement stmt : stmtList.getStmts()) {
            compileStatement(stmt);
        }
    }

    private void compileStatement(Statement stmt) {
        if (stmt instanceof ExpressionStatement) {
            compileExpression(((ExpressionStatement) stmt).getLhs());
        } else if (stmt instanceof IfStatement) {
            compileIfStatement((IfStatement) stmt);
        } else if (stmt instanceof WhileStatement) {
            compileWhileStatement((WhileStatement) stmt);
        } else if (stmt instanceof ForStatement) {
            compileForStatement((ForStatement) stmt);
        } else if (stmt instanceof ReturnStatement) {
            compileReturnStatement((ReturnStatement) stmt);
        } else if (stmt instanceof DeclarationStatement) {
            compileDeclarationStatement((DeclarationStatement) stmt);
        } else if (stmt instanceof PrintStatement) {
            compilePrintStatement((PrintStatement) stmt);
        } else if (stmt instanceof BreakStatement) {
            compileBreakStatement((BreakStatement) stmt);
        } else if (stmt instanceof ContinueStatement) {
            compileContinueStatement((ContinueStatement) stmt);
        } else if (stmt instanceof NullStatement) {
            compileNullStatement((NullStatement) stmt);
        } else {
            throw new UnsupportedOperationException("Unsupported statement type: " + stmt.getClass().getSimpleName());
        }
    }

    private void compileNullStatement(NullStatement stmt) {

    }

    private void compileContinueStatement(ContinueStatement stmt) {
        if (loopStartStack.isEmpty()) {
            throw new IllegalStateException("continue statement not inside a loop.");
        }
        int loopStartIndex = loopStartStack.peek();
        emit(Instruction.Code.JUMP, loopStartIndex);
    }

    private void compileBreakStatement(BreakStatement stmt) {
        if (loopEndStack.isEmpty()) {
            throw new IllegalStateException("break statement not inside a loop.");
        }
        Instruction breakJump = emit(Instruction.Code.JUMP, -1);
        loopEndStack.peek().add(breakJump);
    }

    private void compilePrintStatement(PrintStatement stmt) {
        for (Expression arg : stmt.getArgs()) {
            compileExpression(arg);
        }
        emit(Instruction.Code.PRINT);
    }

    private void compileDeclarationStatement(DeclarationStatement decl) {
        compileExpression(decl.getInitializer());
        emit(Instruction.Code.SET_LOCAL, getLocalIndex(decl.getName()));
    }

    private void compileReturnStatement(ReturnStatement ret) {
        if (ret.getValue() != null) {
            compileExpression(ret.getValue());
            emit(Instruction.Code.RETURN);
        } else {
            emit(Instruction.Code.RETURN_VOID);
        }
    }

    private void compileIfStatement(IfStatement ifStmt) {
        compileExpression(ifStmt.getCondition());

        Instruction jumpIfFalse = emit(Instruction.Code.JUMP_IF_FALSE, -1);

        compileStatementList(ifStmt.getIfBlock());


        Instruction jumpToEnd = emit(Instruction.Code.JUMP, -1);

        backpatch(jumpIfFalse, instructions.size());

        if (ifStmt.getElseBlock() != null) {
            compileStatementList(ifStmt.getElseBlock());
        }

        backpatch(jumpIfFalse, instructions.size());
    }

    private void compileWhileStatement(WhileStatement whileStmt) {
        int loopStartIndex = instructions.size();
        loopStartStack.push(loopStartIndex);
        loopEndStack.push(new ArrayList<>());

        compileExpression(whileStmt.getCondition());

        Instruction jumpIfFalse = emit(Instruction.Code.JUMP_IF_FALSE, -1);

        compileStatementList(whileStmt.getBlock());

        emit(Instruction.Code.JUMP, loopStartIndex);

        backpatch(jumpIfFalse, instructions.size());

        for (Instruction breakJump : loopEndStack.pop()) {
            backpatch(breakJump, instructions.size());
        }

        loopStartStack.pop();
    }

    private void compileForStatement(ForStatement forStmt) {
        int loopStartIndex = instructions.size();
        loopStartStack.push(loopStartIndex);
        loopEndStack.push(new ArrayList<>());

        compileExpression(forStmt.getCondition());

        Instruction jumpIfFalse = emit(Instruction.Code.JUMP_IF_FALSE, -1);

        compileStatementList(forStmt.getBody());

        emit(Instruction.Code.JUMP, loopStartIndex);

        backpatch(jumpIfFalse, instructions.size());

        for (Instruction breakJump : loopEndStack.pop()) {
            backpatch(breakJump, instructions.size());
        }

        loopStartStack.pop();
    }

    private void compileExpression(Expression expr) {
        if (expr instanceof LiteralExpression) {
            compileLiteralExpression((LiteralExpression) expr);
        } else if (expr instanceof PrimitiveType) {
            compilePrimitiveType((PrimitiveType) expr);
        } else if (expr instanceof VariableReference) {
            compileVariableReference((VariableReference) expr);
        } else if(expr instanceof ArrayType){
            compileArrayType((ArrayType) expr);
         }else if (expr.getOperation() == Expression.Operation.INDEX) {
            compileArrayAccess(expr.getOperands().get(0), expr.getOperands().get(1));
        } else if (expr.getOperation() == Expression.Operation.PUSH) {
            compileArrayPush(expr.getOperands().get(0), expr.getOperands().get(1));
        } else if (expr.getOperation() == Expression.Operation.LENGTH) {
            compileArrayLength(expr.getOperands().get(0));
        }
        else if (expr.getOperation() == Expression.Operation.FUNCALL) {
            compileFunctionCall(expr);
        }
        else {
            switch (expr.getOperation()) {
                case ADD -> compileBinaryOperation(expr, Instruction.Code.ADD);
                case SUB -> compileBinaryOperation(expr, Instruction.Code.SUBTRACT);
                case MUL -> compileBinaryOperation(expr, Instruction.Code.MULTIPLY);
                case DIV -> compileBinaryOperation(expr, Instruction.Code.DIVIDE);
                case MOD -> compileBinaryOperation(expr, Instruction.Code.MODULO);
                case EQUALS -> compileBinaryOperation(expr, Instruction.Code.EQUALS);
                case NOT_EQUALS -> compileBinaryOperation(expr, Instruction.Code.NOT_EQUALS);
                case LESS_THAN -> compileBinaryOperation(expr, Instruction.Code.LESS_THAN);
                case LESS_THAN_EQUAL -> compileBinaryOperation(expr, Instruction.Code.LESS_THAN_EQUAL);
                case GREATER_THAN -> compileBinaryOperation(expr, Instruction.Code.GREATER_THAN);
                case GREATER_THAN_EQUAL -> compileBinaryOperation(expr, Instruction.Code.GREATER_THAN_EQUAL);
                case LOGICAL_AND -> compileBinaryOperation(expr, Instruction.Code.LOGICAL_AND);
                case LOGICAL_OR -> compileBinaryOperation(expr, Instruction.Code.LOGICAL_OR);
                case ASSIGN -> compileAssignment(expr);
                case NEGATE -> compileUnaryOperation(expr, Instruction.Code.NEGATIVE);
                case LOGICAL_NOT -> compileUnaryOperation(expr, Instruction.Code.LOGICAL_NOT);
                case VALUE -> throw new UnsupportedOperationException(
                        "VALUE operation should be handled by specific Expression types like LiteralExpression or VariableReference.");
                default -> throw new UnsupportedOperationException("Unsupported expression operation: " + expr.getOperation());
            }
        }
    }

    private void compileFunctionCall(Expression functionCallExpr) {
        if (functionCallExpr.getOperation() != Expression.Operation.FUNCALL) {
            throw new IllegalArgumentException("Expected a FUNCTION_CALL operation.");
        }

        List<Expression> arguments = functionCallExpr.getOperands();
        for (Expression argument : arguments) {
            compileExpression(argument);
        }
        compileExpression(functionCallExpr.getRhs());

        emit(Instruction.Code.FUNCTION_CALL, arguments.size());
    }

    private void compileArrayLength(Expression expression) {
        compileExpression(expression);

        emit(Instruction.Code.ARRAY_LENGTH);
    }

    private void compileArrayPush(Expression expression, Expression expression1) {
        compileExpression(expression);

        compileExpression(expression1);

        emit(Instruction.Code.ARRAY_PUSH);
    }

    private void compileArrayAccess(Expression expression, Expression expression1) {
        compileExpression(expression);

        compileExpression(expression1);

        emit(Instruction.Code.VECTOR_ACCESS);
    }

    private void compileArrayType(ArrayType arrayType) {
        compilePrimitiveType(arrayType.getBaseType());
        emit(Instruction.Code.VECTOR_CONSTRUCT, 1);
    }
    private void compileUnaryOperation(Expression expr, Instruction.Code opcode) {
        compileExpression(expr.getOperands().get(0));
        instructions.add(new Instruction(opcode));
    }
    private void compileLiteralExpression(LiteralExpression literalExpr) {
        emit(Instruction.Code.PUSH_CONSTANT, addConstant(literalExpr.getValue()));
    }

    private void compileVariableReference(VariableReference varRef) {
        emit(Instruction.Code.GET_LOCAL, getLocalIndex(varRef.getName()));
    }

    private void compilePrimitiveType(PrimitiveType primitiveType) {
        emit(Instruction.Code.PUSH_CONSTANT, addConstant(primitiveType.getType()));
    }

    private void compileBinaryOperation(Expression expr, Instruction.Code opcode) {
        compileExpression(expr.getOperands().get(0));
        compileExpression(expr.getOperands().get(1));
        emit(opcode);
    }

    private void compileAssignment(Expression expr) {
        compileExpression(expr.getOperands().get(1));
        if (expr.getOperands().get(0) instanceof VariableReference) {
            String varName = ((VariableReference) expr.getOperands().get(0)).getName();
            emit(Instruction.Code.SET_LOCAL, getLocalIndex(varName));
        } else {
            throw new UnsupportedOperationException("Left-hand side of assignment must be a variable reference.");
        }
    }
    private void backpatch(Instruction jumpInstruction, int targetIndex) {

        assert List.of(Instruction.Code.JUMP, Instruction.Code.JUMP, Instruction.Code.JUMP_IF_FALSE)
                .contains(jumpInstruction) : "Invalid jump operation";

        int offset = targetIndex - instructions.indexOf(jumpInstruction);

        jumpInstruction.setArg1(offset);
    }

    private int getLocalIndex(String name) {
        return name.hashCode();
    }

    private int addConstant(Object value) {
        return value.hashCode();
    }

    private Instruction emit(Instruction.Code code) {
        return emit(code, 0);
    }

    private Instruction emit(Instruction.Code code, int arg1) {
        Instruction instruction = new Instruction(code, arg1);
        instructions.add(instruction);
        return instruction;
    }
}
