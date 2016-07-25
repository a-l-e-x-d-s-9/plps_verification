package compiler;

import conditions.*;
import effects.*;
import fr.uga.pddl4j.parser.*;
import modules.*;
import plpEtc.Predicate;
import plpFields.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PDDLCompiler {

    private static List<AchievePLP> achievePLPs;
    private static List<ObservePLP> observePLPs;
    private static List<PLPParameter> observableValues;
    private static List<Effect> possibleEffects;

    public static void setAchievePLPs(List<AchievePLP> achievePLPs) {
        PDDLCompiler.achievePLPs = achievePLPs;
    }

    public static void setObservePLPs(List<ObservePLP> observePLPs) {
        PDDLCompiler.observePLPs = observePLPs;
    }

    public static String producePDDL () {

        observableValues = new LinkedList<>();
        possibleEffects = new LinkedList<>();

        /* Get all possible effects and observable parameters */
        for (ObservePLP oPLP : observePLPs) {
            if (oPLP.getGoal().getClass().isAssignableFrom(PLPParameter.class)) {
                observableValues.add((PLPParameter) oPLP.getGoal());
            }
            possibleEffects.addAll(oPLP.getSideEffects());
        }

        for (AchievePLP aPLP : achievePLPs) {
            possibleEffects.add(aPLP.getGoal().createProperEffect());
            possibleEffects.addAll(aPLP.getSideEffects());
        }

        /* Compile each PLP */
        Domain domain = new Domain(new Symbol(Symbol.Kind.DOMAIN, "PLPDomain"));
        //StringBuilder output = new StringBuilder();
        for (AchievePLP aPLP : achievePLPs) {
            domain.addOperator(compile(aPLP));
            //output.append(compile(aPLP));
            //output.append("\n");
        }

        for (ObservePLP oPLP : observePLPs) {
            domain.addOperator(compile(oPLP));
            //output.append(compile(oPLP));
        }

        return domain.toString();
    }

    private static Op compile(ObservePLP oPLP) {
        //StringBuilder sb = new StringBuilder();
        //sb.append("(:action ").append(oPLP.getBaseName()).append("\n");

        List<TypedSymbol> pddlparams = getParameters(oPLP);
        Exp pddlpreconds = getPreconditions(oPLP);
        Exp pddleffects = new Exp(Connective.AND);

        //sb.append(":effect ");
        //int numEffects = 0;
        //StringBuilder effectsSB = new StringBuilder();

        /* Add KNOW effect */
        if (oPLP.getGoal().getClass().isAssignableFrom(PLPParameter.class)) {
            PLPParameter goal = ((PLPParameter) oPLP.getGoal());
            Exp knowEffect = new Exp(Connective.ATOM);
            List<Symbol> tempSymbols = new LinkedList<>();

            tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, "(KV_" + goal.getName().toUpperCase()));
            for (String field : goal.getParamFieldValues()) {
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, field));
                // effectsSB.append(field).append(" ");
            }
            knowEffect.setAtom(tempSymbols);
            pddleffects.addChild(knowEffect);
            //effectsSB.deleteCharAt(effectsSB.length()-1);
            //effectsSB.append(") ");
            //numEffects++;
        }

        for (Effect se: oPLP.getSideEffects()){
            Exp compiledSE = compile(se);
            if (compiledSE != null) {
                pddleffects.addChild(compiledSE);
            }
            //if (!compiledSE.equals("")) numEffects++;
            //effectsSB.append(compiledSE).append(" "); // TODO: FIX
        }


        /*if (numEffects > 1) sb.append("(and ");
        sb.append(effectsSB.toString());

        sb.deleteCharAt(sb.length()-1);
        if (numEffects > 1) sb.append(")");
        sb.append("\n");
        //TODO: change goals into one goal with AND*/

        //sb.append(")");
        // return sb.toString();
        if (pddleffects.getChildren().size() == 1) pddleffects = pddleffects.getChildren().get(0);
        return new Op(new Symbol(Symbol.Kind.ACTION,oPLP.getBaseName()), pddlparams, pddlpreconds, pddleffects);
    }

    public static Op compile(AchievePLP aPLP) {
        //StringBuilder sb = new StringBuilder();
        //sb.append("(:action ").append(aPLP.getBaseName()).append("\n");

        List<TypedSymbol> pddlparams = getParameters(aPLP);
        Exp pddlpreconds = getPreconditions(aPLP);
        Exp pddleffects = new Exp(Connective.AND);

        //sb.append(":effect ");

        //int numEffects = 0;
        //StringBuilder effectSB = new StringBuilder();
        Exp compiledGoal = compile(aPLP.getGoal().createProperEffect());
        if (compiledGoal != null)
            pddleffects.addChild(compiledGoal);
        //effectSB.append(goalEffect).append(" "); // TODO: FIX
        //if (!goalEffect.equals("")) numEffects++;
        for (Effect se: aPLP.getSideEffects()){
            Exp compiledSE = compile(se);
            if (compiledSE != null) {
                pddleffects.addChild(compiledSE);
            }
            //if (!compiledSE.equals("")) numEffects++;
            //effectsSB.append(compiledSE).append(" "); // TODO: FIX
        }

        /*if (numEffects > 1) sb.append("(and ");
        sb.append(effectSB.toString());

        sb.deleteCharAt(sb.length()-1);
        if (numEffects > 1) sb.append(")");
        sb.append("\n");
        //TODO: check if AND works fine

        sb.append(")");
        return sb.toString();*/
        if (pddleffects.getChildren().size() == 1) pddleffects = pddleffects.getChildren().get(0);
        return new Op(new Symbol(Symbol.Kind.ACTION,aPLP.getBaseName()), pddlparams, pddlpreconds, pddleffects);
    }

    private static List<TypedSymbol> getParameters(PLP plp) {
        List<TypedSymbol> params = new LinkedList<>();
        //sb.append(":parameters (");

        Iterator<PLPParameter> execParamIterator = plp.getExecParams().iterator();
        while (execParamIterator.hasNext()) {
            params.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, execParamIterator.next().toString())));
            //sb.append("?").append(execParamIterator.next());
            //if (execParamIterator.hasNext()) sb.append(" ");
        }
        //sb.append(")\n");

        return params;
    }

    private static Exp getPreconditions(PLP plp) {

        // TODO: Check the first connector
        Exp pddlprecond = new Exp(Connective.AND);
        //sb.append(":precondition (and ");

        Iterator<Condition> preCondIterator = plp.getPreConditions().iterator();
        Iterator<PLPParameter> execParamIterator = plp.getExecParams().iterator();
        Iterator<Effect> effectsIterator;
        // boolean hasPreCond = false;

        /* Add regular preconditions from the PLP */
        while (preCondIterator.hasNext()) {
            Condition preCond = preCondIterator.next();

            /* Check if the PLP precond contains an execution parameter
                if so, add it to the PDDL preconditions */
            boolean containsExecParam = false;
            while (execParamIterator.hasNext()) {
                PLPParameter param = execParamIterator.next();
                if (preCond.containsParam(param.getName())) {
                    pddlprecond.addChild(compile(preCond));
                    // sb.append(compile(preCond)).append(" "); // TODO: FIX
                    containsExecParam = true;
                    // hasPreCond = true;
                    break;
                }
            }

            /* Check if the PLP precond can be effected by some action
                if so, add it to the PDDL preconditions */
            if (!containsExecParam) {
                effectsIterator = possibleEffects.iterator();
                while (effectsIterator.hasNext()) {
                    if (preCond.sharesParams(effectsIterator.next())) {
                        pddlprecond.addChild(compile(preCond));
                        // sb.append(compile(preCond)).append(" "); // TODO: FIX
                        // hasPreCond = true;
                        break;
                    }
                }
            }
        }

        /* Add KNOW preconditions */
        for (PLPParameter param : plp.getInputParams()) {
            for (ObservationGoal og : observableValues) {
                if (og.getClass().isAssignableFrom(PLPParameter.class) && og.containsParam(param.getName())) {
                    PLPParameter goal = ((PLPParameter) og);
                    Exp knowPrecond = new Exp(Connective.ATOM);
                    List<Symbol> tempSymbols = new LinkedList<>();
                    tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, "KV_" + goal.getName().toUpperCase()));

                    //sb.append("(K_").append(goal.getName().toUpperCase()).append(" ");
                    for (String field : goal.getParamFieldValues()) {
                        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, field));
                        //sb.append(field).append(" ");
                    }
                    knowPrecond.setAtom(tempSymbols);
                    pddlprecond.addChild(knowPrecond);
                    //sb.deleteCharAt(sb.length()-1);
                    //sb.append(") ");

                    //hasPreCond = true;
                    break;
                }
            }
        }
        /*for (PLPParameter param : aPLP.getExecParams()) {
            for (ObservationGoal og : observableValues) {
                if (og.getClass().isAssignableFrom(PLPParameter.class) && og.containsParam(param)) {
                    sb.append("(K_").append(param.getName().toUpperCase()).append(") ");
                    break;
                }
            }
        }*/

        /*if (hasPreCond) {
            sb.deleteCharAt(sb.length()-1);
            sb.append(")").append("\n");
        }
        if (!hasPreCond) sb.delete(sb.length() - 19, sb.length());*/
        // return sb;
        if (pddlprecond.getChildren().size() == 0)
            return new Exp(Connective.TRUE);
        if (pddlprecond.getChildren().size() == 1)
            return pddlprecond.getChildren().get(0);
        return pddlprecond;
    }

    public static Exp compile(Formula formula) {
        if (formula.getOperator().equals("=") &&
                formula.getRightExpr().toUpperCase().equals("NULL")&&
                formula.getLeftExpr().matches(PLPParameter.PLPParameterRegex)) {

            for (PLPParameter param : observableValues) {
                if (param.containsParam(formula.getLeftExpr()))
                    return generateInformationLoss(formula.getLeftExpr());
            }

        }

        if (!formula.getOperator().equals("="))
            throw new IllegalArgumentException("Forumla operator: " + formula.getOperator() + " isn't supported yet");
        Exp result = new Exp(Connective.EQUAL_ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,formula.getLeftExpr()));
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE,formula.getRightExpr()));
        //return "(" + formula.getLeftExpr() + " " + formula.getOperator() + " " + formula.getRightExpr() + ")";
        result.setAtom(tempSymbols);
        return result;
    }

    public static Exp generateInformationLoss(String parameter) {
        Exp knowEffect = new Exp(Connective.ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        //StringBuilder sb = new StringBuilder();

        //sb.append("(not (K_");//.append(equality.getLeftExpr().toUpperCase()).append(" ");

        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(parameter);
        boolean isFirstMatch = true;
        while (matcher.find()) {
            if (isFirstMatch)
                tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, "KV_" + matcher.group().toUpperCase()));
            else
                tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, matcher.group()));
            //sb.append((isFirstMatch ? matcher.group().toUpperCase() : matcher.group())).append(" ");
            isFirstMatch = false;
        }

        knowEffect.setAtom(tempSymbols);
        /*sb.deleteCharAt(sb.length() - 1);
        sb.append("))");
        return sb.toString();*/
        Exp notKnow = new Exp(Connective.NOT);
        notKnow.addChild(knowEffect);
        return notKnow;
    }

    public static Exp compile(NotCondition nCond) {
        Exp result = new Exp(Connective.NOT);
        result.addChild(compile(nCond.getCondition()));
        return result; // TODO: FIX for a more generic case or throw error
        // return "(not " + compile(nCond.getCondition()) + ")";
    }

    public static Exp compile(Predicate predicate) {
        Exp result = new Exp(Connective.ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.PREDICATE, predicate.getName()));
        for (String value : predicate.getValues()) {
            tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, value));
        }
        result.setAtom(tempSymbols);
        /*int stringLength = Arrays.toString(predicate.getValues().toArray()).length();
        if (stringLength <= 2)
            return "(" + predicate.getName() + ")";
        return "(" + predicate.getName() +
                " " + Arrays.toString(predicate.getValues().toArray()).substring(1, stringLength - 1).replaceAll(",", "") +
                ")";*/
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
        /*if (qCond.getParams().size() > 1)
            throw new IllegalArgumentException("More than one quantified variable isn't supported yet");*/
        quantifiedVar.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, qCond.getParams().get(0))));
        result.setVariables(quantifiedVar);
        result.addChild(compile(qCond.getCondition()));

        return result;
        /*String paramsString = Arrays.toString(qCond.getParams().toArray());
        return (isForall ? "(forall (" : "(exists (") +
                paramsString.substring(1, paramsString.length() - 1) +
                ") " + compile(qCond.getCondition()) + ")"; // TODO: FIX*/
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
        else {
            throw new UnsupportedOperationException("Unsupported condition " + c + " of type " + c.getClass());
        } // TODO: add the new conditions
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
        else {
            throw new UnsupportedOperationException("Unsupported effect " + e + " of type " + e.getClass());
        } // TODO: add the new conditions
    }

    public static Exp compile(AssignmentEffect aEffect) {
        if (aEffect.getExpression().toUpperCase().equals("NULL")) {
            for (PLPParameter param : observableValues) {
                if (param.containsParam(aEffect.getParam().getName())) {
                    return generateInformationLoss(aEffect.getParam().toString());
                }
            }
        }
        Exp result = new Exp(Connective.EQUAL_ATOM);
        List<Symbol> tempSymbols = new LinkedList<>();
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, aEffect.getParam().toString()));
        tempSymbols.add(new Symbol(Symbol.Kind.VARIABLE, aEffect.getExpression().toString()));
        result.setAtom(tempSymbols);
        return result;
        //return "(" + aEffect.getParam() + " = " + aEffect.getExpression() + ")";
    }

    public static Exp compile(NotEffect nEffect) {
        Exp result = new Exp(Connective.NOT);
        result.addChild(compile(nEffect.getEffect()));
        return result;
        //return "(not " + compile(nEffect.getEffect()) + ")"; // TODO: FIX for a more generic case or throw error
    }

    public static Exp compile(ForAllEffect faEffect) {
        Exp result = new Exp(Connective.FORALL);

        List<TypedSymbol> quantifiedVar = new LinkedList<>();
        /*if (qCond.getParams().size() > 1)
            throw new IllegalArgumentException("More than one quantified variable isn't supported yet");*/
        quantifiedVar.add(new TypedSymbol(new Symbol(Symbol.Kind.VARIABLE, faEffect.getParams().get(0))));
        result.setVariables(quantifiedVar);
        result.addChild(compile(faEffect.getEffect()));

        return result;
        /*String paramsString = Arrays.toString(faEffect.getParams().toArray());
        return "(forall (" + paramsString.substring(1, paramsString.length() - 1) +
                ") " + compile(faEffect.getEffect()) + ")"; // TODO: FIX*/
    }

    public static Exp compile(AndEffect aEffect) {
        Exp result = new Exp(Connective.AND);
        for (Effect e : aEffect.getEffects()) {
            result.addChild(compile(e));
        }
        return result;
        /*StringBuilder effectsSB = new StringBuilder();

        effectsSB.deleteCharAt(effectsSB.length()-1);
        return "(and " + effectsSB.toString() + ")";*/
    }

