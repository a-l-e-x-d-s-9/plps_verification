package cs.bgu.maorash.compiler;

import cs.bgu.maorash.plps.etc.*;
import cs.bgu.maorash.plps.modules.AchievePLP;
import cs.bgu.maorash.plps.modules.ObservePLP;
import cs.bgu.maorash.plps.modules.PLP;
import cs.bgu.maorash.plps.plpFields.ObservationGoal;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PDDLCompiler {

    private static List<AchievePLP> achievePLPs;
    private static List<ObservePLP> observePLPs;
    private static List<PLPParameter> observableValues;
    private static List<Condition> possibleEffects;

    public static String producePDDL (String dirPath) {
        loadPLPs(dirPath);

        observableValues = new LinkedList<>();
        possibleEffects = new LinkedList<>();

        for (ObservePLP oPLP : observePLPs) {
            if (oPLP.getGoal().getClass().isAssignableFrom(PLPParameter.class)) {
                observableValues.add((PLPParameter) oPLP.getGoal());
            }
            possibleEffects.addAll(oPLP.getSideEffects());
        }

        for (AchievePLP aPLP : achievePLPs) {
            possibleEffects.addAll(aPLP.getGoals());
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
        sb.append("(:action ").append("observe_").append(oPLP.getBaseName()).append("\n");

        compileHeader(oPLP,sb);

        sb.append(":effect (and ");

        if (oPLP.getGoal().getClass().isAssignableFrom(PLPParameter.class)) {
            PLPParameter goal = ((PLPParameter)oPLP.getGoal());
            sb.append("(K_").append(goal.getName().toUpperCase()).append(" ");
            for (String field : goal.getParamFieldValues()) {
                sb.append(field).append(" ");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append(") ");
        }

        for (Condition se: oPLP.getSideEffects()){
            sb.append(se.toPDDL()).append(" ");
        }

        sb.deleteCharAt(sb.length()-1);
        sb.append(")").append("\n");
        //TODO: change goals into one goal with AND

        sb.append(")");
        return sb.toString();
    }

    public static String compile(AchievePLP aPLP) {
        StringBuilder sb = new StringBuilder();
        sb.append("(:action ").append("achieve_").append(aPLP.getBaseName()).append("\n");

        compileHeader(aPLP,sb);

        sb.append(":effect (and ");
        for (Condition c: aPLP.getGoals()) {
            sb.append(c.toPDDL()).append(" ");
        }
        for (Condition se: aPLP.getSideEffects()){
            sb.append(se.toPDDL()).append(" ");
        }

        sb.deleteCharAt(sb.length()-1);
        sb.append(")").append("\n");
        //TODO: change goals into one goal with AND

        sb.append(")");
        return sb.toString();
    }

    private static StringBuilder compileHeader(PLP aPLP, StringBuilder sb) {
        sb.append(":parameters (");

        Iterator<PLPParameter> execParamIterator = aPLP.getInputExecParams().iterator();
        while (execParamIterator.hasNext()) {
            sb.append("?").append(execParamIterator.next());
            if (execParamIterator.hasNext()) sb.append(" ");
        }
        sb.append(")\n");

        sb.append(":precondition (and ");

        Iterator<Condition> preCondIterator = aPLP.getPreConditions().iterator();
        execParamIterator = aPLP.getInputExecParams().iterator();
        Iterator<Condition> effectsIterator;
        boolean hasPreCond = false;

        while (preCondIterator.hasNext()) {
            Condition preCond = preCondIterator.next();

            boolean containsExecParam = false;
            while (execParamIterator.hasNext()) {
                PLPParameter param = execParamIterator.next();
                if (preCond.containsParam(param)) {
                    sb.append(preCond.toPDDL()).append(" ");
                    containsExecParam = true;
                    hasPreCond = true;
                    break;
                }
            }

            if (!containsExecParam) {
                effectsIterator = possibleEffects.iterator();
                while (effectsIterator.hasNext()) {
                    if (preCond.sharesParams(effectsIterator.next())) {
                        sb.append(preCond.toPDDL()).append(" ");
                        hasPreCond = true;
                        break;
                    }
                }
            }
        }

        for (PLPParameter param : aPLP.getInputParams()) {
            for (ObservationGoal og : observableValues) {
                if (og.getClass().isAssignableFrom(PLPParameter.class) && og.containsParam(param)) {
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
        /*for (PLPParameter param : aPLP.getInputExecParams()) {
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


    public static String compile(Equality equality) {
        if (equality.getRightExpr().toUpperCase().equals("NULL")
                && equality.getLeftExpr().getClass().isAssignableFrom(PLPParameter.class)) {

            StringBuilder sb = new StringBuilder();
            sb.append("(not (K_").append(equality.getLeftExpr().getName().toUpperCase()).append(" ");
            for (String field : equality.getLeftExpr().getParamFieldValues()) {
                sb.append(field).append(" ");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("))");
            return sb.toString();
        }
        return "(" + equality.getLeftExpr() + " = " + equality.getRightExpr() + ")";
    }

    public static String compile(NotCondition nCond) {
        return "(not " + nCond.getCondition().toPDDL() + ")";
    }

    public static String compile(Predicate predicate) {
        int stringLength = Arrays.toString(predicate.getValues().toArray()).length();
        if (stringLength <= 2)
            return "(" + predicate.getName() + ")";
        return "(" + predicate.getName() +
                " " + Arrays.toString(predicate.getValues().toArray()).substring(1, stringLength - 1).replaceAll(",", "") +
                ")";
    }

    public static String compile(ForAllCondition faCond) {
        int stringLength = Arrays.toString(faCond.getForAllParams().toArray()).length();
        return "(forall (" + Arrays.toString(faCond.getForAllParams().toArray()).substring(1, stringLength - 1) +
                ") " + faCond.getCondition().toPDDL() + ")";
    }

    public static void loadPLPs(String dirPath) {

        /*try {
            Files.walk(Paths.get(dirPath)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                }
            });
        } catch (IOException e) {
            System.out.println("Error loading directory\n"+e);
        }*/

        achievePLPs = new LinkedList<>();
        observePLPs = new LinkedList<>();

        AchievePLP walkThroughGateway = new AchievePLP("walk_through_gateway");
        walkThroughGateway.addInputExecParam("areaA");
        walkThroughGateway.addInputExecParam("areaB");
        walkThroughGateway.addInputExecParam("gateway");

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
        Equality pre3 = new Equality("current_Aspeed", "0");
        Equality pre4 = new Equality("current_Lspeed", "0");
        walkThroughGateway.addPreCondition(pre1);
        walkThroughGateway.addPreCondition(pre2);
        walkThroughGateway.addPreCondition(pre3);
        walkThroughGateway.addPreCondition(pre4);

        NotCondition conC = new NotCondition(new Predicate("arm_moving"));
        walkThroughGateway.addConcurrencyCondition(conC);

        PLPParameter gatewaylocforall = new PLPParameter("gateway_location");
        gatewaylocforall.addParamFieldValue("gw");
        Equality temp = new Equality(gatewaylocforall,"Null");
        ForAllCondition sideE = new ForAllCondition(temp);
        sideE.addParam("gw");
        walkThroughGateway.addSideEffect(sideE);
        Predicate sideE2 = new Predicate("at");
        sideE2.addValue("areaA");
        walkThroughGateway.addSideEffect(new NotCondition(sideE2));

        Predicate goal = new Predicate("at");
        goal.addValue("areaB");
        walkThroughGateway.addGoal(goal);

        ObservePLP observeGateway = new ObservePLP("gatewayAB");

        observeGateway.addInputExecParam("areaA");
        observeGateway.addInputExecParam("areaB");
        observeGateway.addInputExecParam("gateway");

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
        Equality pre7 = new Equality("current_Aspeed", "0");
        Equality pre8 = new Equality("current_Lspeed", "0");
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

    }

}
