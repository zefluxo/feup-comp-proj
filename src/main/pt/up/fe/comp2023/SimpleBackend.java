package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.ollirToJasmin.OllirToJasmin;
public class SimpleBackend implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        OllirToJasmin ollirToJasmin = new OllirToJasmin();
        JasminResult result = ollirToJasmin.toJasmin(ollirResult);
        return result;

    }
}
