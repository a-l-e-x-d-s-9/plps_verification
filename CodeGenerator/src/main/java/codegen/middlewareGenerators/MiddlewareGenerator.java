package codegen.middlewareGenerators;

import codegen.common.ParameterGlue;
import codegen.common.PythonWriter;
import codegen.monitorGenerators.PLPClassesGenerator;
import codegen.monitorGenerators.PLPHarnessGenerator;
import codegen.monitorGenerators.PLPLogicGenerator;
import fr.uga.pddl4j.exceptions.UnexpectedExpressionException;
import fr.uga.pddl4j.parser.*;
import modules.ObservePLP;
import modules.PLP;
import plpFields.PLPParameter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MiddlewareGenerator {

    // TODO: parameter/variable history
    // TODO: constants

    // The PDDL domain that contains the relevant actions (Domain from pddl4j)
    public enum DomainType {
        NEAR_FULLY_OBSERVABLE, PARTIALLY_OBSERVABLE
    }
    private static Domain domain;
    private static HashMap<String,List<ParameterGlue>> triggerPublishers; // <topic_name,param_glues>
    public static DomainType domainType = DomainType.NEAR_FULLY_OBSERVABLE;

    public static void setDomain(Domain dom) {
        domain = dom;
    }

    public static String generateMiddleware(PLP plp, Op pddlAction, String plpFolderPath) {
        triggerPublishers = new HashMap<>();
        PLPHarnessGenerator.parameterLocations = new HashMap<>();
        PythonWriter writer = new PythonWriter();

        writer.writeResourceFileContent("/middleware/imports.py");
        writer.newLine();
        PLPHarnessGenerator.handleGlueFile(writer,plp,plpFolderPath);
        writer.newLine();

        writer.writeIndentedBlock(PLPClassesGenerator.GeneratePLPClasses(plp,false));

        writer.newLine();
        writer.newLine();

        // Dispatcher class
        writer.writeLine(String.format("class %s_dispatcher(object):", plp.getBaseName()));
        writer.indent();
        writer.newLine();

        // Constructor
        writer.writeLine("def __init__(self):");
        writer.indent();
        writer.writeLine("self.update_knowledge_client = rospy.ServiceProxy(\"/kcl_rosplan/update_knowledge_base\", KnowledgeUpdateService)");
        writer.writeLine("self.query_client = rospy.ServiceProxy(\"/kcl_rosplan/query_knowledge_base\", KnowledgeQueryService)");
        writer.writeLine("self.message_store = mongodb_store.message_store.MessageStoreProxy()");
        writer.writeLine("self.action_feedback_pub = rospy.Publisher(\"/kcl_rosplan/action_feedback\", ActionFeedback, queue_size=10)");
        writer.newLine();

        if (domainType == DomainType.PARTIALLY_OBSERVABLE) {
            writer.writeLine("self.change_assumptions_fail_client = rospy.ServiceProxy(\"/plp_middleware/change_assumptions_failed_action\", ChangeOnFail)");
            writer.writeLine("self.change_assumptions_contradiction_client = rospy.ServiceProxy(\"/plp_middleware/change_assumptions_contradiction\", ChangeOnContradiction)");
            writer.newLine();
        }

        // Create a map of the different trigger publishers
        for (PLPParameter execParam : plp.getExecParams()) {
            ParameterGlue paramGlue = PLPHarnessGenerator.parameterLocations.get(execParam.toString());
            if (paramGlue == null)
                throw new RuntimeException("Execution parameter: "+execParam.toString()+ " wasn't found in glue file");

            if (!triggerPublishers.containsKey(paramGlue.getRosTopic())) {
                List<ParameterGlue> lst = new LinkedList<>();
                lst.add(paramGlue);
                triggerPublishers.put(paramGlue.getRosTopic(),lst);
            }
            else {
                if (!triggerPublishers.get(paramGlue.getRosTopic()).get(0).getMessageType()
                        .equals(paramGlue.getMessageType())) {
                    throw new RuntimeException("Execution parameters with the same topic have different message types in glue file");
                }
                triggerPublishers.get(paramGlue.getRosTopic()).add(paramGlue);
            }
        }

        // Generate trigger publishers

        int publisherCounter = 0;
        for (List<ParameterGlue> gluelst : triggerPublishers.values()) {
            writer.writeLine(String.format("self.action_publisher_%d = " +
                    "rospy.Publisher(\"" + gluelst.get(0).getRosTopic() + "\", " + gluelst.get(0).getMessageType() +
                    ", queue_size=10)", publisherCounter));
            publisherCounter++;
        }

        if (plp.getExecParams().size() == 0) {
            writer.writeLine("# TODO: Implement subscriber/client to trigger module execution - no execution parameters defined.");
        }

        writer.newLine();
        writer.writeLine(String.format("self.plp_params = PLP_%s_parameters()",plp.getBaseName()));
        writer.writeLine(String.format("self.plp_vars = PLP_%s_variables()",plp.getBaseName()));
        writer.writeLine("self.current_action = None");
        if (domainType == DomainType.PARTIALLY_OBSERVABLE &&
                plp.getClass().isAssignableFrom(ObservePLP.class) &&
                !((ObservePLP) plp).isGoalParameter()) {
            writer.writeLine("self.sense_contradiction = None");
        }
        writer.newLine();

        writer.writeLine("rospy.Subscriber(\"/kcl_rosplan/action_dispatch\", ActionDispatch, self.dispatch_action)");
        writer.newLine();

        // Subscribe to parameter topics
        writer.writeLine("# TODO: uncomment the following lines to receive values for input parameters if needed");
        writer.writeLine("# For example, for more trigger conditions or variable calculation");
        writer.newLine();
        PLPHarnessGenerator.generateAllParamTopics(writer,plp,false);

        writer.dendent();
        writer.newLine();

        // Parameters update callback functions
        for (PLPParameter param : plp.getInputParams()) {
            PLPHarnessGenerator.generateParameterUpdateFunction(writer, param, false, false, false);
        }
        for (PLPParameter param : plp.getOutputParams()) {
            PLPHarnessGenerator.generateParameterUpdateFunction(writer, param, false, true, false);
        }

        // Parameters_updated function
        writer.newLine();
        writer.writeLine("def parameters_updated(self):");
        writer.indent();
        writer.writeLine("self.calculate_variables()");
        writer.writeLine("if self.current_action == None:");
        writer.indent();
        writer.writeLine("return");
        writer.dendent();
        writer.newLine();
        if (domainType == DomainType.PARTIALLY_OBSERVABLE &&
                plp.getClass().isAssignableFrom(ObservePLP.class) &&
                !((ObservePLP) plp).isGoalParameter()) {
            writer.writeLine("# Validate with the assumption manager that the value sensed is the value assumed");
            writer.writeLine("if self.plp_params." + ((ObservePLP) plp).getResultParameter().simpleString()
                    + " is not None:");
            writer.indent();
            writer.writeLine("parametersDic = self.toDictionary(self.current_action.parameters)");
            writer.writeLine("req = ChangeOnContradictionRequest()");
            Exp pCond;
            try {
                pCond = pddlAction.getEffects().getChildren().get(0).getChildren().get(0);
            }
            catch (Exception e) {
                throw new RuntimeException("PDDL sensing action: "+pddlAction.getName()+" is not of valid compilation result format");
            }
            if (pCond.getConnective() != Connective.ATOM)
                throw new RuntimeException("Contradiction detection support is currently just for ATOM");
            StringBuilder sb = new StringBuilder("\"(").append(pCond.getAtom().get(0).getImage()).append("\"");
            for (int i=1;i<pCond.getAtom().size();i++)
                sb.append("+ \" \" +").append(" parametersDic[\"").append(pCond.getAtom().get(i).getImage().replace("?","")).append("\"]");
            sb.append("+ \")\"");
            writer.writeLine("gPred = " + sb.toString());
            writer.writeLine("req.grounded_pred = gPred");
            PLPParameter resultParam = ((ObservePLP) plp).getResultParameter();
            ParameterGlue mapping = PLPHarnessGenerator.parameterLocations.get(resultParam.toString());
            if (mapping == null)
                throw new RuntimeException("[" + plp.getBaseName() + "] No glue mapping for result parameter: "+resultParam.toString());
            if (mapping.hasFieldInMessage() && mapping.getFieldType().substring(0,4).equals("bool"))
                writer.writeLine("req.sensed = self.plp_params."+resultParam.simpleString());
            else if ((mapping.hasFieldInMessage() && mapping.getFieldType().equals("Bool")) ||
                    (!mapping.hasFieldInMessage() && mapping.getMessageType().equals("Bool")))
                writer.writeLine("req.sensed = self.plp_params."+resultParam.simpleString()+".data");
            else
                writer.writeLine("req.sensed = # TODO: boolean result value from self.plp_params."+resultParam.simpleString());
            writer.writeLine("self.sense_contradiction = self.change_assumptions_contradiction_client.call(req)");
            writer.dendent();
            writer.newLine();
        }
        writer.writeLine("if self.detect_success():");
        writer.indent();
        writer.writeLine(String.format("rospy.loginfo(\"%s_action_dispatcher: detected success\")", plp.getBaseName()));
        writer.writeLine("self.update_success()");
        writer.writeLine("self.reset_dispatcher()");
        writer.dendent();
        writer.writeLine("elif self.detect_failures():");
        writer.indent();
        writer.writeLine(String.format("rospy.loginfo(\"%s_action_dispatcher: detected failure\")", plp.getBaseName()));
        writer.writeLine("self.update_fail()");
        writer.writeLine("self.reset_dispatcher()");
        writer.dendent();
        writer.dendent();
        writer.newLine();

        // Variables functions
        PLPLogicGenerator.generateVariablesFunctions(plp,writer,false);

        // Condition checker functions
        PLPLogicGenerator.conditionMethods = new HashMap<>();
        PLPLogicGenerator.generateAllConditionCheckers(writer,plp,false);
        writer.newLine();

        // Termination checker functions
        PLPLogicGenerator.generateTerminationDetectors(writer,plp,false);
        writer.newLine();

        // DispatchAction function (gets the command from ROSPlan and activates trigger)
        writer.writeLine("def dispatch_action(self, action):");
        writer.indent();
        writer.writeLine("if not action.name == \"" + plp.getBaseName() + "\":");
        writer.indent();
        writer.writeLine("return");
        writer.dendent();
        writer.writeLine("self.current_action = action");
        writer.writeLine("self.sense_contradiction = None");
        writer.newLine();

        if (plp.getExecParams().size() > 0) {
            writer.writeLine("for pair in action.parameters:");
            writer.indent();
            for (PLPParameter execParam : plp.getExecParams()) {
                writer.writeLine("if pair.key == \"" + execParam.simpleString().toLowerCase() + "\":");
                // TODO: do the PDDL->PLP mapping and maybe remove tolowercase?
                writer.indent();
                if (!PLPHarnessGenerator.parameterLocations.containsKey(execParam.toString()))
                    throw new RuntimeException("Execution param: " + execParam.toString() + " doesn't have a glue mapping");
                ParameterGlue mapping = PLPHarnessGenerator.parameterLocations.get(execParam.toString());
                writer.writeLine("# Query the DB to get the real value of the PDDL parameter value received");
                writer.writeLine("query_result = self.message_store.query_named(pair.value, " +
                        (mapping.hasFieldInMessage() ? mapping.getFieldType() : mapping.getMessageType()) + "._type, False)");
                writer.writeLine("# If there isn't a special value sent, use the PDDL parameter value received");
                writer.writeLine("if not query_result:");
                writer.indent();
                writer.writeLine("self.plp_params." + execParam.simpleString() + " = pair.value");
                writer.dendent();
                writer.writeLine("else:");
                writer.indent();
                writer.writeLine("self.plp_params." + execParam.simpleString() + " = query_result[0][0]");
                writer.dendent();
                writer.dendent();
                writer.newLine();
            }
            writer.dendent();

            writer.writeLine("# If some of the execution parameters weren't assigned:");
            writer.writeLine("# Check if they were saved as output params from other modules");
            for (PLPParameter execParam : plp.getExecParams()) {
                //if (!PLPHarnessGenerator.parameterLocations.containsKey(execParam.toString()))
                writer.writeLine("if not self.plp_params." + execParam.simpleString() + ":");
                writer.indent();
                ParameterGlue mapping = PLPHarnessGenerator.parameterLocations.get(execParam.toString());
                writer.writeLine(String.format("query_result = self.message_store.query_named(\"output_%s\", %s._type, False)",
                        execParam.simpleString(), (mapping.hasFieldInMessage() ? mapping.getFieldType() : mapping.getMessageType())));
                writer.writeLine("if query_result:");
                writer.indent();
                writer.writeLine(String.format("self.plp_params.%s = query_result[0][0]", execParam.simpleString()));
                writer.dendent();
                writer.dendent();
            }
            writer.newLine();
            writer.writeLine("# TODO: if the execution parameters have default values, add them here using self.plp_params.<param_name> = <val>");
            writer.newLine();
            writer.writeLine("# Check if the action can be dispatched (every execution parameter has a value)");
        }
        writer.writeLine("if self.check_can_dispatch():");
        writer.indent();

        if (plp.getExecParams().size() > 0) {
            publisherCounter = 0;
            for (List<ParameterGlue> gluelst : triggerPublishers.values()) {
                if (publisherCounter > 0) writer.newLine();
                if (gluelst.size() > 1) {
                    writer.writeLine(String.format("in_%s = %s()",
                            gluelst.get(0).getMessageType().toLowerCase(), gluelst.get(0).getMessageType()));
                    for (ParameterGlue parGlue : gluelst) {
                        writer.writeLine(String.format("in_%s.%s = self.plp_params.%s",
                                parGlue.getMessageType().toLowerCase(),
                                parGlue.getField(), parGlue.getParameterName()));
                    }
                    writer.writeLine(String.format("self.action_publisher_%d.publish(in_%s)",
                            publisherCounter, gluelst.get(0).getMessageType().toLowerCase()));
                } else {
                    writer.writeLine(String.format("self.action_publisher_%d.publish(self.plp_params.%s)",
                            publisherCounter, gluelst.get(0).getParameterName()));
                }
                publisherCounter++;
            }
        }
        else {
            writer.writeLine("# TODO: Implement PLP module triggering (module dispatch) - no execution parameters defined");
        }
        writer.dendent();
        writer.writeLine("else:");
        writer.indent();
        writer.writeLine("rospy.loginfo(\"Failed at running action: %s. Conditions not met for dispatch\", action.name)");
        writer.dendent();
        writer.dendent();

        // CheckCanDispatch function
        writer.newLine();
        writer.writeLine("def check_can_dispatch(self):");
        writer.indent();
        writer.writeLine("canDispatch = True");
        if (plp.getExecParams().size() > 0) {
            writer.write("if (");
            int paramCounter = 0;
            for (PLPParameter execParam : plp.getExecParams()) {
                if (paramCounter > 0)
                    writer.writeNoIndent(" or ");
                writer.writeNoIndent("self.plp_params." + execParam.simpleString() + " is None");
                paramCounter++;
            }
            writer.writeNoIndent("):");
            writer.newLine();
            writer.indent();
            writer.writeLine("canDispatch = False");
            writer.dendent();
            writer.newLine();
        }
        else {
            writer.writeLine("# No defined execution parameters for module");
        }
        writer.writeLine("# TODO: Optionally, add more trigger requirements using self.plp_params.<parameter_name> and/or self.plp_vars.<variable_name>");
        writer.newLine();

        writer.writeLine("return canDispatch");
        writer.dendent();

        // Update Success Function
        writer.newLine();
        writer.writeLine("def update_success(self):");
        writer.indent();
        writer.writeLine("# Update the effects in the KMS");
        generateKMSUpdates(writer, pddlAction);
        writer.writeLine("# Update the Planning System on failure");
        writer.writeLine("actionFeedback = ActionFeedback()");
        writer.writeLine("actionFeedback.action_id = self.current_action.action_id");
        writer.writeLine("actionFeedback.status = \"action achieved\"");
        writer.writeLine("self.action_feedback_pub.publish(actionFeedback)");
        writer.dendent();

        // Update Fail Function
        writer.newLine();
        writer.writeLine("def update_fail(self):");
        writer.indent();
        writer.writeLine("actionFeedback = ActionFeedback()");
        writer.writeLine("actionFeedback.action_id = self.current_action.action_id");
        writer.writeLine("actionFeedback.status = \"action failed\"");
        writer.writeLine("self.action_feedback_pub.publish(actionFeedback)");
        writer.dendent();

        // KMS UPDATES FUNCTIONS
        writer.newLine();
        writer.writeResourceFileContent("/middleware/KMSUpdateFunction.py");
        //

        if (domainType == DomainType.PARTIALLY_OBSERVABLE) {
            writer.newLine();
            writer.writeResourceFileContent("/middleware/assumptionManagerCalls.py");
        }

        // Reset dispatcher function
        writer.newLine();
        writer.writeLine("def reset_dispatcher(self):");
        writer.indent();
        writer.writeLine(String.format("self.plp_params = PLP_%s_parameters()",plp.getBaseName()));
        writer.writeLine(String.format("self.plp_vars = PLP_%s_variables()",plp.getBaseName()));
        writer.writeLine("self.current_action = None");
        if (domainType == DomainType.PARTIALLY_OBSERVABLE &&
                plp.getClass().isAssignableFrom(ObservePLP.class) &&
                !((ObservePLP) plp).isGoalParameter()) {
            writer.writeLine("self.sense_contradiction = None");
        }
        writer.dendent();

        // Main Function
        writer.newLine();
        writer.newLine();
        writer.dendent();
        writer.writeLine("if __name__ == '__main__':");
        writer.indent();
        writer.writeLine("try:");
        writer.indent();
        writer.writeLine("rospy.init_node(\"plp_" + plp.getBaseName() + "_action_dispatcher\", anonymous=False)");
        writer.writeLine("rospy.loginfo(\"Starting " + plp.getBaseName() + " action dispatcher\")");
        writer.writeLine(plp.getBaseName() + "_dispatcher()");
        writer.writeLine("rospy.spin()");
        writer.newLine();
        writer.dendent();
        writer.writeLine("except rospy.ROSInterruptException:");
        writer.indent();
        writer.writeLine("pass");
        writer.dendent();
        writer.dendent();

        return writer.end();
    }

    private static void generateKMSUpdates(PythonWriter writer, Op pddlAction) {
        writer.writeLine("parametersDic = self.toDictionary(self.current_action.parameters)");
        writeKMSUpdate(pddlAction.getEffects(), writer, false);
    }

    // Currently doesn't support nested quantified effects (one quantified variable)
    // Currently doesn't support conditional effects
    /**
     * Writes to writer a python block that updates the KMS on the given effects.
     * @param effects The effects to update the KMS on
     * @param writer The writer to write the python block to
     * @param inForAll Indicates if this is a recursive call (if the effects were quantified)
     */
    private static void writeKMSUpdate(Exp effects, PythonWriter writer, boolean inForAll) {
        if (effects.getConnective().equals(Connective.AT_START)
                || effects.getConnective().equals(Connective.AT_END)) {
            writeKMSUpdate(effects.getChildren().get(0), writer, inForAll);
        }
        else if (effects.getConnective().equals(Connective.AND)) {
            for (Exp effect : effects.getChildren()) {
                writeKMSUpdate(effect, writer, inForAll);
            }
        }
        else if (effects.getConnective().equals(Connective.FORALL)) {
            writer.writeLine("instance_query_client = rospy.ServiceProxy(\"/kcl_rosplan/get_current_instances\", GetInstanceService)");
            writer.writeLine("forAllInstances = instance_query_client.call(\"" +
                    effects.getVariables().get(0).getTypes().get(0).toString() + "\").instances");
            writer.writeLine("for forAllInstance in forAllInstances:");
            writer.indent();
            writeKMSUpdate(effects.getChildren().get(0), writer, true);
            writer.dendent();
        }
        else if (effects.getConnective().equals(Connective.NOT)) {
            Exp child = effects.getChildren().get(0);
            if (!child.getConnective().equals(Connective.ATOM)) {
                System.err.println("Unsupported connective after NOT effect: " + child.getConnective());
            }
            else {
                String changeType = "KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE";
                changeAtomEffect(effects.getChildren().get(0), writer, inForAll, changeType);
            }
        }
        else if (effects.getConnective().equals(Connective.ATOM)) {
            String changeType = "KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE";
            changeAtomEffect(effects, writer, inForAll, changeType);
        }
        else if (effects.getConnective().equals(Connective.WHEN)) {
            writer.newLine();
            writer.writeLine("result = True");
            queryConditionValue(effects.getChildren().get(0), writer, false, false);
            writer.writeLine("if result:");
            writer.indent();
            writeKMSUpdate(effects.getChildren().get(1), writer, inForAll);
            writer.dendent();
            writer.newLine();
        }
        else {
            System.err.println("Unsupported connective type: " + effects.getConnective());
        }
    }

    /**
     * Helper function is called by writeKMSUpdate. It does the writing itself.
     * @param effects The effects to update the KMS on
     * @param writer The writer to write the python block to
     * @param inForAll If the effects were quantified
     * @param changeType ADD knowledge or REMOVE knowledge
     */
    private static void changeAtomEffect(Exp effects, PythonWriter writer, boolean inForAll, String changeType) {
        //System.out.println(effects.getConnective().toString());
        List<TypedSymbol> predArgs = getPredParams(effects.getAtom().get(0).toString());
        writer.write("self.changeKMSFact(\""+effects.getAtom().get(0).toString()+"\", [");
        for (int i=1;i<effects.getAtom().size();i++) {
            if (inForAll) {
                writer.writeNoIndent("[\"" + predArgs.get(i-1).getImage().substring(1) + "\", " +
                        "parametersDic[\"" + effects.getAtom().get(i).toString().substring(1) + "\"]"
                        + " if \"" + effects.getAtom().get(i).toString().substring(1) +
                        "\" in parametersDic else forAllInstance]");
            }
            else {
                writer.writeNoIndent("[\"" + predArgs.get(i - 1).getImage().substring(1) + "\", " +
                        "parametersDic[\"" + effects.getAtom().get(i).toString().substring(1) + "\"]]");
            }
        }
        writer.writeNoIndent("], " + changeType + ")");
        writer.newLine();
    }


    private static void queryConditionValue(Exp exp, PythonWriter writer, boolean inForAll, boolean inNot) {
        if (exp.getConnective().equals(Connective.AT_START)
                || exp.getConnective().equals(Connective.AT_END)
                || exp.getConnective().equals(Connective.OVER_ALL)) {
            queryConditionValue(exp.getChildren().get(0), writer, inForAll, inNot);
        }
        else if (exp.getConnective().equals(Connective.AND)) {
            for (Exp child : exp.getChildren()) {
                queryConditionValue(child,writer,inForAll, inNot);
            }
        }
        else if (exp.getConnective().equals(Connective.FORALL)) {
            if (inForAll) {
                throw new RuntimeException("Currently unsupported nested quantified conditions: "+exp.toString());
            }
            writer.writeLine("instance_query_client = rospy.ServiceProxy(\"/kcl_rosplan/get_current_instances\", GetInstanceService)");
            writer.writeLine("forAllInstances = instance_query_client.call(\"" +
                    exp.getVariables().get(0).getTypes().get(0).toString() + "\").instances");
            writer.writeLine("result_fa = True");
            writer.writeLine("for forAllInstance in forAllInstances:");
            writer.indent();
            queryConditionValue(exp.getChildren().get(0), writer, true, inNot);
            writer.dendent();
            writer.writeLine("result = result & " + (inNot ? "(not result_fa" : "result_fa)"));
        }
        else if (exp.getConnective().equals(Connective.NOT)) {
            Exp child = exp.getChildren().get(0);
            queryConditionValue(exp.getChildren().get(0), writer, inForAll, !inNot);
        }
        else if (exp.getConnective().equals(Connective.ATOM)) {
            queryAtomValue(exp, writer, inForAll, inNot);
        }
        else {
            System.err.println("Unsupported connective type: " + exp.getConnective());
        }
    }

    /**
     * Helper function is called by queryConditionValue. It queries for atom conditions
     * @param condition The effects to update the KMS on
     * @param writer The writer to write the python block to
     * @param inForAll If the condition was quantified
     */
    private static void queryAtomValue(Exp condition, PythonWriter writer, boolean inForAll, boolean inNot) {
        //System.out.println(effects.getConnective().toString());
        List<TypedSymbol> predArgs = getPredParams(condition.getAtom().get(0).toString());
        String resParName = (inForAll ? "result_fa" : "result");
        writer.write((inNot ? resParName + " = " + resParName +" & (not " :
                resParName + " = " + resParName +" & ")+"self.validateKMSFact(\""+condition.getAtom().get(0).toString()+"\", [");
        for (int i=1;i<condition.getAtom().size();i++) {
            if (inForAll) {
                writer.writeNoIndent("[\"" + predArgs.get(i-1).getImage().substring(1) + "\", " +
                        "parametersDic[\"" + condition.getAtom().get(i).toString().substring(1) + "\"]"
                        + " if \"" + condition.getAtom().get(i).toString().substring(1) +
                        "\" in parametersDic else forAllInstance]");
            }
            else {
                writer.writeNoIndent("[\"" + predArgs.get(i - 1).getImage().substring(1) + "\", " +
                        "parametersDic[\"" + condition.getAtom().get(i).toString().substring(1) + "\"]]");
            }
        }
        writer.writeNoIndent("])" + (inNot ? ")" : ""));
        writer.newLine();
    }

    /**
     * Returns a list of the predicate's parameters.
     * @param predName The name of the predicate
     * @return The predicate's parameters
     */
    private static List<TypedSymbol> getPredParams(String predName) {
        for (NamedTypedList pred : domain.getPredicates()) {
            if (pred.getName().toString().equals(predName)) {
                return pred.getArguments();
            }
        }
        System.err.println("Didn't find predicate of name: " + predName);
        return null;
    }

    /*public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: <pddl-domain-path> <output-path>");
            return;
        }

        Parser pddlParser = new Parser();
        try {
            pddlParser.parseDomain(args[0]);
        } catch (FileNotFoundException e) {
            System.err.println("Wrong path to domain file");
        }

        MiddlewareGenerator gen = new MiddlewareGenerator();
        Domain domain = pddlParser.getDomain();
        String KMSUpdates = gen.generateKMSUpdates(domain);
        PrintWriter writer;
        try {
            writer = new PrintWriter(args[1]);
            writer.println(KMSUpdates);
            writer.close();
            System.out.println("Output file at: "+args[1]);
        } catch (FileNotFoundException e) {
            System.err.println("Bad output file path");
        }
    }*/
}
