package codeGen;

import modules.AchievePLP;

import java.io.PrintWriter;

public class CodeGenerator {

    public static void GenerateAchieveScripts(AchievePLP aPLP, String path) {
        String PLPClasses = PLPClassesGenerator.GeneratePLPClasses(aPLP);
        String PLPModule = PLPModuleGenerator.GeneratePLPModule(aPLP);
        String PLPHarness = PLPHarnessGenerator.GeneratePLPHarness(aPLP);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path+"\\PLP"+aPLP.getBaseName()+"Classes.py", "UTF-8");
            writer.print(PLPClasses);
            writer = new PrintWriter(path+"\\PLP"+aPLP.getBaseName()+".py", "UTF-8");
            writer.print(PLPModule);
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
