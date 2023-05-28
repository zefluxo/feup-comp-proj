package pt.up.fe.comp2023.ollirToJasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.*;

public class OllirToJasmin implements JasminBackend {
    public OllirToJasmin() {
        this.jasminCode = new StringBuilder();
        this.reports = new ArrayList<>();
        this.className = "";
        this.superClass = "java/lang/Object";
        this.imports = new ArrayList<>();
        this.numberOfConditions = 0;
        this.stackLimit = 0;
        this.currentStack = 0;
        this.localRegisters = new HashSet<>();
    }
    StringBuilder jasminCode;
    ArrayList<Report> reports;
    String className;
    String superClass;
    ArrayList<String> imports;
    int numberOfConditions;
    int stackLimit;
    int currentStack;
    Set<Integer> localRegisters;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();
        ollirClass.buildVarTables();
        this.imports = ollirClass.getImports();

        this.jasminCode.append(headerToJasmin(ollirClass));

        ArrayList<Field> fields = ollirClass.getFields();
        for (Field field : fields) jasminCode.append(fieldToJasmin(field)).append('\n');
        jasminCode.append('\n');

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
                accessModifier = "";
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
            case THIS -> {
                typeString = "L" + this.className + ";";
            }
            case OBJECTREF, CLASS -> {
                typeString = "L" + getClassFQN(((ClassType) type).getName()) + ";";
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

        header.append(accessModifiers == AccessModifiers.DEFAULT ? "public " : accessModifier);
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
        HashMap<String, Descriptor> varTable = ollirMethod.getVarTable();
        ArrayList<Instruction> listOfInstr = ollirMethod.getInstructions();
        this.stackLimit = 0;
        this.currentStack = 0;
        this.localRegisters = new HashSet<>();
        updateRegisters(0);

        // Header
        if (isConstructMethod && methodAccessModifier == AccessModifiers.DEFAULT) {
            jasminMethod.append("public ");
        } else {
            jasminMethod.append(accessModifierToString(methodAccessModifier));
        }
        if (isStaticMethod) jasminMethod.append("static ");
        if (isFinalMethod) jasminMethod.append("final ");
        jasminMethod.append(isConstructMethod ? "<init>" : methodName);
        jasminMethod.append("(");
        // Parameters
        for (Element param : paramList) jasminMethod.append(typeToString(param.getType()));
        jasminMethod.append(")").append(typeToString(returnType)).append('\n');

        // Instructions
        StringBuilder jasminInstructions = new StringBuilder();
        boolean hasReturn = false;
        for (Instruction instruction : listOfInstr) {
            List<String> instructionLabel =  ollirMethod.getLabels(instruction);
            if (!instructionLabel.isEmpty()) jasminInstructions.append(instructionLabel.get(0)).append(":\n");
            jasminInstructions.append(instructionToJasmin(instruction, varTable));
            if (instruction.getInstType() != InstructionType.NOPER) jasminInstructions.append('\n');
            if (instruction.getInstType() == InstructionType.RETURN) hasReturn = true;

            if (instruction.getInstType() == InstructionType.CALL && (((CallInstruction) instruction).getReturnType().getTypeOfElement() != ElementType.VOID)) {
                jasminInstructions.append("pop\n");
                updateStack(-1);
            }
        }
        if (!hasReturn) jasminInstructions.append("return\n");

        // Limits
        int localsLimit = Math.max(this.localRegisters.size(), paramList.size()+1);
        jasminMethod.append(".limit stack ").append(this.stackLimit).append('\n');
        jasminMethod.append(".limit locals ").append(localsLimit).append('\n');

        jasminMethod.append(jasminInstructions);

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
            case ANDB -> {
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

        // Increment (i++)
        if (rhs.getInstType() == InstructionType.BINARYOPER) {
            Element leftOperand = ((BinaryOpInstruction) rhs).getLeftOperand();
            Element rightOperand = ((BinaryOpInstruction) rhs).getRightOperand();

            if (!leftOperand.isLiteral() && ((Operand) leftOperand).getName().equals(dest.getName()) && rightOperand.isLiteral() && Integer.parseInt(((LiteralElement) rightOperand).getLiteral()) == 1) {
                int varNum = varTable.get(((Operand) leftOperand).getName()).getVirtualReg();
                jasminAssign.append("iinc ").append(varNum).append(" 1");
                updateStack(-1);
                return jasminAssign;
            } else if (!rightOperand.isLiteral() && ((Operand) rightOperand).getName().equals(dest.getName()) && leftOperand.isLiteral() && Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) == 1) {
                int varNum = varTable.get(((Operand) rightOperand).getName()).getVirtualReg();
                jasminAssign.append("iinc ").append(varNum).append(" 1");
                updateStack(-1);
                return jasminAssign;
            }
        }

        if (dest instanceof ArrayOperand) {
            jasminAssign.append(toStack(dest, varTable));
            jasminAssign.append(toStack((Operand) ((ArrayOperand) dest).getIndexOperands().get(0), varTable));
        }

        jasminAssign.append(instructionToJasmin(rhs, varTable));
        if (rhs.getInstType() != InstructionType.NOPER) jasminAssign.append('\n');

        if (dest instanceof ArrayOperand) {
            updateStack(-3);
            return jasminAssign.append("iastore");
        }

        int varNum = varTable.get(dest.getName()).getVirtualReg();
        updateRegisters(varNum);
        String isShort = " ";
        if (0 <= varNum && varNum <= 3) isShort = "_";

        updateStack(-1);
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
        Type firstArgClassType = firstArg.getType();

        if (invocationType == CallType.invokespecial || invocationType == CallType.invokevirtual || invocationType == CallType.arraylength) {
            jasminCall.append(toStack(firstArg, varTable));
        }

        if (listOfOperands != null) {
            for (Element operand : listOfOperands) {
                jasminCall.append(toStack(operand, varTable));
            }
        }

        int stackDiff = 0;
        switch (invocationType) {

            case invokevirtual -> {
                jasminCall.append("invokevirtual ");
                stackDiff--;
            }
            case invokeinterface -> {
                jasminCall.append("invokeinterface ");
            }
            case invokespecial -> {
                jasminCall.append("invokespecial ");
                stackDiff--;
            }
            case invokestatic -> {
                jasminCall.append("invokestatic ");
            }
            case NEW -> {
                if (firstArg.getType().getTypeOfElement() == ElementType.ARRAYREF) {
                    return jasminCall.append("newarray int");
                }
                jasminCall.append("new ");
                stackDiff++;
            }
            case arraylength -> {
                return jasminCall.append("arraylength");
            }
            case ldc -> {
                jasminCall.append("ldc ");
            }
            default -> throw new IllegalStateException("Unexpected value: " + invocationType);
        }

        if (invocationType == CallType.invokespecial && firstArgClassType.getTypeOfElement() == ElementType.THIS) jasminCall.append(this.superClass);
        else if (invocationType == CallType.invokevirtual || invocationType == CallType.invokespecial) jasminCall.append(getClassFQN(((ClassType) firstArgClassType).getName()));
        else jasminCall.append(getClassFQN(firstArg.getName()));

        if (invocationType == CallType.invokespecial) jasminCall.append("/<init>(");
        else if (invocationType != CallType.NEW) jasminCall.append("/").append(secondArg.getLiteral().replace("\"", "")).append("(");
        else {
            jasminCall.append("\ndup");
            updateStack(1);
        }

        if (listOfOperands != null) {
            for (Element op : listOfOperands) {
                jasminCall.append(typeToString(op.getType()));
                updateStack(-1);
            }
        }

        updateStack(stackDiff);

        if (invocationType != CallType.NEW) jasminCall.append(")").append(typeToString(returnType));

        return jasminCall;
    }

