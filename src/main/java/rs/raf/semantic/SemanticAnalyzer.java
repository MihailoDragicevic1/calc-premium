package rs.raf.semantic;

import rs.raf.ast.*;
import rs.raf.calc.CalcPremium;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class SemanticAnalyzer{
    private final CalcPremium calc;
    private final Stack<Map<String, PrimitiveType>> scopeStack = new Stack<>();

    public SemanticAnalyzer(CalcPremium calc) {
        this.calc = calc;
    }

    public void analyze(FunctionDeclarationList functionDeclarationList) {
        //Dodaj globalni opseg(mozda nije potrebno)
        scopeStack.push(new HashMap<>());

        for (FunctionDeclaration function : functionDeclarationList.getFunDeclarations()) {
            analyzeFunction(function);
        }

        // Ukloni globalni opseg(mozda nije potrebno)
        scopeStack.pop();
    }

    private void analyzeFunction(FunctionDeclaration function) {
        scopeStack.push(new HashMap<>());

        ArgumentList argsList = function.getArgs();
        if (argsList != null) {
            for (Argument argument : argsList.getArguments()) {
                Map<String, PrimitiveType> currentScope = scopeStack.peek();

                if (currentScope.containsKey(argument.getIdentifier())) {
                    reportError(argument.getLocation(), "Duplicate argument name: %s", argument.getIdentifier());
                }
                currentScope.put(argument.getIdentifier(), argument.getType());
            }
        }
        analyzeStatementList(function.getBody(), function.getReturnType());

        scopeStack.pop();
    }

    private void analyzeStatementList(StatementList statementList, PrimitiveType expectedReturnType) {
        // Dodajemo novi opseg na stek
        scopeStack.push(new HashMap<>());

        for (Statement statement : statementList.getStmts()) {
            analyzeStatement(statement, expectedReturnType);
        }

        // Uklanjamo opseg nakon završetka analize bloka
        scopeStack.pop();
    }

    private void analyzeStatement(Statement statement, PrimitiveType expectedReturnType) {
        if (statement instanceof DeclarationStatement declStmt) {
            Map<String, PrimitiveType> currentScope = scopeStack.peek();

            if (currentScope.containsKey(declStmt.getName())) {
                reportError(declStmt.getLocation(), "Variable already declared in current scope: %s", declStmt.getName());
            } else {
                if (declStmt.getInitializer() != null) {
                    PrimitiveType initializerType = analyzeExpression(declStmt.getInitializer());

                    PrimitiveType expectedType = mapTypeToPrimitiveType(declStmt.getType(), declStmt.getLocation());
                    if (!typesMatch(expectedType, initializerType)) {
                        reportError(declStmt.getLocation(),
                                "Type mismatch in variable initialization. Expected: %s, Found: %s",
                                expectedType.getType().getStringRepresentation(),
                                initializerType.getType().getStringRepresentation());
                    }
                }
                currentScope.put(declStmt.getName(), mapTypeToPrimitiveType(declStmt.getType(), declStmt.getLocation()));
            }
        } else if (statement instanceof ReturnStatement returnStmt) {
            PrimitiveType returnType = analyzeExpression(returnStmt.getValue());
            if (!typesMatch(expectedReturnType, returnType)) {
                reportError(returnStmt.getLocation(), "Return type mismatch. Expected: %s, Found: %s",
                        expectedReturnType.getType().getStringRepresentation(),
                        returnType.getType().getStringRepresentation());
            }
        } else if (statement instanceof IfStatement ifStmt) {
            PrimitiveType conditionType = analyzeExpression(ifStmt.getCondition());
            if (!isBoolean(conditionType)) {
                reportError(ifStmt.getCondition().getLocation(), "Condition in 'if' statement must be of type boolean. Found: %s",
                        conditionType.getType().getStringRepresentation());
            }
            analyzeStatementList(ifStmt.getIfBlock(), expectedReturnType);
            if (ifStmt.getElseBlock() != null) {
                analyzeStatementList(ifStmt.getElseBlock(), expectedReturnType);
            }
        } else if (statement instanceof WhileStatement whileStmt) {
            PrimitiveType conditionType = analyzeExpression(whileStmt.getCondition());
            if (!isBoolean(conditionType)) {
                reportError(whileStmt.getCondition().getLocation(), "Condition in 'while' statement must be of type boolean. Found: %s",
                        conditionType.getType().getStringRepresentation());
            }
            analyzeStatementList(whileStmt.getBlock(), expectedReturnType);
        } else if (statement instanceof ForStatement forStmt) {
            if (forStmt.getDeclaration() != null) {
                analyzeStatement(forStmt.getDeclaration(), expectedReturnType);
            }
            if (forStmt.getCondition() != null) {
                PrimitiveType conditionType = analyzeExpression(forStmt.getCondition());
                if (!isBoolean(conditionType)) {
                    reportError(forStmt.getCondition().getLocation(), "Condition in 'for' statement must be of type boolean. Found: %s",
                            conditionType.getType().getStringRepresentation());
                }
            }
            if (forStmt.getIncrementExpression() != null) {
                analyzeStatement(forStmt.getIncrementExpression(), expectedReturnType);
            }
            analyzeStatementList(forStmt.getBody(), expectedReturnType);
        } else if (statement instanceof PrintStatement printStmt) {
            for (Expression arg : printStmt.getArgs()) {
                analyzeExpression(arg);
            }
        } else if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {

        }
    }

    private PrimitiveType analyzeExpression(Expression expression) {

        if (expression instanceof VariableReference varRef) {
            return findVariable(varRef.getName(), varRef.getLocation());
        } else if (expression instanceof LiteralExpression literalExpr) {
            if (literalExpr.getValue() instanceof Integer) {
                return new PrimitiveType(literalExpr.getLocation(), Type.INT);
            } else if (literalExpr.getValue() instanceof Boolean) {
                return new PrimitiveType(literalExpr.getLocation(), Type.BOOL);
            } else {
                reportError(literalExpr.getLocation(), "Unsupported literal type: %s",
                        literalExpr.getValue().getClass().getSimpleName());
                return new PrimitiveType(literalExpr.getLocation(), Type.VOID);
            }

        } else if (expression instanceof ExpressionList exprList) {
            for (Expression expr : exprList.getExpressions()) {
                analyzeExpression(expr);
            }
            return new PrimitiveType(expression.getLocation(), Type.VOID);
        } else if (expression instanceof Expression complexExpr) {
            switch (complexExpr.getOperation()) {
                case ADD, SUB, MUL, DIV, MOD, POW -> {
                    PrimitiveType leftType = analyzeExpression(complexExpr.getOperands().get(0));
                    PrimitiveType rightType = analyzeExpression(complexExpr.getOperands().get(1));
                    if (!isNumeric(leftType) || !isNumeric(rightType)) {
                        reportError(complexExpr.getLocation(), "Arithmetic operations require numeric types. Found: %s and %s",
                                leftType.getType().getStringRepresentation(), rightType.getType().getStringRepresentation());
                    }
                    return leftType;
                }
                case LOGICAL_AND, LOGICAL_OR -> {
                    PrimitiveType leftType = analyzeExpression(complexExpr.getOperands().get(0));
                    PrimitiveType rightType = analyzeExpression(complexExpr.getOperands().get(1));
                    if (!isBoolean(leftType) || !isBoolean(rightType)) {
                        reportError(complexExpr.getLocation(), "Logical operations require boolean types. Found: %s and %s",
                                leftType.getType().getStringRepresentation(), rightType.getType().getStringRepresentation());
                    }
                    return new PrimitiveType(complexExpr.getLocation(), Type.BOOL);
                }
                case EQUALS, NOT_EQUALS, LESS_THAN, LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL -> {
                    PrimitiveType leftType = analyzeExpression(expression.getOperands().get(0));
                    PrimitiveType rightType = analyzeExpression(expression.getOperands().get(1));
                    if (!typesMatch(mapTypeToPrimitiveType(leftType.getType(),leftType.getLocation()),mapTypeToPrimitiveType(rightType.getType(),rightType.getLocation()))){
                        reportError(expression.getLocation(),
                                "Comparison requires matching types. Found: %s and %s",
                                leftType.getType().getStringRepresentation(),
                                rightType.getType().getStringRepresentation());
                    }
                    return new PrimitiveType(expression.getLocation(), Type.BOOL);
                }
                case NEGATE, LOGICAL_NOT -> {
                    PrimitiveType operandType = analyzeExpression(expression.getOperands().get(0));
                    if (expression.getOperation() == Expression.Operation.NEGATE && !isNumeric(mapTypeToPrimitiveType(operandType.getType(),operandType.getLocation()))) {
                        reportError(expression.getLocation(),
                                "Negation requires numeric type. Found: %s",
                                operandType.getType().getStringRepresentation());
                    } else if (expression.getOperation() == Expression.Operation.LOGICAL_NOT && !isBoolean(mapTypeToPrimitiveType(operandType.getType(),operandType.getLocation()))) {
                        reportError(expression.getLocation(),
                                "Logical NOT requires boolean type. Found: %s",
                                operandType.getType().getStringRepresentation());
                    }
                    return operandType;
                }
                case ASSIGN -> {
                    PrimitiveType leftType = analyzeExpression(expression.getOperands().get(0));
                    PrimitiveType rightType = analyzeExpression(expression.getOperands().get(1));
                    if (!typesMatch(mapTypeToPrimitiveType(leftType.getType(),leftType.getLocation()),mapTypeToPrimitiveType(rightType.getType(),rightType.getLocation()))) {
                        reportError(expression.getLocation(),
                                "Assignment requires matching types. Found: %s and %s",
                                leftType.getType().getStringRepresentation(),
                                rightType.getType().getStringRepresentation());
                    }
                    return leftType;
                }
                case INDEX -> {
                    PrimitiveType arrayType = analyzeExpression(expression.getOperands().get(0));
                    PrimitiveType indexType = analyzeExpression(expression.getOperands().get(1));
                    if (!isIndexable(arrayType.getType())) {
                        reportError(expression.getLocation(),
                                "Indexing requires an array type. Found: %s",
                                arrayType.getType().getStringRepresentation());
                    }
                    if (!isInteger(indexType.getType())) {
                        reportError(expression.getLocation(),
                                "Array index must be an integer. Found: %s",
                                indexType.getType().getStringRepresentation());
                    }
                    return new PrimitiveType(expression.getLocation(), getElementType(arrayType.getType()));
                }
                default -> {
                    reportError(complexExpr.getLocation(), "Unsupported operation: %s", complexExpr.getOperation().getLabel());
                    return new PrimitiveType(complexExpr.getLocation(), Type.VOID);
                }
            }
        }
        return new PrimitiveType(expression.getLocation(), Type.VOID);
    }

    private PrimitiveType findVariable(String name, Location location) {
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            Map<String, PrimitiveType> scope = scopeStack.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        reportError(location, "Undefined variable: %s", name);
        return new PrimitiveType(location, Type.VOID);
    }

    private boolean isIndexable(Type type) {
        return type == Type.ARR;
    }
    private boolean typesMatch(PrimitiveType expected, PrimitiveType actual) {
        return expected.getType() == actual.getType();
    }

    private boolean isBoolean(PrimitiveType type) {
        return type.getType() == Type.BOOL;
    }

    private boolean isNumeric(PrimitiveType type) {
        return type.getType() == Type.INT;
    }
    private boolean isInteger(Type type) {
        return type == Type.INT;
    }
    private void reportError(Location location, String message, Object... args) {
        calc.error(location, message, args);
    }

    private PrimitiveType mapTypeToPrimitiveType(Type type, Location location) {
        return new PrimitiveType(location, type);
    }
    private Type getElementType(Type arrayType) {
        if (arrayType == Type.ARR) {
            return Type.INT;
        } else {
            throw new IllegalArgumentException("Type is not indexable: " + arrayType.getStringRepresentation());
        }
    }
}