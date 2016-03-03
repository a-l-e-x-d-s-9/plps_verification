package compiler;

import conditions.*;
import effects.*;
import modules.*;
import plpEtc.Predicate;
import plpFields.*;

import java.util.Arrays;
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

        StringBuilder output = new StringBuilder();
        for (AchievePLP aPLP : achievePLPs) {
            output.append(compile(aPLP));
            output.append("\n");
        }

        for (ObservePLP oPLP : observePLPs) {
            output.append(compile(oPLP));
        }

        return output.toString();
    }

    private static String compile(ObservePLP oPLP) {
        StringBuilder sb = new StringBuilder();
        sb.append("(:action ").append(oPLP.getBaseName()).append("\n");

        compileHeader(oPLP,sb);

        sb.append(":effect ");
        int numEffects = 0;
        StringBuilder effectsSB = new StringBuilder();

        if (oPLP.getGoal().getClass().isAssignableFrom(PLPParameter.class)) {
            PLPParameter goal = ((PLPParameter)oPLP.getGoal());
            effectsSB.append("(K_").append(goal.getName().toUpperCase()).append(" ");
            for (String field : goal.getParamFieldValues()) {
                effectsSB.append(field).append(" ");
            }
            effectsSB.deleteCharAt(effectsSB.length()-1);
            effectsSB.append(") ");
            numEffects++;
        }

        for (Effect se: oPLP.getSideEffects()){
            String compiledSE = compile(se);
            if (!compiledSE.equals("")) numEffects++;
            effectsSB.append(compiledSE).append(" "); // TODO: FIX
        }


        if (numEffects > 1) sb.append("(and ");
        sb.append(effectsSB.toString());

        sb.deleteCharAt(sb.length()-1);
        if (numEffects > 1) sb.append(")");
        sb.append("\n");
        //TODO: change goals into one goal with AND

        sb.append(")");
        return sb.toString();
    }

    public static String compile(AchievePLP aPLP) {
        StringBuilder sb = new StringBuilder();
        sb.append("(:action ").append(aPLP.getBaseName()).append("\n");

        compileHeader(aPLP,sb);

        sb.append(":effect ");

        int numEffects = 0;
        StringBuilder effectSB = new StringBuilder();
        String goalEffect = compile(aPLP.getGoal().createProperEffect());
        effectSB.append(goalEffect).append(" "); // TODO: FIX
        if (!goalEffect.equals("")) numEffects++;
        for (Effect se: aPLP.getSideEffects()){
            String eff = compile(se);
            if (!goalEffect.equals("")) numEffects++;
            effectSB.append(eff).append(" "); // TODO: FIX
        }

        if (numEffects > 1) sb.append("(and ");
        sb.append(effectSB.toString());

        sb.deleteCharAt(sb.length()-1);
        if (numEffects > 1) sb.append(")");
        sb.append("\n");
        //TODO: check if AND works fine

        sb.append(")");
        return sb.toString();
    }

    private static StringBuilder compileHeader(PLP aPLP, StringBuilder sb) {
        sb.append(":parameters (");

        Iterator<PLPParameter> execParamIterator = aPLP.getExecParams().iterator();
        while (execParamIterator.hasNext()) {
            sb.append("?").append(execParamIterator.next());
            if (execParamIterator.hasNext()) sb.append(" ");
        }
        sb.append(")\n");

        sb.append(":precondition (and ");

        Iterator<Condition> preCondIterator = aPLP.getPreConditions().iterator();
        execParamIterator = aPLP.getExecParams().iterator();
        Iterator<Effect> effectsIterator;
        boolean hasPreCond = false;

        while (preCondIterator.hasNext()) {
            Condition preCond = preCondIterator.next();

            boolean containsExecParam = false;
            while (execParamIterator.hasNext()) {
                PLPParameter param = execParamIterator.next();
                if (preCond.containsParam(param.getName())) {
                    sb.append(compile(preCond)).append(" "); // TODO: FIX
                    containsExecParam = true;
                    hasPreCond = true;
                    break;
                }
            }

            if (!containsExecParam) {
                effectsIterator = possibleEffects.iterator();
                while (effectsIterator.hasNext()) {
                    if (preCond.sharesParams(effectsIterator.next())) {
                        sb.append(compile(preCond)).append(" "); // TODO: FIX
                        hasPreCond = true;
                        break;
                    }
                }
            }
        }

        for (PLPParameter param : aPLP.getInputParams()) {
            for (ObservationGoal og : observableValues) {
                if (og.getClass().isAssignableFrom(PLPParameter.class) && og.containsParam(param.getName())) {
                    PLPParameter goal = ((PLPParameter) og);
                    sb.append("(K_").append(goal.getName().toUpperCase()).append(" ");
                    for (String field : goal.getParamFieldValues()) {
                        sb.append(field).append(" ");
                    }
                    sb.deleteCharAt(sb.length()-1);
                    sb.append(") ");

                    hasPreCond = true;
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

        if (hasPreCond) {
            sb.deleteCharAt(sb.length()-1);
            sb.append(")").append("\n");
        }
        if (!hasPreCond) sb.delete(sb.length() - 19, sb.length());
        return sb;
    }

    public static String compile(Formula formula) {
        if (formula.getOperator().equals("=") &&
                formula.getRightExpr().toUpperCase().equals("NULL")&&
                formula.getLeftExpr().matches(PLPParameter.PLPParameterRegex)) {

            for (PLPParameter param : observableValues) {
                if (param.containsParam(formula.getLeftExpr()))
                    return generateInformationLoss(formula.getLeftExpr());
            }

        }
        //if (!formula.getOperator().equals("=")) {
            // TODO: inform the user of unsupported formula
        //}
        return "(" + formula.getLeftExpr() + " " + formula.getOperator() + " " + formula.getRightExpr() + ")";
    }

    public static String generateInformationLoss(String parameter) {
        StringBuilder sb = new StringBuilder();
        sb.append("(not (K_");//.append(equality.getLeftExpr().toUpperCase()).append(" ");

        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(parameter);
        boolean isFirstMatch = true;
        while (matcher.find()) {
            sb.append((isFirstMatch ? matcher.group().toUpperCase() : matcher.group())).append(" ");
            isFirstMatch = false;
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("))");
        return sb.toString();
    }

    public static String compile(NotCondition nCond) {
        return "(not " + compile(nCond.getCondition()) + ")"; // TODO: FIX for a more generic case or throw error
    }

    public static String compile(Predicate predicate) {
        int stringLength = Arrays.toString(predicate.getValues().toArray()).length();
        if (stringLength <= 2)
            return "(" + predicate.getName() + ")";
        return "(" + predicate.getName() +
                " " + Arrays.toString(predicate.getValues().toArray()).substring(1, stringLength - 1).replaceAll(",", "") +
                ")";
    }

    public static String compile(QuantifiedCondition qCond) {
        boolean isForall = (qCond.getQuantifier() == QuantifiedCondition.Quantifier.FORALL);
        String paramsString = Arrays.toString(qCond.getParams().toArray());
        return (isForall ? "(forall (" : "(exists (") +
                paramsString.substring(1, paramsString.length() - 1) +
                ") " + compile(qCond.getCondition()) + ")"; // TODO: FIX
    }

    public static String compile(Condition c) {
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

    public static String compile(Effect e) {
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

    public static String compile(AssignmentEffect aEffect) {
        if (aEffect.getExpression().toUpperCase().equals("NULL")) {
            for (PLPParameter param : observableValues) {
                if (param.containsParam(aEffect.getParam().getName())) {
                    return generateInformationLoss(aEffect.getParam().toString());
                }
            }
        }
        //if (!formula.getOperator().equals("=")) {
        // TODO: inform the user of unsupported formula
        //}
        return "(" + aEffect.getParam() + " = " + aEffect.getExpression() + ")";
    }

    public static String compile(NotEffect nEffect) {
        return "(not " + compile(nEffect.getEffect()) + ")"; // TODO: FIX for a more generic case or throw error
    }

    public static String compile(ForAllEffect faEffect) {
        String paramsString = Arrays.toString(faEffect.getParams().toArray());
        return "(forall (" + paramsString.substring(1, paramsString.length() - 1) +
                ") " + compile(faEffect.getEffect()) + ")"; // TODO: FIX
    }

    public static String compile(AndEffect aEffect) {
        StringBuilder effectsSB = new StringBuilder();
        for (Effect e : aEffect.getEffects()) {
            effectsSB.append(compile(e)).append(" "); // TODO: fix
        }
        effectsSB.deleteCharAt(effectsSB.length()-1);
        return "(and " + effectsSB.toString() + ")";
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
