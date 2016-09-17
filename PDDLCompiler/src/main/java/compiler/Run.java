package compiler;


import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Parser;
import loader.PLPLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Run {
    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: <dir path>");
        }
        else {

            PLPLoader.loadFromDirectory(args[0]);
            PDDLCompiler.setAchievePLPs(PLPLoader.getAchievePLPs());
            PDDLCompiler.setObservePLPs(PLPLoader.getObservePLPs());
            PDDLCompiler.setMaintainPLPs(PLPLoader.getMaintainPLPs());
            PDDLCompiler.setDetectPLPs(PLPLoader.getDetectPLPs());
            String compiledPDDL = PDDLCompiler.producePDDL();
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(args[0]+"/domain.pddl", "UTF-8");
                writer.print(compiledPDDL);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (writer != null)
                    writer.close();
            }

            /*Parser pddlParser = new Parser();
            Domain domain = null;
            try {
                pddlParser.parseDomain(args[0]);
                domain = pddlParser.getDomain();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(domain.toString());*/
        }
    }

}
