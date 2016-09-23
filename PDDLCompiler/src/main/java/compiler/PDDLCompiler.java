package compiler;

import conditions.*;
import conditions.Condition;
import effects.*;
import fr.uga.pddl4j.parser.*;
import loader.PLPLoader;
import modules.*;
import plpEtc.Predicate;
import plpFields.*;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public enum ROSPlanFit {
        DURATIVE_ACTIONS, CLASSICAL
    }
    public enum AssumptionType {
        TRUE_STRONG, TRUE_WEAK, TRUE_WEAK_NO_CHANGE, FALSE_STRONG, FALSE_WEAK, FALSE_WEAK_NO_CHANGE
    }

    private static String domainName = "PLPDomain";
    private static String problemName = "PLPDomain_problem";

    private static String knowValuePred = "KV_";
    private static String knowHoldsPred = "KNOW_";
    private static String knowNotHoldsPred = "KNOW_NOT_";

    private static List<AchievePLP> achievePLPs;
    private static List<ObservePLP> observePLPs;
    private static List<MaintainPLP> maintainPLPs;
    private static List<DetectPLP> detectPLPs;
    public static List<PLPParameter> observableValues;
    public static List<Effect> possibleEffects;

    public static Logger logger = Logger.getLogger("PLP->PDDL Logger");;

    public static CompilerPrompts prompts = CompilerPrompts.SOME_PROMPTS;
    public static Mode compilerMode;
    public static ROSPlanFit rosplanFit = ROSPlanFit.CLASSICAL;

    public static List<RequireKey> requirements;

    public static PLP currentPLP;
    public static List<TypedSymbol> currentPDDLParameters;

    public static Map<String, AssumptionType> assumptions;

    public static Map<String, Integer> predicates;
    public static Map<String, Integer> knowPredicates; // Only for PO mode

    static void setAchievePLPs(List<AchievePLP> achievePLPs) {
        PDDLCompiler.achievePLPs = achievePLPs;
    }

    static void setObservePLPs(List<ObservePLP> observePLPs) { PDDLCompiler.observePLPs = observePLPs; }

    static void setMaintainPLPs(List<MaintainPLP> maintainPLPs) { PDDLCompiler.maintainPLPs = maintainPLPs; }

    static void setDetectPLPs(List<DetectPLP> detectPLPs) {
        PDDLCompiler.detectPLPs = detectPLPs;
    }

    static String[] producePDDL() {
        observableValues = new LinkedList<>();
        possibleEffects = new LinkedList<>();
        requirements = new LinkedList<>();
        requirements.add(RequireKey.STRIPS);
        predicates = new TreeMap<>();
        knowPredicates = new TreeMap<>();

        String[] resultFiles = new String[2];
        loadObservableAndEffects();

        // Generate domain file
        Domain domain = new Domain(new Symbol(Symbol.Kind.DOMAIN, domainName));

        for (AchievePLP aPLP : achievePLPs) {
            currentPLP = aPLP;
            Op compiledPLP = compile(aPLP);
            if (compiledPLP != null)
                domain.addOperator(compiledPLP);
        }

        for (ObservePLP oPLP : observePLPs) {
            if (!oPLP.isGoalParameter() && compilerMode != Mode.PARTIALLY_OBSERVABLE) {
                continue;
            }
            else {
                currentPLP = oPLP;
                Op compiledPLP = compile(oPLP);
                if (compiledPLP != null)
                    domain.addOperator(compiledPLP);
            }
        }

        for (MaintainPLP mPLP : maintainPLPs) {
            if (!mPLP.isInitiallyTrue()) {
                currentPLP = mPLP;
                Op compiledPLP = compile(mPLP);
                if (compiledPLP != null)
                    domain.addOperator(compiledPLP);
            }
        }

        for (DetectPLP dPLP : detectPLPs) {
            currentPLP = dPLP;
            Op compiledPLP = compile(dPLP);
            if (compiledPLP != null)
                domain.addOperator(compiledPLP);
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
        if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
            for (String pred_name : knowPredicates.keySet()) {
                NamedTypedList ntl = new NamedTypedList(new Symbol(Symbol.Kind.PREDICATE, pred_name));
                for (int i = 1; i <= knowPredicates.get(pred_name); i++) {
                    ntl.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, "?par" + i)));
                }
                domain.addPredicate(ntl);
            }
        }

        resultFiles[0] = domain.toString();

        // Generate template problem file

        Problem problem = new Problem(new Symbol(Symbol.Kind.PROBLEM,problemName));
        //requirements.forEach(problem::addRequirement);
        problem.setDomain(new Symbol(Symbol.Kind.DOMAIN,domainName));

        int maxVariables = 0;
        for (String pred_name : predicates.keySet()) {
            Exp exp = new Exp(Connective.ATOM);
            List<Symbol> tempSymbols = new LinkedList<>();
            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE,pred_name));
            for (int i=1; i<=predicates.get(pred_name); i++) {
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,"example"+i));
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

        if (pddleffects.getChildren().size() == 0) {
            return null;
        }

        // Finished loading preconditions and effects. Now building parameters and loading predicates
        loadParamsAndPreds(pddlpreconds);
        loadParamsAndPreds(pddleffects);

        if (rosplanFit.equals(ROSPlanFit.DURATIVE_ACTIONS)) {
            pddlpreconds = updateDurative(pddlpreconds);
            pddleffects = updateDurative(pddleffects);
            requirements.add(RequireKey.DURATIVE_ACTIONS);
        }

        Op result = new Op(new Symbol(Symbol.Kind.ACTION,plp.getBaseName()), currentPDDLParameters, pddlpreconds, pddleffects);

        if (rosplanFit.equals(ROSPlanFit.DURATIVE_ACTIONS)) {
            Exp durationChild = new Exp(Connective.EQUAL_ATOM);
            List<Symbol> symbols = new LinkedList<>();
            symbols.add(new Symbol(Symbol.Kind.VARIABLE,"?duration"));
            symbols.add(new Symbol(Symbol.Kind.DURATION_VARIABLE,"10"));
            durationChild.setAtom(symbols);
            result.setDuration(durationChild);
        }
        return result;

    }

    private static Exp updateDurative(Exp exp) {
        if (!exp.getConnective().equals(Connective.NOT) && !exp.getConnective().equals(Connective.ATOM)) {
            for (int i=0;i<exp.getChildren().size();i++) {
                exp.getChildren().set(i,updateDurative(exp.getChildren().get(i)));
            }
            return exp;
        }
        else {
            Exp temp = new Exp(Connective.AT_START);
            temp.addChild(exp);
            return temp;
        }
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

        // Uncommenting the following lines requires changes when checking if there are no effects for the action
        //if (pddleffects.getChildren().size() == 1)  {
        //    pddleffects = pddleffects.getChildren().get(0);
        //}
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
        for (PLPParameter param :
                Stream.concat(plp.getInputParams().stream(), plp.getExecParams().stream()).collect(Collectors.toList())) {
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
            String predname = atom.get(0).getImage();
            if (compilerMode == Mode.PARTIALLY_OBSERVABLE) {
                if (!predname.contains(knowHoldsPred.toLowerCase())
                        && !predname.contains(knowNotHoldsPred.toLowerCase())) {
                    predicates.put(predname, atom.size() - 1);
                    if (!predname.contains(knowValuePred.toLowerCase())) {
                        knowPredicates.put(knowHoldsPred + predname, atom.size() - 1);
                        knowPredicates.put(knowNotHoldsPred + predname, atom.size() - 1);
                    }
                }
            }
            else {
                predicates.put(predname, atom.size() - 1);
            }
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
        else if (exp.getConnective() == Connective.AT_START) {
            loadParamsAndPreds(exp.getChildren().get(0), excludeParams);
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
                tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, knowValuePred + matcher.group().toUpperCase()));
            else
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, "?"+matcher.group()));
            //sb.append((isFirstMatch ? matcher.group().toUpperCase() : matcher.group())).append(" ");
            isFirstMatch = false;
        }

        knowPred.setAtom(tempSymbols);
        return knowPred;
    }


    public static Exp compile(Formula formula) {
        if (formula.getOperator().equals("=") && formula.getLeftExpr().matches(PLPParameter.PLPParameterRegex)) {
            if (formula.getRightExpr().toUpperCase().equals("NULL")) {
                for (PLPParameter param : observableValues) {
                    if (param.containsParam(formula.getLeftExpr())) {
                        Exp notKnow = new Exp(Connective.NOT);
                        notKnow.addChild(generateKVPred(formula.getLeftExpr()));
                        return notKnow;
                    }
                }
            }
            else if (formula.getRightExpr().toUpperCase().equals("TRUE")) {
                if (!canBeEffected(formula))
                    if (!promptCondNotEffected(formula))
                        return null;
                return createPredicateFromPLPParameter(PLPParameter.createParamFromString(formula.getLeftExpr()));
            }
            else if (formula.getRightExpr().toUpperCase().equals("FALSE")) {
                if (!canBeEffected(formula))
                    if (!promptCondNotEffected(formula))
                        return null;
                Exp notExp = new Exp(Connective.NOT);
                notExp.addChild(createPredicateFromPLPParameter(PLPParameter.createParamFromString(formula.getLeftExpr())));
                return notExp;
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

    private static Exp createPredicateFromPLPParameter(PLPParameter plpParam) {
        Exp result = new Exp(Connective.ATOM);
        List<Symbol> values = new LinkedList<>();
        values.add(new Symbol(Symbol.Kind.VARIABLE,plpParam.getName()));
        values.addAll(plpParam.getParamFieldValues().stream().map(field -> new Symbol(Symbol.Kind.VARIABLE, "?"+field)).collect(Collectors.toList()));
        result.setAtom(values);
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
        if (currentPLP.getConstantsNames().containsAll(predicate.getValues())) {
            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, predicate.simpleString()));
        }
        else {
            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, predicate.getName()));
            for (String value : predicate.getValues()) {
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, "?" + value));
            }
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
        else if (aEffect.getExpression().toUpperCase().equals("TRUE")) {
                return createPredicateFromPLPParameter(aEffect.getParam());
        }
        else if (aEffect.getExpression().toUpperCase().equals("FALSE")) {
            Exp notExp = new Exp(Connective.NOT);
            notExp.addChild(createPredicateFromPLPParameter(aEffect.getParam()));
            return notExp;
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
            requirements.add(reqkey);
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

    // METHODS FOR PARTIALLY OBSERVABLE MODE

    private static Exp createKnowPair(Exp exp) {
        Exp result = new Exp(Connective.AND);
        Exp knowExp = createKnowExp(exp);
        if (knowExp == null)
            return exp;
        result.addChild(exp);
        result.addChild(knowExp);
        return result;
    }

    public static Exp createKnowExp(Exp exp) {
        return createKnowExp(exp, true);
    }

    static Exp createKnowExp(Exp exp, boolean holds) {
        if (exp.getConnective() == Connective.ATOM) {
            Exp result = new Exp(Connective.ATOM);
            List<Symbol> symbols = new LinkedList<>();
            // Check if it's a know value predicate
            if (exp.getAtom().get(0).getImage().contains(knowValuePred.toLowerCase())) {
                return null;
            }
            symbols.add(new Symbol(Symbol.Kind.PREDICATE,(holds? knowHoldsPred : knowNotHoldsPred)+exp.getAtom().get(0).getImage()));
            for (int i=1;i<exp.getAtom().size();i++) {
                symbols.add(exp.getAtom().get(i));
            }
            result.setAtom(symbols);
            return result;
        }
        else if (exp.getConnective() == Connective.NOT) {
            return createKnowExp(exp.getChildren().get(0), !holds);
        }
        else if (exp.getConnective() == Connective.FORALL || exp.getConnective() == Connective.EXISTS) {
            Exp result = new Exp(holds ? exp.getConnective() : getOppositeConnective(exp.getConnective()));
            result.setVariables(exp.getVariables());
            Exp knowExp = createKnowExp(exp.getChildren().get(0),holds);
            if (knowExp == null)
                return null;
            result.addChild(knowExp);
            return result;
        }
        else if (exp.getConnective() == Connective.AND || exp.getConnective() == Connective.OR) {
            Exp result = new Exp(holds ? exp.getConnective() : getOppositeConnective(exp.getConnective()));
            for (Exp child : exp.getChildren()) {
                Exp knowExp = createKnowExp(child, holds);
                if (knowExp == null)
                    return null;
                result.addChild(knowExp);
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

    private static Connective getOppositeConnective(Connective connective) {
        switch (connective) {
            case AND:
                return Connective.OR;
            case OR:
                return Connective.AND;
            case FORALL:
                return Connective.EXISTS;
            case EXISTS:
                return Connective.FORALL;
            default:
                throw new RuntimeException("Unexpected connective in getOppositeConnective");
        }
    }

    private static List<Exp> getCondSensingEffs(Condition goal) {
        List<Exp> senseEffs = new LinkedList<>();

        Exp senseEff = new Exp(Connective.WHEN);
        Exp compiledGoal = compile(goal);
        if (compiledGoal != null) {
            Exp knowTrueExp = createKnowExp(compiledGoal, true);
            Exp knowFalseExp = createKnowExp(compiledGoal, false);
            if (knowTrueExp == null || knowFalseExp == null)
                return senseEffs;

            senseEff.addChild(compiledGoal);
            Exp andExp1 = new Exp(Connective.AND);
            andExp1.addChild(knowTrueExp);
            Exp notExp1 = new Exp(Connective.NOT);
            notExp1.addChild(knowFalseExp);
            andExp1.addChild(notExp1);
            senseEff.addChild(andExp1);

            senseEffs.add(senseEff);

            senseEff = new Exp(Connective.WHEN);
            Exp notCompiledGoal = new Exp(Connective.NOT);
            notCompiledGoal.addChild(compiledGoal);
            senseEff.addChild(notCompiledGoal);
            Exp andExp2 = new Exp(Connective.AND);
            andExp2.addChild(knowFalseExp);
            Exp notExp2 = new Exp(Connective.NOT);
            notExp2.addChild(knowTrueExp);
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
            Exp knowCondTrue = createKnowExp(cond);
            Exp knowEffTrue = createKnowExp(eff);
            if (knowEffTrue == null && knowCondTrue != null) {
                // (Kp,q)
                condEffect.addChild(knowCondTrue);
                condEffect.addChild(eff);
                res.add(condEffect);
                return res;
            }
            else if (knowEffTrue != null && knowCondTrue == null) {
                // (p,q AND Kq)
                Exp andExp = new Exp(Connective.AND);
                andExp.addChild(eff);
                andExp.addChild(knowEffTrue);
                condEffect.addChild(cond);
                condEffect.addChild(andExp);
                res.add(condEffect);
                return res;
            }
            else if (knowEffTrue == null) {
                // (p,q)
                res.add(exp);
                return res;
            }
            condEffect.addChild(knowCondTrue);
            condEffect.addChild(knowEffTrue);
            res.add(condEffect);

            // (-K-p,-K-q)
            Exp knowNotCond = createKnowExp(cond,false);
            Exp knowNotEff = createKnowExp(cond,false);
            if (knowNotCond != null && knowNotEff != null) { // Will always happen
                condEffect = new Exp(Connective.WHEN);
                Exp notCond = new Exp(Connective.NOT);
                notCond.addChild(knowNotCond);
                condEffect.addChild(notCond);
                Exp notEff = new Exp(Connective.NOT);
                notEff.addChild(knowNotEff);
                condEffect.addChild(notEff);
                res.add(condEffect);
            }
        }
        else {
            Exp knowCondTrue = createKnowExp(exp, true);
            Exp knowCondFalse = createKnowExp(exp, false);
            // p
            res.add(exp);
            // Kp
            if (knowCondTrue != null)
                res.add(knowCondTrue);
            // -K-p
            if (knowCondFalse != null) {
                Exp notExp = new Exp(Connective.NOT);
                notExp.addChild(knowCondFalse);
                res.add(notExp);
            }
        }
        return res;
    }

    // METHODS FOR POprob MODE

    public static String finishPOproblem(String folderPath) {
        assumptions = new TreeMap<>();
        Parser pddlParser = new Parser();
        try {
            pddlParser.parse(folderPath+"/domain.pddl",folderPath+"/problem.pddl");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Wrong folder path (can't find domain.pddl and problem.pddl in path): "+folderPath);
        }
        PLPLoader.loadFromDirectory(folderPath);

        Domain domain = pddlParser.getDomain();
        Problem problem = pddlParser.getProblem();

        // Set up new problem with old problem values
        Problem resultProblem = new Problem(problem.getName());
        resultProblem.setDomain(problem.getDomain());
        //problem.getRequirements().forEach(resultProblem::addRequirement);
        problem.getObjects().forEach(resultProblem::addObject);
        resultProblem.setGoal(problem.getGoal());

        // Update initial state
        for (NamedTypedList pred : domain.getPredicates()) {
            String predName = pred.getName().getImage();
            if (predName.contains(knowValuePred.toLowerCase())
                    || predName.contains(knowHoldsPred.toLowerCase())
                    || predName.contains(knowNotHoldsPred.toLowerCase())) {
                continue;
            }
            List<Exp> groundedPredList = groundPredicate(pred.getName().getImage(), pred.getArguments().size(),
                    problem.getObjects());

            for (Exp gPred : groundedPredList) {
                if (inInitialState(gPred,problem.getInit(),true)) {
                    //holds in initial state
                    resultProblem.addInitialFact(gPred);
                    resultProblem.addInitialFact(createKnowExp(gPred,true));
                }
                else if (inInitialState(gPred,problem.getInit(),false)) {
                    //doesn't hold in initial state
                    resultProblem.addInitialFact(createKnowExp(gPred,false));
                }
                else {
                    //unknown in initial state
                    if (canBeAchieved(gPred, domain.getOperators())) {
                        // optimistically assume holds but not KNOW holds
                        resultProblem.addInitialFact(gPred);
                        assumptions.put(gPred.toString(),AssumptionType.TRUE_WEAK);
                        logger.log(Level.INFO,"Grounded predicate: "+gPred.toString()+" isn't known in the Initial State but can be sensed/achieved.");
                    }
                    else if  (canBeSensed(gPred, domain.getOperators())) {
                        resultProblem.addInitialFact(gPred);
                        assumptions.put(gPred.toString(),AssumptionType.TRUE_WEAK_NO_CHANGE);
                        logger.log(Level.INFO,"Grounded predicate: "+gPred.toString()+" isn't known in the Initial State but can be sensed/achieved.");
                    }
                    else {
                        // optimistically assume holds and KNOW holds
                        resultProblem.addInitialFact(gPred);
                        resultProblem.addInitialFact(createKnowExp(gPred,true));
                        assumptions.put(gPred.toString(),AssumptionType.TRUE_STRONG);
                        logger.log(Level.INFO,"Grounded predicate: "+gPred.toString()+" isn't known in the Initial State and can't be sensed/achieved.");
                    }
                }
            }
        }
        return resultProblem.toString();
    }

    private static boolean canBeAchieved(Exp gPred, List<Op> actions) {
        for (Op action : actions) {
            if (canActionAchieve(gPred,action.getEffects()))
                return true;
        }
        return false;
    }

    private static boolean canActionAchieve(Exp gPred, Exp actionEff) {
        switch (actionEff.getConnective()) {
            case AND:
                boolean res = false;
                for (Exp childEff : actionEff.getChildren()) {
                    if (canActionAchieve(gPred,childEff))
                        res = true;
                }
                return res;
            case ATOM:
                return gPred.getAtom().get(0).getImage().equals(actionEff.getAtom().get(0).getImage());
            case WHEN:
                return canActionAchieve(gPred,actionEff.getChildren().get(1));
            case NOT:
                return false;
            case FORALL:
            case AT_START:
            case AT_END:
            case OVER_ALL:
                return canActionAchieve(gPred, actionEff.getChildren().get(0));
            default:
                throw new RuntimeException("Unexpected connective in: "+actionEff+" when checking what it achieves");
        }
    }

    private static boolean canBeSensed(Exp gPred, List<Op> actions) {
        for (ObservePLP oPLP : PLPLoader.getObservePLPs()) {
            if (!oPLP.isGoalParameter()) {
                for (Op action : actions) {
                    // Check if the PLP was compiled to a PDDL action
                    if (action.getName().getImage().equals(oPLP.getBaseName())) {
                        Exp compiledCond = compile((Condition) oPLP.getGoal());
                        if (compiledCond != null && compiledCond.getConnective() == Connective.ATOM &&
                                compiledCond.getAtom().get(0).getImage().equals(gPred.getAtom().get(0).getImage())) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the given grounded predicate is in the initial state (according to holds)
     * @param gPred the grounded predicate
     * @param init the initial state
     * @param holds if gPred holds or doesn't hold (not gPred)
     * @return
     */
    private static boolean inInitialState(Exp gPred, List<Exp> init, boolean holds) {
        for (Exp initPred : init) {
            boolean result = true;
            if (holds && initPred.getAtom().size() == gPred.getAtom().size()) {
                for (int i=0;i<initPred.getAtom().size();i++) {
                    if (!initPred.getAtom().get(i).getImage().equals(gPred.getAtom().get(i).getImage())) {
                        result = false;
                    }
                }
            }
            else if (!holds && initPred.getConnective() == Connective.NOT &&
                    initPred.getChildren().get(0).getAtom().size() == gPred.getAtom().size()) {
                Exp baseInit = initPred.getChildren().get(0);
                for (int i=0;i<baseInit.getAtom().size();i++) {
                    if (!baseInit.getAtom().get(i).getImage().equals(gPred.getAtom().get(i).getImage())) {
                        result = false;
                    }
                }
            }
            else
                result = false;

            if (result) return true;
        }
        return false;
    }

    public static List<Exp> groundPredicate(String predName, int objectsLeft, List<TypedSymbol> objectList) {
        return groundPredicate(predName, objectsLeft, objectList, new LinkedList<>());
    }

    public static List<Exp> groundPredicate(String predName, int objectsLeft,
                                                          List<TypedSymbol> objectList, List<TypedSymbol> chosenObjects) {
        List<Exp> groundedList = new LinkedList<>();
        if (objectsLeft == 0) {
            Exp tempExp = new Exp(Connective.ATOM);
            List<Symbol> tempSymbols = new LinkedList();
            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE,predName));
            for (TypedSymbol object : chosenObjects) {
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,object.getImage()));
            }
            tempExp.setAtom(tempSymbols);
            groundedList.add(tempExp);
        }
        else {
            for (TypedSymbol object : objectList) {
                chosenObjects.add(object);
                groundedList.addAll(groundPredicate(predName, objectsLeft - 1, objectList, chosenObjects));
                chosenObjects.remove(object);
            }
        }
        return groundedList;
    }
}
