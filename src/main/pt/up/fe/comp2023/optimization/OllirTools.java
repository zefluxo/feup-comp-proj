package pt.up.fe.comp2023.optimization;

public class OllirTools {
    private String preCode;
    private String code;
    private String opType;
    private boolean isTerminal = false;

    public OllirTools() {
        this.preCode = "";
        this.code = "";
        this.opType = "";
    }

    public OllirTools(String preCode, String code, String opType) {
        this.preCode = preCode;
        this.code = code;
        this.opType = opType;
    }

    public void signalIdentifier() {
        this.isTerminal = true;
    }

    public String getPreCode(){
        return this.preCode;
    }

    public String getCode(){
        return this.code;
    }

    public String getOpType(){
        return this.opType;
    }

    public boolean isTerminal() {
        return this.isTerminal;
    }

    public void resetCode(){
        this.preCode = "invalid";
        this.code = "invalid";
        this.opType = "invalid";
    }

    public static String tempVarToString(int tempVarCounter) { return "t" + tempVarCounter; }

    public static String getOllirType(String type) {
        switch (type) {
            case "int":
                return "i32";
            case "boolean":
                return "bool";
            case "int[]":
                return "array.i32";
            default:
                return type;
        }
    }
}
