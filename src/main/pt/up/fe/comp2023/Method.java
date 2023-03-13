package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Method {
    private final Type returnType;
    private final String methodName;
    private final List<Symbol> arguments;

    public Method(String methodName, Type returnType, List<Symbol> arguments) {

        this.methodName = methodName;
        this.returnType = returnType;
        this.arguments = arguments;

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

    public String toString() {
        return this.returnType.toString() + " | " + this.methodName + " | " + this.arguments;
    }
}
