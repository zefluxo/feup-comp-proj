package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;


public class OllirCodeGenerator extends AJmmVisitor<String, String> {
    private final JmmNode root;
    private final SimpleSymbolTable symbolTable;

    public OllirCodeGenerator(JmmNode root, SimpleSymbolTable symbolTable) {
        this.root = root;
        this.symbolTable = symbolTable;
    }

    public String generateOllir() {
        return visit(this.root, "");
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithRoot);
        addVisit("ClassDeclaration", this::dealWithClass);
        addVisit("MainFuncDeclaration", this::dealWithMainFuncDeclaration);
        addVisit("FuncDeclaration", this::dealWithFuncDeclaration);
    }

    private String dealWithRoot(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // Imports
        List<String> imports = this.symbolTable.getImports();
        if (!imports.isEmpty()) {
            for (String importName : imports) {
                if (!importName.equals("")) ret += s + "import " + importName + ";\n";
            }
        }



        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("ClassDeclaration")) {
                ret += s + visit(child, s);
                return ret;
            }
        }

        return null;
    }

    private String dealWithClass(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        if (this.symbolTable.getSuper() == "null")
            ret += s + this.symbolTable.getClassName() + " {\n";
        else
            ret += s + this.symbolTable.getClassName() + " extends " + this.symbolTable.getSuper() + " {\n";
        String s2 = s + "\t";

        // Class Fields
        for (Symbol field : symbolTable.getFields()) {
            if (field.getType().isArray())
                ret += s2 + ".field private " + field.getName() + ".array." + OllirTools.getOllirType(field.getType().getName()) + ";\n";
            else
                ret += s2 + ".field private " + field.getName() + "." + OllirTools.getOllirType(field.getType().getName()) + ";\n";
        }

        // Default Constructor
        ret += s2 + ".construct " + jmmNode.get("className") + "().V {\n";
        ret += s2 + "\tinvokespecial(this, \"<init>\").V;\n";
        ret += s2 + "}\n";

        // Class Methods
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("MainFuncDeclaration") || child.getKind().equals("FuncDeclaration")) {
                ret += visit(child, s2);
                ret += "\n";
            }
        }

        ret += s + "}\n";

        return ret;
    }

    private String dealWithMainFuncDeclaration(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");

        // Method Header
        Type argType = symbolTable.getParameters("main").get(0).getType();
        String argTypeStr = argType.isArray() ? ".array." + argType.getName() : argType.getName();
        String ret = s + ".method public static main(" + jmmNode.get("argsName") + argTypeStr + ").V {\n";

        // Method Body
        int tempVarCounter = 0;
        String s2 = s + "\t";
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("Expr") || child.getKind().equals("Assignment")
                    || child.getKind().equals("MethodStatement")) {
                OllirStatementGenerator ollirStatementGenerator = new OllirStatementGenerator(child, symbolTable, "main", tempVarCounter);
                ret += ollirStatementGenerator.generateOllir(s2) + "\n";
                tempVarCounter = ollirStatementGenerator.getTempVarCounter();
            }
        }

        ret += s + "}";

        return ret;
    }

    private String dealWithFuncDeclaration(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");

        // Method Header
        String ret = s + ".method public " + jmmNode.get("methodName") + "(";
        List<Symbol> parameters = this.symbolTable.getParameters(jmmNode.get("methodName"));
        if (!parameters.isEmpty()) {
            for (Symbol arg : parameters) {
                Type argType = arg.getType();
                String ollirType = OllirTools.getOllirType(arg.getType().getName());
                String argTypeStr = argType.isArray() ? ".array." + ollirType : "." + ollirType;
                ret += arg.getName() + argTypeStr + ", ";
            }
            ret = ret.substring(0, ret.length() - 2);
        }
        ret += ")." + OllirTools.getOllirType(this.symbolTable.getReturnType(jmmNode.get("methodName")).getName()) + " {\n";

        // Method Body
        int tempVarCounter = 0;
        String s2 = s + "\t";
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("Expr") || child.getKind().equals("Assignment")
                    || child.getKind().equals("MethodStatement")) {
                OllirStatementGenerator ollirStatementGenerator = new OllirStatementGenerator(child, symbolTable, jmmNode.get("methodName"), tempVarCounter);
                ret += ollirStatementGenerator.generateOllir(s2) + "\n";
                tempVarCounter = ollirStatementGenerator.getTempVarCounter();
            }
        }

        // Method Return
        JmmNode returnNode = jmmNode.getChildren().get(jmmNode.getChildren().size() - 1);
        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(returnNode, symbolTable, jmmNode.get("methodName"), tempVarCounter);
        OllirTools ollirTools = ollirExpressionGenerator.generateOllirTools(s2);

        // get preCode
        if (ollirTools.getPreCode() != "")
            ret += ollirTools.getPreCode() + "\n";

        if (!ollirTools.isTerminal()) {
            // get tempVar
            tempVarCounter++;
            ret += s2 + OllirTools.tempVarToString(tempVarCounter) + "." + ollirTools.getOpType() + " :=." + ollirTools.getOpType() + " " + ollirTools.getCode() + ";\n";
            ret += s2 + "ret." + ollirTools.getOpType() + " " + OllirTools.tempVarToString(tempVarCounter) + "." + ollirTools.getOpType() + ";\n";
        } else {
            ret += s2 + "ret." + ollirTools.getOpType() + " " + ollirTools.getCode() + ";\n";
        }
        ret += s + "}";

        return ret;
    }
}