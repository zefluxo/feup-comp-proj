package pt.up.fe.comp2023.visitors.symbolTable;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeVisitor extends AJmmVisitor<String, Type> {

    // TODO: Probably a better way to do this
    @Override
    protected void buildVisitor() {
        addVisit("IntArray", this::dealWithIntArray);
        addVisit("BoolType", this::dealWithBool);
        addVisit("IntType", this::dealWithInt);
        addVisit("TypeName", this::dealWithType);
    }

    private Type dealWithIntArray(JmmNode node, String s) {
        return new Type("int", true);
    }

    private Type dealWithInt(JmmNode node, String s) {
        return new Type("int", false);
    }

    private Type dealWithBool(JmmNode node, String s) {
        return new Type("boolean", false);
    }

    private Type dealWithType(JmmNode node, String s) {
        if (node.hasAttribute("isArray")) {
            return new Type(node.get("typeName"), true);
        }

        return new Type(node.get("typeName"), false);
    }
}
