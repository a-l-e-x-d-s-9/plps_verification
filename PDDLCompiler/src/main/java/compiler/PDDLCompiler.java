package compiler;

import conditions.*;
import conditions.Condition;
import effects.*;
import fr.uga.pddl4j.parser.*;
import modules.*;
import plpEtc.Predicate;
import plpFields.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PDDLCompiler {

    public enum CompilerPrompts {
        NO_PROMPTS, ALL_PROMPTS, SOME_PROMPTS
    }
    public enum Mode {
        NEAR_FULLY_OBSERVABLE, PARTIALLY_OBSERVABLE
    }

    private static String domainName = "PLPDomain";
    private static String problemName = "PLPDomain_problem";

    private static List<AchievePLP> achievePLPs;
    private static List<ObservePLP> observePLPs;
    private static List<MaintainPLP> maintainPLPs;
    private static List<DetectPLP> detectPLPs;
    public static List<PLPParameter> observableValues;
    public static List<Effect> possibleEffects;

    public static Logger logger;

    public static CompilerPrompts prompts = CompilerPrompts.NO_PROMPTS;
    public static Mode compilerMode;

    public static List<RequireKey> requirements;

    public static PLP currentPLP;
    public static List<TypedSymbol> currentPDDLParameters;

    public static Map<String, Boolean> assumptions;

    public static Map<String, Integer> predicates;

    static void setAchievePLPs(List<AchievePLP> achievePLPs) {
        PDDLCompiler.achievePLPs = achievePLPs;
    }

    static void setObservePLPs(List<ObservePLP> observePLPs) { PDDLCompiler.observePLPs = observePLPs; }

    static void setMaintainPLPs(List<MaintainPLP> maintainPLPs) { PDDLCompiler.maintainPLPs = maintainPLPs; }

    static void setDetectPLPs(List<DetectPLP> detectPLPs) {
        PDDLCompiler.detectPLPs = detectPLPs;
    }

    static String[] producePDDL() {
        logger = Logger.getLogger("PLP->PDDL Logger");
        observableValues = new LinkedList<>();
        possibleEffects = new LinkedList<>();
        requirements = new LinkedList<>();
        assumptions = new HashMap<>();
        predicates = new HashMap<>();

        String[] resultFiles = new String[2];
        loadObservableAndEffects();

        // Generate domain file
        Domain domain = new Domain(new Symbol(Symbol.Kind.DOMAIN, domainName));

        for (AchievePLP aPLP : achievePLPs) {
            currentPLP = aPLP;
            domain.addOperator(compile(aPLP));
        }

        for (ObservePLP oPLP : observePLPs) {
            if (oPLP.isGoalParameter()) {
                currentPLP = oPLP;
                domain.addOperator(compile(oPLP));
            }
            else if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                currentPLP = oPLP;
                domain.addOperator(compile(oPLP));
            }
        }

        for (MaintainPLP mPLP : maintainPLPs) {
            if (!mPLP.isInitiallyTrue()) {
                currentPLP = mPLP;
                domain.addOperator(compile(mPLP));
            }
        }

        for (DetectPLP dPLP : detectPLPs) {
            currentPLP = dPLP;
            domain.addOperator(compile(dPLP));
        }

        // Load requirements into domain
        requirements.forEach(domain::addRequirement);

        // Edited PDDL4J to print OBJECT in types

        for (String pred_name : predicates.keySet()) {
            NamedTypedList ntl = new NamedTypedList(new Symbol(Symbol.Kind.PREDICATE,pred_name));
            for (int i=1; i<=predicates.get(pred_name); i++) {
                ntl.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE,"?par"+i)));
            }
            domain.addPredicate(ntl);
        }

        resultFiles[0] = domain.toString();

        // Generate problem file

        Problem problem = new Problem(new Symbol(Symbol.Kind.PROBLEM,problemName));
        requirements.forEach(problem::addRequirement);
        problem.setDomain(new Symbol(Symbol.Kind.DOMAIN,domainName));

        int maxVariables = 0;
        for (String pred_name : predicates.keySet()) {
            Exp exp = new Exp(Connective.ATOM);
            List<Symbol> tempSymbols = new LinkedList<>();
            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE,pred_name));
            for (int i=1; i<=predicates.get(pred_name); i++) {
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,"?example"+i));
                maxVariables = (i > maxVariables ? i : maxVariables);
            }
            exp.setAtom(tempSymbols);
            problem.addInitialFact(exp);
        }
        for (int i=0; i<maxVariables; i++) {
            problem.addObject(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE,"example"+(i+1))));
        }

        resultFiles[1] = problem.toString();

        return resultFiles;
    }

    private static void loadObservableAndEffects() {
    /* Get all possible effects and observable parameters */
        for (ObservePLP oPLP : observePLPs) {
            if (oPLP.isGoalParameter()) {
                observableValues.add((PLPParameter) oPLP.getGoal());
                possibleEffects.addAll(oPLP.getSideEffects());
            }
            else if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                possibleEffects.addAll(oPLP.getSideEffects());
            }
        }

        for (AchievePLP aPLP : achievePLPs) {
            possibleEffects.add(aPLP.getGoal().createProperEffect());
            possibleEffects.addAll(aPLP.getSideEffects());
        }

        for (MaintainPLP mPLP : maintainPLPs) {
            if (!mPLP.isInitiallyTrue()) {
                possibleEffects.add(mPLP.getMaintainedCondition().createProperEffect());
                possibleEffects.addAll(mPLP.getSideEffects());
            }
        }

        for (DetectPLP dPLP : detectPLPs) {
            possibleEffects.add(dPLP.getGoal().createProperEffect());
            possibleEffects.addAll(dPLP.getSideEffects());
        }
    }

    private static Op compile(PLP plp) {

        currentPDDLParameters = new LinkedList<>();
        Exp pddlpreconds = getPreconditions(plp);
        Exp pddleffects = getEffects(plp);

        // Finished loading preconditions and effects. Now building parameters and loading predicates
        loadParamsAndPreds(pddlpreconds);
        loadParamsAndPreds(pddleffects);

        return new Op(new Symbol(Symbol.Kind.ACTION,plp.getBaseName()), currentPDDLParameters, pddlpreconds, pddleffects);
    }

    private static Exp getEffects(PLP plp) {

        Exp pddleffects = new Exp(Connective.AND);

        if (plp.getClass().isAssignableFrom(ObservePLP.class)) {
            /* Add KNOW effect */
            ObservePLP oPLP = (ObservePLP) plp;
            if (oPLP.isGoalParameter()) {
                PLPParameter goal = ((PLPParameter) oPLP.getGoal());
                Exp knowEffect = generateKVPred(goal.toString());
                pddleffects.addChild(knowEffect);
            }
            else {
                List<Exp> senseEffs = getCondSensingEffs((Condition) oPLP.getGoal());
                for (Exp senseEff : senseEffs) {
                    pddleffects.addChild(senseEff);
                }
            }
        }
        else if (plp.getClass().isAssignableFrom(AchievePLP.class)) {
            AchievePLP aPLP = (AchievePLP) plp;
            Exp compiledGoal = compile(aPLP.getGoal().createProperEffect());
            if (compiledGoal != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                    List<Exp> kCondEffects = getKcondEffs(compiledGoal);
                    for (Exp kEff : kCondEffects) {
                        pddleffects.addChild(kEff);
                    }
                }
                else
                    pddleffects.addChild(compiledGoal);
            }
        }
        else if (plp.getClass().isAssignableFrom(MaintainPLP.class)) {
            MaintainPLP mPLP = (MaintainPLP) plp;
            Exp compiledMaintainedEffect = compile(mPLP.getMaintainedCondition().createProperEffect());
            if (compiledMaintainedEffect != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                    List<Exp> kCondEffects = getKcondEffs(compiledMaintainedEffect);
                    for (Exp kEff : kCondEffects) {
                        pddleffects.addChild(kEff);
                    }
                }
                else
                    pddleffects.addChild(compiledMaintainedEffect);
            }
        }
        else if (plp.getClass().isAssignableFrom(DetectPLP.class)) {
            DetectPLP dPLP = (DetectPLP) plp;
            Exp compiledGoal = compile(dPLP.getGoal().createProperEffect());
            if (compiledGoal != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                    List<Exp> kCondEffects = getKcondEffs(compiledGoal);
                    for (Exp kEff : kCondEffects) {
                        pddleffects.addChild(kEff);
                    }
                }
                else
                    pddleffects.addChild(compiledGoal);
            }
        }

        for (Effect se: plp.getSideEffects()){
            Exp compiledSE = compile(se);
            if (compiledSE != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                    List<Exp> kCondEffects = getKcondEffs(compiledSE);
                    for (Exp kEff : kCondEffects) {
                        pddleffects.addChild(kEff);
                    }
                }
                else
                    pddleffects.addChild(compiledSE);
            }
        }

        if (pddleffects.getChildren().size() == 1)  {
            pddleffects = pddleffects.getChildren().get(0);
        }
        return pddleffects;
    }

    private static Exp getPreconditions(PLP plp) {

        Exp pddlprecond = new Exp(Connective.AND);

        /* Add regular preconditions from the PLP */
        for (Condition cond : plp.getPreConditions()) {
            // If the condition is a predicate, add it
            Exp compiledCond = compile(cond);
            if (compiledCond != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE)
                    pddlprecond.addChild(createKnowPair(compiledCond));
                else
                    pddlprecond.addChild(compiledCond);
            }
        }
        for (Condition cond : plp.getConcurrencyConditions()) {
            // If the condition is a predicate, add it
            Exp compiledCond = compile(cond);
            if (compiledCond != null) {
                if (compilerMode == Mode.PARTIALLY_OBSERVABLE)
                    pddlprecond.addChild(createKnowPair(compiledCond));
                else
                    pddlprecond.addChild(compiledCond);
            }
        }

        /* Add KNOW VALUE preconditions */
        for (PLPParameter param : plp.getInputParams()) {
            for (ObservationGoal og : observableValues) {
                if (og.getClass().isAssignableFrom(PLPParameter.class) && og.containsParam(param.getName())) {
                    PLPParameter goal = ((PLPParameter) og);
                    pddlprecond.addChild(generateKVPred(goal.toString()));
                    break;
                }
            }
        }

        if (pddlprecond.getChildren().size() == 0)
            return new Exp(Connective.TRUE);
        if (pddlprecond.getChildren().size() == 1)
            return pddlprecond.getChildren().get(0);
        return pddlprecond;
    }


    private static boolean canBeEffected(Condition cond) {
        for (Effect eff : possibleEffects) {
            if (cond.sharesParams(eff)) {
                return true;
            }
        }
        return false;
    }


    private static void loadParamsAndPreds(Exp exp) {
        loadParamsAndPreds(exp, new LinkedList<>());
    }


    private static void loadParamsAndPreds(Exp exp, List<TypedSymbol> excludeParams) {
        if (exp.getConnective() == Connective.AND || exp.getConnective() == Connective.OR) {
            for (Exp childExp : exp.getChildren()) {
                loadParamsAndPreds(childExp, excludeParams);
            }
        }
        else if (exp.getConnective() == Connective.ATOM) {
            List<Symbol> atom = exp.getAtom();
            predicates.put(atom.get(0).getImage(),atom.size()-1);
            for (int i=1; i<atom.size(); i++) {
                boolean match = false;
                for (TypedSymbol excludets : excludeParams) {
                    if (excludets.getImage().equals(atom.get(i).getImage()))
                        match = true;
                }
                if (!match) {
                    boolean exists = false;
                    for (TypedSymbol ts : currentPDDLParameters)
                        if (ts.getImage().equals(atom.get(i).getImage()))
                            exists = true;
                    if (!exists)
                        currentPDDLParameters.add(new TypedSymbol(atom.get(i)));
                }
            }
        }
        else if (exp.getConnective() == Connective.EXISTS || exp.getConnective() == Connective.FORALL) {
            excludeParams.addAll(exp.getVariables());
            loadParamsAndPreds(exp.getChildren().get(0), excludeParams);
            excludeParams.removeAll(exp.getVariables());
        }
        else if (exp.getConnective() == Connective.NOT) {
            loadParamsAndPreds(exp.getChildren().get(0), excludeParams);
        }
        else if (exp.getConnective() == Connective.WHEN) {
            loadParamsAndPreds(exp.getChildren().get(0), excludeParams);
            loadParamsAndPreds(exp.getChildren().get(1), excludeParams);
        }
        else {
            throw new RuntimeException("Unexpected connective time while loading predicates and parameters for actions: "+exp.getConnective());
        }
    }

    public static Exp generateKVPred(String parameter) {
        Exp knowPred = new Exp(Connective.ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();

        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(parameter);
        boolean isFirstMatch = true;
        while (matcher.find()) {
            if (isFirstMatch)
                tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, "KV_" + matcher.group().toUpperCase()));
            else
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, "?"+matcher.group()));
            //sb.append((isFirstMatch ? matcher.group().toUpperCase() : matcher.group())).append(" ");
            isFirstMatch = false;
        }

        knowPred.setAtom(tempSymbols);
        return knowPred;
    }


    public static Exp compile(Formula formula) {
        if (formula.getOperator().equals("=") &&
                formula.getRightExpr().toUpperCase().equals("NULL")&&
                formula.getLeftExpr().matches(PLPParameter.PLPParameterRegex)) {

            for (PLPParameter param : observableValues) {
                if (param.containsParam(formula.getLeftExpr())) {
                    Exp notKnow = new Exp(Connective.NOT);
                    notKnow.addChild(generateKVPred(formula.getLeftExpr()));
                    return notKnow;
                }
            }

        }
        if (!formula.getOperator().equals("="))
            throw new IllegalArgumentException("Unsupported formula operator: " + formula.getOperator() + " at " + currentPLP.getBaseName());

        Exp result = new Exp(Connective.EQUAL_ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,formula.getLeftExpr()));
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,formula.getRightExpr()));
        result.setAtom(tempSymbols);

        if (!canBeEffected(formula)) {
            if (!promptCondNotEffected(formula)) {
                return null;
            }
        }
        else {
            if (!promptComplex(formula.toString())) {
                return null;
            }
        }

        return result;
    }

    public static Exp compile(NotCondition nCond) {
        if (nCond.getCondition().getClass().isAssignableFrom(QuantifiedCondition.class)) {
            if (((QuantifiedCondition) nCond.getCondition()).getQuantifier() == QuantifiedCondition.Quantifier.FORALL) {
                logger.log(Level.WARNING,"["+currentPLP.getBaseName()+"] Unsupported. PDDL doesn't allow negative quantified(forall) preconditions");
                return null;
            }
        }
        Exp result = new Exp(Connective.NOT);
        Exp childRes = compile(nCond.getCondition());

        if (childRes == null)
            return null;

        if (!promptComplex(nCond.toString(),RequireKey.NEGATIVE_PRECONDITIONS))
            return null;

        result.addChild(childRes);

        return result;
    }


    public static Exp compile(Predicate predicate) {
        Exp result = new Exp(Connective.ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, predicate.getName()));
        for (String value : predicate.getValues()) {
            tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, "?"+value));
        }
        result.setAtom(tempSymbols);

        if (!canBeEffected(predicate)) {
            logCondNotEffected(predicate, true);
        }

        return result;
    }

    public static Exp compile(QuantifiedCondition qCond) {
        boolean isForall = (qCond.getQuantifier() == QuantifiedCondition.Quantifier.FORALL);
        Exp result;
        if (isForall)
            result = new Exp(Connective.FORALL);
        else
            result = new Exp(Connective.EXISTS);

        List<TypedSymbol> quantifiedVar = new LinkedList<>();
        for (String param : qCond.getParams()) {
            quantifiedVar.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, "?"+param)));
        }
        result.setVariables(quantifiedVar);

        Exp compiledChild = compile(qCond.getCondition());
        if (compiledChild == null)
            return null;

        result.addChild(compile(qCond.getCondition()));

        if (!canBeEffected(qCond)) {
            if (!promptCondNotEffected(qCond)) {
                return null;
            }
        }
        else {
            if (!promptComplex(qCond.toString(),RequireKey.ADL)) {
                return null;
            }
        }

        return result;
    }

    public static Exp compile(BitwiseOperation cond) {
        Exp result;
        if (cond.getOperation() == BitwiseOperation.Operation.AND) {
            result = new Exp(Connective.AND);
        }
        else {
            result = new Exp(Connective.OR);
        }
        for (Condition childCond : cond.getConditions()) {
            Exp childExp = compile(childCond);
            if (childExp != null) {
                result.addChild(childExp);
            }
        }
        if (result.getChildren().size() == 0)
            return null;
        if (result.getChildren().size() == 1)
            return result.getChildren().get(0);
        return result;
    }

        public static Exp compile(Condition c) {
        if (c.getClass().isAssignableFrom(Formula.class)) {
            return compile((Formula) c);
        }
        else if (c.getClass().isAssignableFrom(Predicate.class)) {
            return compile((Predicate) c);
        }
        else if (c.getClass().isAssignableFrom(QuantifiedCondition.class)) {
            return compile((QuantifiedCondition) c);
        }
        else if (c.getClass().isAssignableFrom(NotCondition.class)) {
            return compile((NotCondition) c);
        }
        else if (c.getClass().isAssignableFrom(BitwiseOperation.class)) {
            return compile ((BitwiseOperation) c);
        }
        else {
            throw new UnsupportedOperationException("["+currentPLP.getBaseName()+"] Unsupported condition " + c + " of type " + c.getClass());
        }
    }

    public static Exp compile(Effect e) {
        if (e.getClass().isAssignableFrom(Predicate.class)) {
            return compile((Predicate) e);
        }
        else if (e.getClass().isAssignableFrom(AssignmentEffect.class)) {
            return compile((AssignmentEffect) e);
        }
        else if (e.getClass().isAssignableFrom(ForAllEffect.class)) {
            return compile((ForAllEffect) e);
        }
        else if (e.getClass().isAssignableFrom(NotEffect.class)) {
            return compile((NotEffect) e);
        }
        else if (e.getClass().isAssignableFrom(AndEffect.class)) {
            return compile((AndEffect) e);
        }
        else if (e.getClass().isAssignableFrom(ConditionalEffect.class)) {
            return compile((ConditionalEffect) e);
        }
        else {
            throw new UnsupportedOperationException("Unsupported effect " + e + " of type " + e.getClass());
        }
    }

    public static Exp compile(ConditionalEffect cEffect) {

        Exp compiledCondition = compile(cEffect.getCondition());
        Exp compiledEffect = compile(cEffect.getEffect());

        if (compiledCondition == null || compiledEffect == null)
            return null;

        if (!promptComplex(cEffect.toString(),RequireKey.CONDITIONAL_EFFECTS)) {
            return null;
        }

        Exp result = new Exp(Connective.WHEN);
        result.addChild(compiledCondition);
        result.addChild(compiledEffect);

        return result;
    }

    public static Exp compile(AssignmentEffect aEffect) {
        if (aEffect.getExpression().toUpperCase().equals("NULL")) {
            for (PLPParameter param : observableValues) {
                if (param.containsParam(aEffect.getParam().getName())) {
                    Exp notKnow = new Exp(Connective.NOT);
                    notKnow.addChild(generateKVPred(aEffect.getParam().toString()));
                    return notKnow;
                }
            }
        }
        Exp result = new Exp(Connective.EQUAL_ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, aEffect.getParam().toString()));
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, aEffect.getExpression().toString()));
        result.setAtom(tempSymbols);


        if (!promptComplex(aEffect.toString())) {
            return null;
        }

        return result;
    }

    public static Exp compile(NotEffect nEffect) {
        // Can only be not (predicate) because of XSD restriction
        Exp result = new Exp(Connective.NOT);
        Exp compiledChild = compile(nEffect.getEffect());

        if (compiledChild == null)
            return null;

        result.addChild(compiledChild);
        return result;
    }

    public static Exp compile(ForAllEffect faEffect) {
        Exp result = new Exp(Connective.FORALL);

        List<TypedSymbol> quantifiedVar = new LinkedList<>();
        for (String param : faEffect.getParams()) {
            quantifiedVar.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, "?"+param)));
        }
        result.setVariables(quantifiedVar);

        Exp compiledChild = compile(faEffect.getEffect());

        if (compiledChild == null)
            return null;

        result.addChild(compiledChild);

        if (!promptComplex(faEffect.toString(),RequireKey.ADL)) {
            return null;
        }

        return result;
    }

    public static Exp compile(AndEffect aEffect) {
        Exp andExp = new Exp(Connective.AND);
        for (Effect childEff : aEffect.getEffects()) {
            Exp childExp = compile(childEff);
            if (childExp != null) {
                andExp.addChild(childExp);
            }
        }
        if (andExp.getChildren().size() == 0)
            return null;
        if (andExp.getChildren().size() == 1)
            return andExp.getChildren().get(0);
        return andExp;
    }


    private static boolean promptCondNotEffected(Condition cond) {
        if (prompts == CompilerPrompts.NO_PROMPTS) {
            logCondNotEffected(cond,false);
            return false;
        }
        System.out.println("["+currentPLP.getBaseName()+"] Condition: " + cond.toString() + " can't be changed.\nAdd to PDDL anyway? (y/n)");
        Scanner s = new Scanner(System.in);
        if (s.nextLine().equals("y"))
            return true;
        return false;
    }

    private static boolean promptComplex(String text) {
        if (prompts == CompilerPrompts.NO_PROMPTS) {
            logCondComplex(text,false);
            return false;
        }
        System.out.println("["+currentPLP.getBaseName()+"] Condition/effect: " + text + " is complex for PDDL. New Requirements may be needed.\nAdd to PDDL anyway? (y/n)");
        Scanner s = new Scanner(System.in);
        if (s.nextLine().equals("y"))
            return true;
        return false;
    }

    private static boolean promptComplex(String text, RequireKey reqkey) {
        if (prompts == CompilerPrompts.NO_PROMPTS) {
            logCondComplex(text,reqkey,true);
            return true;
        }
        if (requirements.contains(reqkey))
            return true;

        System.out.println("["+currentPLP.getBaseName()+"] Condition/effect: " + text + " requires " +reqkey+".\nAdd to PDDL anyway? (y/n)");
        Scanner s = new Scanner(System.in);
        if (s.nextLine().equals("y")) {
            requirements.add(reqkey);
            return true;
        }
        return false;
    }

    private static void logCondNotEffected(Condition cond, boolean added) {
        if (added) logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Added condition: " + cond.toString() + " even though it can't be changed by any PLP.");
        else logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Skipped condition: " + cond.toString() + ". It can't be changed by any PLP.");
    }

    private static void logCondComplex(String text, boolean added) {
        if (added) logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Added condition/effect: " + text + " even though it's complex.");
        else logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Skipped condition/effect: " + text + ". It's complex.");
    }

    private static void logCondComplex(String text, RequireKey reqKey, boolean added) {
        if (added) logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Added condition/effect: " + text + " even though it requires " + reqKey);
        else logger.log(Level.INFO, "["+currentPLP.getBaseName()+"] Skipped condition/effect: " + text + ". It requires "+reqKey);
    }

    // METHODS SPECIFIC FOR PARTIALLY OBSERVABLE MODE

    private static Exp createKnowPair(Exp exp) {
        Exp result = new Exp(Connective.AND);
        result.addChild(exp);
        result.addChild(createKnowExp(exp));
        return result;
    }

    public static Exp createKnowExp(Exp exp) {
        return createKnowExp(exp, true);
    }

    static Exp createKnowExp(Exp exp, boolean holds) {
        if (exp.getConnective() == Connective.ATOM) {
            Exp result = new Exp(Connective.ATOM);
            List<Symbol> symbols = new LinkedList<>();
            symbols.add(new Symbol(Symbol.Kind.PREDICATE,(holds? "KNOW_" : "KNOW_NOT_")+exp.getAtom().get(0).getImage()));
            for (int i=1;i<exp.getAtom().size();i++) {
                symbols.add(exp.getAtom().get(i));
            }
            result.setAtom(symbols);
            return result;
        }
        else if (exp.getConnective() == Connective.NOT) {
            return createKnowExp(exp.getChildren().get(0), !holds);
        }
        else if (exp.getConnective() == Connective.FORALL) {
            Exp result = new Exp(holds ? Connective.FORALL : Connective.EXISTS);
            result.setVariables(exp.getVariables());
            result.addChild(createKnowExp(exp.getChildren().get(0),holds));
            return result;
        }
        else if (exp.getConnective() == Connective.EXISTS) {
            Exp result = new Exp(holds ? Connective.EXISTS : Connective.FORALL);
            result.setVariables(exp.getVariables());
            result.addChild(createKnowExp(exp.getChildren().get(0),holds));
            return result;
        }
        else if (exp.getConnective() == Connective.AND) {
            Exp result = new Exp(holds ? Connective.AND : Connective.OR);
            for (Exp child : exp.getChildren()) {
                result.addChild(createKnowExp(child, holds));
            }
            return result;
        }
        else if (exp.getConnective() == Connective.OR) {
            Exp result = new Exp(holds ? Connective.OR : Connective.AND);
            for (Exp child : exp.getChildren()) {
                result.addChild(createKnowExp(child, holds));
            }
            return result;
        }
        else if (exp.getConnective() == Connective.WHEN) {
            throw new RuntimeException("Cannot create KNOW expression for conditional effect");
        }
        else {
            throw new RuntimeException("Unsupported connective for know exp: "+exp.getConnective());
        }
    }

    private static List<Exp> getCondSensingEffs(Condition goal) {
        List<Exp> senseEffs = new LinkedList<>();

        Exp senseEff = new Exp(Connective.WHEN);
        Exp compiledGoal = compile(goal);
        if (compiledGoal != null) {
            senseEff.addChild(compiledGoal);
            Exp andExp1 = new Exp(Connective.AND);
            andExp1.addChild(createKnowExp(compiledGoal, true));
            Exp notExp1 = new Exp(Connective.NOT);
            notExp1.addChild(createKnowExp(compiledGoal, false));
            andExp1.addChild(notExp1);
            senseEff.addChild(andExp1);

            senseEffs.add(senseEff);

            senseEff = new Exp(Connective.WHEN);
            Exp notCompiledGoal = new Exp(Connective.NOT);
            notCompiledGoal.addChild(compiledGoal);
            senseEffs.add(notCompiledGoal);
            Exp andExp2 = new Exp(Connective.AND);
            andExp2.addChild(createKnowExp(compiledGoal, false));
            Exp notExp2 = new Exp(Connective.NOT);
            notExp2.addChild(createKnowExp(compiledGoal, true));
            andExp2.addChild(notExp2);
            senseEff.addChild(andExp2);

            senseEffs.add(senseEff);
        }
        return senseEffs;
    }

    private static List<Exp> getKcondEffs(Exp exp) {
        List<Exp> res = new LinkedList<>();

        if (exp.getConnective() == Connective.WHEN) {
            // TODO: collapse conditional effects into one condition
            Exp cond = exp.getChildren().get(0);
            Exp eff = exp.getChildren().get(1);

            // (Kp,Kq)
            Exp condEffect = new Exp(Connective.WHEN);
            condEffect.addChild(createKnowExp(cond));
            condEffect.addChild(createKnowExp(eff));
            res.add(condEffect);

            // (-K-p,-K-q)
            condEffect = new Exp(Connective.WHEN);
            Exp notCond = new Exp(Connective.NOT);
            notCond.addChild(createKnowExp(cond,false));
            condEffect.addChild(notCond);
            Exp notEff = new Exp(Connective.NOT);
            notEff.addChild(createKnowExp(eff,false));
            condEffect.addChild(notEff);
            res.add(condEffect);
        }
        else {
            // p
            res.add(exp);
            // Kp
            res.add(createKnowExp(exp, true));
            // -K-p
            Exp notExp = new Exp(Connective.NOT);
            notExp.addChild(createKnowExp(exp, false));
            res.add(notExp);
        }
        return res;
    }


    public static String finishPOproblem(String arg) {
        return null;
    }

}
