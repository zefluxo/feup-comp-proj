package pt.up.fe.comp2023.symbolTable;

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
        StringBuilder allImports = new StringBuilder();

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("ImportDeclaration")) {
                allImports.append(visit(child, ""));
                allImports.append('/');
            }
        }

        return allImports.toString();
    }

    private String dealWithImport(JmmNode node, String a) {
        StringBuilder ret = new StringBuilder();

        for (JmmNode child: node.getChildren()) {
            ret.append(visit(child, a));
            ret.append('.');
        }

        return ret.substring(0, ret.length() - 1);
    }

    private String dealWithModule(JmmNode node, String a) {
        return node.get("modName");
    }
}
