package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;


import java.util.HashMap;

public class ConstantPropagationPassThrough extends AJmmVisitor<String, String> {

    public HashMap<String, JmmNode> constants;

    public ConstantPropagationPassThrough() {
        this.constants = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Assignment", this::dealWithAssignment);
        setDefaultVisit(this::dealWithDefault);
    }

    private String dealWithDefault(JmmNode node, String s) {

        for (JmmNode child: node.getChildren()) {
            visit(child, "");
        }

        return "";
    }


    private String dealWithAssignment(JmmNode node, String s) {
        String varName = node.get("varName");
        JmmNode rhs = node.getJmmChild(0);

        System.out.println(node);

        if (!constants.containsKey(varName)) {

            if (!rhs.getKind().equals("Integer")) return "";
            constants.put(varName, rhs);

        } else constants.remove(varName);

        return "";

    }
}
