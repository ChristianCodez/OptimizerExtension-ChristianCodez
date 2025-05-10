package cpsc326;

public class ASTOptimizer implements Visitor {

    private Expr currExpr;

    public Expr optimize(Expr node) {
        node.accept(this);
        return currExpr;
    }

    // --------------------------------------------------------------------
    // General AST Nodes
    // --------------------------------------------------------------------

    public void visit(Program node) {
        for (FunDef f : node.functions)
            f.accept(this);
    }

    public void visit(FunDef node) {
        for (Stmt stmt : node.stmts)
            stmt.accept(this);
    }

    public void visit(StructDef node) {
    }

    public void visit(DataType node) {
    }

    public void visit(VarDef node) {
    }

    // --------------------------------------------------------------------
    // Statements
    // --------------------------------------------------------------------

    public void visit(VarStmt node) {
        node.expr.ifPresent(expr -> {
            currExpr = expr;
            expr.accept(this);
            node.expr = java.util.Optional.of(currExpr);
        });
    }

    public void visit(AssignStmt node) {
        node.expr.accept(this);
        node.expr = currExpr;
    }

    public void visit(ReturnStmt node) {
        if (node.expr != null) {
            node.expr.accept(this);
            node.expr = currExpr;
        }
    }

    public void visit(WhileStmt node) {
        node.condition.accept(this);
        node.condition = currExpr;
        for (Stmt s : node.stmts)
            s.accept(this);
    }

    public void visit(ForStmt node) {
        node.fromExpr.accept(this);
        node.fromExpr = currExpr;
        node.toExpr.accept(this);
        node.toExpr = currExpr;
        for (Stmt s : node.stmts)
            s.accept(this);
    }

    public void visit(IfStmt node) {
        node.condition.accept(this);
        node.condition = currExpr;
        for (Stmt s : node.ifStmts)
            s.accept(this);
        node.elseIf.ifPresent(e -> e.accept(this));
        node.elseStmts.ifPresent(stmts -> {
            for (Stmt s : stmts)
                s.accept(this);
        });
    }

    // --------------------------------------------------------------------
    // Expressions
    // --------------------------------------------------------------------

    public void visit(BasicExpr node) {
        currExpr = node;
    }

    public void visit(UnaryExpr node) {
        node.expr.accept(this);
        node.expr = currExpr;

        if (node.expr instanceof BasicExpr b && b.rvalue instanceof SimpleRValue s) {
            Token lit = s.literal;
            if (node.unaryOp.lexeme.equals("not") && lit.tokenType == TokenType.BOOL_VAL) {
                boolean val = Boolean.parseBoolean(lit.lexeme);
                Token folded = new Token(TokenType.BOOL_VAL, String.valueOf(!val), lit.line, lit.column);
                currExpr = wrapLiteral(folded);
                return;
            }
        }

        currExpr = node;
    }

