package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class SimpleOllir implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult jmmSemanticsResult) {
        OllirCodeGenerator codeGenerator = new OllirCodeGenerator(jmmSemanticsResult.getRootNode(), (SimpleSymbolTable) jmmSemanticsResult.getSymbolTable());

        String ollirCode = codeGenerator.generateOllir();

        return new OllirResult(jmmSemanticsResult, ollirCode, new ArrayList<>());
    }
}
