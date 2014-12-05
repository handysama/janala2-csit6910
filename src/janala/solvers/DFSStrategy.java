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

package janala.solvers;

import java.util.ArrayList;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 7/9/12
 * Time: 7:21 PM
 */
public class DFSStrategy extends Strategy {
    @Override
    public int solve(ArrayList<Element> history, int historySize, History solver) {
        int j, to = -1, ret;

        for (j = 0; j < historySize; j++) {
            Element tmp = history.get(j);
            BranchElement current;
            if (tmp instanceof BranchElement) {
                current = (BranchElement)tmp;
                if (current.isForceTruth && !current.branch) {
                    if ((ret = dfs(history,j,to,solver)) != -1) {
                        return ret;
                    }
                    to = j;
                } else if (current.isForceTruth) {
                    to = j;
                }
            }
        }

        if (j >= historySize) {
            j = historySize-1;
        }

        return dfs(history, j, to, solver);
    }

    private int dfs(ArrayList<Element> history, int from, int to, History solver) {
        for (int i=from; i > to; i--) {
            Element tmp = history.get(i);
            if (tmp instanceof BranchElement) {
                BranchElement current = (BranchElement) tmp;
                if (!current.done && current.pathConstraintIndex != -1) {
                    if (solver.solveAt(current.pathConstraintIndex)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
