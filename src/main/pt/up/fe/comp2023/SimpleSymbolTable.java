package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Arrays;
import java.util.List;

public class SimpleSymbolTable implements SymbolTable{

    private JmmNode rootNode;
    private List<String> imports;
    private String className;
    private String superClassName;
    private List<Method> methods;

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
        String result = visitor.visit(this.rootNode);

        if (result.indexOf('/') != -1) {
            this.className = result.substring(0, result.indexOf('/'));
            this.superClassName = result.substring(result.indexOf('/') + 1, result.length());
        } else {
            this.className = result;
        }
    }

    private void readMethods() {
        MethodVisitor visitor = new MethodVisitor();
        this.methods = visitor.visit(this.rootNode);
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
        return null;
    }

    // TODO: Find a better way to do this
    public List<Method> myGetMethods() {
        return this.methods;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }


    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return null;
    }
}