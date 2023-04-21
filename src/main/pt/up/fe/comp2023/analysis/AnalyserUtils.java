package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;


import java.util.HashMap;

public class AnalyserUtils {

    public static Type getType(JmmNode node) {
        String typeName = node.get("type");
        boolean isArray = node.hasAttribute("isArray") && node.get("isArray").equals("true");
        return new Type(typeName, isArray);
    }

    public static String binaryOpReturnType(String op) {
        return switch (op) {
            case "*", "/", "+", "-" -> "int";
            case "<", "&&" -> "boolean";
            default -> "invalid";
        };
    }

    public static String binaryOpOperandType(String op) {
        return switch (op) {
            case "*", "/", "+", "-", "<" -> "int";
            case "&&" -> "boolean";
            default -> "invalid";
        };
    }

}
