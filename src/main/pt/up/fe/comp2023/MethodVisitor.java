package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class MethodVisitor extends AJmmVisitor<String, List<Method>> {
    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithRoot);
        addVisit("ClassDeclaration", this::dealWithClass);
    }

    private List<Method> dealWithRoot(JmmNode node, String s) {
        List<Method> ret = new ArrayList<Method>();

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("ClassDeclaration")) {
                ret.addAll(visit(child, ""));
            }
        }

        return ret;
    }

    private List<Method> dealWithClass(JmmNode node, String s) {
        List<Method> ret = new ArrayList<>();

        // TODO: Check how to elegantly verify if an argument is an array
        for (JmmNode child: node.getChildren()) {
            TypeVisitor visitor = new TypeVisitor();
            if (child.getKind().equals("FuncDeclaration")) {
                String retType = visitor.visit(child.getJmmChild(0));
                Type returnType = new Type(retType, false);

                String methodName = child.get("methodName");
                List<Symbol> arguments = new ArrayList<>();

                for (JmmNode newChild: child.getChildren()) {
                    if (newChild.getKind().equals("ArgumentDeclaration")) {
                        String typeName = visitor.visit(newChild.getJmmChild(0));

                        Type type = new Type(typeName, false);
                        arguments.add(new Symbol(type, newChild.get("varName")));
                    }
                }

                ret.add(new Method(methodName, returnType, arguments));
            }
        }

        return ret;
    }
}
