/*
 * Copyright (c) 2012, NTT Multimedia Communications Laboratories, Inc. and Koushik Sen
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package janala.solvers;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 */

import gnu.trove.iterator.TIntLongIterator;
import janala.config.Config;
import janala.interpreters.*;
import janala.interpreters.StringValue;
import janala.utils.FileUtil;
import janala.utils.MyLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CVC4Solver implements Solver {
    LinkedList<InputElement> inputs;
    ArrayList<Constraint> constraints;
    int pathConstraintIndex;
    private final static Logger logger = MyLogger.getLogger(CVC4Solver.class.getName());
    private final static Logger tester = MyLogger.getTestLogger(Config.mainClass+"."+Config.iteration);

    public void setInputs(LinkedList<InputElement> inputs)
    {
        this.inputs = inputs;
    }

    public void setPathConstraint(ArrayList<Constraint> pathConstraint) {
        this.constraints = pathConstraint;
    }

    public void setPathConstraintIndex(int pathConstraintIndex) {
        this.pathConstraintIndex = pathConstraintIndex;
    }

    private void print(Constraint con, PrintStream out, LinkedHashSet<String> freeVars, CVC3Solver.CONSTRAINT_TYPE type, TreeMap<String,Long> soln) {
        if (con instanceof SymbolicInt) {
            SymbolicInt c = (SymbolicInt)con;
            boolean first2 = true;
            for ( TIntLongIterator it = c.linear.iterator(); it.hasNext(); ) {
                it.advance();

                int integer = it.key();
                freeVars.add("x"+integer);
                long l = it.value();
                if (first2) {
                    first2 = false;
                } else {
                    out.print(" + ");
                }
                out.print('x');
                out.print(integer);
                out.print("*(");
                out.print(l);
                out.print(')');

            }
            if (c.constant != 0) {
                out.print("+(");
                out.print(c.constant);
                out.print(')');
            }
            if (c.op == SymbolicInt.COMPARISON_OPS.EQ) {
                out.print(" = ");
            } else if (c.op == SymbolicInt.COMPARISON_OPS.NE) {
                out.print(" /= ");
            } else if (c.op == SymbolicInt.COMPARISON_OPS.LE) {
                out.print(" <= ");
            } else if (c.op == SymbolicInt.COMPARISON_OPS.LT) {
                out.print(" < ");
            } else if (c.op == SymbolicInt.COMPARISON_OPS.GE) {
                out.print(" >= ");
            } else if (c.op == SymbolicInt.COMPARISON_OPS.GT) {
                out.print(" > ");
            }
            out.print("0");
        } else if (con instanceof SymbolicIntCompareConstraint) {
            SymbolicIntCompareConstraint c = (SymbolicIntCompareConstraint)con;
            out.print('(');
            if (c.left.sym!=null) freeVars.add(c.left.sym);
            out.print(c.left);
            out.print(')');
            out.print('-');
            out.print('(');
            if (c.right.sym!=null) freeVars.add(c.right.sym);
            out.print(c.right);
            out.print(')');
            if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.EQ) {
                out.print(" = ");
            } else if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.NE) {
                out.print(" /= ");
            } else if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.LE) {
                out.print(" <= ");
            } else if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.LT) {
                out.print(" < ");
            } else if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.GE) {
                out.print(" >= ");
            } else if (c.op == SymbolicIntCompareConstraint.COMPARISON_OPS.GT) {
                out.print(" > ");
            }
            out.print("0");
        } else if (con instanceof SymbolicOrConstraint) {
            SymbolicOrConstraint or = (SymbolicOrConstraint)con;

            boolean first2 = true;
            for(Constraint c:or.constraints) {
                if (first2) {
                    first2 = false;
                } else {
                    out.print(" OR ");
                }
                out.print("(");
                print(c,out, freeVars, type, soln);
                out.print(")");
            }
            if (or.constraints.isEmpty()) {
                out.print(" TRUE ");
            }
        } else if (con instanceof SymbolicAndConstraint) {
            SymbolicAndConstraint and = (SymbolicAndConstraint)con;

            boolean first2 = true;
            for(Constraint c:and.constraints) {
                if (first2) {
                    first2 = false;
                } else {
                    out.print(" AND ");
                }
                out.print("(");
                print(c,out, freeVars, type, soln);
                out.print(")");
            }
            if (and.constraints.isEmpty()) {
                out.print(" FALSE ");
            }
        } else if (con instanceof SymbolicNotConstraint) {
            SymbolicNotConstraint not = (SymbolicNotConstraint)con;
            out.print(" NOT ");
            out.print("(");
            print(not.constraint,out, freeVars, type, soln);
            out.print(")");
        } else if (con instanceof SymbolicTrueConstraint) {
            out.print(" TRUE ");
        } else if (con instanceof SymbolicFalseConstraint) {
            out.print(" FALSE ");
        } else if (con instanceof SymbolicStringPredicate) {
            SymbolicStringPredicate str = (SymbolicStringPredicate) con;
            Constraint intConstraint = str.getFormula(freeVars, type, soln);
            print(intConstraint, out, freeVars, type, soln);
        } else {
            throw new RuntimeException("Unimplemented constraint type "+con);
        }
    }


    public void visitSymbolicInt(SymbolicInt c) {

    }

    public void visitSymbolicOr(SymbolicOrConstraint c) {

    }

    public void visitSymbolicStringPredicate(SymbolicStringPredicate c) {

    }

    public void visitSymbolicAnd(SymbolicAndConstraint c) {

    }

    public void visitSymbolicNot(SymbolicNotConstraint c) {

    }

    public void visitSymbolicTrue(SymbolicTrueConstraint c) {

    }

    public void visitSymbolicFalse(SymbolicFalseConstraint c) {

    }

    public void visitSymbolicIntCompare(SymbolicIntCompareConstraint c) {

    }

    public enum RESULT_TYPE {TRUE, FALSE, UNKNOWN};

    private boolean quickUnsatCheck(CVC3Solver.CONSTRAINT_TYPE type) {
        if (type == CVC3Solver.CONSTRAINT_TYPE.INT) {
            Constraint last = constraints.get(pathConstraintIndex);
            if (last instanceof SymbolicStringPredicate) {
                for (int i=0; i<pathConstraintIndex;i++) {
                    Constraint tmp = constraints.get(i);
                    if (tmp.equals(last)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private RESULT_TYPE writeFormula(String extra, CVC3Solver.CONSTRAINT_TYPE type, TreeMap<String,Long> soln) {
        try {
            boolean allTrue = true;
            Constraint tmp;
            PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(Config.instance.formulaFile+".tmp")));
            if (quickUnsatCheck(type)) {
                return RESULT_TYPE.FALSE;
            }

            LinkedHashSet<String> freeVars = new LinkedHashSet<String>();
            for (int i=0; i<pathConstraintIndex;i++) {
                out.print("ASSERT ");
                //System.out.println("Constraint "+i+":" + constraints.get(i));
                tmp = constraints.get(i).substitute(soln);
                if (tmp != SymbolicTrueConstraint.instance) {
                    allTrue = false;
                }
                print(tmp, out, freeVars, type, soln);
                out.println(";");
            }
            if (extra != null) {
                out.print("ASSERT ");
                out.print(extra);
                out.println(";");
            }

            out.print("CHECKSAT ");



            //System.out.println("Constraint "+pathConstraintIndex+": !" + constraints.get(pathConstraintIndex));
            tmp = constraints.get(pathConstraintIndex).not().substitute(soln);
            if (tmp != SymbolicTrueConstraint.instance) {
                allTrue = false;
            }
            print(tmp,out, freeVars, type, soln);
            out.println(";");
            out.println("COUNTERMODEL;");
            out.close();

            CVC3Solver.concatFile(freeVars, Config.instance.formulaFile+".tmp", Config.instance.formulaFile, true);
            return allTrue?RESULT_TYPE.TRUE:RESULT_TYPE.UNKNOWN;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.log(Level.SEVERE,"{0}",ioe);
            Runtime.getRuntime().halt(1);
            return RESULT_TYPE.UNKNOWN;
        }
    }

    private void writeInputs(TreeMap<String, Long> soln) {
        try {
            FileUtil.moveFile(Config.instance.inputs, Config.instance.inputs + ".bak");
            PrintStream out = new PrintStream(
                    new BufferedOutputStream(
                            new FileOutputStream(Config.instance.inputs)));

            for (InputElement ielem: inputs) {
                Integer sym = ielem.symbol;
                Value val = ielem.value;
                if (sym.intValue() == Config.instance.scopeBeginSymbol) {
                    out.println(Config.instance.scopeBeginMarker);
                } else if (sym.intValue() == Config.instance.scopeEndSymbol) {
                    out.println(Config.instance.scopeEndMarker);
                } else {
                    //System.out.println("sym "+sym);
                    Long l = soln.get("x"+sym);
                    if (l != null) {
                        out.println(l);
                        //System.out.println("l = " + l);
                    } else {
                        if (val instanceof StringValue) {
                            StringValue sval = (StringValue)val;
                            String old = sval.getConcrete();
                            IntValue tmp = sval.getSymbolic().getField("length");
                            int len = (int)(long)tmp.substituteInLinear(soln);
                            StringBuilder ret = new StringBuilder();
                            for (int i=0 ; i< len; i++) {
                                Long v = soln.get("x"+sym+"__"+i);

                                char c;
                                if (v != null) {
                                    c = (char)(long)v;
                                    //System.out.println("--1");
                                } else if (i < old.length()){
                                    //System.out.println("--2"+old+" "+old.length());
                                    c = old.charAt(i);
                                } else {
                                    //System.out.println("--3");
                                    c = 'a';
                                }
                                ret.append(c);
                                //System.out.println("~~~~~~~~~~~~~~~\"" + ret + "\"");
                            }
                            out.println(ret);
                        } else {
                            out.println(val.getConcrete());
                        }

                    }
                }
            }

            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.log(Level.SEVERE,"{0}",ioe);
            Runtime.getRuntime().halt(1);
        }
    }

    private String processInputs(BufferedReader br, TreeMap<String, Long> soln) {
        String line = null;
        String negatedSolution = null;

        try {
            if (Config.instance.printFormulaAndSolutions) {
                System.out.println("-----------Solution-------------");
            }

            line = br.readLine();
            if (Config.instance.printFormulaAndSolutions) {
                System.out.println(line);
            }
            if (!line.startsWith("sat")) {
                if (!line.contains("unsat")) {
                    logger.log(Level.SEVERE,line);
                    logger.log(Level.SEVERE, "Call to CVC4 failed (concolic.cvc4 = "
                            + Config.instance.cvc4Command + ")");
                    Runtime.getRuntime().halt(1);
                }
                logger.log(Level.INFO,"-- Infeasible");
                while ((line = br.readLine()) != null) {
                    if (Config.instance.printFormulaAndSolutions) {
                        System.out.println(line);
                    }
                }
                br.close();
                return null;
            } else {
                while ((line = br.readLine()) != null) {
                    if (Config.instance.printFormulaAndSolutions) {
                        System.out.println(line);
                    }

                    String[] tokens = line.split(" ");
                    if (tokens.length == 5) {
                        String tmp = tokens[4].trim();
                        tmp = tmp.substring(0, tmp.indexOf(";"));

                        if (negatedSolution != null) {
                            negatedSolution += " AND ("+tokens[0] +" = "+tmp+" )";
                        } else {
                            negatedSolution = "("+tokens[0] +" = "+tmp+" )";
                        }

                        long val = Long.parseLong(tmp);
                        soln.put(tokens[0], val);
                    }
                }
                br.close();

                negatedSolution = "(NOT ("+negatedSolution+"))";
                return negatedSolution;
            }
        }  catch (IOException ioe) {
            ioe.printStackTrace();
            logger.log(Level.SEVERE,"{0}",ioe);
            Runtime.getRuntime().halt(1);
            return null;
        }
    }

    public boolean solve() {
        int count = 0, MAX_COUNT = 100;
        String extra = null, negatedSolution, negatedSolution2;
        while(count < MAX_COUNT) {
            TreeMap<String, Long> soln = new TreeMap<String, Long>();
            negatedSolution = solve(extra, CVC3Solver.CONSTRAINT_TYPE.INT, soln);
            if (negatedSolution != null) {
                negatedSolution2 = solve(null, CVC3Solver.CONSTRAINT_TYPE.STR, soln);
                if (negatedSolution2 != null) {
                    writeInputs(soln);
                    tester.log(Level.INFO,"Feasible = true at "+pathConstraintIndex);
                    return true;
                } else {
                    if (extra != null) {
                        extra = extra + " AND " + negatedSolution;
                    } else {
                        extra = negatedSolution;
                    }
                }
            } else {
                tester.log(Level.INFO,"Feasible = false at "+pathConstraintIndex);
                return false;
            }
            count++;
        }
        tester.log(Level.INFO,"Feasible = false at "+pathConstraintIndex);
        return false;
    }


    public String solve(String extra, CVC3Solver.CONSTRAINT_TYPE type, TreeMap<String,Long> soln) {
        try {
            RESULT_TYPE res;
            String negatedSolution;
            if ((res = writeFormula(extra, type, soln)) == RESULT_TYPE.TRUE) {
                return "";
            } else if (res == RESULT_TYPE.FALSE) {
                return null;
            }

            ProcessBuilder builder = new ProcessBuilder(new String[]{Config.instance.cvc4Command,"--lang", "cvc4", Config.instance.formulaFile});
            builder.redirectErrorStream(true);
            Process process = builder.start();

            (new StreamGobbler(process.getErrorStream(),"ERROR")).start();

            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            negatedSolution = processInputs(br, soln);
            process.waitFor();
            return negatedSolution;

        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.log(Level.SEVERE,"{0}",ioe);
            Runtime.getRuntime().halt(1);
            return null;
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            logger.log(Level.SEVERE,"{0}",ie);
            Runtime.getRuntime().halt(1);
            return null;
        }
    }
}

