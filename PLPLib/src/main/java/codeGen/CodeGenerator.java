package codeGen;

import modules.AchievePLP;

import java.io.PrintWriter;

public class CodeGenerator {

    public static void GenerateAchieveScripts(AchievePLP aPLP, String path) {
        String PLPClasses = PLPClassesGenerator.GeneratePLPClasses(aPLP);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(path+"\\PLP"+aPLP.getBaseName()+"Classes.py", "UTF-8");
            writer.print(PLPClasses);
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
