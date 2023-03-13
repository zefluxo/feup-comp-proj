package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ImportVisitor extends AJmmVisitor<String, String> {
    @Override
    protected void buildVisitor() {

        addVisit("Program", this::dealWithRoot);
        addVisit("ImportDeclaration", this::dealWithImport);
        addVisit("ModuleDeclaration", this::dealWithModule);

    }

    private String dealWithRoot(JmmNode node, String a) {
        String allImports = "";

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("ImportDeclaration")) {
                allImports += visit(child, "");
                allImports += '/';
            }
        }

        return allImports;
    }

    private String dealWithImport(JmmNode node, String a) {
        String ret = "";

        for (JmmNode child: node.getChildren()) {
            ret += visit(child, a);
            ret += '.';
        }

        return ret.substring(0, ret.length() - 1);
    }

    private String dealWithModule(JmmNode node, String a) {
        return node.get("modName");
    }
}
