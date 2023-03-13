package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeVisitor extends AJmmVisitor<String, String> {

    // TODO: Probably a better way to do this
    @Override
    protected void buildVisitor() {
        addVisit("IntArray", this::dealWithIntArray);
        addVisit("BoolType", this::dealWithBool);
        addVisit("IntType", this::dealWithInt);
        addVisit("TypeName", this::dealWithType);
    }

    private String dealWithIntArray(JmmNode node, String s) {
        return "IntArray";
    }

    private String dealWithInt(JmmNode node, String s) {
        return "Integer";
    }

    private String dealWithBool(JmmNode node, String s) {
        return "Boolean";
    }

    private String dealWithType(JmmNode node, String s) {
        return node.get("typeName");
    }
}
