package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.SimpleSymbolTable;
import pt.up.fe.comp2023.symbolTable.TypeVisitor;
import pt.up.fe.comp2023.symbolTable.entities.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Analyser extends PreorderJmmVisitor<SimpleSymbolTable, List<Report>> {

    static List<String> PRIMITIVES = Arrays.asList("int", "boolean", "void");
    public List<Report> reports;
    private Method currMethod;
    private String className;
    private String superClassName;

    public Analyser() {
        this.reports = new ArrayList<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("FuncDeclaration", this::dealWithFuncDeclaration);
        addVisit("MainFuncDeclaration", this::dealWithFuncDeclaration);
        addVisit("VarDeclaration", this::dealWithVarDeclaration);
        addVisit("ArgumentDeclaration", this::dealWithVarDeclaration);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("Integer", this::dealWithInteger);
        addVisit("NotOp", this::dealWithNotOp);
        addVisit("LengthOp", this::dealWithLength);
        addVisit("ObjectDeclaration", this::dealWithObjectDeclaration);
        addVisit("ArrayDeclaration", this::dealWithArrayDeclaration);
        addVisit("ParenthesesOp", this::dealWithParenthesesOp);
        addVisit("ClassMethodCall", this::dealWithClassMethodCall);
        addVisit("ObjIdentifier", this::dealWithThis);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("IfStatement", this::dealWithCondition);
        addVisit("WhileStatement", this::dealWithCondition);

        setDefaultValue(ArrayList::new);
    }

    private List<Report> dealWithCondition(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        JmmNode condition = jmmNode.getJmmChild(0);

        Type conditionType = getType(condition, symbolTable);

        if (!conditionType.getName().equals("boolean") || conditionType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Invalid conditional expression in statement"
            ));
        }

        return reports;
    }

    private List<Report> dealWithArrayAssignment(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        JmmNode index = jmmNode.getJmmChild(0);
        JmmNode newValue = jmmNode.getJmmChild(1);
        String varName = jmmNode.get("varName");

        Type varType = getVarType(varName, symbolTable);

        if (varType == null) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Variable used in assignment is not declared"
            ));
        }

        Type indexType = getType(index, symbolTable);
        Type newValueType = getType(newValue, symbolTable);

        if (indexType.isArray() || !indexType.getName().equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Invalid array index on assignment"
            ));
        }

        if (!newValueType.equals(varType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Value assigned to variable is of a different type"
            ));
        }

        return reports;
    }

    private List<Report> dealWithAssignment(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        String varName = jmmNode.get("varName");
        JmmNode newValue = jmmNode.getJmmChild(0);

        Type type = getVarType(varName, symbolTable);
        if (type == null) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Variable used in assignment is not declared"
            ));
            return reports;
        }

        Type newValueType = getType(newValue, symbolTable);
        List<String> importedClasses = new ArrayList<>();

        for (String imp: symbolTable.getImports()) {
            String[] modules = imp.split("\\.");
            String importedClass = modules[modules.length - 1];

            importedClasses.add(importedClass);
        }

        if (importedClasses.contains(type.getName()) && importedClasses.contains(newValueType.getName())) return reports;
        if (superClassName != null) {
            if (type.getName().equals(superClassName) && newValueType.getName().equals(className)) return reports;
        }

        if (!type.equals(newValueType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Value assigned to variable is of a different type"
            ));

        }

        return reports;
    }

    private List<Report> dealWithThis(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", className);
        jmmNode.put("isArray", "false");
        return reports;
    }

    private List<Report> dealWithClassMethodCall(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        String methodName = jmmNode.get("methodName");
        List<JmmNode> arguments = Collections.emptyList();

        if (jmmNode.getNumChildren() > 1) {
            arguments = new ArrayList<>(jmmNode.getChildren().subList(1, jmmNode.getNumChildren()));
        }

        JmmNode identifier = jmmNode.getJmmChild(0);
        Type idType = getType(identifier, symbolTable);

        if (PRIMITIVES.contains(idType.getName())) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempted method call on primitive type"
            ));
            return reports;
        }

        if (idType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempted method call on array"
            ));
            return reports;
        }

        if (idType.getName().equals(className)) {

            List<String> methodNames = symbolTable.getMethods();
            if (!methodNames.contains(methodName)) {
                if (superClassName != null) return reports;
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(jmmNode.get("lineStart")),
                        Integer.parseInt(jmmNode.get("colStart")),
                        methodName + " is not defined"
                ));
                return reports;
            }

            Method calledMethod = symbolTable.getMethod(methodName);
            List<Symbol> methodArguments = calledMethod.getArguments();

            if (calledMethod.isStatic()) {
                // Static methods cannot be called with "this" or a variable of the class type (i.e: A a; a.foo())
                if (identifier.getKind().equals("ObjIdentifier") || (identifier.getKind().equals("Identifier") && !identifier.get("varName").equals(className))) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(jmmNode.get("lineStart")),
                            Integer.parseInt(jmmNode.get("colStart")),
                            "Static method cannot be referenced from an instance"
                    ));
                    return reports;
                }
            }

            if (methodArguments.size() != arguments.size()) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(jmmNode.get("lineStart")),
                        Integer.parseInt(jmmNode.get("colStart")),
                        "Wrong number of arguments on " + methodName + " call"
                ));
                return reports;
            }

            for (int i = 0; i < arguments.size(); i++) {

                Symbol currArgument = methodArguments.get(i);
                Type type = getType(arguments.get(i), symbolTable);

                if (!type.equals(currArgument.getType())) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(jmmNode.get("lineStart")),
                            Integer.parseInt(jmmNode.get("colStart")),
                            "Incorrect argument on " + methodName + " call"
                    ));
                    return reports;
                }

            }

            jmmNode.put("type", calledMethod.getReturnType().getName());
            jmmNode.put("isArray", "false");
            return reports;
        }

        List<String> imports = symbolTable.getImports();
        for (String imp: imports) {
            String[] modules = imp.split("\\.");
            String importedClass = modules[modules.length - 1];

            if (idType.getName().equals(importedClass)) {
                jmmNode.put("type", "void");
                jmmNode.put("isArray", "false");
                return reports;
            }

        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(jmmNode.get("lineStart")),
                Integer.parseInt(jmmNode.get("colStart")),
                "Unreferenced identifier to method"
        ));
        return reports;
    }

    private List<Report> dealWithParenthesesOp(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        Type type = getType(jmmNode.getJmmChild(0), symbolTable);

        if (!type.getName().equals("invalid")) {
            jmmNode.put("type", type.getName());
            jmmNode.put("isArray", String.valueOf(type.isArray()));
        }

        return reports;
    }

    private List<Report> dealWithArrayDeclaration(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        JmmNode expectedInt = jmmNode.getJmmChild(0);
        Type type = getType(expectedInt, symbolTable);

        if (!type.getName().equals("int") || type.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Trying to declare array with invalid length"
            ));
            return reports;
        }

        jmmNode.put("type", "int");
        jmmNode.put("isArray", "true");
        return reports;
    }

    private List<Report> dealWithObjectDeclaration(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        String objectClass = jmmNode.get("objName");

        if (objectClass.equals(className)) {
            jmmNode.put("type", className);
            jmmNode.put("isArray", "false");
            return reports;
        }

        List<String> imports = symbolTable.getImports();
        for (String imp: imports) {
            String[] modules = imp.split("\\.");
            String importedClass = modules[modules.length - 1];

            if (objectClass.equals(importedClass)) {
                jmmNode.put("type", importedClass);
                jmmNode.put("isArray", "false");
                return reports;
            }
        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(jmmNode.get("lineStart")),
                Integer.parseInt(jmmNode.get("colStart")),
                "No class matching constructor"
        ));
        return reports;
    }

    private List<Report> dealWithLength(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        JmmNode expectedArray = jmmNode.getJmmChild(0);
        Type type = getType(expectedArray, symbolTable);

        if (!type.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Array required for length method call"
            ));
            return reports;
        }

        jmmNode.put("type", "int");
        jmmNode.put("isArray", "false");
        return reports;
    }

    private List<Report> dealWithNotOp(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        JmmNode child = jmmNode.getJmmChild(0);
        Type expectedBoolean = getType(child, symbolTable);

        if (!expectedBoolean.getName().equals("boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempted not operator on a non boolean"
            ));
            return reports;
        }

        jmmNode.put("type", "boolean");
        jmmNode.put("isArray", "false");
        return reports;
    }

    private List<Report> dealWithInteger(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;


        jmmNode.put("type", "int");
        jmmNode.put("isArray", "false");
        return new ArrayList<Report>();
    }

    private List<Report> dealWithBoolean(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "boolean");
        jmmNode.put("isArray", "false");
        return new ArrayList<Report>();
    }

    private List<Report> dealWithArrayAccess(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        JmmNode id = jmmNode.getJmmChild(0);
        JmmNode index = jmmNode.getJmmChild(1);

        Type idType = getType(id, symbolTable);

        if (!idType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Attempted array access on non array variable"
            ));
            return reports;
        }

        String expectedReturnType = idType.getName();
        Type indexType = getType(index, symbolTable);

        if (indexType.isArray() || !indexType.getName().equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Invalid array index"
            ));
            return reports;
        }


        jmmNode.put("type", expectedReturnType);
        return reports;
    }

    private List<Report> dealWithBinaryOp(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        jmmNode.put("type", "invalid");

        String op = jmmNode.get("op");
        String expectedOperandType = AnalyserUtils.binaryOpOperandType(op);
        String expectedReturnType = AnalyserUtils.binaryOpReturnType(op);

        JmmNode lhs = jmmNode.getJmmChild(0);
        JmmNode rhs = jmmNode.getJmmChild(1);

        Type lhsType = getType(lhs, symbolTable);
        Type rhsType = getType(rhs, symbolTable);


        if (lhsType.isArray() || rhsType.isArray()) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Array found in binary operation"
            ));
            return reports;
        }

        boolean correctLhsType = lhsType.getName().equals(expectedOperandType);
        boolean correctRhsType = rhsType.getName().equals(expectedOperandType);

        if (!(correctLhsType && correctRhsType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Invalid operand type for binary operation"
            ));
            return reports;
        }

        jmmNode.put("type", expectedReturnType);
        jmmNode.put("isArray", "false");
        return reports;
    }

    private List<Report> dealWithIdentifier(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        if (jmmNode.hasAttribute("type")) return reports;

        String id = jmmNode.get("varName");

        for (Symbol local: currMethod.getLocalVariables()) {
            if (local.getName().equals(id)) {
                Type type = local.getType();

                jmmNode.put("type", type.getName());
                jmmNode.put("isArray", String.valueOf(type.isArray()));

                return reports;
            }
        }

        for (Symbol argument: currMethod.getArguments()) {
            if (argument.getName().equals(id)) {
                Type type = argument.getType();

                jmmNode.put("type", type.getName());
                jmmNode.put("isArray", String.valueOf(type.isArray()));

                return reports;
            }
        }

        for (Symbol field: symbolTable.getFields()) {
            if (field.getName().equals(id)) {
                Type type = field.getType();

                jmmNode.put("type", type.getName());
                jmmNode.put("isArray", String.valueOf(type.isArray()));

                return reports;
            }
        }

        List<String> imports = symbolTable.getImports();
        for (String imp : imports) {
            String[] modules = imp.split("\\.");
            String importedClass = modules[modules.length - 1];

            if (importedClass.equals(id)) {
                jmmNode.put("type", importedClass);
                return reports;
            }
        }

        if (className.equals(id)) {
            jmmNode.put("type", className);
            return reports;
        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(jmmNode.get("lineStart")),
                Integer.parseInt(jmmNode.get("colStart")),
                "Variable " + id + " is not declared"
        ));
        jmmNode.put("type", "invalid");
        return reports;
    }

    private List<Report> dealWithVarDeclaration(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        TypeVisitor typeVisitor = new TypeVisitor();
        Type type = typeVisitor.visit(jmmNode.getJmmChild(0), "");

        if (PRIMITIVES.contains(type.getName())) return reports;
        if (type.getName().equals(className)) return reports;

        List<String> imports = symbolTable.getImports();
        for (String imp: imports) {
            String[] modules = imp.split("\\.");
            String importedClass = modules[modules.length - 1];

            if (importedClass.equals(type.getName())) {
                return reports;
            }
        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(jmmNode.get("lineStart")),
                Integer.parseInt(jmmNode.get("colStart")),
                "Unknown type of variable " + jmmNode.get("varName")
        ));
        return reports;
    }

    private List<Report> dealWithFuncDeclaration(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        String methodName = jmmNode.hasAttribute("methodName") ? jmmNode.get("methodName") : "main";
        this.currMethod = symbolTable.getMethod(methodName);

        if (!jmmNode.getKind().equals("MainFuncDeclaration")) {

            Type returnType = currMethod.getReturnType();
            JmmNode returnNode = jmmNode.getJmmChild(jmmNode.getChildren().size() - 1);

            Type returnExprType = getType(returnNode, symbolTable);

            if (returnExprType.getName().equals("void")) return reports;
            if (!returnType.equals(returnExprType)) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(jmmNode.get("lineStart")),
                        Integer.parseInt(jmmNode.get("colStart")),
                        "Incorrect return value for " + currMethod.getMethodName()
                ));
            }

        }

        return reports;
    }

    private List<Report> dealWithClassDeclaration(JmmNode jmmNode, SimpleSymbolTable symbolTable) {
        String className = jmmNode.get("className");
        this.className = className;
        this.superClassName = null;

        if (!jmmNode.getAttributes().contains("superClassName")) return reports;
        String superClass = jmmNode.get("superClassName");
        this.superClassName = superClass;

        if (className.equals(superClass)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("lineStart")),
                    Integer.parseInt(jmmNode.get("colStart")),
                    "Class" + className + "is extending itself"
            ));
            return reports;
        }

        List<String> imports = symbolTable.getImports();
        for (String imp: imports) {
            String[] modules = imp.split("\\.");

            if (modules[modules.length - 1].equals(superClass)) {
                return reports;
            }
        }

        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(jmmNode.get("lineStart")),
                Integer.parseInt(jmmNode.get("colStart")),
                "Missing reference for " + superClass
        ));
        return reports;
    }

    private Type getType(JmmNode node, SimpleSymbolTable symbolTable) {
        if (!node.hasAttribute("type")) visit(node, symbolTable);
        return AnalyserUtils.getType(node);
    }

    private Type getVarType(String varName, SimpleSymbolTable symbolTable) {
        for (Symbol local: currMethod.getLocalVariables()) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        for (Symbol argument: currMethod.getArguments()) {
            if (argument.getName().equals(varName)) {
                return argument.getType();
            }
        }

        for (Symbol field: symbolTable.getFields()) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        return null;
    }
}
