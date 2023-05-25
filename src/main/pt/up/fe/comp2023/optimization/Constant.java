package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;

import java.util.Objects;

public class Constant {

    private final String value;
    private final Symbol local;
    private final String method;

    public Constant(String value, Symbol local, String method) {

        this.value = value;
        this.local = local;
        this.method = method;

    }

    public String getValue() {
        return value;
    }

    public Symbol getLocal() {
        return local;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Constant constant)) return false;
        return value == constant.value && local.equals(constant.local) && method.equals(constant.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, local, method);
    }
}
