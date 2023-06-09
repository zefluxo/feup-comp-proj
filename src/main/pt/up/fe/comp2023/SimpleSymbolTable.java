package pt.up.fe.comp2023;


import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.symbolTable.entities.Method;
import pt.up.fe.comp2023.symbolTable.ClassVisitor;
import pt.up.fe.comp2023.symbolTable.ImportVisitor;
import pt.up.fe.comp2023.symbolTable.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleSymbolTable implements SymbolTable {

    private final JmmNode rootNode;
    private List<String> imports;
    private String className;
    private String superClassName;
    private List<Method> methods;
    private List<Symbol> fields;


    public SimpleSymbolTable(JmmNode node) {
        this.rootNode = node;

        readImports();
        readClass();
        readMethods();

    }
    private void readImports() {
        ImportVisitor visitor = new ImportVisitor();
        String imports = visitor.visit(this.rootNode);
        this.imports = Arrays.asList(imports.split("/", 0));
    }

    private void readClass() {

        ClassVisitor visitor = new ClassVisitor();
        Pair<List<String>, List<Symbol>> result = visitor.visit(this.rootNode);

        List<String> classInfo = result.a;
        List<Symbol> fields = result.b;

        this.className = classInfo.get(0);
        this.superClassName = "null";
        if (classInfo.size() == 2) {
            this.superClassName = classInfo.get(1);
        }

        this.fields = fields;

    }

    private void readMethods() {
        MethodVisitor visitor = new MethodVisitor();
        this.methods = visitor.visit(this.rootNode);
    }

    public Method getMethod(String methodName) {
        for (Method method: this.methods) {
            if (method.getMethodName().equals(methodName)) return method;
        }

        return null;
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        List<String> methodNames = new ArrayList<>();
        for (Method method: this.methods) {
            methodNames.add(method.getMethodName());
        }

        return methodNames;
    }

    @Override
    public Type getReturnType(String s) {
        for (Method method: this.methods) {
            if (method.getMethodName().equals(s)) return method.getReturnType();
        }

        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        for (Method method: this.methods) {
            if (method.getMethodName().equals(s)) return method.getArguments();
        }

        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        for (Method method: this.methods) {
            if (method.getMethodName().equals(s)) return method.getLocalVariables();
        }

        return null;
    }

    public Pair<Symbol, Character> findVariable(String varName, String exploredMethod) {
        List<Symbol> methodParameters = this.getParameters(exploredMethod);
        List<Symbol> classFields = this.getFields();
        List<Symbol> localVariables = this.getLocalVariables(exploredMethod);
        List<String> imports = this.getImports();

        for (Symbol symbol : localVariables) {
            if (symbol.getName().equals(varName)) {
                return new Pair<>(symbol, 'l');
            }
        }

        for (Symbol symbol : methodParameters) {
            if (symbol.getName().equals(varName)) {
                return new Pair<>(symbol, 'p');
            }
        }

        for (Symbol symbol : classFields) {
            if (symbol.getName().equals(varName)) {
                return new Pair<>(symbol, 'f');
            }
        }

        for (String importName : imports) {
            String[] importNameSplit = importName.split("\\.");
            String lastModule = importNameSplit[importNameSplit.length - 1];
            if (varName.equals(lastModule))
                return new Pair<>(new Symbol(null, varName), 'i');
        }


        return null;
    }
}
