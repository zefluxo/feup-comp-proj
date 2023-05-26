package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.analysis.Analyser;
import pt.up.fe.specs.util.SpecsCollections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleAnalysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        SimpleSymbolTable symbolTable = new SimpleSymbolTable(jmmParserResult.getRootNode());

        List<Report> parserReports = jmmParserResult.getReports();

        Analyser analyser = new Analyser();
        analyser.visit(jmmParserResult.getRootNode(), symbolTable);

        List<Report> semanticReports = analyser.reports;
        List<Report> reports = SpecsCollections.concat(parserReports, semanticReports);

        return new JmmSemanticsResult(jmmParserResult, symbolTable, reports);
    }
}
