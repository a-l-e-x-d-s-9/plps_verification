package compiler

import fr.uga.pddl4j.parser.Connective
import fr.uga.pddl4j.parser.Exp
import fr.uga.pddl4j.parser.Symbol
import fr.uga.pddl4j.parser.TypedSymbol

import java.lang.reflect.Type

class PDDLCompilerTest extends GroovyTestCase {
    void setUp() {
        super.setUp()

    }

    void tearDown() {

    }

    void testCreateKnowExp() {
        Exp exp = new Exp(Connective.AND);
        List<Symbol> symbols;
        Exp e1 = new Exp(Connective.NOT);
        Exp e11 = new Exp(Connective.ATOM);
        symbols = new LinkedList<>();
        symbols.add(new Symbol(Symbol.Kind.PREDICATE, "pred1"));
        symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?gw"));
        symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?par2"));
        symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?par3"));
        e11.setAtom(symbols);
        Exp fe11 = new Exp(Connective.FORALL);
        List<TypedSymbol> lst = new LinkedList<>();
        lst.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE,"?gw")));
        fe11.setVariables(lst);
        fe11.addChild(e11);

        e1.addChild(fe11);

        Exp e2 = new Exp(Connective.ATOM);
        symbols = new LinkedList<>();
        symbols.add(new Symbol(Symbol.Kind.PREDICATE, "pred2"));
        symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?par1"));
        symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?par2"));
        e2.setAtom(symbols);

        exp.addChild(e1);
        exp.addChild(e2);
        println PDDLCompiler.createKnowExp(exp).toString();
    }
}
