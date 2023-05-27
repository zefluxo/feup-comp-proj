package pt.up.fe.comp2023.optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

public class ConstantPropagation extends AJmmVisitor<String, String> {

    public boolean hasChanged = false;
    private final JmmNode root;
    private final Map<String, JmmNode> constants;
    private final List<Pair<String, JmmNode>> assignments;

    public ConstantPropagation(JmmNode root) {

        this.root = root;
        this.constants = new HashMap<>();
        this.assignments = new ArrayList<>();

    }

    @Override
    protected void buildVisitor() {

        addVisit("IfStatement", this::dealWithIfStatement);
        addVisit("WhileStatement", this::dealWithWhileStatement);
        addVisit("Identifier", this::dealWithIdentifier);
        addVisit("Assignment", this::dealWithAssignment);
        setDefaultVisit(this::dealWithDefault);

    }

    private String dealWithDefault(JmmNode node, String s) {

        for (JmmNode child: node.getChildren()) {
            visit(child , "");
        }

        return "";

    }

    private String dealWithIfStatement(JmmNode node, String s) {

        JmmNode condition = node.getJmmChild(0);
        JmmNode brackets = node.getJmmChild(1);
        ConstantPropagation ifVisitor = new ConstantPropagation(brackets);

        ifVisitor.visit(ifVisitor.root, "");
        if (ifVisitor.hasChanged) hasChanged = true;

        visit(condition, "");
        visit(brackets, "");

        return "";
    }

    private String dealWithWhileStatement(JmmNode node, String s) {

        JmmNode condition = node.getJmmChild(0);
        JmmNode brackets = node.getJmmChild(1);
        ConstantPropagation whileVisitor = new ConstantPropagation(brackets);

        whileVisitor.visit(whileVisitor.root, "");
        if (whileVisitor.hasChanged) hasChanged = true;

        List<Pair<String,JmmNode>> whileAssignments = whileVisitor.getAssignments();

        if (condition.getKind().equals("BinaryOp")) {

            JmmNode lhs = condition.getJmmChild(0);
            JmmNode rhs = condition.getJmmChild(1);

            if (lhs.getKind().equals("Identifier")) {

                String lhsName = lhs.get("varName");

                boolean visitLhs = true;
                for (Pair<String, JmmNode> assignment: whileAssignments) {

                    if (assignment.a.equals(lhsName)) {
                        visitLhs = false;
                        break;
                    }

                }

                if (visitLhs) visit(lhs, "");

            }

            if (rhs.getKind().equals("Identifier")) {

                String rhsName = rhs.get("varName");

                boolean visitRhs = true;
                for (Pair<String, JmmNode> assignment: whileAssignments) {

                    if (assignment.a.equals(rhsName)) {
                        visitRhs = false;
                        break;
                    }

                }

                if (visitRhs) visit(rhs, "");

            }

        }

        visit(brackets, "");
        return "";
    }

    private String dealWithIdentifier(JmmNode node, String s) {

        String varName = node.get("varName");
        JmmNode constant = constants.get(varName);

        if (constant == null) return "";

        node.replace(new JmmNodeImpl(constant.getKind(), constant));
        hasChanged = true;

        return "";

    }

    private String dealWithAssignment(JmmNode node, String s) {
        String varName = node.get("varName");
        JmmNode rhs = node.getJmmChild(0);

        assignments.add(new Pair<>(varName, rhs));
        if (!constants.containsKey(varName)) {

            if (!rhs.getKind().equals("Integer")) {
                visit(rhs, "");
                return "";
            }

            constants.put(varName, rhs);

        } else constants.remove(varName);

        visit(rhs, "");

        return "";

    }


    public JmmNode getRoot() { return root; }
    public List<Pair<String, JmmNode>> getAssignments() { return assignments; }
}
