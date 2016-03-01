package codeGen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PythonWriter {
    StringBuilder codeBuilder;
    String tab = "    ";
    int currentTabLevel;

    public PythonWriter() {
        this.tab = tab;
        codeBuilder = new StringBuilder();
        currentTabLevel = 0;
    }


    public PythonWriter(int indentLevel) {
        this.tab = tab;
        codeBuilder = new StringBuilder();
        currentTabLevel = 0;
        this.currentTabLevel = indentLevel;
    }

    public void write(String text) {
        for (int i=0 ; i<currentTabLevel ; i++)
            codeBuilder.append(tab);
        codeBuilder.append(text);
    }

    public void writeLine(String line) {
        for (int i=0 ; i<currentTabLevel ; i++)
            codeBuilder.append(tab);
        codeBuilder.append(line).append("\n");
    }

    public void setIndent(int indent) {
        currentTabLevel = indent;
    }

    public void newLine() {
        codeBuilder.append("\n");
    }

    public void writeIndentedBlock(String block) {
        codeBuilder.append(block);
    }

    public void writeFileContent(String path) {
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while((line = bufferedReader.readLine()) != null) {
                writeLine(line);
            }
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            throw new RuntimeException("Error loading existing file into PythonWriter: File not found");
        }
        catch(IOException ex) {
            throw new RuntimeException("Error while reading from existing file into PythonWriter");
        }
    }

    public void indent() {
        currentTabLevel++;
    }

    public void dendent() {
        if (currentTabLevel == 0) {
            throw new RuntimeException("Trying to dendent when tab level at 0");
        }
        currentTabLevel--;
    }

    public String end() {
        if (currentTabLevel != 0) {
            throw new RuntimeException("Trying to end code gen when tab level at " + currentTabLevel + " instead of 0");
        }
        return codeBuilder.toString();
    }

    public int getCurrentTabLevel() {
        return currentTabLevel;
    }
}
