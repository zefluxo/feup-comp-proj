package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysisToOllir.OllirCodeGenerator;
import pt.up.fe.comp2023.optimization.ConstantPropagation;

import java.util.ArrayList;

public class SimpleOllir implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        SimpleSymbolTable symbolTable = new SimpleSymbolTable(jmmSemanticsResult.getRootNode());

        var config = jmmSemanticsResult.getConfig();
        if (config.containsKey("optimize") && config.get("optimize").equals("true")) {

            ConstantPropagation visitor = new ConstantPropagation(jmmSemanticsResult.getRootNode(), symbolTable);

            while (true) {

                System.out.println("Propagating constants!");
                visitor.visit(jmmSemanticsResult.getRootNode());
                if (!visitor.hasChanged) break;

                visitor = new ConstantPropagation(visitor.getRoot(), symbolTable);

            }

            System.out.println("Propagated all constants!");
            System.out.println(visitor.getRoot().toTree());
            System.exit(0);

        }

        OllirCodeGenerator codeGenerator = new OllirCodeGenerator(jmmSemanticsResult.getRootNode(), (SimpleSymbolTable) jmmSemanticsResult.getSymbolTable());
        String ollirCode = codeGenerator.generateOllir();
        return new OllirResult(jmmSemanticsResult, ollirCode, new ArrayList<>());
    }
}
