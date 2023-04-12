package pt.up.fe.comp2023.visitors.symbolTable;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class ClassVisitor extends AJmmVisitor<String, Pair<List<String>, List<Symbol>>> {

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithRoot);
        addVisit("ClassDeclaration", this::dealWithClass);
    }

    private Pair<List<String>, List<Symbol>> dealWithRoot(JmmNode node, String s) {

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("ClassDeclaration")) {
                return visit(child, "");
            }
        }

        return null;

    }

    private Pair<List<String>, List<Symbol>> dealWithClass(JmmNode node, String s) {
        List<String> classInfo = new ArrayList<>();
        List<Symbol> fields = new ArrayList<>();

        classInfo.add(node.get("className"));
        if (node.hasAttribute("superClassName"))
            classInfo.add(node.get("superClassName"));

        TypeVisitor visitor = new TypeVisitor();
        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("VarDeclaration")) {
                Type type = visitor.visit(child.getJmmChild(0));
                fields.add(new Symbol(type, child.get("varName")));
            }
        }

        return new Pair<>(classInfo, fields);
    }

}
