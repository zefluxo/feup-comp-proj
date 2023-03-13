package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ClassVisitor extends AJmmVisitor<String, String> {

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithRoot);
        addVisit("ClassDeclaration", this::dealWithClass);
    }

    private String dealWithRoot(JmmNode node, String s) {
        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("ClassDeclaration")) {
                return visit(child, "");
            }
        }

        return "";
    }

    private String dealWithClass(JmmNode node, String s) {
        String ret = node.get("className");
        if (node.hasAttribute("superClassName"))
            ret += "/" + node.get("superClassName");

        return ret;
    }

}
