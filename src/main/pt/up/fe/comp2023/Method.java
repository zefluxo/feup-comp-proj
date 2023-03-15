package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Method {
    private final Type returnType;
    private final String methodName;
    private final List<Symbol> arguments;
    private final List<Symbol> localVariables;

    public Method(String methodName, Type returnType, List<Symbol> arguments, List<Symbol> localVariables) {

        this.methodName = methodName;
        this.returnType = returnType;
        this.arguments = arguments;
        this.localVariables = localVariables;

    }

    public List<Symbol> getArguments() {
        return arguments;
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public String toString() {
        return this.returnType + " | " + this.methodName + " | " + this.arguments;
    }

}
