package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.SimpleOllir;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConstantPropagation extends PreorderJmmVisitor<SimpleSymbolTable, List<Constant>> {

    public boolean hasChanged = false;
    private JmmNode root;
    private List<Constant> constants;
    private List<Constant> currMethodConstants;

    public ConstantPropagation(JmmNode root, SimpleSymbolTable symbolTable) {

        ConstantPropagationPassThrough constantGetter = new ConstantPropagationPassThrough();
        constantGetter.visit(root, symbolTable);

        this.root = root;
        this.constants = constantGetter
                        .numOfAssignments
                        .entrySet().stream()
                        .filter(entry -> (entry.getValue() == 1))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
                        .keySet().stream().collect(Collectors.toList());

        constants.forEach(constant -> System.out.println(constant));

    }

    @Override
    protected void buildVisitor() {

        addVisit("FuncDeclaration", this::dealWithMethodDeclaration);
        addVisit("MainFuncDeclaration", this::dealWithMethodDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Identifier", this::dealWithIdentifier);

        setDefaultValue(ArrayList::new);

    }

    private List<Constant> dealWithMethodDeclaration(JmmNode node, SimpleSymbolTable symbolTable) {

        String methodName = node.getKind().equals("FuncDeclaration") ? node.get("methodName") : "main";
        this.currMethodConstants = constants.stream()
                                            .filter(constant -> constant.getMethod().equals(methodName))
                                            .collect(Collectors.toList());

        return constants;

    }

    private List<Constant> dealWithVarDeclaration(JmmNode node, SimpleSymbolTable symbolTable) {

        if (node.hasAttribute("visited")) return constants;

        String varName = node.get("varName");
        for (Constant constant: currMethodConstants) {

            if (constant.getLocal().getName().equals(varName)) {

                for (JmmNode child: node.getChildren()) { child.delete(); }

                node.delete();
                break;

            }

        }

        return constants;

    }

    private List<Constant> dealWithAssignment(JmmNode node, SimpleSymbolTable symbolTable) {

        String varName = node.get("varName");
        for (Constant constant: currMethodConstants) {

            if (constant.getLocal().getName().equals(varName) &&
                !constant.getValue().equals("")) {

                JmmNode parent = node.getJmmParent();
                parent.removeJmmChild(node);
                this.hasChanged = true;
                break;

            }

        }

        return constants;
    }

    private List<Constant> dealWithIdentifier(JmmNode node, SimpleSymbolTable symbolTable) {

        String varName = node.get("varName");
        for (Constant constant: currMethodConstants) {

            String constantName = constant.getLocal().getName();
            if (constantName.equals(varName)) {

                JmmNode parent = node.getJmmParent();
                if (parent == null) {
                    System.out.println("Null parent");
                    break;
                }

                int index = parent.removeJmmChild(node);
                JmmNode newChild = new JmmNodeImpl("Integer");
                newChild.put("val", constant.getValue());

                parent.add(newChild, index);
                this.hasChanged = true;
                break;

            }

        }

        return constants;
    }


    public JmmNode getRoot() {
        return root;
    }
}
