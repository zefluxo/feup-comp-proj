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

            ConstantPropagation visitor = new ConstantPropagation(jmmSemanticsResult.getRootNode());

            do {

                visitor.visit(jmmSemanticsResult.getRootNode());
                visitor = new ConstantPropagation(visitor.getRoot());

            } while (visitor.hasChanged);


        }

        OllirCodeGenerator codeGenerator = new OllirCodeGenerator(jmmSemanticsResult.getRootNode(), (SimpleSymbolTable) jmmSemanticsResult.getSymbolTable());
        String ollirCode = codeGenerator.generateOllir();
        return new OllirResult(jmmSemanticsResult, ollirCode, new ArrayList<>());
    }
}
