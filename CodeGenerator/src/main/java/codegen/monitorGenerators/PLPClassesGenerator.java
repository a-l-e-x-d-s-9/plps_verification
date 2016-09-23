package codegen.monitorGenerators;

import codegen.common.PythonWriter;
import modules.PLP;
import plpFields.PLPParameter;
import plpFields.Variable;

import java.util.LinkedList;
import java.util.List;

public class PLPClassesGenerator {

    /**
     * Generates code for the PLP parameters class and PLP variables class
     * @param plp The PLP whose parameters and variables classes will be generated
     * @param isMonitor true if this will be part of the monitoring code (false for ROSPlan middleware generation)
     * @return The generated code
     */
    public static String GeneratePLPClasses(PLP plp, boolean isMonitor) {
        PythonWriter generator = new PythonWriter();

        generator.writeLine(String.format("class PLP_%s_parameters(object):",plp.getBaseName()));
        generator.indent();
        generator.writeLine("def __init__(self):");
        generator.indent();
        generator.writeLine("self.callback = None");
        generator.writeLine("# Execution Parameters");
        for (PLPParameter p : plp.getExecParams()) {
            generator.writeLine(String.format("self.%s = None", p.simpleString()));
        }
        generator.writeLine("# Input Parameters");
        for (PLPParameter p : plp.getInputParams()) {
            generator.writeLine(String.format("self.%s = None", p.simpleString()));
        }
        generator.writeLine("# Output Parameters");
        for (PLPParameter p : plp.getOutputParams()) {
            generator.writeLine(String.format("self.%s = None", p.simpleString()));
        }
        generator.newLine();
        generator.dendent();

        // Next, set functions
        if (isMonitor) {
            List<PLPParameter> allParams = new LinkedList<>(plp.getExecParams());
            allParams.addAll(plp.getInputParams());
            allParams.addAll(plp.getOutputParams());
            for (PLPParameter p : allParams) {
                String pName = p.simpleString();
                generator.writeLine(String.format("def set_%1$s(self, a_%1$s):", pName));
                generator.indent();
                generator.writeLine(String.format("self.%1$s = a_%1$s", pName));
                generator.writeLine("if self.callback:");
                generator.indent();
                generator.writeLine("self.callback.parameters_updated()");
                generator.dendent();
                generator.dendent();
                generator.newLine();
            }
        }

        generator.dendent();
        generator.writeLine(String.format("class PLP_%s_variables(object):",plp.getBaseName()));
        generator.indent();
        generator.writeLine("def __init__(self):");
        generator.indent();
        for (Variable var : plp.getVariables()) {
            generator.writeLine(String.format("self.%s = None", var.getName()));
        }
        generator.newLine();
        generator.dendent();
        generator.dendent();

        return generator.end();
    }
}
