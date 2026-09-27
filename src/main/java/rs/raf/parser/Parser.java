package rs.raf.parser;

import calcpremium.parser.CalcPremiumParser;
import lombok.Getter;

import org.antlr.v4.runtime.*;
import rs.raf.calc.CalcPremium;

public class Parser {
    private final CalcPremium compiler;

    @Getter
    private CalcPremiumParser calculatorPremiumParser;

    public Parser(CalcPremium compiler) {
        this.compiler = compiler;
    }

    public CalcPremiumParser.StartContext getSyntaxTree(Lexer tokens) {
        CommonTokenStream tokenStream = new CommonTokenStream(tokens);
        calculatorPremiumParser = new CalcPremiumParser(tokenStream);
        calculatorPremiumParser.removeErrorListeners();
        calculatorPremiumParser.addErrorListener(compiler.errorListener());

        return calculatorPremiumParser.start();
    }

}