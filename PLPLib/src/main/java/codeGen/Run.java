package codeGen;

import loader.PLPLoader;
import modules.AchievePLP;

import java.io.PrintWriter;

public class Run {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: <dir path>");
        }
        else {
            PLPLoader.loadFromDirectory(args[0]);
           /* System.out.println(PLPLoader.getAchievePLPs().get(0).toString());
            System.out.println(PLPLoader.getAchievePLPs().get(1).toString());*/
            String path = args[0];
            for (AchievePLP aPLP : PLPLoader.getAchievePLPs()) {
                CodeGenerator.GenerateAchieveScripts(aPLP, path);
            }
        }
    }
}
