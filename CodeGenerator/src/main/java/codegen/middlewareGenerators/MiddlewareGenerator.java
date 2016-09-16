package codegen.middlewareGenerators;

import codegen.common.ParameterGlue;
import codegen.common.PythonWriter;
import codegen.monitorGenerators.PLPClassesGenerator;
import codegen.monitorGenerators.PLPHarnessGenerator;
import codegen.monitorGenerators.PLPLogicGenerator;
import fr.uga.pddl4j.parser.*;
import modules.PLP;
import plpFields.PLPParameter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MiddlewareGenerator {

    // TODO: parameter/variable history
    // TODO: constants
    // TODO: remove set functions from plp_parameters and comment out irrelevant parameter topics and write "uncomment only if needed for trigger"

    // The PDDL domain that contains the relevant actions (Domain from pddl4j)
    private static Domain domain;
    private static HashMap<String,List<ParameterGlue>> triggerPublishers; // <topic_name,param_glues>

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
        writer.writeLine("self.message_store = mongodb_store.message_store.MessageStoreProxy()");
        writer.writeLine("self.action_feedback_pub = rospy.Publisher(\"/kcl_rosplan/action_feedback\", ActionFeedback, queue_size=10)");
        writer.newLine();

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
                writer.writeLine(String.format("self.plp_params.%1$s = self.message_store.query_named(\"output_%1$s\", %2$s._type, False)",
                        execParam.simpleString(), (mapping.hasFieldInMessage() ? mapping.getFieldType() : mapping.getMessageType())));
                writer.dendent();
            }
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

        // Reset dispatcher function
        writer.newLine();
        writer.writeLine("def reset_dispatcher(self):");
        writer.indent();
        writer.writeLine(String.format("self.plp_params = PLP_%s_parameters()",plp.getBaseName()));
        writer.writeLine(String.format("self.plp_vars = PLP_%s_variables()",plp.getBaseName()));
        writer.writeLine("self.current_action = None");
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

    /**
     * Returns a list of the predicate's parameters.
     * @param predName The name of the predicate
     * @return The prdicate's parameters
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
