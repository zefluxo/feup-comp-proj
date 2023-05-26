package pt.up.fe.comp2023.analysisToOllir;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2023.SimpleSymbolTable;

import java.util.List;

public class OllirExpressionGenerator extends PreorderJmmVisitor<String, OllirTools> {
    private final JmmNode statementRoot;
    private final SimpleSymbolTable symbolTable;
    private final String exploredMethod;
    private int tempVarCount;

    public OllirExpressionGenerator(JmmNode statementRoot, SimpleSymbolTable symbolTable, String exploredMethod, int tempVarCount) {
        this.statementRoot = statementRoot;
        this.symbolTable = symbolTable;
        this.exploredMethod = exploredMethod;
        this.tempVarCount = tempVarCount;
    }

    public OllirTools generateOllirTools(String s) {
        return visit(this.statementRoot, s);
    }

    protected void buildVisitor() {
        addVisit("ParenthesesOp", this::dealWithParenthesesOp);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("ClassMethodCall", this::dealWithClassMethodCall);
        addVisit("LengthOp", this::dealWithLengthOp);
        addVisit("ObjectDeclaration", this::dealWithObjectDeclaration);
        addVisit("ArrayDeclaration", this::dealWithArrayDeclaration);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Integer", this::dealWithInteger);
        addVisit("Boolean", this::dealWithBoolean);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("ObjIdentifier", this::dealWithObjIdentifier);

        setDefaultValue(OllirTools::new);
    }

    private OllirTools dealWithParenthesesOp(JmmNode jmmNode, String s) {
        return visit(jmmNode.getJmmChild(0), s);
    }

    private OllirTools dealWithArrayAccess(JmmNode node, String s) {
        String code = "";
        String preCode = "";
        String opType = "i32";

        // explore first expression
        OllirTools auxOllirTools = visit(node.getJmmChild(0), s);
        // explore second expression
        OllirTools auxOllirTools2 = visit(node.getJmmChild(1), s);

        // add preCode
        if (auxOllirTools.getPreCode() != "") { preCode += auxOllirTools.getPreCode(); }
        if (auxOllirTools2.getPreCode() != "") { preCode += auxOllirTools2.getPreCode(); }

        // check of terminal expression
        String arrayIdentifier = "";
        if (!auxOllirTools.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools.getOpType();

            // add to preCode
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + auxOllirTools.getOpType() + " " + auxOllirTools.getCode() + ";";

            arrayIdentifier += tempVar;
        }
        else {
            arrayIdentifier += auxOllirTools.getCode();
        }

        // check of terminal expression
        String arrayIndex = "";
        if (!auxOllirTools2.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools2.getOpType();

            // add to preCode
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + auxOllirTools2.getOpType() + " " + auxOllirTools2.getCode() + ";";

            arrayIndex += tempVar;
        }
        else {
            arrayIndex += auxOllirTools2.getCode();
        }

        // add to code
        code += arrayIdentifier + "[" + arrayIndex + "].i32";

        return new OllirTools(preCode, code, opType);
    }

    private OllirTools dealWithClassMethodCall(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = "";

        String arguments = "";
        // get arguments and the necessary preCode to support them
        int numArgs = (jmmNode.getChildren().size() - 1);
        for (int i = 0; i < numArgs; i++) {
            OllirTools auxOllirTools = visit(jmmNode.getJmmChild(1 + i), s);

            // add preCode
            if (auxOllirTools.getPreCode() != "") { preCode += auxOllirTools.getPreCode(); }

            // check of terminal expression
            if (!auxOllirTools.isTerminal()) {
                // create a temporary variable to store the result of the operation
                this.tempVarCount++;
                String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools.getOpType();

                // add to preCode
                if (preCode != "") preCode += "\n";
                preCode += s + tempVar + " :=." + auxOllirTools.getOpType() + " " + auxOllirTools.getCode() + ";";

                // add to arguments
                arguments +=  ", " + tempVar;
            }
            else {
                arguments += ", " + auxOllirTools.getCode();
            }
        }

        // visit object identifier
        OllirTools auxOllirTools = visit(jmmNode.getJmmChild(0), s);

        // get the object identifier and necessary preCode to support it
        if (auxOllirTools.getPreCode() != "") {
            if (preCode != "") preCode += "\n";
            preCode += auxOllirTools.getPreCode();
        }

        String objName = "";
        if ( !auxOllirTools.isTerminal() ) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools.getOpType();

            // add to preCode
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + auxOllirTools.getOpType() + " " + auxOllirTools.getCode() + ";";

            // add to arguments
            objName += tempVar;
        }
        else {
            objName = auxOllirTools.getCode();
        }

        // static method call or virtual method call
        if (!objName.contains(".") && !objName.equals("this")){
            code += "invokestatic(" + objName + ", \"" + jmmNode.get("methodName") + "\"" + arguments + ").V";
            return new OllirTools(preCode, code, opType);
        }