    public void visit(BinaryExpr node) {
        node.lhs.accept(this);
        node.lhs = currExpr;
        node.rhs.accept(this);
        node.rhs = currExpr;

        if (node.lhs instanceof BasicExpr && node.rhs instanceof BasicExpr) {
            BasicExpr l = (BasicExpr) node.lhs;
            BasicExpr r = (BasicExpr) node.rhs;

            if (l.rvalue instanceof SimpleRValue && r.rvalue instanceof SimpleRValue) {
                SimpleRValue lv = (SimpleRValue) l.rvalue;
                SimpleRValue rv = (SimpleRValue) r.rvalue;
                Token lt = lv.literal;
                Token rt = rv.literal;

                try {
                    String op = node.binaryOp.lexeme;

                    // --- Strength Reduction: Replace * or / with shifts for powers of 2 ---
                    if (lt.tokenType == TokenType.INT_VAL && rt.tokenType == TokenType.INT_VAL) {
                        int a = Integer.parseInt(lt.lexeme);
                        int b = Integer.parseInt(rt.lexeme);

                        // Check if 'b' is a power of 2 (for * or / optimizations)
                        if (b > 0 && (b & (b - 1)) == 0) { // e.g., 2, 4, 8, 16...
                            int shift = Integer.numberOfTrailingZeros(b); // log2(b)
                            switch (op) {
                                case "*":
                                    currExpr = wrapLiteral(
                                            new Token(TokenType.INT_VAL,
                                                    Integer.toString(a << shift),
                                                    lt.line, lt.column));
                                    return;
                                case "/":
                                    currExpr = wrapLiteral(
                                            new Token(TokenType.INT_VAL,
                                                    Integer.toString(a >> shift),
                                                    lt.line, lt.column));
                                    return;
                            }
                        }
                    }

                    // Handle equality and relational operators first (they always return bool)
                    if (op.equals("==") || op.equals("!=") || op.equals("<") ||
                            op.equals("<=") || op.equals(">") || op.equals(">=")) {

                        boolean result = false;

                        // Integer comparisons
                        if (lt.tokenType == TokenType.INT_VAL && rt.tokenType == TokenType.INT_VAL) {
                            int a = Integer.parseInt(lt.lexeme);
                            int b = Integer.parseInt(rt.lexeme);
                            switch (op) {
                                case "==":
                                    result = a == b;
                                    break;
                                case "!=":
                                    result = a != b;
                                    break;
                                case "<":
                                    result = a < b;
                                    break;
                                case "<=":
                                    result = a <= b;
                                    break;
                                case ">":
                                    result = a > b;
                                    break;
                                case ">=":
                                    result = a >= b;
                                    break;
                            }
                        }
                        // Double comparisons
                        else if (lt.tokenType == TokenType.DOUBLE_VAL && rt.tokenType == TokenType.DOUBLE_VAL) {
                            double a = Double.parseDouble(lt.lexeme);
                            double b = Double.parseDouble(rt.lexeme);
                            switch (op) {
                                case "==":
                                    result = a == b;
                                    break;
                                case "!=":
                                    result = a != b;
                                    break;
                                case "<":
                                    result = a < b;
                                    break;
                                case "<=":
                                    result = a <= b;
                                    break;
                                case ">":
                                    result = a > b;
                                    break;
                                case ">=":
                                    result = a >= b;
                                    break;
                            }
                        }
                        // Boolean comparisons
                        else if (lt.tokenType == TokenType.BOOL_VAL && rt.tokenType == TokenType.BOOL_VAL) {
                            boolean a = Boolean.parseBoolean(lt.lexeme);
                            boolean b = Boolean.parseBoolean(rt.lexeme);
                            switch (op) {
                                case "==":
                                    result = a == b;
                                    break;
                                case "!=":
                                    result = a != b;
                                    break;
                            }
                        }
                        // Null comparisons
                        else if (lt.tokenType == TokenType.NULL_VAL || rt.tokenType == TokenType.NULL_VAL) {
                            result = lt.tokenType == rt.tokenType;
                            if (op.equals("!="))
                                result = !result;
                        }

                        currExpr = wrapLiteral(
                                new Token(TokenType.BOOL_VAL, Boolean.toString(result), lt.line, lt.column));
                        return;
                    }

                    // Handle arithmetic operations (only for int/double)
                    if (lt.tokenType == TokenType.INT_VAL && rt.tokenType == TokenType.INT_VAL) {
                        int a = Integer.parseInt(lt.lexeme);
                        int b = Integer.parseInt(rt.lexeme);
                        int result = 0;
                        switch (op) {
                            case "+":
                                result = a + b;
                                break;
                            case "-":
                                result = a - b;
                                break;
                            case "*":
                                result = a * b;
                                break;
                            case "/":
                                if (b != 0)
                                    result = a / b;
                                else
                                    throw new ArithmeticException();
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        currExpr = wrapLiteral(
                                new Token(TokenType.INT_VAL, Integer.toString(result), lt.line, lt.column));
                        return;
                    }

                    if (lt.tokenType == TokenType.DOUBLE_VAL && rt.tokenType == TokenType.DOUBLE_VAL) {
                        double a = Double.parseDouble(lt.lexeme);
                        double b = Double.parseDouble(rt.lexeme);
                        double result = 0.0;
                        switch (op) {
                            case "+":
                                result = a + b;
                                break;
                            case "-":
                                result = a - b;
                                break;
                            case "*":
                                result = a * b;
                                break;
                            case "/":
                                if (b != 0.0)
                                    result = a / b;
                                else
                                    throw new ArithmeticException();
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        currExpr = wrapLiteral(
                                new Token(TokenType.DOUBLE_VAL, Double.toString(result), lt.line, lt.column));
                        return;
                    }

                    // Handle boolean operations
                    if (lt.tokenType == TokenType.BOOL_VAL && rt.tokenType == TokenType.BOOL_VAL) {
                        boolean a = Boolean.parseBoolean(lt.lexeme);
                        boolean b = Boolean.parseBoolean(rt.lexeme);
                        boolean result = false;
                        switch (op) {
                            case "and":
                                result = a && b;
                                break;
                            case "or":
                                result = a || b;
                                break;
                            default:
                                throw new UnsupportedOperationException();
                        }
                        currExpr = wrapLiteral(
                                new Token(TokenType.BOOL_VAL, Boolean.toString(result), lt.line, lt.column));
                        return;
                    }

                    // Handle string concatenation
                    if (lt.tokenType == TokenType.STRING_VAL && rt.tokenType == TokenType.STRING_VAL
                            && op.equals("+")) {
                        String a = lt.lexeme.substring(0, lt.lexeme.length());
                        String b = rt.lexeme.substring(0, rt.lexeme.length());
                        String result = a + b;
                        currExpr = wrapLiteral(
                                new Token(TokenType.STRING_VAL, result, lt.line, lt.column));
                        return;
                    }

                } catch (Exception e) {
                    // silently ignore if folding fails
                }
            }
        }

        currExpr = node;
    }

    // --------------------------------------------------------------------
    // RValues (no changes for optimization)
    // --------------------------------------------------------------------

    public void visit(CallRValue node) {
    }

    public void visit(SimpleRValue node) {
    }

    public void visit(NewStructRValue node) {
    }

    public void visit(NewArrayRValue node) {
    }

    public void visit(VarRValue node) {
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    private Expr wrapLiteral(Token token) {
        SimpleRValue simp = new SimpleRValue();
        simp.literal = token;
        BasicExpr expr = new BasicExpr();
        expr.rvalue = simp;
        return expr;
    }
}