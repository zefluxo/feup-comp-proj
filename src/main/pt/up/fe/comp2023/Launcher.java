package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        try {
            TestUtils.noErrors(parserResult.getReports());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        System.out.println(parserResult.getRootNode().toTree());

        // Generate symbol table
        SimpleSymbolTable symbolTable = new SimpleSymbolTable(parserResult.getRootNode());
        System.out.println(symbolTable.print());

        // Run semantic analysis
        SimpleAnalysis analysis = new SimpleAnalysis();
        JmmSemanticsResult semanticsResult = analysis.semanticAnalysis(parserResult);

        System.out.println(semanticsResult.getRootNode().toTree());

        // Check for semantic errors
        try {
            TestUtils.noErrors(semanticsResult.getReports());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Generate OLLIR
        SimpleOllir ollir = new SimpleOllir();
        OllirResult ollirResult = ollir.toOllir(semanticsResult);
        System.out.println(ollirResult.getOllirCode());

        // Generate Jasmin
        SimpleBackend jasmin = new SimpleBackend();
        JasminResult jasminResult = jasmin.toJasmin(ollirResult);
        System.out.println(jasminResult.getJasminCode());
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