    private StringBuilder gotoInstruction(GotoInstruction instruction) {
        return new StringBuilder().append("goto ").append(instruction.getLabel());
    }

    private StringBuilder branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminBranch = new StringBuilder();
        String label = instruction.getLabel();
        List<Element> operands = instruction.getOperands();
        Element firstOp = operands.get(0);
        int currentStackLimit = this.stackLimit;

        boolean firstIsZero = isZero(firstOp, varTable);
        StringBuilder firstOpString = toStack(firstOp, varTable);

        if (instruction instanceof OpCondInstruction) {
            Element secondOp = operands.get(1);

            boolean secondIsZero = isZero(secondOp, varTable);
            StringBuilder secondOpString = toStack(secondOp, varTable);

            if (firstIsZero) {
                jasminBranch.append(secondOpString).append("ifgt ");
                if (this.stackLimit > currentStackLimit) this.stackLimit--;
                updateStack(-2);
            } else if (secondIsZero) {
                jasminBranch.append(firstOpString).append("igle ");
                if (this.stackLimit > currentStackLimit) this.stackLimit--;
                updateStack(-2);
            } else {
                jasminBranch.append(firstOpString).append(secondOpString).append("if_icmplt ");
                updateStack(-2);
            }
        } else if (instruction instanceof SingleOpCondInstruction) {
            jasminBranch.append(firstOpString);
            jasminBranch.append("ifne ");
            updateStack(-1);
        }