/*    public static void loadPLPs(String dirPath) {

        *//*try {
            Files.walk(Paths.get(dirPath)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                }
            });
        } catch (IOException e) {
            System.out.println("Error loading directory\n"+e);
        }*//*

        achievePLPs = new LinkedList<>();
        observePLPs = new LinkedList<>();

        AchievePLP walkThroughGateway = new AchievePLP("walk_through_gateway");
        walkThroughGateway.addExecParam("areaA");
        walkThroughGateway.addExecParam("areaB");
        walkThroughGateway.addExecParam("gateway");

        PLPParameter gatewayloc = new PLPParameter("gateway_location");
        gatewayloc.addParamFieldValue("gateway");
        walkThroughGateway.addInputParam(gatewayloc);
        walkThroughGateway.addInputParam("laser_scan");
        walkThroughGateway.addInputParam("odometry");
        walkThroughGateway.addInputParam("current_Aspeed");
        walkThroughGateway.addInputParam("current_Lspeed");
        walkThroughGateway.addInputParam("arm_moving");

        walkThroughGateway.addOutputParam("result");

        // TODO: Non-observable params

        Predicate pre1 = new Predicate("at");
        pre1.addValue("areaA");
        Predicate pre2 = new Predicate("connected");
        pre2.addValue("areaA"); pre2.addValue("areaB"); pre2.addValue("gateway");
        Formula pre3 = new Formula("current_Aspeed", "0", "=");
        Formula pre4 = new Formula("current_Lspeed", "0", "=");
        walkThroughGateway.addPreCondition(pre1);
        walkThroughGateway.addPreCondition(pre2);
        walkThroughGateway.addPreCondition(pre3);
        walkThroughGateway.addPreCondition(pre4);

        NotCondition conC = new NotCondition(new Predicate("arm_moving"));
        walkThroughGateway.addConcurrencyCondition(conC);

        //PLPParameter gatewaylocforall = new PLPParameter("gateway_location");
        //gatewaylocforall.addParamFieldValue("gw");
        PLPParameter np = new PLPParameter("gateway_location");
        np.addParamFieldValue("gw");
        Effect temp = new AssignmentEffect(np,"Null");
        ForAllEffect sideE = new ForAllEffect(temp);
        sideE.addParam("gw");
        walkThroughGateway.addSideEffect(sideE);
        Predicate sideE2 = new Predicate("at");
        sideE2.addValue("areaA");
        walkThroughGateway.addSideEffect(new NotEffect(sideE2));

        Predicate goal = new Predicate("at");
        goal.addValue("areaB");
        walkThroughGateway.setGoal(goal);

        ObservePLP observeGateway = new ObservePLP("gatewayAB");

        observeGateway.addExecParam("areaA");
        observeGateway.addExecParam("areaB");
        observeGateway.addExecParam("gateway");

        observeGateway.addInputParam("rgb_image");
        observeGateway.addInputParam("depth_image");
        observeGateway.addInputParam("current_Aspeed");
        observeGateway.addInputParam("current_Lspeed");
        observeGateway.addInputParam("arm_moving");

        observeGateway.addOutputParam(gatewayloc);

        Predicate pre5 = new Predicate("at");
        pre5.addValue("areaA");
        Predicate pre6 = new Predicate("connected");
        pre6.addValue("areaA"); pre6.addValue("areaB"); pre6.addValue("gateway");
        Formula pre7 = new Formula("current_Aspeed", "0", "=");
        Formula pre8 = new Formula("current_Lspeed", "0", "=");
        observeGateway.addPreCondition(pre5);
        observeGateway.addPreCondition(pre6);
        observeGateway.addPreCondition(pre7);
        observeGateway.addPreCondition(pre8);

        NotCondition conC2 = new NotCondition(new Predicate("arm_moving"));
        observeGateway.addConcurrencyCondition(conC2);

        observeGateway.setGoal(gatewayloc);

        observePLPs.add(observeGateway);
        achievePLPs.add(walkThroughGateway);

        System.out.println(observeGateway.toString());
        System.out.println("------------------------");
        System.out.println(walkThroughGateway.toString());
        System.out.println("------------------------");

    }*/

}
