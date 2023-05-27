package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class ConstantFolding extends PreorderJmmVisitor<String, String> {

    public boolean hasChanged = false;
    private final JmmNode root;

    public ConstantFolding(JmmNode root) {
        this.root = root;
    }

    @Override
    protected void buildVisitor() {

        addVisit("BinaryOp", this::dealWithBinaryOp);
        setDefaultValue(String::new);

    }

    private String dealWithBinaryOp(JmmNode node, String s) {

        JmmNode lhs = node.getJmmChild(0);
        JmmNode rhs = node.getJmmChild(1);
        String op = node.get("op");

        if (op.equals("<") || op.equals("&&")) return "";

        if (lhs.getKind().equals("Integer") && rhs.getKind().equals("Integer")) {

            int lhsValue = Integer.parseInt(lhs.get("val"));
            int rhsValue = Integer.parseInt(rhs.get("val"));
            int result = switch (op) {
                case "*" -> lhsValue * rhsValue;
                case "/" -> lhsValue / rhsValue;
                case "+" -> lhsValue + rhsValue;
                case "-" -> lhsValue - rhsValue;
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };

            lhs.put("val", String.valueOf(result));
            node.replace(new JmmNodeImpl(lhs.getKind(), lhs));
            hasChanged = true;

        }

        return "";
    }

    public JmmNode getRoot() {
        return root;
    }
}
