package codegen.common;

import codegen.middlewareGenerators.MiddlewareGenerator;
import codegen.monitorGenerators.PLPClassesGenerator;
import codegen.monitorGenerators.PLPHarnessGenerator;
import codegen.monitorGenerators.PLPLogicGenerator;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Exp;
import fr.uga.pddl4j.parser.Op;
import fr.uga.pddl4j.parser.Parser;
import loader.PLPLoader;
import modules.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class CodeGenerator {

    public static String packageName;
    public static String outputTopic = "/plp/messages";
    public static List<String> importsForPackage;
    public static String pathBreak = "";

    public static void main(String[] args)
    {
        // TODO
        importsForPackage = new LinkedList<>();

        if (args.length > 1 && args[0].equals("-monitor")) {

            String path = args[1];

            pathBreak = path.contains("\\") ? "\\" : "/";

            // Complete path with with '/' or '\' according to what to OS uses
            if (!path.endsWith(pathBreak)) { path = path.concat(pathBreak); }

            PLPLoader.loadFromDirectory(path);

            packageName = "plp_monitors";
            generateROSPackage(path);

            copyResourceFile("PLPClasses.py",path+packageName+pathBreak+"scripts"+pathBreak);
            for (AchievePLP aPLP : PLPLoader.getAchievePLPs()) {
                CodeGenerator.GenerateMonitoringScripts(aPLP, path);
            }
            for (MaintainPLP mPLP : PLPLoader.getMaintainPLPs()) {
                CodeGenerator.GenerateMonitoringScripts(mPLP, path);
            }
            for (ObservePLP oPLP : PLPLoader.getObservePLPs()) {
                CodeGenerator.GenerateMonitoringScripts(oPLP, path);
            }
            for (DetectPLP dPLP : PLPLoader.getDetectPLPs()) {
                CodeGenerator.GenerateMonitoringScripts(dPLP, path);
            }

            generateCMakeLists(path+packageName);
            generatePackageXMLFile(path+packageName);
            copyResourceFile("PLPMessage.msg", path+packageName+pathBreak+"msg"+pathBreak);
        }
        else if (args.length > 3 && args[0].equals("-dispatcher")) {
            String plpPath = args[2];
            String pddlPath = args[3];

            if (args[1].equals("-nfo"))
                MiddlewareGenerator.domainType = MiddlewareGenerator.DomainType.NEAR_FULLY_OBSERVABLE;
            else if (args[1].equals("-po"))
                MiddlewareGenerator.domainType = MiddlewareGenerator.DomainType.PARTIALLY_OBSERVABLE;
            else {
                printUsageInstructions();
                return;
            }

            pathBreak = plpPath.contains("\\") ? "\\" : "/";

            // Complete path with with '/' or '\' according to what to OS uses
            if (!plpPath.endsWith(pathBreak)) { plpPath = plpPath.concat(pathBreak); }

            packageName = "plp_middleware";
            generateROSPackage(plpPath);

            PLPLoader.loadFromDirectory(plpPath);

            Parser pddlParser = new Parser();
            try {
                pddlParser.parseDomain(pddlPath);
            } catch (FileNotFoundException e) {
                System.err.println("Wrong path to domain file");
            }
            Domain domain = pddlParser.getDomain();
            MiddlewareGenerator.setDomain(domain);

            PythonWriter launchFileWriter = new PythonWriter();
            launchFileWriter.writeLine("<launch>");
            launchFileWriter.indent();
            if (MiddlewareGenerator.domainType == MiddlewareGenerator.DomainType.PARTIALLY_OBSERVABLE)
                launchFileWriter.writeLine("<node name=\"plp_middleware_assumption_manager\" pkg=\"" + packageName + "\" type=\"plp_middleware_assumption_manager.py\" required=\"true\" output=\"screen\"/>");
            // Go over every PDDL action and find the corresponding PLP
            for (Op pddlAction : domain.getOperators()) {
                boolean generatedMiddleware = false;

                for (AchievePLP aPLP : PLPLoader.getAchievePLPs()) {
                    if (aPLP.getBaseName().equals(pddlAction.getName().toString())) {
                        //System.out.println(aPLP.getBaseName());
                        String middlewareCode = MiddlewareGenerator.generateMiddleware(aPLP, pddlAction, plpPath);
                        writeStringToFile(middlewareCode,
                                plpPath+packageName+pathBreak+"scripts"+pathBreak
                                        +"plp_"+aPLP.getBaseName()+"_action_dispatcher.py");
                        generatedMiddleware = true;
                        launchFileWriter.writeLine("<node name=\"plp_" + aPLP.getBaseName() + "_action_dispatcher\" pkg=\"" + packageName + "\" type=\"plp_"+aPLP.getBaseName()+"_action_dispatcher.py\" required=\"true\" output=\"screen\"/>");
                    }
                }
                if (!generatedMiddleware)
                    for (MaintainPLP mPLP : PLPLoader.getMaintainPLPs()) {
                        if (mPLP.getBaseName().equals(pddlAction.getName().toString())) {
                            //System.out.println(aPLP.getBaseName());
                            String middlewareCode = MiddlewareGenerator.generateMiddleware(mPLP, pddlAction, plpPath);
                            writeStringToFile(middlewareCode,
                                    plpPath+packageName+pathBreak+"scripts"+pathBreak
                                            +"plp_"+mPLP.getBaseName()+"_action_dispatcher.py");
                            generatedMiddleware = true;
                            launchFileWriter.writeLine("<node name=\"plp_" + mPLP.getBaseName() + "_action_dispatcher\" pkg=\"" + packageName + "\" type=\"plp_"+mPLP.getBaseName()+"_action_dispatcher.py\" required=\"true\" output=\"screen\"/>");
                        }
                    }
                if (!generatedMiddleware)
                    for (ObservePLP oPLP : PLPLoader.getObservePLPs()) {
                        if (oPLP.getBaseName().equals(pddlAction.getName().toString())) {
                            //System.out.println(aPLP.getBaseName());
                            String middlewareCode = MiddlewareGenerator.generateMiddleware(oPLP, pddlAction, plpPath);
                            writeStringToFile(middlewareCode,
                                    plpPath+packageName+pathBreak+"scripts"+pathBreak
                                            +"plp_"+oPLP.getBaseName()+"_action_dispatcher.py");
                            generatedMiddleware = true;
                            launchFileWriter.writeLine("<node name=\"plp_" + oPLP.getBaseName() + "_action_dispatcher\" pkg=\"" + packageName + "\" type=\"plp_"+oPLP.getBaseName()+"_action_dispatcher.py\" required=\"true\" output=\"screen\"/>");
                        }
                    }
                if (!generatedMiddleware)
                    for (DetectPLP dPLP : PLPLoader.getDetectPLPs()) {
                        if (dPLP.getBaseName().equals(pddlAction.getName().toString())) {
                            //System.out.println(aPLP.getBaseName());
                            String middlewareCode = MiddlewareGenerator.generateMiddleware(dPLP, pddlAction, plpPath);
                            writeStringToFile(middlewareCode,
                                    plpPath+packageName+pathBreak+"scripts"+pathBreak
                                            +"plp_"+dPLP.getBaseName()+"_action_dispatcher.py");
                            generatedMiddleware = true;
                            launchFileWriter.writeLine("<node name=\"plp_" + dPLP.getBaseName() + "_action_dispatcher\" pkg=\"" + packageName + "\" type=\"plp_"+dPLP.getBaseName()+"_action_dispatcher.py\" required=\"true\" output=\"screen\"/>");
                        }
                    }

                if (!generatedMiddleware) {
                    System.err.println("Couldn't find PLP for PDDL action: "+pddlAction.getName());
                }
            }

            launchFileWriter.dendent();
            launchFileWriter.writeLine("</launch>");

            // Create Package Files
            writeStringToFile(launchFileWriter.end(),plpPath+packageName+pathBreak+"launch"+pathBreak+"middleware_launch.launch");
            generateCMakeLists(plpPath+packageName);
            generatePackageXMLFile(plpPath+packageName);
        }
        else {
            printUsageInstructions();
        }
    }

    public static void printUsageInstructions() {
        System.out.println("Usage:");
        System.out.println("-monitor <PLP folder path>");
        System.out.println("-dispatcher -nfo <PLP folder path> <PDDL domain path>");
        System.out.println("-dispatcher -po <PLP folder path> <PDDL domain path>");
    }

    public static void generateROSPackage(String path) {
        File pack = new File(path+packageName);
        pack.mkdir();

        File scripts = new File(path+packageName+pathBreak+"scripts");
        scripts.mkdir();

        File msg = new File(path+packageName+pathBreak+"msg");
        msg.mkdir();

        File launch = new File(path+packageName+pathBreak+"launch");
        launch.mkdir();
    }

    public static void GenerateMonitoringScripts(PLP plp, String path) {
        String PLPClasses = PLPClassesGenerator.GeneratePLPClasses(plp, true);
        String PLPModule = PLPLogicGenerator.GeneratePLPModule(plp);
        String PLPHarness = PLPHarnessGenerator.GeneratePLPHarness(plp, path);

        writeStringToFile(PLPClasses, path+packageName+pathBreak+"scripts"+pathBreak+"PLP_"+plp.getBaseName()+"_classes.py");

        writeStringToFile(PLPModule, path+packageName+pathBreak+"scripts"+pathBreak+"PLP_"+plp.getBaseName()+"_logic.py");

        writeStringToFile(PLPHarness, path+packageName+pathBreak+"scripts"+pathBreak+"PLP_"+plp.getBaseName()+"_ros_harness.py");
    }

    private static void writeStringToFile(String string, String fullPath) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fullPath, "UTF-8");
            writer.print(string);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (writer != null)
                writer.close();
        }
    }

    private static void copyResourceFile(String fileName, String resultPath) {
        copyResourceFile(fileName,resultPath,fileName);
    }

    private static void copyResourceFile(String fileName, String resultPath, String outputFileName) {
        PythonWriter generator = new PythonWriter();
        generator.writeResourceFileContent("/"+fileName);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+pathBreak+outputFileName, "UTF-8");
            writer.print(generator.end());
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (writer != null)
                writer.close();
        }
    }

    private static void generateCMakeLists(String resultPath) {
        PythonWriter generator = new PythonWriter();

        StringBuilder packagesSB = new StringBuilder();
        StringBuilder messagesSB = new StringBuilder();
        for (String pack : importsForPackage) {
            packagesSB.append("  ").append(pack.replace(".msg","")).append("\n");
            if (pack.endsWith(".msg"))
                messagesSB.append("  ").append(pack.replace(".msg","")).append("\n");
        }
        if (packagesSB.length() > 0) packagesSB.deleteCharAt(packagesSB.length()-1);
        if (messagesSB.length() > 0) messagesSB.deleteCharAt(messagesSB.length()-1);

        generator.writeResourceFileContent("/CMakeLists.txt",
                packageName, packagesSB.toString(), messagesSB.toString());
        //generator.writeFileContent(CodeGenerator.class.getResource("/CMakeLists.txt").getPath(),
        //        packagesSB.toString(), messagesSB.toString());

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+pathBreak+"CMakeLists.txt", "UTF-8");
            writer.print(generator.end());
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (writer != null)
                writer.close();
        }
    }

    private static void generatePackageXMLFile(String resultPath) {
        PythonWriter generator = new PythonWriter();

        StringBuilder packagesSB = new StringBuilder();
        for (String pack : importsForPackage) {
            packagesSB.append("  ").append("<build_depend>").append(pack.replace(".msg","")).append("</build_depend>").append("\n");
        }
        packagesSB.append("\n");
        for (String pack : importsForPackage) {
            packagesSB.append("  ").append("<run_depend>").append(pack.replace(".msg","")).append("</run_depend>").append("\n");
        }

        generator.writeResourceFileContent("/package.xml",
                packageName, packagesSB.toString());

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+pathBreak+"package.xml", "UTF-8");
            writer.print(generator.end());
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (writer != null)
                writer.close();
        }
    }



}
