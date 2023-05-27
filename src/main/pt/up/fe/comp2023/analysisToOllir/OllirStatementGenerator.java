package pt.up.fe.comp2023.analysisToOllir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.SimpleSymbolTable;


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
        addVisit("IfStatement", this::dealWithIfStatement);
        addVisit("WhileStatement", this::dealWithWhileStatement);
        addVisit("Expr", this::dealWithExpr);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    private String dealWithMethodStatement(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // expand statement
        for (int i = 0; i < jmmNode.getNumChildren(); i++) {
            ret += visit(jmmNode.getJmmChild(i), s) + "\n";
        }

        return ret;
    }

    private String dealWithIfStatement(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(0), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirTools = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        if (ollirTools.getPreCode() != "") {
            ret += ollirTools.getPreCode() + "\n";
        }

        // if header
        if (!ollirTools.isTerminal()) {
            this.tempVarCounter++;
            String tempVar = "t" + this.tempVarCounter;
            String type = ollirTools.getOpType();
            ret += s + tempVar + "." + type + " :=." + type + " " + ollirTools.getCode() + ";\n";
            ret += s + "if (" + tempVar + "." + type + ") goto if_then_0;" + "\n";
        } else {
            ret += s + "if (" + ollirTools.getCode() + ") goto if_then_0;" + "\n";
        }

        // else statement
        String s2 = s + "\t";
        if (jmmNode.getNumChildren() > 2) {
            ret += visit(jmmNode.getJmmChild(2), s2) + "\n";
            ret += s2 + "goto if_end_0;" + "\n";
        }

        // then statement
        ret += s + "if_then_0:" + "\n";
        ret += visit(jmmNode.getJmmChild(1), s2) + "\n";

        // end marker
        ret += s + "if_end_0:";

        return ret;
    }

    private String dealWithWhileStatement(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // start marker
        ret += s + "goto while_cond_0;" + "\n";
        ret += s + "while_body_0:" + "\n";

        // body
        String s2 = s + "\t";
        ret += visit(jmmNode.getJmmChild(1), s2) + "\n";

        // condition
        ret += s + "while_cond_0:" + "\n";

        // deal with condition expression
        String condition = "";
        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(0), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirTools = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        if (ollirTools.getPreCode() != "") {
            ret += ollirTools.getPreCode() + "\n";
        }

        if (!ollirTools.isTerminal()) {
            this.tempVarCounter++;
            String tempVar = "t" + this.tempVarCounter;
            String type = ollirTools.getOpType();
            ret += s + tempVar + "." + type + " :=." + type + " " + ollirTools.getCode() + ";\n";
            condition = tempVar + "." + type;
        } else {
            condition = ollirTools.getCode();
        }

        ret += s + "if (" + condition + ") goto while_body_0;" + "\n";

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

        String type = "";
        if(var.a.getType().isArray()) {
            type += "array.";
        }
        type += OllirTools.getOllirType(var.a.getType().getName());

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

    private String dealWithArrayAssignment(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = "";

        // expand first expression
        OllirExpressionGenerator ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(0), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirToolsFirst = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        // expand second expression
        ollirExpressionGenerator = new OllirExpressionGenerator(jmmNode.getJmmChild(1), this.symbolTable, this.exploredMethod, tempVarCounter);
        OllirTools ollirToolsSecond = ollirExpressionGenerator.generateOllirTools(s);
        this.tempVarCounter = ollirExpressionGenerator.getTempVarCount();

        // write preCode
        if (ollirToolsFirst.getPreCode() != "") {
            ret += ollirToolsFirst.getPreCode() + "\n";
        }
        if (ollirToolsSecond.getPreCode() != "") {
            ret += ollirToolsSecond.getPreCode() + "\n";
        }

        // check variable
        Pair<Symbol, Character> varInfo = this.symbolTable.findVariable(jmmNode.get("varName"), this.exploredMethod);
        String var;

        if (varInfo.b.equals('p')) {
            int index = this.symbolTable.getParameters(this.exploredMethod).indexOf(varInfo.a) + 1; // could give an error
            if (index < 1) return "";
            var = "$" + index + "." + varInfo.a.getName();
        } else {
            var = varInfo.a.getName();
        }

        // check if first expression is terminal
        if (!ollirToolsFirst.isTerminal()) {
            this.tempVarCounter++;
            String tempVar = "t" + this.tempVarCounter;
            String type = ollirToolsFirst.getOpType();
            ret += s + tempVar + "." + type + " :=." + type + " " + ollirToolsFirst.getCode() + ";\n";
            ret += s + var + "[" + tempVar + "." + type + "].i32" + " :=.i32 " + ollirToolsSecond.getCode() + ";";
        } else {
            ret += s + var + "[" + ollirToolsFirst.getCode() + "].i32" + " :=.i32 " + ollirToolsSecond.getCode() + ";";
        }

        return ret;
    }

    public int getTempVarCounter() {
        return tempVarCounter;
    }
}
