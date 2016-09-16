package old_codeGen;

import modules.PLP;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import plpEtc.FieldType;
import plpFields.Constant;
import plpFields.PLPParameter;
import plpFields.ProgressMeasure;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PLPHarnessGenerator {

    private static Map<String,ParameterGlue> parameterLocations;
    public static String GeneratePLPHarness(PLP plp, String path) {

        parameterLocations = new HashMap<>();
        PythonWriter generator = new PythonWriter();

        generator.writeLine("import rospy");
        generator.writeLine("import sys");

        generator.newLine();
        handleGlueFile(generator, plp, path);

        generator.writeLine(String.format("from %s.msg import PlpMessage",CodeGenerator.packageName));
        generator.writeLine(String.format("from PLP%s import *",plp.getBaseName()));
        generator.writeLine(String.format("from PLP%sClasses import *",plp.getBaseName()));
        generator.newLine();
        generator.writeLine(String.format("PLP_TOPIC = \"%s\"",CodeGenerator.outputTopic));

        generator.newLine();
        generator.writeLine(String.format("class Plp%sRosHarness(object):",plp.getBaseName()));
        generator.indent();
        generator.newLine();
        generateInitFunction(generator,plp);


        generator.writeFileContent(PLPHarnessGenerator.class.getResource("/HarnessMain.txt").getPath(),
                plp.getBaseName(), plp.getClass().getSimpleName());

        generator.dendent();
        // Capture method
        generator.newLine();
        generator.writeLine("def capture_params(self):");
        generator.indent();
        generator.writeLine("capture_file = open(self.capture_filename, \"w\")");
        for (PLPParameter param : plp.getExecParams()) {
            generateCaptureParameter(generator, param);
        }
        for (PLPParameter param : plp.getInputParams()) {
            generateCaptureParameter(generator, param);
        }
        for (PLPParameter param : plp.getOutputParams()) {
            generateCaptureParameter(generator, param);
        }
        generator.newLine();
        generator.writeLine("capture_file.close()");
        generator.writeLine("rospy.loginfo(\"<PLP:" + plp.getBaseName() +"> captured parameters at trigger time to file '%s'\" % self.capture_filename)");
        generator.dendent();
        generator.newLine();

        for (PLPParameter param : plp.getExecParams()) {
            generateParameterUpdateFunction(generator, param, true);
        }
        for (PLPParameter param : plp.getInputParams()) {
            generateParameterUpdateFunction(generator, param, false);
        }
        for (PLPParameter param : plp.getOutputParams()) {
            generateParameterUpdateFunction(generator, param, false);
        }

        // trigger function
        generator.writeLine("def check_trigger(self):");
        generator.indent();
        generator.writeLine("# The execution parameters are considered the trigger");
        generator.writeLine("# If you require that other parameters are assigned add them using self.plp_params.<param_name> and uncomment the relevant line in the update function above");
        generator.writeLine(String.format("# All the parameters are defined in PLP%sClasses.py",plp.getBaseName()));
        generator.writeLine("# Any other condition can be added, too");
        StringBuilder triggerCheck = new StringBuilder();
        triggerCheck.append("return not (");
        for (PLPParameter param : plp.getExecParams()) {
            triggerCheck.append(String.format("(self.parameters.%s is None) or ",param.simpleString()));
        }
        // TODO: check if no params
        triggerCheck.delete(triggerCheck.length()-4,triggerCheck.length());
        triggerCheck.append(")");
        generator.writeLine(triggerCheck.toString());
        generator.dendent();
        generator.newLine();
        generator.dendent();

        // main function
        generator.writeLine("if __name__ == '__main__':");
        generator.indent();
        generator.writeLine("try:");
        generator.indent();
        generator.writeLine(String.format("rospy.loginfo(\"<PLP:%s> node starting\")",plp.getBaseName()));
        generator.writeLine("harness = PlpWaypointRosHarness()");
        generator.writeLine("rospy.loginfo(\"<PLP:%s> started\")");
        generator.writeLine("# Progress measures callbacks");
        for (ProgressMeasure pm : plp.getProgressMeasures()) {
            generator.writeLine(String.format("rospy.Timer(rospy.Duration(%s), harness.plp.monitor_progress_%s)",
                    pm.getFrequency(),pm.getCondition().simpleString()));
        }
        generator.writeLine("rospy.spin()");
        generator.dendent();
        generator.writeLine("except rospy.ROSInterruptException:");
        generator.writeLine("pass");
        generator.dendent();
        return generator.end();
    }

    private static void generateInitFunction(PythonWriter generator, PLP plp) {
        generator.writeLine("def __init__(self):");
        generator.newLine();
        generator.indent();
        generator.writeLine("self.plp_constants = {");
        generator.indent();
        for (Constant constant : plp.getConstants()) {
            String constantLine = String.format("\"%s\": ",constant.getName());
            if (constant.getValue() == null)
                constantLine += "''' TODO: wasn't specified ''',";
            else {
                if (constant.getType().equals(FieldType.String))
                    constantLine += String.format("\"%s\",", constant.getValue());
                else
                    constantLine += constant.getValue() + ",";
            }
            generator.writeLine(constantLine);
        }
        generator.dendent();
        generator.writeLine("}");
        generator.writeLine("# The following method call is for any initialization code you might need");
        generator.writeLine("self.node_setup()");
        generator.newLine();
        generator.writeFileContent(PLPHarnessGenerator.class.getResource("/HarnessInit.txt").getPath());
        generator.newLine();
        generator.writeLine("# ROS related stuff");
        generator.writeLine(String.format("rospy.init_node(\"plp_%s\", anonymous=False)",plp.getBaseName()));
        generator.writeLine("self.publisher = rospy.Publisher(PLP_TOPIC, PlpMessage, queue_size=5)");

        for (PLPParameter param : plp.getExecParams()) {
            generateParamTopic(generator, param);
        }
        for (PLPParameter param : plp.getInputParams()) {
            generateParamTopic(generator, param);
        }
        for (PLPParameter param : plp.getOutputParams()) {
            generateParamTopic(generator, param);
        }
        generator.newLine();
    }

    private static void generateParameterUpdateFunction(PythonWriter generator, PLPParameter param, boolean isTriggerParam) {
        generator.writeLine(String.format("def param_%s_updated(self, msg):",param.simpleString()));
        generator.indent();
        generator.writeLine(String.format("self.plp_params.set_%s(msg)",param.simpleString()));
        if (isTriggerParam)
            generator.writeLine("self.consider_trigger()");
        else {
            generator.writeLine("# If this parameter effects the trigger for the robotic module, uncomment the following line");
            generator.writeLine("# self.consider_trigger()");
        }
        generator.dendent();
        generator.newLine();
    }

    private static void generateCaptureParameter(PythonWriter generator, PLPParameter param) {
        generator.writeLine(String.format("capture_file.write(\"Parameter: %s, Value: \")",param.toString()));
        generator.writeLine(String.format("capture_file.write(repr(self.plp_params.%s))\\n",param.simpleString()));
    }

    private static void generateParamTopic(PythonWriter generator, PLPParameter param) {
        ParameterGlue paramGlue = parameterLocations.get(param.toString());
        if (paramGlue == null)
            throw new RuntimeException("parameter "+param.toString()+" wasn't found in glue file");
        generator.writeLine(String.format("rospy.Subscriber(\"%s\", %s, self.param_%s_updated)",
                paramGlue.getRosTopic(),paramGlue.getMessageType(),param.simpleString()));
    }

    /**
     * Writed the required imports into the given PythonWriter and saves all the parameter mapping for later code generation
     * @param generator the PythonWriter that will print the imports
     * @param plp the PLP for which the glue file is handled
     * @param gluePath the path to the glue file
     */
    private static void handleGlueFile(PythonWriter generator, PLP plp, String gluePath) {

        if (!gluePath.endsWith("\\")) { gluePath = gluePath.concat("\\"); }
        gluePath = gluePath.concat(plp.getGlueFile());

        File glueFile = new File(gluePath);
        if (!glueFile.isFile() || !glueFile.getName().substring(glueFile.getName().lastIndexOf(".") + 1).equalsIgnoreCase("xml")) {
            throw new IllegalArgumentException("path for glue file " + gluePath + " is not a legal xml");
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(glueFile);

            doc.getDocumentElement().normalize();

            Element rootElement = doc.getDocumentElement();

            if (!rootElement.getNodeName().equals("code_generator_glue")) {
                throw new IllegalArgumentException("file " + gluePath + " is not a legal glue file");
            }

            NodeList importNodes = rootElement.getElementsByTagName("import");
            for (int i=0; i<importNodes.getLength(); i++) {
                Element importElement = (Element) importNodes.item(i);
                String pack = importElement.getAttribute("from");
                if (pack != null){
                    generator.write("from "+pack+" ");
                    /// Adding it to the list, to later add it to the CMakeLists and package.xml
                    CodeGenerator.importsForPackage.add(pack);
                }
                generator.write("import ");
                NodeList classesNodes = importElement.getElementsByTagName("python_class");
                for (int j=0; j<classesNodes.getLength()-1; j++) {
                    generator.write((classesNodes.item(j).getTextContent()+", "));
                }
                generator.writeLine((classesNodes.item(classesNodes.getLength()-1)).getTextContent());
            }

            NodeList parameterNodes = rootElement.getElementsByTagName("parameter_location");
            for (int i=0; i<parameterNodes.getLength(); i++) {
                Element paramLocationElement = (Element) parameterNodes.item(i);
                Element paramElement = (Element) paramLocationElement.getElementsByTagName("param").item(0);
                PLPParameter plpParam = new PLPParameter(paramElement.getAttribute("name"));

                NodeList paramFields = paramElement.getElementsByTagName("field");
                for (int j = 0; j < paramFields.getLength(); j++) {
                    plpParam.addParamFieldValue(((Element) paramFields.item(j)).getAttribute("value"));
                }
                Element topicElement = (Element) paramLocationElement.getElementsByTagName("ROS_topic").item(0);
                ParameterGlue paramGlue = new ParameterGlue(topicElement.getAttribute("name"),
                                                                topicElement.getAttribute("message_type"),
                                                                topicElement.getAttribute("field_in_message"));
                parameterLocations.put(plpParam.toString(), paramGlue);
            }

            for (PLPParameter param : plp.getExecParams())
                if (!parameterLocations.containsKey(param.toString()))
                    throw new RuntimeException("glue file " + gluePath + " has some parameters missing");
            for (PLPParameter param : plp.getInputParams())
                if (!parameterLocations.containsKey(param.toString()))
                    throw new RuntimeException("glue file " + gluePath + " has some parameters missing");
            for (PLPParameter param : plp.getOutputParams())
                if (!parameterLocations.containsKey(param.toString()))
                    throw new RuntimeException("glue file " + gluePath + " has some parameters missing");


        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
