package pt.up.fe.comp2023;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;


public class OllirStatementGenerator extends AJmmVisitor<String, String> {

    private final JmmNode statementRoot;
    private final SimpleSymbolTable symbolTable;
    private final String exploredMethod;
    private int tempVarCounter;

    public OllirStatementGenerator(JmmNode statementRoot, SimpleSymbolTable symbolTable, String exploredMethod, int tempVarCounter) {
        this.statementRoot = statementRoot;
        this.symbolTable = symbolTable;
        this.exploredMethod = exploredMethod;
        this.tempVarCounter = tempVarCounter;
    }

    public String generateOllir(String s) {
        return visit(this.statementRoot, s);
    }

    protected void buildVisitor() {
        addVisit("MethodStatement", this::dealWithMethodStatement);
        addVisit("Expr", this::dealWithExpr);
        addVisit("Assignment", this::dealWithAssignment);
    }

    private String dealWithMethodStatement(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // expand statement
        ret += s + "{" + "\n";
        String s2 = s + "\t";
        ret += visit(jmmNode.getJmmChild(0), s2) + "\n";
        ret += s + "}";

        return ret;
    }

    private String dealWithExpr(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // expand expression
        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(0), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirTools = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        // write preCode
        if (ollirTools.getPreCode() != "") {
            ret += ollirTools.getPreCode() + "\n";
        }

        // write code
        ret += s + ollirTools.getCode() + ";";
        return ret;
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // expand expression
        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(0), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirTools = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        if (ollirTools.getPreCode() != "") {
            ret += ollirTools.getPreCode() + "\n";
        }

        // get the variable
        Pair<Symbol, Character> var = this.symbolTable.findVariable(jmmNode.get("varName"), this.exploredMethod);
        if ( var == null ) return "";

        String type = OllirTools.getOllirType(var.a.getType().getName());
        if (var.b == 'l') {
            ret += s + var.a.getName() + "." + type + " :=." + type + " " + ollirTools.getCode() + ";";
        } else if (var.b == 'p') {
            int index = this.symbolTable.getParameters(this.exploredMethod).indexOf(var.a) + 1; // could give an error
            if (index < 1) return "";
            ret += s + "$" + (index) +  "." + var.a.getName() + "." + type + " :=." + type + " " + ollirTools.getCode() + ";";
        } else if (var.b == 'f') {
            if (!ollirTools.isTerminal()) {
                this.tempVarCounter++;
                String tempVar = "t" + this.tempVarCounter;
                ret += s + tempVar + "." + type + " :=." + type + " " + ollirTools.getCode() + ";\n";
                ret += s + "putfield(this, " + var.a.getName() + "." + type + ", " + tempVar + "." + type + ").V;";
            } else {
                ret += s + "putfield(this, " + var.a.getName() + "." + type + ", " + ollirTools.getCode() + ").V;";
            }
        }

        return ret;
    }

    public int getTempVarCounter() {
        return tempVarCounter;
    }
}