        jasminBranch.append(label);

        return jasminBranch;
    }

    private StringBuilder returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        Element operand = instruction.getOperand();
        StringBuilder jasminReturn;
        if (instruction.getElementType() == ElementType.VOID) {
            jasminReturn = new StringBuilder();
        } else {
            jasminReturn = toStack(operand, varTable);
            updateStack(-1);
        }

        return jasminReturn.append(returnTypeToString(instruction.getReturnType())).append("return");
    }

    private StringBuilder putFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminPutField = new StringBuilder();
        Operand firstOperand = (Operand) instruction.getFirstOperand();
        Operand secondOperand = (Operand) instruction.getSecondOperand();
        Element thirdOperand = instruction.getThirdOperand();

        jasminPutField.append(toStack(firstOperand, varTable));
        jasminPutField.append(toStack(thirdOperand, varTable));

        updateStack(-2);

        return jasminPutField.append("putfield ").append(getClassFQN(firstOperand.getName())).append("/").append(secondOperand.getName()).append(" ").append(typeToString(secondOperand.getType()));
    }

    private StringBuilder getFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminGetField = new StringBuilder();
        Operand firstOperand = (Operand) instruction.getFirstOperand();
        Operand secondOperand = (Operand) instruction.getSecondOperand();
        Type fieldType = instruction.getFieldType();

        jasminGetField.append(toStack(firstOperand, varTable));

        return jasminGetField.append("getfield ").append(getClassFQN(firstOperand.getName())).append("/").append(secondOperand.getName()).append(" ").append(typeToString(fieldType));
    }

    private StringBuilder unaryOperInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminUnaryOper = new StringBuilder();
        Element operand = instruction.getOperand();
        OperationType operationType = instruction.getOperation().getOpType();
        boolean isBooleanOp = instruction.getOperation().getTypeInfo().getTypeOfElement() == ElementType.BOOLEAN;

        jasminUnaryOper.append(toStack(operand, varTable));

        jasminUnaryOper.append(operationToJasmin(operationType));
        updateStack(-1);

        if (isBooleanOp) {
            if (operationType == OperationType.NOTB) {
                jasminUnaryOper.append(" FALSE").append(this.numberOfConditions).append("\n");
                jasminUnaryOper.append("iconst_0\n");
                updateStack(1);
                jasminUnaryOper.append("goto END").append(this.numberOfConditions).append("\n");
                jasminUnaryOper.append("FALSE").append(this.numberOfConditions).append(":\n");
                jasminUnaryOper.append("iconst_1\n");
                updateStack(1);
                jasminUnaryOper.append("END").append(this.numberOfConditions).append(":");

                this.numberOfConditions++;
            } else jasminUnaryOper.append(getCondition());
        }

        return jasminUnaryOper;
    }

    private StringBuilder binaryOperInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminBinaryOper = new StringBuilder();
        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();
        OperationType operationType = instruction.getOperation().getOpType();
        boolean isBooleanOp = instruction.getOperation().getTypeInfo().getTypeOfElement() == ElementType.BOOLEAN;
        int currentStackLimit = this.stackLimit;

        boolean firstIsZero = isZero(leftOperand, varTable);
        StringBuilder firstOpString = toStack(leftOperand, varTable);

        boolean secondIsZero = isZero(rightOperand, varTable);
        StringBuilder secondOpString = toStack(rightOperand, varTable);

        if (!isBooleanOp) {
            jasminBinaryOper.append(firstOpString).append(secondOpString);
            jasminBinaryOper.append(operationToJasmin(operationType));
            updateStack(-2);
        } else {
            if (operationType == OperationType.ANDB) {
                jasminBinaryOper.append(firstOpString).append(secondOpString).append("iand");
                updateStack(-2);
            } else {
                if (firstIsZero) {
                    jasminBinaryOper.append(secondOpString).append("ifgt");
                    if (this.stackLimit > currentStackLimit) this.stackLimit--;
                    updateStack(-2);
                } else if (secondIsZero) {
                    jasminBinaryOper.append(firstOpString).append("igle");
                    if (this.stackLimit > currentStackLimit) this.stackLimit--;
                    updateStack(-2);
                } else {
                    jasminBinaryOper.append(firstOpString).append(secondOpString).append("if_icmplt");
                    updateStack(-2);
                }
                jasminBinaryOper.append(getCondition());
            }
        }

        return jasminBinaryOper;
    }

    private StringBuilder getCondition() {
        StringBuilder condition = new StringBuilder();
        condition.append(" FALSE").append(this.numberOfConditions).append("\n");
        condition.append("iconst_1\n");
        updateStack(1);
        condition.append("goto END").append(this.numberOfConditions).append("\n");
        condition.append("FALSE").append(this.numberOfConditions).append(":\n");
        condition.append("iconst_0\n");
        updateStack(1);
        condition.append("END").append(this.numberOfConditions).append(":");

        this.numberOfConditions++;

        return condition;
    }

    private StringBuilder nOperInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminNOper = new StringBuilder();
        Element singleOperand = instruction.getSingleOperand();

        jasminNOper.append(toStack(singleOperand, varTable));

        if (singleOperand instanceof ArrayOperand) {
            jasminNOper.append(toStack((Operand) ((ArrayOperand) singleOperand).getIndexOperands().get(0), varTable));
            jasminNOper.append("iaload\n");
            updateStack(1);
        }

        return jasminNOper;
    }

    private boolean isZero(Element operand, HashMap<String, Descriptor> varTable) {
        if (operand.isLiteral()) {
            return ((LiteralElement) operand).getLiteral().equals("0");
        } else {
            return varTable.get(((Operand) operand).getName()).getVirtualReg() == 0;
        }
    }

    private StringBuilder toStack(Element operand, HashMap<String, Descriptor> varTable) {
        StringBuilder stack = new StringBuilder();
        if (operand.isLiteral()) {
            stack.append(toStack((LiteralElement) operand));
        } else {
            stack.append(toStack((Operand) operand, varTable));
        }

        return stack;
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

        updateStack(1);

        return stack.append('\n');
    }

    private StringBuilder toStack(Operand operand, HashMap<String, Descriptor> varTable) {
        StringBuilder stack = new StringBuilder();
        String name = operand.getName();
        int varNum = varTable.get(name).getVirtualReg();
        ElementType type = operand.getType().getTypeOfElement();
        if (operand instanceof ArrayOperand) type = ElementType.ARRAYREF;
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

        updateStack(1);
        updateRegisters(varNum);

        if (0 <= varNum && varNum <= 3) isShort = "_";

        return stack.append(isShort).append(varNum).append('\n');
    }

    private String getClassFQN(String className) {
        if (className.equals("this")) return this.className;
        for (String imported : this.imports) {
            if (imported.endsWith(className)) return imported.replace("\\.", "/");
        }
        return className;
    }

    private void updateRegisters(int register) {
        this.localRegisters.add(register);
    }

    private void updateStack(int numberOfRegisters) {
        this.currentStack += numberOfRegisters;
        if (this.currentStack > this.stackLimit) this.stackLimit = this.currentStack;
    }
}
