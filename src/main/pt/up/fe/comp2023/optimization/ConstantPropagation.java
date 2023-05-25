package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConstantPropagation extends PreorderJmmVisitor<SimpleSymbolTable, List<Report>> {

    private boolean hasChanged = false;
    private List<Constant> currentLocals;

    public ConstantPropagation(JmmNode root) {

        ConstantPropagationPassThrough constantGetter = new ConstantPropagationPassThrough();
        constantGetter.visit(root);

        this.currentLocals = constantGetter
                             .numOfAssignments
                             .entrySet().stream()
                             .filter(entry -> (entry.getValue() == 1))
                             .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
                             .keySet().stream().collect(Collectors.toList());

    }

    @Override
    protected void buildVisitor() {



    }

}
