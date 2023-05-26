package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ConstantPropagationPassThrough extends PreorderJmmVisitor<SimpleSymbolTable, List<Symbol>> {

    private String methodName;
    private List<Symbol> locals;
    private List<String> localNames;
    public HashMap<Constant, Integer> numOfAssignments;

    public ConstantPropagationPassThrough() {
        this.numOfAssignments = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit("FuncDeclaration", this::dealWithMethodDeclaration);
        addVisit("MainFuncDeclaration", this::dealWithMethodDeclaration);
        addVisit("Assignment", this::dealWithAssignment);
        setDefaultValue(ArrayList::new);
    }

    private List<Symbol> dealWithMethodDeclaration(JmmNode node, SimpleSymbolTable symbolTable) {

        String methodName = node.getKind().equals("FuncDeclaration") ? node.get("methodName") : "main";
        this.methodName = methodName;
        this.locals = symbolTable.getMethod(methodName)
                                 .getLocalVariables();

        this.localNames = locals.stream()
                                .map(symbol -> symbol.getName())
                                .collect(Collectors.toList());

        return locals;

    }

    private List<Symbol> dealWithAssignment(JmmNode node, SimpleSymbolTable symbolTable) {

        String varName = node.get("varName");
        JmmNode rhs = node.getJmmChild(0);

        if (!rhs.getKind().equals("Integer")) return locals;

        Symbol local = this.locals.stream()
                                  .filter(l -> varName.equals(l.getName()))
                                  .findAny()
                                  .orElse(null);

        String value = rhs.get("val");

        Constant possibleConstant = new Constant(value, local, this.methodName);
        if (numOfAssignments.putIfAbsent(possibleConstant, 1) != null) {
            numOfAssignments.put(possibleConstant, numOfAssignments.get(possibleConstant) + 1);
        }

        return locals;

    }
}