        List<String> methods = this.symbolTable.getMethods();
        String calledMethod = jmmNode.get("methodName");

        // check if called method belongs to class
        if (methods.contains(calledMethod)) {
            String returnType = OllirTools.getOllirType(this.symbolTable.getReturnType(calledMethod).getName()); //wrong
            code += "invokevirtual(" + objName + ", \"" + calledMethod + "\"" + arguments + ")." + returnType;
            opType = returnType;

            return new OllirTools(preCode, code, opType);
        }

        // check if declaration comes from assign
        JmmNode auxNode = jmmNode;
        while(auxNode.getJmmParent().getKind().equals("ParenthesesOp")) {
            auxNode = auxNode.getJmmParent();
        }

        // In case of method coming from import, assume return type
        auxNode = auxNode.getJmmParent();
        if (auxNode.getKind().equals("Assignment")) {
            // get the variable
            Pair<Symbol, Character> var = this.symbolTable.findVariable(auxNode.get("varName"), this.exploredMethod);
            if ( var == null ) return new OllirTools("", "", "");

            String returnType = OllirTools.getOllirType(var.a.getType().getName());
            code += "invokevirtual(" + objName + ", \"" + jmmNode.get("methodName") + "\"" + arguments + ")." + returnType;
            opType = returnType;
        } else {
            code += "invokevirtual(" + objName + ", \"" + jmmNode.get("methodName") + "\"" + arguments + ").V";
        }

