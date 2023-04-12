package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class SimpleAnalysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        List<Report> reports = jmmParserResult.getReports();
        SimpleSymbolTable symbolTable = new SimpleSymbolTable(jmmParserResult.getRootNode());

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
