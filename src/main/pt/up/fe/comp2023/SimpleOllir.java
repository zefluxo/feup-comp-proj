package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.analysisToOllir.OllirCodeGenerator;
import pt.up.fe.comp2023.optimization.ConstantFolding;
import pt.up.fe.comp2023.optimization.ConstantPropagation;

import java.util.ArrayList;
import java.util.HashMap;

public class SimpleOllir implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {

        var config = jmmSemanticsResult.getConfig();
        if (config.containsKey("optimize") && config.get("optimize").equals("true")) {

            ConstantPropagation constPropVisitor = new ConstantPropagation(jmmSemanticsResult.getRootNode());
            ConstantFolding constFoldVisitor = new ConstantFolding(jmmSemanticsResult.getRootNode());

            do {

                constPropVisitor.visit(constPropVisitor.getRoot());
                constFoldVisitor.visit(constFoldVisitor.getRoot());

                if (!constPropVisitor.hasChanged && !constFoldVisitor.hasChanged) break;

                constPropVisitor = new ConstantPropagation(constFoldVisitor.getRoot());
                constFoldVisitor = new ConstantFolding(constPropVisitor.getRoot());

            } while (true);


        }

        OllirCodeGenerator codeGenerator = new OllirCodeGenerator(jmmSemanticsResult.getRootNode(), (SimpleSymbolTable) jmmSemanticsResult.getSymbolTable());
        String ollirCode = codeGenerator.generateOllir();
        return new OllirResult(jmmSemanticsResult, ollirCode, new ArrayList<>());
    }
}
