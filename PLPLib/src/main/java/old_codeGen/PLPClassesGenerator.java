package old_codeGen;

import modules.PLP;
import plpFields.PLPParameter;
import plpFields.Variable;

public class PLPClassesGenerator {

    public static String GeneratePLPClasses(PLP plp) {
        PythonWriter generator = new PythonWriter();

        generator.writeLine(String.format("class PLP%sParameters(object):",plp.getBaseName()));
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
        generator.writeLine("# Input Parameters");
        for (PLPParameter p : plp.getOutputParams()) {
            generator.writeLine(String.format("self.%s = None", p.simpleString()));
        }
        generator.newLine();
        generator.dendent();

        // Next, set functions
        for (PLPParameter p : plp.getInputParams()) {
            String pName = p.simpleString();
            generator.writeLine(String.format("def set_%1$s(self, a_%1$s):",pName));
            generator.indent();
            generator.writeLine(String.format("self.%1$s = a_%1$s",pName));
            generator.writeLine("if self.callback:");
            generator.indent();
            generator.writeLine("self.callback.parameters_updated()");
            generator.dendent();
            generator.dendent();
            generator.newLine();
        }

        generator.dendent();
        generator.writeLine(String.format("class PLP%sVariables(object):",plp.getBaseName()));
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
