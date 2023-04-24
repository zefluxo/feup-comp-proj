package pt.up.fe.comp2023.ollirToJasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;

public class OllirToJasmin implements JasminBackend {
    public OllirToJasmin() {
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.className = "";
        this.superClass = "java/lang/Object";
    }
    StringBuilder jasminCode;
    ArrayList<Report> reports;
    String className;
    String superClass;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        ollirClass.buildVarTables();

        this.jasminCode.append(headerToJasmin(ollirClass));

        ArrayList<Field> fields = ollirClass.getFields();
        for (Field field : fields) jasminCode.append(fieldToJasmin(field)).append('\n');

        ArrayList<Method> methods = ollirClass.getMethods();
        for (Method method : methods) jasminCode.append(methodToJasmin(method).append('\n'));

        return new JasminResult(ollirResult, jasminCode.toString(), reports);
    }

    private String accessModifierToString(AccessModifiers accessModifiers) {
        String accessModifier;
        switch (accessModifiers) {

            case PUBLIC -> {
                accessModifier = "public ";
            }
            case PRIVATE -> {
                accessModifier = "private ";
            }
            case PROTECTED -> {
                accessModifier = "protected ";
            }
            case DEFAULT -> {
                accessModifier = "";    //not sure
            }
            default -> throw new IllegalStateException("Unexpected value: " + accessModifiers);
        }
        return accessModifier;
    }

    private String typeToString(Type type) {
        String typeString;
        switch (type.getTypeOfElement()) {
            case INT32 -> {
                typeString = "I";
            }
            case BOOLEAN -> {
                typeString = "Z";
            }
            case ARRAYREF -> {
                typeString = "[" + typeToString(((ArrayType) type).getElementType());
            }
            case OBJECTREF, CLASS, THIS -> {
                typeString = "L" + this.className + ";";
            }
            case STRING -> {
                typeString = "Ljava/lang/String;";
            }
            case VOID -> {
                typeString = "V";
            }
            default -> throw new IllegalStateException("Unexpected value: " + type.getTypeOfElement());
        }
        return typeString;
    }

    private String returnTypeToString(Type type) {
        String returnTypeString;
        switch (type.getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                returnTypeString = "i";
            }
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING -> {
                returnTypeString = "a";
            }
            case VOID -> {
                returnTypeString = "";
            }
            default -> throw new IllegalStateException("Unexpected value: " + type.getTypeOfElement());
        }
        return returnTypeString;
    }

    private StringBuilder headerToJasmin(ClassUnit ollirClass) {
        StringBuilder header = new StringBuilder().append(".class ");
        AccessModifiers accessModifiers = ollirClass.getClassAccessModifier();
        boolean isStatic = ollirClass.isStaticClass();
        boolean isFinal = ollirClass.isFinalClass();
        this.className = ollirClass.getClassName();
        this.superClass = ollirClass.getSuperClass();
        String accessModifier = accessModifierToString(accessModifiers);

        header.append(accessModifier);
        if (isStatic) header.append("static ");
        if (isFinal) header.append("final ");
        header.append(className).append('\n');

        if (superClass == null) superClass = "java/lang/Object";
        header.append(".super ").append(superClass).append("\n\n");

        return header;
    }

    private StringBuilder fieldToJasmin(Field ollirField) {
        StringBuilder jasminField = new StringBuilder().append(".field ");
        AccessModifiers fieldAccessModifier = ollirField.getFieldAccessModifier();
        boolean isStaticField = ollirField.isStaticField();
        boolean isFinalField = ollirField.isFinalField();
        String fieldName = ollirField.getFieldName();
        boolean isInitialized = ollirField.isInitialized();
        int initialValue = ollirField.getInitialValue();
        Type fieldType = ollirField.getFieldType();

        jasminField.append(accessModifierToString(fieldAccessModifier));
        if (isStaticField) jasminField.append("static ");
        if (isFinalField) jasminField.append("final ");
        jasminField.append(fieldName).append(" ");
        jasminField.append(typeToString(fieldType));

        if (isInitialized) jasminField.append(" = ").append(initialValue);

        return jasminField;
    }

    private StringBuilder methodToJasmin(Method ollirMethod) {
        StringBuilder jasminMethod = new StringBuilder().append(".method ");
        AccessModifiers methodAccessModifier = ollirMethod.getMethodAccessModifier();
        boolean isStaticMethod = ollirMethod.isStaticMethod();
        boolean isFinalMethod = ollirMethod.isFinalMethod();
        boolean isConstructMethod = ollirMethod.isConstructMethod();
        String methodName = ollirMethod.getMethodName();
        ArrayList<Element> paramList = ollirMethod.getParams();
        Type returnType = ollirMethod.getReturnType();
        int stackLimit = 99;
        int localsLimit = 99;
        HashMap<String, Descriptor> varTable = ollirMethod.getVarTable();
        ArrayList<Instruction> listOfInstr = ollirMethod.getInstructions();

        // Header
        jasminMethod.append(accessModifierToString(methodAccessModifier));
        if (isStaticMethod) jasminMethod.append("static ");
        if (isFinalMethod) jasminMethod.append("final ");
        jasminMethod.append(isConstructMethod ? "<init>" : methodName);
        jasminMethod.append("(");
        // Parameters
        for (Element param : paramList) jasminMethod.append(typeToString(param.getType()));
        jasminMethod.append(")").append(typeToString(returnType)).append('\n');

        // Limits
        jasminMethod.append(".limit stack ").append(stackLimit).append('\n');
        jasminMethod.append(".limit locals ").append(localsLimit).append('\n');

        // Instructions
        for (Instruction instruction : listOfInstr) {
            jasminMethod.append(instructionToJasmin(instruction, varTable));
            if (instruction.getInstType() != InstructionType.NOPER) jasminMethod.append('\n');
        }
        if (ollirMethod.isConstructMethod()) jasminMethod.append("return\n");

        jasminMethod.append(".end method");

        if (ollirMethod.isConstructMethod()) jasminMethod.append("\n\n\n\n");

        return jasminMethod;
    }

    private StringBuilder instructionToJasmin(Instruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminInstruction = new StringBuilder();
        InstructionType instrType = instruction.getInstType();

        switch (instrType) {

            case ASSIGN -> {
                jasminInstruction.append(assignInstruction((AssignInstruction) instruction, varTable));
            }
            case CALL -> {
                jasminInstruction.append(callInstruction((CallInstruction) instruction, varTable));
            }
            case GOTO -> {
                jasminInstruction.append(gotoInstruction((GotoInstruction) instruction));
            }
            case BRANCH -> {
                jasminInstruction.append(branchInstruction((CondBranchInstruction) instruction, varTable));
            }
            case RETURN -> {
                jasminInstruction.append(returnInstruction((ReturnInstruction) instruction, varTable));
            }
            case PUTFIELD -> {
                jasminInstruction.append(putFieldInstruction((PutFieldInstruction) instruction, varTable));
            }
            case GETFIELD -> {
                jasminInstruction.append(getFieldInstruction((GetFieldInstruction) instruction, varTable));
            }
            case UNARYOPER -> {
                jasminInstruction.append(unaryOperInstruction((UnaryOpInstruction) instruction, varTable));
            }
            case BINARYOPER -> {
                jasminInstruction.append(binaryOperInstruction((BinaryOpInstruction) instruction, varTable));
            }
            case NOPER -> {
                jasminInstruction.append(nOperInstruction((SingleOpInstruction) instruction, varTable));
            }
            default -> throw new IllegalStateException("Unexpected value: " + instrType);
        }

        if (instrType == InstructionType.CALL && ((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID) {
            jasminInstruction.append("\npop");
        }

        return jasminInstruction;
    }

    private StringBuilder operationToJasmin(OperationType operationType) {
        StringBuilder jasminOperation = new StringBuilder();

        switch (operationType) {

            case ADD -> {
                jasminOperation.append("iadd");
            }
            case SUB -> {
                jasminOperation.append("isub");
            }
            case MUL -> {
                jasminOperation.append("imul");
            }
            case DIV -> {
                jasminOperation.append("idiv");
            }
            case AND -> {
                jasminOperation.append("iand");
            }
            case LTH -> {
                jasminOperation.append("if_icmplt");
            }
            case NOTB -> {
                jasminOperation.append("ifeq");
            }
            default -> throw new IllegalStateException("Unexpected value: " + operationType);
        }

        return jasminOperation;
    }

    private StringBuilder assignInstruction(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminAssign = new StringBuilder();
        Operand dest = (Operand) instruction.getDest();
        Instruction rhs = instruction.getRhs();
        Type typeOfAssign = instruction.getTypeOfAssign();

        jasminAssign.append(instructionToJasmin(rhs, varTable));
        if (rhs.getInstType() != InstructionType.NOPER) jasminAssign.append('\n');

        int varNum = varTable.get(dest.getName()).getVirtualReg();
        String isShort = " ";
        if (0 <= varNum && varNum <= 3) isShort = "_";

        jasminAssign.append(returnTypeToString(typeOfAssign)).append("store").append(isShort).append(varNum);

        return jasminAssign;
    }

    private StringBuilder callInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCall = new StringBuilder();
        Operand firstArg = (Operand) instruction.getFirstArg();
        LiteralElement secondArg = (LiteralElement) instruction.getSecondArg();
        ArrayList<Element> listOfOperands = instruction.getListOfOperands();
        CallType invocationType = instruction.getInvocationType();
        Type returnType = instruction.getReturnType();

        if (invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual) {
            jasminCall.append(toStack(firstArg, varTable));
        }

        for (Element operand : listOfOperands) {
            if (operand.isLiteral()) {
                jasminCall.append(toStack((LiteralElement) operand));
            } else {
                jasminCall.append(toStack((Operand) operand, varTable));
            }
        }

        switch (invocationType) {

            case invokevirtual -> {
                jasminCall.append("invokevirtual ");
            }
            case invokeinterface -> {
                jasminCall.append("invokeinterface ");
            }
            case invokespecial -> {
                jasminCall.append("invokespecial ");
            }
            case invokestatic -> {
                jasminCall.append("invokestatic ");
            }
            case NEW -> {
                jasminCall.append("new ");
            }
            case arraylength -> {
                jasminCall.append("arraylength ");
            }
            case ldc -> {
                jasminCall.append("ldc ");
            }
            default -> throw new IllegalStateException("Unexpected value: " + invocationType);
        }

        Type firstArgClassType = firstArg.getType();
        switch (firstArgClassType.getTypeOfElement()) {

            case OBJECTREF -> {
                jasminCall.append(this.className);
            }
            case CLASS -> {
                jasminCall.append(firstArg.getName());
            }
            case THIS -> {
                jasminCall.append(this.superClass);
            }
            default -> throw new IllegalStateException("Unexpected value: " + firstArgClassType.getTypeOfElement());
        }
        if (invocationType != CallType.NEW) jasminCall.append("/").append(secondArg.getLiteral(), 1, secondArg.getLiteral().length() - 1).append("(");
        else jasminCall.append("\ndup");

        for (Element op : listOfOperands) jasminCall.append(typeToString(op.getType()));

        if (invocationType != CallType.NEW) jasminCall.append(")").append(typeToString(returnType));

        return jasminCall;
    }

    private StringBuilder gotoInstruction(GotoInstruction instruction) {
        // Wasn't for this checkpoint but it was simple
        return new StringBuilder().append("goto ").append(instruction.getLabel());
    }

    private StringBuilder branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminBranch = new StringBuilder();

        //TODO

        return jasminBranch;
    }

    private StringBuilder returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        Element operand = instruction.getOperand();
        StringBuilder jasminReturn;
        if (instruction.getElementType() == ElementType.VOID) {
            jasminReturn = new StringBuilder();
        } else if (operand.isLiteral()) {
            jasminReturn = toStack((LiteralElement) operand);
        } else {
            jasminReturn = toStack((Operand) operand, varTable);
        }

        return jasminReturn.append(returnTypeToString(instruction.getReturnType())).append("return");
    }

    private StringBuilder putFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminPutField = new StringBuilder();
        Operand firstOperand = (Operand) instruction.getFirstOperand();
        Operand secondOperand = (Operand) instruction.getSecondOperand();
        LiteralElement thirdOperand = (LiteralElement) instruction.getThirdOperand();
        Type fieldType = instruction.getFieldType();

        jasminPutField.append(toStack(firstOperand, varTable));
        jasminPutField.append(toStack(thirdOperand));

        return jasminPutField.append("putfield ").append(firstOperand.getName()).append("/").append(secondOperand.getName()).append(" ").append(typeToString(fieldType));
    }

    private StringBuilder getFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminGetField = new StringBuilder();
        Operand firstOperand = (Operand) instruction.getFirstOperand();
        Operand secondOperand = (Operand) instruction.getSecondOperand();
        Type fieldType = instruction.getFieldType();

        jasminGetField.append(toStack(firstOperand, varTable));

        return jasminGetField.append("getfield ").append(firstOperand.getName()).append("/").append(secondOperand.getName()).append(" ").append(typeToString(fieldType));
    }

    private StringBuilder unaryOperInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminUnaryOper = new StringBuilder();
        Element operand = instruction.getOperand();
        OperationType operationType = instruction.getOperation().getOpType();

        if (operand.isLiteral()) {
            jasminUnaryOper.append(toStack((LiteralElement) operand));
        } else {
            jasminUnaryOper.append(toStack((Operand) operand, varTable));
        }

        jasminUnaryOper.append(operationToJasmin(operationType)).append('\n');

        return jasminUnaryOper;
    }

    private StringBuilder binaryOperInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminBinaryOper = new StringBuilder();
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();
        OperationType operationType = instruction.getOperation().getOpType();

        if (leftOperand.isLiteral()) {
            jasminBinaryOper.append(toStack((LiteralElement) leftOperand));
        } else {
            jasminBinaryOper.append(toStack((Operand) leftOperand, varTable));
        }
        if (rightOperand.isLiteral()) {
            jasminBinaryOper.append(toStack((LiteralElement) rightOperand));
        } else {
            jasminBinaryOper.append(toStack((Operand) rightOperand, varTable));
        }

        jasminBinaryOper.append(operationToJasmin(operationType));

        return jasminBinaryOper;
    }

    private StringBuilder nOperInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminNOper = new StringBuilder();
        Element singleOperand = instruction.getSingleOperand();

        if (singleOperand.isLiteral()) {
            jasminNOper.append(toStack((LiteralElement) singleOperand));
        } else {
            jasminNOper.append(toStack((Operand) singleOperand, varTable));
        }

        return jasminNOper;
    }

    private StringBuilder toStack(LiteralElement literalElement) {
        StringBuilder stack = new StringBuilder();
        String literal = literalElement.getLiteral();
        ElementType type = literalElement.getType().getTypeOfElement();

        switch (type) {

            case INT32, BOOLEAN -> {
                int literalInt = Integer.parseInt(literal);

                if (-1 <= literalInt && literalInt <= 5) {
                    stack.append("iconst_");
                    if (literalInt == -1) {
                        stack.append("m");
                    }
                    stack.append(literal);
                } else if (-128 <= literalInt && literalInt <= 127) {
                    stack.append("bipush ").append(literal);
                } else if (-32768 <= literalInt && literalInt <= 32767) {
                    stack.append("sipush ").append(literal);
                } else {
                    stack.append("ldc ").append(literal);
                }
            }
            case ARRAYREF, OBJECTREF, CLASS, THIS, STRING, VOID -> {
                stack.append("ldc ").append(literal);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        return stack.append('\n');
    }

    private StringBuilder toStack(Operand operand, HashMap<String, Descriptor> varTable) {
        StringBuilder stack = new StringBuilder();
        String name = operand.getName();
        int varNum = varTable.get(name).getVirtualReg();
        ElementType type = operand.getType().getTypeOfElement();
        String isShort = " ";

        switch (type) {

            case INT32, BOOLEAN -> {
                stack.append("iload");
            }
            case ARRAYREF, OBJECTREF, CLASS, STRING, VOID -> {
                stack.append("aload");
            }
            case THIS -> {
                stack.append("aload");
                varNum = 0;
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }

        if (0 <= varNum && varNum <= 3) isShort = "_";

        return stack.append(isShort).append(varNum).append('\n');
    }
}