        // create  resulting OllirTools
        return new OllirTools(preCode, code, opType);
    }

    private OllirTools dealWithLengthOp(JmmNode jmmNode, String s) {
        String opType = "i32";
        String preCode = "";
        String code = "";

        OllirTools auxOllirTools = visit(jmmNode.getJmmChild(0), s);

        // add preCode
        if (auxOllirTools.getPreCode() != "") { preCode += auxOllirTools.getPreCode(); }

        // check of terminal expression
        if (!auxOllirTools.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools.getOpType();

            // add to preCode
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + auxOllirTools.getOpType() + " " + auxOllirTools.getCode() + ";";

            // add to code
            code += "arraylength(" + tempVar + ".array.i32" + ").i32";
        }
        else {
            code += "arraylength(" + auxOllirTools.getCode() + ").i32";
        }

        return new OllirTools(preCode, code, opType);
    }

    private OllirTools dealWithObjectDeclaration(JmmNode jmmNode, String s) {
        String opType = jmmNode.get("objName");
        String preCode = "";
        String code = "";

        // check if declaration comes from assign
        JmmNode auxNode = jmmNode;
        while(auxNode.getJmmParent().getKind().equals("ParenthesesOp")) {
            auxNode = auxNode.getJmmParent();
        }

        auxNode = auxNode.getJmmParent();
        if (auxNode.getKind().equals("Assignment")) {
            code += "new (" + jmmNode.get("objName") + ")." + jmmNode.get("objName");
            String varName = auxNode.get("varName");
            code += ";\n" + s + "invokespecial(" + varName + "." + jmmNode.get("objName") + ", \"<init>\").V";

            OllirTools res = new OllirTools(preCode, code, opType);
            return res;
        }

        this.tempVarCount++;
        String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + opType;
        preCode += s + tempVar + "." + jmmNode.get("objName") +  " :=." + jmmNode.get("objName") + " new (" + jmmNode.get("objName") + ")." + jmmNode.get("objName") + ";";
        preCode += "\n" + s + "invokespecial(" + tempVar + ", \"<init>\").V;";
        code += tempVar;

        OllirTools res = new OllirTools(preCode, code, opType);
        res.signalIdentifier();

        return res;
    }

    private OllirTools dealWithArrayDeclaration(JmmNode jmmNode, String s) {
        String opType = "array.i32";
        String preCode = "";
        String code = "";

        OllirTools auxOllirTools = visit(jmmNode.getJmmChild(0), s);

        // add preCode
        if (auxOllirTools.getPreCode() != "") { preCode += auxOllirTools.getPreCode(); }

        // check of terminal expression
        if (!auxOllirTools.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(tempVarCount) + "." + auxOllirTools.getOpType();

            // add to preCode
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + auxOllirTools.getOpType() + " " + auxOllirTools.getCode() + ";";

            // add to code
            code += "new(array, " + tempVar + ".i32" + ").array.i32";
        }
        else {
            code += "new(array, " + auxOllirTools.getCode() + ").array.i32";
        }

        return new OllirTools(preCode, code, opType);
    }

    private OllirTools dealWithBinaryOp(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = "";

        // visit left and right children
        OllirTools leftOllirTools = visit(jmmNode.getJmmChild(0), s);
        OllirTools rightOllirTools = visit(jmmNode.getJmmChild(1), s);

        // get the necessary preCode to support the children
        if (leftOllirTools.getPreCode() != "") {
            preCode += leftOllirTools.getPreCode();
        }
        if (rightOllirTools.getPreCode() != "") {
            if (preCode != "") preCode += "\n";
            preCode += rightOllirTools.getPreCode();
        }

        // get the left and right children code

        String leftCode = leftOllirTools.getCode();
        String rightCode = rightOllirTools.getCode();
        // make code expression
        // verify if the children are terminal expressions
        if (!leftOllirTools.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(this.tempVarCount) + "." + leftOllirTools.getOpType();

            // add preceding operation
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + leftOllirTools.getOpType() + " " + leftOllirTools.getCode() + ";";

            leftCode = tempVar;
        }
        if (!rightOllirTools.isTerminal()) {
            // create a temporary variable to store the result of the operation
            this.tempVarCount++;
            String tempVar = OllirTools.tempVarToString(this.tempVarCount) + "." + rightOllirTools.getOpType();

            // add preceding operation
            if (preCode != "") preCode += "\n";
            preCode += s + tempVar + " :=." + rightOllirTools.getOpType() + " " + rightOllirTools.getCode() + ";";

            rightCode = tempVar;
        }

        // create operation code
        // determine the operation type
        if (jmmNode.get("op").equals("+") || jmmNode.get("op").equals("/") || jmmNode.get("op").equals("*") || jmmNode.get("op").equals("-")) {
            opType = leftOllirTools.getOpType();
        } else {
            opType = OllirTools.getOllirType("boolean");
        }

        String operator = jmmNode.get("op");
        code += leftCode + " " + operator + "." + leftOllirTools.getOpType() + " " + rightCode;

        // determine the operation type
        if (jmmNode.get("op").equals("+") || jmmNode.get("op").equals("/") || jmmNode.get("op").equals("*") || jmmNode.get("op").equals("-")) {
            opType = leftOllirTools.getOpType();
        } else {
            opType = OllirTools.getOllirType("boolean");
        }

        // create  resulting OllirTools
        OllirTools res = new OllirTools(preCode, code, opType);

        return res;
    }

    private OllirTools dealWithInteger(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = OllirTools.getOllirType("int");


        // get the value
        code += jmmNode.get("val") + "." + opType;

        // create  resulting OllirTools
        OllirTools res = new OllirTools(preCode, code, opType);
        res.signalIdentifier();

        return res;
    }

    private OllirTools dealWithBoolean(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = OllirTools.getOllirType("boolean");

        // get the boolean value
        if (jmmNode.getKind().equals("true")) {
            code += "1." + opType;
        } else {
            code += ("0." + opType);
        }

        // create  resulting OllirTools
        OllirTools res = new OllirTools(preCode, code, opType);
        res.signalIdentifier();

        return res;
    }

    private OllirTools dealWithIdentifier(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = "";

        boolean signalIdentifier = false;

        // get the variable
        Pair<Symbol, Character> var = this.symbolTable.findVariable(jmmNode.get("varName"), this.exploredMethod);
        if ( var == null ) return new OllirTools("", "", "");

        if (var.b == 'l') {
            // get the variable type
            if(var.a.getType().isArray()) { opType += "array."; }
            opType += OllirTools.getOllirType(var.a.getType().getName());
            code += var.a.getName() + "." + opType;
            signalIdentifier = true;
        } else if (var.b == 'p') {
            // get the variable type
            if(var.a.getType().isArray()) { opType += "array."; }
            opType += OllirTools.getOllirType(var.a.getType().getName());
            // get the parameter index
            int index = this.symbolTable.getParameters(this.exploredMethod).indexOf(var.a) + 1; // could give an error
            if (index < 1) return new OllirTools("", "", "");
            code += "$" + (index) +  "." + var.a.getName() + "." + opType;
            signalIdentifier = true;
        } else if (var.b == 'f') {
            // get the variable type
            if(var.a.getType().isArray()) { opType += "array."; }
            opType += OllirTools.getOllirType(var.a.getType().getName());
            // create a temporary variable to store the result of the operation
            code += "getfield(this, " + var.a.getName() + "." + opType + ")." + opType;
        } else if (var.b == 'i') {
            code += var.a.getName();
            signalIdentifier = true;
        }

        // create  resulting OllirTools
        OllirTools res = new OllirTools(preCode, code, opType);
        if (signalIdentifier) res.signalIdentifier();

        return res;
    }

    private OllirTools dealWithObjIdentifier(JmmNode jmmNode, String s) {
        String code = "";
        String preCode = "";
        String opType = "";

        code += "this";
        opType += this.symbolTable.getClassName();

        // create  resulting OllirTools
        OllirTools res = new OllirTools(preCode, code, opType);
        res.signalIdentifier();

        return res;
    }

    public int getTempVarCount() {
        return tempVarCount;
    }
}
