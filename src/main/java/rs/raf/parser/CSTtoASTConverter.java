package rs.raf.parser;

import calcpremium.parser.CalcPremiumLexer;
import calcpremium.parser.CalcPremiumParser;
import calcpremium.parser.CalcPremiumVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;
import rs.raf.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSTtoASTConverter extends AbstractParseTreeVisitor<Tree> implements CalcPremiumVisitor<Tree> {
    @Override
    public Tree visitStart(CalcPremiumParser.StartContext ctx) {
        var funDeclarations = ctx.topLevel()
                .stream()
                .map(this::visit)
                .map(x -> (FunctionDeclaration) x)
                .collect(Collectors.toCollection(ArrayList::new));

        return new FunctionDeclarationList(getLocation(ctx), funDeclarations);
    }

    @Override
    public Tree visitTopLevel(CalcPremiumParser.TopLevelContext ctx) {
        if (ctx.defineFunction() != null) {
            return visit(ctx.defineFunction());
        } else {
            throw new AssertionError ("Function Definition not found");
        }
    }

    @Override
    public Tree visitDefineFunction(CalcPremiumParser.DefineFunctionContext ctx) {
        var name = ctx.IDENTIFIER().getText();
        var args = (ArgumentList) visitArgumentList(ctx.argumentList());
        var returnType = (PrimitiveType) visit(ctx.returnType);
        var declLoc = getLocation(ctx.start).span(getLocation(ctx.returnType.start));

        StatementList body = null;
        if (ctx.body != null) {
            body = (StatementList) visit(ctx.body);
        }

        return new FunctionDeclaration(declLoc, args, name, returnType, body);
    }

    @Override
    public Tree visitArgumentList(CalcPremiumParser.ArgumentListContext ctx) {
        // Visit each argument in the list and collect the results into a list
        var arguments = ctx.argument()
                .stream()
                .map(this::visit)
                .map(x -> (Argument) x) // Cast each visited result to an Argument
                .collect(Collectors.toCollection(ArrayList::new));

        // Create an ArgumentList node from the collected arguments
        return new ArgumentList(getLocation(ctx), arguments);
    }
    @Override
    public Tree visitArgument(CalcPremiumParser.ArgumentContext ctx) {
        // Extract the type and identifier for the argument
        PrimitiveType type = (PrimitiveType) visit(ctx.typeId());
        String identifier = ctx.IDENTIFIER().getText();

        // Create and return an Argument node
        return new Argument(getLocation(ctx), type, identifier);
    }

    @Override
    public Tree visitStatementList(CalcPremiumParser.StatementListContext ctx) {
        // Collect all declaring statements into a list
        var stmts = ctx.statement()
                .stream()
                .map(this::visit)
                .map(x -> (Statement) x) // Assuming each `declaringStmt` maps to a `Statement`
                .toList();

        // Return a StatementList node
        return new StatementList(getLocation(ctx), stmts);
    }

    @Override
    public Tree visitStatement(CalcPremiumParser.StatementContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public Tree visitDeclareStatement(CalcPremiumParser.DeclareStatementContext ctx) {
        var type =  typeBuilder(ctx.typeId().getText());
        var name = ctx.IDENTIFIER().getText();
        var value = (Expression) visit(ctx.value);

        return new DeclarationStatement(getLocation(ctx), type, name, value);
    }

    @Override
    public Tree visitReturnStatement(CalcPremiumParser.ReturnStatementContext ctx) {
        // Collect return expression
        var exp = (Expression) visit(ctx.expression());
        return new ReturnStatement(getLocation(ctx.RETURN()), exp);
    }

    @Override
    public Tree visitLoopControlStatement(CalcPremiumParser.LoopControlStatementContext ctx) {
        if (ctx.BREAK() != null) {
            return new BreakStatement(getLocation(ctx.BREAK()));
        }

        return new ContinueStatement(getLocation(ctx.CONTINUE()));
    }

    @Override
    public Tree visitNullStatement(CalcPremiumParser.NullStatementContext ctx) {
        return new NullStatement(getLocation(ctx));
    }

    @Override
    public Tree visitIfStatement(CalcPremiumParser.IfStatementContext ctx) {
        Expression condition = (Expression) visit(ctx.expression());
        StatementList ifBlock = (StatementList) visit(ctx.then);
        StatementList elseBlock = (StatementList) visit(ctx.otherwise);
        return new IfStatement(getLocation(ctx), condition, ifBlock, elseBlock);
    }

    @Override
    public Tree visitWhileStatement(CalcPremiumParser.WhileStatementContext ctx) {
        Expression condition = (Expression) visit(ctx.expression());
        StatementList block = (StatementList) visit(ctx.body);
        return new WhileStatement(getLocation(ctx), condition, block);
    }

    @Override
    public Tree visitForStatement(CalcPremiumParser.ForStatementContext ctx) {
        DeclarationStatement declaration = (DeclarationStatement) visit(ctx.declareStatement());
        Expression condition = (Expression) visit(ctx.expression());
        ExpressionStatement incrementExpr = (ExpressionStatement) visit(ctx.assignment());
        StatementList block = (StatementList) visit(ctx.body);

        return new ForStatement(getLocation(ctx), declaration, condition, incrementExpr, block);
    }

    @Override
    public Tree visitAssignment(CalcPremiumParser.AssignmentContext ctx) {
        // Visit the left-hand side (target)
        var lhs = (Expression) visit(ctx.expression(0));
        var location = getLocation(ctx.expression(0));

        if (ctx.expression().size() > 1) {
            // Handle full assignment with left-hand side and right-hand side
            var rhs = (Expression) visit(ctx.expression(1));
            location = location.span(getLocation(ctx.expression(1)));

            // Return an assignment operation expression
            return new ExpressionStatement(location, lhs, rhs);
        } else {
            // Handle standalone expression as a statement (e.g., `x;`)
            return new ExpressionStatement(location, lhs, null);
        }
    }

    @Override
    public Tree visitExpression(CalcPremiumParser.ExpressionContext ctx) {
        return (Expression) visit(ctx.orExpression());
    }

    @Override
    public Tree visitOrExpression(CalcPremiumParser.OrExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            Expression.Operation exprOp;
            if (op.getType() == CalcPremiumLexer.LOGICAL_OR) {
                exprOp = Expression.Operation.LOGICAL_OR;
            } else {
                throw new IllegalArgumentException("unhandled expr op " + op);
            }
            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitAndExpression(CalcPremiumParser.AndExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            Expression.Operation exprOp;
            if (op.getType() == CalcPremiumLexer.LOGICAL_AND) {
                exprOp = Expression.Operation.LOGICAL_AND;
            } else {
                throw new IllegalArgumentException("unhandled expr op " + op);
            }
            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitCompareExpression(CalcPremiumParser.CompareExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case CalcPremiumLexer.EQ -> Expression.Operation.EQUALS;
                case CalcPremiumLexer.NEQ -> Expression.Operation.NOT_EQUALS;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitRelationalExpression(CalcPremiumParser.RelationalExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case CalcPremiumLexer.LT -> Expression.Operation.LESS_THAN;
                case CalcPremiumLexer.LTE -> Expression.Operation.LESS_THAN_EQUAL;
                case CalcPremiumLexer.GT -> Expression.Operation.GREATER_THAN;
                case CalcPremiumLexer.GTE -> Expression.Operation.GREATER_THAN_EQUAL;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitAdditionExpression(CalcPremiumParser.AdditionExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case CalcPremiumLexer.PLUS -> Expression.Operation.ADD;
                case CalcPremiumLexer.MINUS -> Expression.Operation.SUB;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitMultiplicationExpression(CalcPremiumParser.MultiplicationExpressionContext ctx) {
        var value = (Expression) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expression) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
                case CalcPremiumLexer.STAR -> Expression.Operation.MUL;
                case CalcPremiumLexer.DIVIDE -> Expression.Operation.DIV;
                case CalcPremiumLexer.MODUO -> Expression.Operation.MOD;
                default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expression(loc, exprOp, List.of(value, rhs));
        }
        return value;
    }

    @Override
    public Tree visitUnaryExpression(CalcPremiumParser.UnaryExpressionContext ctx) {
        // Get the operator and the value expression
        var operator = ctx.unaryOp;
        var value = (Expression) visit(ctx.unarySuffix());

        if (operator != null) {
            // Map the operator to its corresponding operation
            var operation = switch (operator.getType()) {
                case CalcPremiumParser.MINUS -> Expression.Operation.NEGATE;
                case CalcPremiumParser.LOGICAL_NOT -> Expression.Operation.LOGICAL_NOT;
                default -> throw new AssertionError("Unknown unary operator: " + operator.getText());
            };

            // Return the unary expression
            return new Expression(
                    getLocation(ctx).span(getLocation(operator)),
                    operation,
                    List.of(value)
            );
        }

        // Return the value directly if no operator
        return value;
    }

    private Expression currExpr;
    @Override
    public Tree visitUnarySuffix(CalcPremiumParser.UnarySuffixContext ctx) {
        // Visit the initial term
        var expression = (Expression) visit(ctx.term());
        currExpr = expression;
        // Process each unary suffix operator
        for (var suffix : ctx.unarySuffixOp()) {
            expression = (Expression) visit (suffix);;
        }

        return expression;
    }

    @Override
    public Tree visitFuncall(CalcPremiumParser.FuncallContext ctx) {
        // Base expression (function being called)
        assert ctx.args != null; // This can be removed or replaced with a null check

        List<Expression> args;

        // Check if args is not null and has expressions
        args = ctx.args.expression()
                .stream()
                .map(this::visit)
                .map(x -> (Expression) x)
                .toList();

        // Construct a FunctionCallExpression
        return new Expression(
                getLocation(ctx),
                Expression.Operation.FUNCALL,
                args
        );
    }

    @Override
    public Tree visitArrIdx(CalcPremiumParser.ArrIdxContext ctx) {
        // Base expression (array being accessed)
        var base = currExpr;

        // Index expression
        var index = (Expression) visit(ctx.index);

        // Construct an array index expression
        return new Expression(
                getLocation(ctx),
                Expression.Operation.INDEX,
                List.of(base, index)
        );
    }

    @Override
    public Tree visitArrayLen(CalcPremiumParser.ArrayLenContext ctx) {
        // Base expression (array whose length is being queried)
        var base = currExpr;

        // Construct an array length expression
        return new Expression(
                getLocation(ctx),
                Expression.Operation.LENGTH,
                List.of(base)
        );
    }

    @Override
    public Tree visitArrayPush(CalcPremiumParser.ArrayPushContext ctx) {
        // Base expression (array to which elements are being added)
        var base = currExpr;

        // Construct an array push expression
        return new Expression(
                getLocation(ctx),
                Expression.Operation.PUSH,
                List.of(base)
        );
    }

    @Override
    public Tree visitTerm(CalcPremiumParser.TermContext ctx) {
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.varRef() != null) {
            return visit(ctx.varRef());
        } else if (ctx.arrayCollect() != null) {
            return visit(ctx.arrayCollect());
        } else if (ctx.expression() != null) {
            return visit(ctx.expression());
        }
        throw new IllegalArgumentException("Unhandled term type: " + ctx.getText());
    }

    @Override
    public Tree visitVarRef(CalcPremiumParser.VarRefContext ctx) {
        return new VariableReference(
                getLocation(ctx),
                ctx.IDENTIFIER().getText()
        );
    }

    @Override
    public Tree visitExpressionList(CalcPremiumParser.ExpressionListContext ctx) {
        var location = getLocation(ctx);

        // Handle empty or null expressions
        List<Expression> expressions = ctx.expression().isEmpty()
                ? List.of()
                : ctx.expression().stream()
                .map(expr -> (Expression) visit(expr))
                .toList();

        return new ExpressionList(location, expressions);
    }

    @Override
    public Tree visitArrayType(CalcPremiumParser.ArrayTypeContext ctx) {
        var baseType = (PrimitiveType) (visit(ctx.typeId()));
        return new ArrayType(
                getLocation(ctx),
                baseType
        );
    }

    @Override
    public Tree visitArrayCollect(CalcPremiumParser.ArrayCollectContext ctx) {
        return null;
    }

    @Override
    public Tree visitTypeId(CalcPremiumParser.TypeIdContext ctx) {
        if (ctx.INT_TYPE() != null) {
            return new PrimitiveType(getLocation(ctx), typeBuilder(ctx.INT_TYPE().getText()));
        } else if (ctx.BOOL_TYPE() != null) {
            return new PrimitiveType(getLocation(ctx), typeBuilder(ctx.BOOL_TYPE().getText()));
        } else if (ctx.VOID_TYPE() != null) {
            return new PrimitiveType(getLocation(ctx), typeBuilder(ctx.VOID_TYPE().getText()));
        } else if (ctx.CHAR_TYPE() != null) {
            return new PrimitiveType(getLocation(ctx), typeBuilder(ctx.CHAR_TYPE().getText()));
        } else if (ctx.STRING_TYPE() != null) {
            return new PrimitiveType(getLocation(ctx), typeBuilder(ctx.STRING_TYPE().getText()));
        } else if (ctx.arrayType() != null) {
            return visit(ctx.arrayType());
        }
        throw new IllegalArgumentException("Unhandled typeId: " + ctx.getText());
    }

    @Override
    public Tree visitLiteral(CalcPremiumParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) {
            return new LiteralExpression(
                    getLocation(ctx),
                    Integer.parseInt(ctx.NUMBER().getText())
            );
        } else if (ctx.TRUE() != null) {
            return new LiteralExpression(
                    getLocation(ctx),
                    true
            );
        } else if (ctx.FALSE() != null) {
            return new LiteralExpression(
                    getLocation(ctx),
                    false
            );
        } else if (ctx.CHAR() != null) {
            return new LiteralExpression(
                    getLocation(ctx),
                    ctx.CHAR().getText().charAt(1)
            );
        } else if (ctx.STRING() != null) {
            return new LiteralExpression(
                    getLocation(ctx),
                    ctx.STRING().getText()
            );
        }
        throw new IllegalArgumentException("Unhandled literal type: " + ctx.getText());
    }

    /* Helpers.  */
    /** Returns the range that this subtree is in.  */
    private static Location getLocation(ParserRuleContext context) {
        return getLocation(context.getStart())
                .span(getLocation(context.getStop ()));
    }

    /** Returns the location this terminal is in.  */
    private static Location getLocation(TerminalNode term) {
        return getLocation(term.getSymbol());
    }

    /** Returns the location this token is in.  */
    private static Location getLocation(Token token) {
        /* The token starts at the position ANTLR provides us.  */
        var start = new Position(token.getLine(), token.getCharPositionInLine());

        /* But it does not provide a convenient way to get where it ends, so we
           have to calculate it based on length.  */
        assert !token.getText ().contains ("\n")
                : "CSTtoASTConverter assumes single-line tokens";
        var length = token.getText ().length ();
        assert length > 0;

        /* And then put it together.  */
        var end = new Position (start.line (), start.column () + length - 1);
        return new Location (start, end);
    }

    private Type typeBuilder(String type) {
        return switch (type.toLowerCase()) {
            case "int" -> Type.INT;
            case "bool" -> Type.BOOL;
            case "void" -> Type.VOID;
            case "char" -> Type.CHAR;
            case "string" -> Type.STRING;
            case "arr" -> Type.ARR;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}