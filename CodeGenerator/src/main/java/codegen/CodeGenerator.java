package codegen;

import loader.PLPLoader;
import modules.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

public class CodeGenerator {

    public static String packageName="plp_modules";
    public static String outputTopic="/plp/messages";
    public static List<String> importsForPackage;

    public static void main(String[] args)
    {
        importsForPackage = new LinkedList<>();
        if (args.length < 1) {
            System.out.println("Usage: <dir path>");
        }
        else {
            String path = args[0];
            if (!path.endsWith("\\")) { path = path.concat("\\"); }
            PLPLoader.loadFromDirectory(args[0]);

            File pack = new File(path+"plp_package");
            pack.mkdir();

            File scripts = new File(path+"plp_package\\scripts");
            scripts.mkdir();

            File msg = new File(path+"plp_package\\msg");
            msg.mkdir();

            copyResourceFile("PLPClasses.py",path+"plp_package\\scripts\\");
            for (AchievePLP aPLP : PLPLoader.getAchievePLPs()) {
                CodeGenerator.GenerateScripts(aPLP, path);
            }
            for (MaintainPLP mPLP : PLPLoader.getMaintainPLPs()) {
                CodeGenerator.GenerateScripts(mPLP, path);
            }
            for (ObservePLP oPLP : PLPLoader.getObservePLPs()) {
                CodeGenerator.GenerateScripts(oPLP, path);
            }
            for (DetectPLP dPLP : PLPLoader.getDetectPLPs()) {
                CodeGenerator.GenerateScripts(dPLP, path);
            }

            generateCMakeLists(path+"\\plp_package");
            generatePackageXMLFile(path+"\\plp_package");
            copyResourceFile("PlpMessage.msg", path+"plp_package\\msg\\");
        }
    }

    public static void GenerateScripts(PLP plp, String path) {
        String PLPClasses = PLPClassesGenerator.GeneratePLPClasses(plp);
        String PLPModule = PLPModuleGenerator.GeneratePLPModule(plp);
        String PLPHarness = PLPHarnessGenerator.GeneratePLPHarness(plp, path);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path+"\\plp_package\\scripts\\PLP"+plp.getBaseName()+"Classes.py", "UTF-8");
            writer.print(PLPClasses);
            writer.close();
            writer = new PrintWriter(path+"\\plp_package\\scripts\\PLP"+plp.getBaseName()+".py", "UTF-8");
            writer.print(PLPModule);
            writer.close();
            writer = new PrintWriter(path+"\\plp_package\\scripts\\PLP"+plp.getBaseName()+"RosHarness.py", "UTF-8");
            writer.print(PLPHarness);
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
        PythonWriter generator = new PythonWriter();
        generator.writeFileContent(CodeGenerator.class.getResource("/"+fileName).getPath());
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+"\\"+fileName, "UTF-8");
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

        generator.writeFileContent(CodeGenerator.class.getResource("/CMakeLists.txt").getPath(),
                packagesSB.toString(), messagesSB.toString());

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+"\\CMakeLists.txt", "UTF-8");
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

        generator.writeFileContent(CodeGenerator.class.getResource("/package.xml").getPath(),
                packagesSB.toString());

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(resultPath+"\\package.xml", "UTF-8");
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
