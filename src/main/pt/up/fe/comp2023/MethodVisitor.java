package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.nio.charset.StandardCharsets;
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

        TypeVisitor visitor = new TypeVisitor();
        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("FuncDeclaration")) {
                Type returnType = visitor.visit(child.getJmmChild(0));

                String methodName = child.get("methodName");
                List<Symbol> arguments = new ArrayList<>();
                List<Symbol> localVariables = new ArrayList<>();

                for (JmmNode newChild: child.getChildren()) {

                    if (newChild.getKind().equals("ArgumentDeclaration")) {
                        Type type = visitor.visit(newChild.getJmmChild(0));
                        arguments.add(new Symbol(type, newChild.get("varName")));
                        continue;
                    }

                    if (newChild.getKind().equals("VarDeclaration")) {
                        Type type = visitor.visit(newChild.getJmmChild(0));
                        localVariables.add(new Symbol(type, newChild.get("varName")));
                    }

                }

                ret.add(new Method(methodName, returnType, arguments, localVariables));

            } else if (child.getKind().equals("MainFuncDeclaration")) {

                String methodName = "main";
                List<Symbol> arguments = new ArrayList<>();
                List<Symbol> localVariables = new ArrayList<>();

                Type argsType = new Type(visitor.visit(child.getJmmChild(0)).getName(), true);
                Type retType = new Type("null", false);
                String argsName = child.get("argsName");

                arguments.add(new Symbol(argsType, argsName));

                for (JmmNode newChild: child.getChildren()) {

                    if (newChild.getKind().equals("VarDeclaration")) {
                        Type type = visitor.visit(newChild.getJmmChild(0));
                        localVariables.add(new Symbol(type, newChild.get("varName")));
                    }

                }

                ret.add(new Method(methodName, retType, arguments, localVariables));

            }
        }

        return ret;
    }
}
