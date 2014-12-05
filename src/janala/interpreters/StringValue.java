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

/*
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 */

package janala.interpreters;

import janala.config.Config;
import janala.solvers.History;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 6/19/12
 * Time: 12:12 PM
 */
public class StringValue extends ObjectValue {
    private String string;
    private SymbolicStringExpression symbolic;

    public StringValue(String string, int address) {
        super(100,address);
        this.string = string;
    }

    public StringValue(String string, SymbolicStringExpression symbolic) {
        super(100, -1);
        this.string = string;
        this.symbolic = symbolic;
    }

    @Override
    public String getConcrete() {
        return string;
    }

    public SymbolicStringExpression getSymbolic() {
        return symbolic;
    }

    private String escapeRE(String str) {
        return str.replaceAll("([^a-zA-z0-9])", "\\\\$1");
    }

    @Override
    public Value invokeMethod(String name, Value[] args, History history) {
        if (name.equals("equals") && args.length == 1) {
            if (args[0] instanceof StringValue) {
                StringValue other = (StringValue)args[0];
                boolean result = string.equals(other.string);
                if (symbolic !=null && other.symbolic !=null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.EQ,symbolic,other.symbolic));
                } else if (symbolic != null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.EQ,symbolic,other.string));
                } else if (other.symbolic!=null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.EQ,string,other.symbolic));
                } else {
                    return new IntValue(result?1:0);
                }
            }
        } else if (name.equals("startsWith") && args.length == 1) {
            if (args[0] instanceof StringValue) {
                StringValue other = (StringValue)args[0];
                boolean result = string.startsWith(other.string);
                if (symbolic != null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.IN,symbolic,escapeRE(other.string)+".*"));
                } else {
                    return new IntValue(result?1:0);
                }
            }
        } else if (name.equals("startsWith") && args.length == 2) {
            if (args[0] instanceof StringValue) {
                StringValue other = (StringValue)args[0];
                IntValue offset = (IntValue)args[1];
                boolean result = string.startsWith(other.string, offset.concrete);
                if (symbolic != null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(
                            SymbolicStringPredicate.COMPARISON_OPS.IN,
                            symbolic,
                            ".{"+offset.concrete+"}"+escapeRE(other.string)+".*"));
                } else {
                    return new IntValue(result?1:0);
                }
            }
        } else if (name.equals("endsWith") && args.length == 1) {
            StringValue other = (StringValue)args[0];
            boolean result = string.endsWith(other.string);
            if (symbolic != null) {
                return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.IN,symbolic,".*"+escapeRE(other.string)));
            } else {
                return new IntValue(result?1:0);
            }
        } else if (name.equals("contains") && args.length == 1) {
            StringValue other = (StringValue)args[0];
            boolean result = string.contains(other.string);
            if (symbolic != null) {
                return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.IN,symbolic,".*"+escapeRE(other.string)+".*"));
            } else {
                return new IntValue(result?1:0);
            }
        } else if (name.equals("concat") && args.length == 1) {
            if (args[0] instanceof StringValue) {
                StringValue other = (StringValue)args[0];
                String result = string.concat(other.string);
                if (symbolic !=null && other.symbolic !=null) {
                    return new StringValue(result, symbolic.concat(other.symbolic));
                } else if (symbolic != null) {
                    return new StringValue(result, symbolic.concatStr(other.string));
                } else if (other.symbolic!=null) {
                    return new StringValue(result,other.symbolic.concatToStr(string));
                } else {
                    return new StringValue(result, null);
                }
            }
        } else if (name.equals("length") && args.length == 0) {
            int result = string.length();
            if (symbolic != null) {
                return symbolic.getField("length");
            } else {
                return new IntValue(result);
            }
        } else if (name.equals("matches") && args.length == 1) {
            if (args[0] instanceof StringValue) {
                StringValue other = (StringValue)args[0];
                boolean result = string.matches(other.string);
                if (symbolic != null) {
                    return new IntValue(result?1:0,new SymbolicStringPredicate(SymbolicStringPredicate.COMPARISON_OPS.IN,symbolic,other.string));
                } else {
                    return new IntValue(result?1:0);
                }
            }
        }
        return super.invokeMethod(name, args, history);
    }

    public int MAKE_SYMBOLIC(History history) {
        IntValue length = new IntValue(string.length());
        int ret = symbol;
        symbol = symbol + inc;
        symbolic = new SymbolicStringExpression(ret, length);
        length.MAKE_SYMBOLIC(history);

        Constraint results = length.symbolic.setop(SymbolicInt.COMPARISON_OPS.GE);
        boolean resultc = length.concrete >= 0;
        history.checkAndSetBranch(resultc, results, 0);
        if (resultc) {
            history.setLastBranchDone();
        }

        results = length.ISUB(new IntValue(Config.instance.maxStringLength)).symbolic.setop(SymbolicInt.COMPARISON_OPS.LE);
        resultc = length.concrete <= Config.instance.maxStringLength;
        history.checkAndSetBranch(resultc, results, 0);
        if (resultc) {
            history.setLastBranchDone();
        }

        //System.out.println("String symbol x"+ret+" = \""+string+"\"");
        return ret;
    }
}
