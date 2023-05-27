package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantPropagation extends AJmmVisitor<String, String> {

    public boolean hasChanged = false;
    private final JmmNode root;
    private final Map<String, JmmNode> constants;

    public ConstantPropagation(JmmNode root) {

        ConstantPropagationPassThrough constantGetter = new ConstantPropagationPassThrough();
        constantGetter.visit(root, "");

        this.root = root;
        this.constants = new HashMap<>(constantGetter.constants);

        System.out.println(constants);

    }

    @Override
    protected void buildVisitor() {

        addVisit("IfStatement", this::dealWithWhileAndIfStatement);
        addVisit("WhileStatement", this::dealWithWhileAndIfStatement);
        addVisit("Identifier", this::dealWithIdentifier);
        setDefaultVisit(this::dealWithDefault);

    }

    private String dealWithDefault(JmmNode node, String s) {

        for (JmmNode child: node.getChildren()) {
            visit(child , "");
        }

        return "";

    }

    private String dealWithWhileAndIfStatement(JmmNode node, String s) {

        JmmNode brackets = node.getJmmChild(1);
        ConstantPropagation whileVisitor = new ConstantPropagation(brackets);

        do {

            for (JmmNode child : brackets.getChildren()) {
                whileVisitor.visit(child);
            }
            whileVisitor = new ConstantPropagation(brackets);

        } while (whileVisitor.hasChanged);

        for (JmmNode child: node.getChildren()) {
            visit(child, "");
        }

        return "";
    }

    private String dealWithIdentifier(JmmNode node, String s) {

        String varName = node.get("varName");
        JmmNode constant = constants.get(varName);

        if (constant == null) return "";

        node.replace(new JmmNodeImpl(constant.getKind(), constant));
        return "";

    }


    public JmmNode getRoot() { return root; }
}
