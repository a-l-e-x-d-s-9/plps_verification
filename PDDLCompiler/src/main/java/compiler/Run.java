package compiler;


import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Parser;
import loader.PLPLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Run {
    public static void main(String[] args) {

        if (args.length < 2 || (!args[0].equals("-NFO") && !args[0].equals("-PO"))) {
            System.out.println("Usage:");
            System.out.println("\t-NFO <plp_dir_path> (For near-fully-observable mode)");
            System.out.println("\t-PO <plp_dir_path> (For partially-observable mode)");
        }
        else {
            if (args[0].equals("-NFO"))
                PDDLCompiler.compilerMode = PDDLCompiler.Mode.NEAR_FULLY_OBSERVABLE;
            else if (args[0].equals("-PO"))
                PDDLCompiler.compilerMode = PDDLCompiler.Mode.PARTIALLY_OBSERVABLE;
            else if (args[0].equals("-POprob")) {
                String problem = PDDLCompiler.finishPOproblem(args[1]);
                printToFile(args[1] + "/problem.pddl", problem);
                return;
            }


            String folderPath = args[1];

            PLPLoader.loadFromDirectory(folderPath);
            PDDLCompiler.setAchievePLPs(PLPLoader.getAchievePLPs());
            PDDLCompiler.setObservePLPs(PLPLoader.getObservePLPs());
            PDDLCompiler.setMaintainPLPs(PLPLoader.getMaintainPLPs());
            PDDLCompiler.setDetectPLPs(PLPLoader.getDetectPLPs());
            String[] compiledPDDL = PDDLCompiler.producePDDL();

            printToFile(folderPath +"/domain.pddl", compiledPDDL[0]);
            printToFile(folderPath +"/problem.pddl", compiledPDDL[1]);
        }
    }

    private static void printToFile(String fullPath, String text) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fullPath, "UTF-8");
            writer.print(text);
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
