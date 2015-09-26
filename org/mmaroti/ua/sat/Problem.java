/**
 *	Copyright (C) Miklos Maroti, 2015
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your 
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.mmaroti.ua.sat;

import java.util.*;

public abstract class Problem {
	public final Map<String, int[]> shapes;

	Problem(Map<String, int[]> shapes) {
		this.shapes = shapes;
	}

	public abstract <BOOL> BOOL compute(BoolAlg<BOOL> alg,
			Map<String, Tensor<BOOL>> tensors);

	public boolean check(Map<String, Tensor<Boolean>> tensors) {
		return compute(BoolAlg.BOOLEAN, tensors);
	}

	public Map<String, Tensor<Boolean>> solveOne(SatSolver solver) {
		solver.clear();

		Map<String, Tensor<Integer>> tensors = new TreeMap<String, Tensor<Integer>>();
		for (String key : shapes.keySet())
			tensors.put(key, solver.tensor(shapes.get(key)));

		solver.ensure(compute(solver, tensors));
		final boolean[] sol = solver.solve();
		if (sol == null)
			return null;

		Func1<Boolean, Integer> LOOKUP = new Func1<Boolean, Integer>() {
			@Override
			public Boolean call(Integer elem) {
				return sol[elem];
			}
		};

		Map<String, Tensor<Boolean>> solution = new TreeMap<String, Tensor<Boolean>>();
		for (String key : tensors.keySet())
			solution.put(key, Tensor.map(LOOKUP, tensors.get(key)));

		return solution;
	}

	public List<Map<String, Tensor<Boolean>>> solveAll(SatSolver solver) {
		solver.clear();

		Map<String, Tensor<Integer>> tensors = new TreeMap<String, Tensor<Integer>>();
		int vars = 0;

		for (String key : shapes.keySet()) {
			int[] shape = shapes.get(key);
			vars += Tensor.getSize(shape);
			tensors.put(key, solver.tensor(shape));
		}

		solver.ensure(compute(solver, tensors));

		List<Map<String, Tensor<Boolean>>> solutions = new ArrayList<Map<String, Tensor<Boolean>>>();
		for (;;) {
			final boolean[] sol = solver.solve();
			if (sol == null)
				return solutions;

			final int[] exclude = new int[vars];

			Func1<Boolean, Integer> LOOKUP = new Func1<Boolean, Integer>() {
				int index = 0;

				@Override
				public Boolean call(Integer elem) {
					exclude[index++] = sol[elem] ? -elem : elem;
					return sol[elem];
				}
			};

			Map<String, Tensor<Boolean>> solution = new TreeMap<String, Tensor<Boolean>>();
			for (String key : tensors.keySet())
				solution.put(key, Tensor.map(LOOKUP, tensors.get(key)));

			solutions.add(solution);
			solver.ensure(exclude);
		}
	}

	public static void main(String[] args) {
		Map<String, int[]> shapes = new HashMap<String, int[]>();
		shapes.put("f", new int[] { 2, 2 });

		Problem problem = new Problem(shapes) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {
				// BOOL a = Tensor.collapse(tensors.get("f"), 2, alg.SUM).get();
				BOOL b = Tensor.collapse(tensors.get("f"), 2, alg.ONE).get();
				return b;
			}
		};

		List<Map<String, Tensor<Boolean>>> solutions = problem
				.solveAll(new MiniSat());
		for (Map<String, Tensor<Boolean>> solution : solutions)
			Tensor.print(solution, System.out);
	}
}
