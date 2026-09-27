package rs.raf.lexer;

import calcpremium.parser.CalcPremiumLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import rs.raf.calc.CalcPremium;

public class Scanner {
    private final CalcPremium compiler;

    public Scanner(CalcPremium compiler) {
        this.compiler = compiler;
    }

    public Lexer getTokens(CharStream chars) {
        var lex = new CalcPremiumLexer(chars);
        lex.removeErrorListeners();
        lex.addErrorListener(compiler.errorListener());
        return lex;
    }
}