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

package org.mmaroti.sat.math;

import java.text.*;
import java.util.*;

import org.mmaroti.sat.core.*;
import org.mmaroti.sat.solvers.*;

public class MonoidalInt {
	public static Tensor<Boolean> decodeMonoid(final int size, String monoid) {
		final List<Integer> elems = new ArrayList<Integer>();
		for (int i = 0; i < monoid.length(); i++) {
			char c = monoid.charAt(i);
			if (c == ' ')
				continue;

			if (c < '0' || c > '9')
				throw new IllegalArgumentException();

			elems.add(c - '0');
		}

		assert elems.size() % size == 0;
		return Tensor.generate(new int[] { size, size, elems.size() / size },
				new Func1<Boolean, int[]>() {
					public Boolean call(int[] elem) {
						int i = elem[1] + elem[2] * size;
						return elems.get(i) == elem[0];
					}
				});
	}

	public static <BOOL> BOOL isFunction(BoolAlg<BOOL> alg, Tensor<BOOL> func) {
		func = Tensor.fold(alg.ONE, 1, func);
		func = Tensor.fold(alg.ALL, func.getOrder(), func);
		return func.get();
	}

	public static <BOOL> BOOL isReflexiveRel(BoolAlg<BOOL> alg, Tensor<BOOL> rel) {
		assert rel.getOrder() == 2 && rel.getDim(0) == rel.getDim(1);

		Tensor<BOOL> t = Tensor.reshape(rel, new int[] { rel.getDim(0) },
				new int[] { 0, 0 });
		t = Tensor.fold(alg.ALL, 1, t);

		return t.get();
	}

	public static <BOOL> BOOL isSymmetricRel(BoolAlg<BOOL> alg, Tensor<BOOL> rel) {
		assert rel.getOrder() == 2 && rel.getDim(0) == rel.getDim(1);

		Tensor<BOOL> t = Tensor.reduce(alg.ALL, "", alg.EQU, rel.named("xy"),
				rel.named("yx"));

		return t.get();
	}

	public static <BOOL> BOOL isTransitiveRel(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel) {
		assert rel.getOrder() == 2 && rel.getDim(0) == rel.getDim(1);

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "xz", alg.AND, rel.named("xy"),
				rel.named("yz"));
		t = Tensor.map2(alg.LEQ, t, rel);
		t = Tensor.fold(alg.ALL, 2, t);

		return t.get();
	}

	public static <BOOL> BOOL isMajorityOp(BoolAlg<BOOL> alg, Tensor<BOOL> op) {
		assert op.getOrder() == 4;

		BOOL t = Tensor.reduce(alg.ALL, "", alg.ID, op.named("xxxy")).get();
		t = alg.and(t, Tensor.reduce(alg.ALL, "", alg.ID, op.named("xxyx"))
				.get());
		t = alg.and(t, Tensor.reduce(alg.ALL, "", alg.ID, op.named("xyxx"))
				.get());

		return t;
	}

	public static <BOOL> BOOL isMaltsevOp(BoolAlg<BOOL> alg, Tensor<BOOL> op) {
		assert op.getOrder() == 4;

		BOOL t = Tensor.reduce(alg.ALL, "", alg.ID, op.named("yxxy")).get();
		t = alg.and(t, Tensor.reduce(alg.ALL, "", alg.ID, op.named("yyxx"))
				.get());

		return t;
	}

	public static <BOOL> BOOL isStabilizer2(BoolAlg<BOOL> alg,
			Tensor<BOOL> func, Tensor<BOOL> monoid) {
		assert func.getOrder() == 3 && monoid.getOrder() == 3;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "xytp", alg.AND,
				func.named("xyz"), monoid.named("ztp"));
		t = Tensor.reduce(alg.ANY, "xtpq", alg.AND, t.named("xytp"),
				monoid.named("ytq"));
		t = Tensor.reduce(alg.ALL, "rpq", alg.EQU, t.named("xtpq"),
				monoid.named("xtr"));
		t = Tensor.fold(alg.ALL, 2, Tensor.fold(alg.ONE, 1, t));

		return t.get();
	}

	public static <BOOL> BOOL isStabilizer3(BoolAlg<BOOL> alg,
			Tensor<BOOL> func, Tensor<BOOL> monoid) {
		assert func.getOrder() == 4 && monoid.getOrder() == 3;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "atpcd", alg.AND,
				func.named("abcd"), monoid.named("btp"));
		t = Tensor.reduce(alg.ANY, "atpqd", alg.AND, t.named("atpcd"),
				monoid.named("ctq"));
		t = Tensor.reduce(alg.ANY, "atpqr", alg.AND, t.named("atpqd"),
				monoid.named("dtr"));
		t = Tensor.reduce(alg.ALL, "spqr", alg.EQU, t.named("atpqr"),
				monoid.named("ats"));
		t = Tensor.fold(alg.ALL, 3, Tensor.fold(alg.ONE, 1, t));

		return t.get();
	}

	public static <BOOL> BOOL isEssentialRel2(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel) {
		assert rel.getOrder() == 2;

		Tensor<BOOL> t1;
		t1 = Tensor.fold(alg.ANY, 1, rel);
		t1 = Tensor.reshape(t1, rel.getShape(), new int[] { 1 });

		Tensor<BOOL> t2;
		t2 = Tensor.reshape(rel, rel.getShape(), new int[] { 1, 0 });
		t2 = Tensor.fold(alg.ANY, 1, t2);
		t2 = Tensor.reshape(t2, rel.getShape(), new int[] { 0 });

		Tensor<BOOL> t;
		t = Tensor.map2(alg.AND, t1, t2);
		t = Tensor.map2(alg.AND, t, Tensor.map(alg.NOT, rel));
		t = Tensor.fold(alg.ANY, 2, t);

		return t.get();
	}

	public static <BOOL> BOOL isEssentialRel3(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel) {
		assert rel.getOrder() == 3;

		Tensor<BOOL> t0;
		t0 = Tensor.fold(alg.ANY, 1, rel);
		t0 = Tensor.reshape(t0, rel.getShape(), new int[] { 1, 2 });

		Tensor<BOOL> t1;
		t1 = Tensor.reshape(rel, rel.getShape(), new int[] { 1, 0, 2 });
		t1 = Tensor.fold(alg.ANY, 1, t1);
		t1 = Tensor.reshape(t1, rel.getShape(), new int[] { 0, 2 });

		Tensor<BOOL> t2;
		t2 = Tensor.reshape(rel, rel.getShape(), new int[] { 2, 0, 1 });
		t2 = Tensor.fold(alg.ANY, 1, t2);
		t2 = Tensor.reshape(t2, rel.getShape(), new int[] { 0, 1 });

		Tensor<BOOL> t;
		t = Tensor.map2(alg.AND, t0, t1);
		t = Tensor.map2(alg.AND, t, t2);
		t = Tensor.map2(alg.AND, t, Tensor.map(alg.NOT, rel));
		t = Tensor.fold(alg.ANY, 3, t);

		return t.get();
	}

	public static <BOOL> BOOL isEssentialOp2(BoolAlg<BOOL> alg,
			Tensor<BOOL> func) {
		assert func.getOrder() == 3;

		Tensor<BOOL> t;
		t = Tensor.reshape(func, func.getShape(), new int[] { 1, 0, 2 });
		t = Tensor.fold(alg.ALL, 2, Tensor.fold(alg.EQS, 1, t));
		BOOL proj = t.get();

		t = Tensor.reshape(func, func.getShape(), new int[] { 1, 2, 0 });
		t = Tensor.fold(alg.ALL, 2, Tensor.fold(alg.EQS, 1, t));
		proj = alg.or(proj, t.get());

		return alg.not(proj);
	}

	public static <BOOL> BOOL isEssentialOp3(BoolAlg<BOOL> alg,
			Tensor<BOOL> func) {
		assert func.getOrder() == 4;

		Tensor<BOOL> t;
		t = Tensor.reshape(func, func.getShape(), new int[] { 1, 0, 2, 3 });
		t = Tensor.fold(alg.ALL, 3, Tensor.fold(alg.EQS, 1, t));
		BOOL proj = t.get();

		t = Tensor.reshape(func, func.getShape(), new int[] { 1, 2, 0, 3 });
		t = Tensor.fold(alg.ALL, 3, Tensor.fold(alg.EQS, 1, t));
		proj = alg.or(proj, t.get());

		t = Tensor.reshape(func, func.getShape(), new int[] { 1, 2, 3, 0 });
		t = Tensor.fold(alg.ALL, 3, Tensor.fold(alg.EQS, 1, t));
		proj = alg.or(proj, t.get());

		return alg.not(proj);
	}

	public static <BOOL> BOOL isCompatibleRel1(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel, Tensor<BOOL> monoid) {
		assert rel.getOrder() == 1 && monoid.getOrder() == 3;

		Tensor<BOOL> t;
		t = Tensor.reduce(alg.ANY, "y", alg.AND, rel.named("x"),
				monoid.named("yxp"));
		t = Tensor.map2(alg.LEQ, t, rel);
		t = Tensor.fold(alg.ALL, 1, t);

		return t.get();
	}

	public static <BOOL> BOOL isCompatibleRel2(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel, Tensor<BOOL> monoid) {
		assert rel.getOrder() == 2 && monoid.getOrder() == 3;

		Tensor<BOOL> t;
		t = Tensor.reduce(alg.ANY, "uyp", alg.AND, rel.named("xy"),
				monoid.named("uxp"));
		t = Tensor.reduce(alg.ANY, "uv", alg.AND, t.named("uyp"),
				monoid.named("vyp"));
		t = Tensor.map2(alg.LEQ, t, rel);
		t = Tensor.fold(alg.ALL, 2, t);

		return t.get();
	}

	public static <BOOL> BOOL isCompatibleRel3(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel, Tensor<BOOL> monoid) {
		assert rel.getOrder() == 3 && monoid.getOrder() == 3;

		Tensor<BOOL> t;
		t = Tensor.reduce(alg.ANY, "uyzp", alg.AND, rel.named("xyz"),
				monoid.named("uxp"));
		t = Tensor.reduce(alg.ANY, "uvzp", alg.AND, t.named("uyzp"),
				monoid.named("vyp"));
		t = Tensor.reduce(alg.ANY, "uvw", alg.AND, t.named("uvzp"),
				monoid.named("wzp"));

		t = Tensor.map2(alg.LEQ, t, rel);
		t = Tensor.fold(alg.ALL, 3, t);

		return t.get();
	}

	public static <SBOOL> Tensor<Boolean> collectUnaryRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				return isCompatibleRel1(alg, rel, monoid);
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectBinaryRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				return isCompatibleRel2(alg, rel, monoid);
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectEssentialBinaryRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL t = isCompatibleRel2(alg, rel, monoid);
				t = alg.and(t, isEssentialRel2(alg, rel));

				return t;
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectQuasiorderRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL t = isCompatibleRel2(alg, rel, monoid);
				t = alg.and(t, isReflexiveRel(alg, rel));
				t = alg.and(t, isTransitiveRel(alg, rel));

				return t;
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectTernaryRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				return isCompatibleRel3(alg, rel, monoid);
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectEssentialTernaryRels(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("rel", new int[] { size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> rel = tensors.get("rel");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL b = isCompatibleRel3(alg, rel, monoid);
				b = alg.and(b, isEssentialRel3(alg, rel));

				return b;
			}
		};

		return prob.solveAll(solver, LIMIT).get("rel");
	}

	public static <SBOOL> Tensor<Boolean> collectBinaryOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer2(alg, func, monoid));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static <SBOOL> Tensor<Boolean> collectEssentialBinaryOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer2(alg, func, monoid));
				res = alg.and(res, isEssentialOp2(alg, func));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static <SBOOL> Tensor<Boolean> collectTernaryOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer3(alg, func, monoid));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static <SBOOL> Tensor<Boolean> collectEssentialTernaryOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer3(alg, func, monoid));
				res = alg.and(res, isEssentialOp3(alg, func));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static <SBOOL> Tensor<Boolean> collectMajorityOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer3(alg, func, monoid));
				res = alg.and(res, isMajorityOp(alg, func));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static <SBOOL> Tensor<Boolean> collectMaltsevOps(
			SatSolver<SBOOL> solver, int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		Problem prob = new Problem("func", new int[] { size, size, size, size }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {

				Tensor<BOOL> func = tensors.get("func");
				Tensor<BOOL> monoid = Tensor.map(alg.LIFT, mon);

				BOOL res = isFunction(alg, func);
				res = alg.and(res, isStabilizer3(alg, func, monoid));
				res = alg.and(res, isMaltsevOp(alg, func));

				return res;
			}
		};

		return prob.solveAll(solver, LIMIT).get("func");
	}

	public static void printBinaryRels(Tensor<Boolean> rels) {
		assert rels.getOrder() == 3;

		for (int p = 0; p < rels.getDim(2); p++) {
			System.out.print("binrel " + p + ":");
			for (int i = 0; i < rels.getDim(0); i++)
				for (int j = 0; j < rels.getDim(1); j++)
					if (rels.getElem(i, j, p))
						System.out.print(" " + i + "" + j);
			System.out.println();
		}
	}

	public static void printTernaryRels(Tensor<Boolean> rels) {
		assert rels.getOrder() == 4;

		for (int p = 0; p < rels.getDim(3); p++) {
			System.out.print("trnrel " + p + ":");
			for (int i = 0; i < rels.getDim(0); i++)
				for (int j = 0; j < rels.getDim(1); j++)
					for (int k = 0; k < rels.getDim(2); k++)
						if (rels.getElem(i, j, k, p))
							System.out.print(" " + i + "" + j + "" + k);
			System.out.println();
		}
	}

	public static void printBinaryOps(Tensor<Boolean> ops) {
		assert ops.getOrder() == 4;

		for (int p = 0; p < ops.getDim(3); p++) {
			System.out.print("binop " + p + ":");
			for (int i = 0; i < ops.getDim(1); i++) {
				System.out.print(" ");
				for (int j = 0; j < ops.getDim(2); j++)
					for (int k = 0; k < ops.getDim(0); k++)
						if (ops.getElem(k, i, j, p))
							System.out.print(k);
			}
			System.out.println();
		}
	}

	public static void printTernaryOps(Tensor<Boolean> ops) {
		assert ops.getOrder() == 5;

		for (int p = 0; p < ops.getDim(4); p++) {
			System.out.print("trnop " + p + ":");
			for (int i = 0; i < ops.getDim(1); i++)
				for (int j = 0; j < ops.getDim(2); j++) {
					System.out.print(" ");
					for (int k = 0; k < ops.getDim(3); k++)
						for (int l = 0; l < ops.getDim(0); l++)
							if (ops.getElem(l, i, j, k, p))
								System.out.print(l);
				}
			System.out.println();
		}
	}

	public static <BOOL> Tensor<BOOL> getCompatibility22(BoolAlg<BOOL> alg,
			Tensor<BOOL> rels, Tensor<BOOL> ops) {
		assert rels.getOrder() == 3 && ops.getOrder() == 4;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "bcxrf", alg.AND,
				rels.named("abr"), ops.named("xacf"));
		t = Tensor.reduce(alg.ANY, "cdxyrf", alg.AND, t.named("bcxrf"),
				ops.named("ybdf"));
		t = Tensor.reduce(alg.ANY, "xyrf", alg.AND, t.named("cdxyrf"),
				rels.named("cdr"));
		t = Tensor.reduce(alg.ALL, "rf", alg.LEQ, t.named("xyrf"),
				rels.named("xyr"));

		return t;
	}

	public static <BOOL> Tensor<BOOL> getCompatibility32(BoolAlg<BOOL> alg,
			Tensor<BOOL> rels, Tensor<BOOL> ops) {
		assert rels.getOrder() == 4 && ops.getOrder() == 4;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "bcdxrt", alg.AND,
				ops.named("xadt"), rels.named("abcr"));
		t = Tensor.reduce(alg.ANY, "cdexyrt", alg.AND, t.named("bcdxrt"),
				ops.named("ybet"));
		t = Tensor.reduce(alg.ANY, "defxyzrt", alg.AND, t.named("cdexyrt"),
				ops.named("zcft"));
		t = Tensor.reduce(alg.ANY, "xyzrt", alg.AND, t.named("defxyzrt"),
				rels.named("defr"));
		t = Tensor.reduce(alg.ALL, "rt", alg.LEQ, t.named("xyzrt"),
				rels.named("xyzr"));

		return t;
	}

	public static <BOOL> BOOL isCompatible32(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel, Tensor<BOOL> op) {
		assert rel.getOrder() == 3 && op.getOrder() == 3;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "bcdx", alg.AND,
				op.named("xad"), rel.named("abc"));
		t = Tensor.reduce(alg.ANY, "cdexy", alg.AND, t.named("bcdx"),
				op.named("ybe"));
		t = Tensor.reduce(alg.ANY, "defxyz", alg.AND, t.named("cdexy"),
				op.named("zcf"));
		t = Tensor.reduce(alg.ANY, "xyz", alg.AND, t.named("defxyz"),
				rel.named("def"));
		t = Tensor.reduce(alg.ALL, "", alg.LEQ, t.named("xyz"),
				rel.named("xyz"));

		return t.get();
	}

	public static <BOOL> Tensor<BOOL> getCompatibility32Alt(
			final BoolAlg<BOOL> alg, Tensor<BOOL> rels, Tensor<BOOL> ops) {
		assert rels.getOrder() == 4 && ops.getOrder() == 4;

		final List<Tensor<BOOL>> rs = Tensor.unconcat(rels);
		final List<Tensor<BOOL>> os = Tensor.unconcat(ops);

		return Tensor.generate(rs.size(), os.size(),
				new Func2<BOOL, Integer, Integer>() {
					@Override
					public BOOL call(Integer a, Integer b) {
						return isCompatible32(alg, rs.get(a), os.get(b));
					}
				});
	}

	public static <BOOL> BOOL isCompatible33(BoolAlg<BOOL> alg,
			Tensor<BOOL> rel, Tensor<BOOL> op) {
		assert rel.getOrder() == 3 && op.getOrder() == 4;

		Tensor<BOOL> t = Tensor.reduce(alg.ANY, "adbecf", alg.AND,
				rel.named("abc"), rel.named("def"));
		t = Tensor.reduce(alg.ANY, "becfgx", alg.AND, t.named("adbecf"),
				op.named("xadg"));
		t = Tensor.reduce(alg.ANY, "cfghxy", alg.AND, t.named("becfgx"),
				op.named("ybeh"));
		t = Tensor.reduce(alg.ANY, "ghixyz", alg.AND, t.named("cfghxy"),
				op.named("zcfi"));
		t = Tensor.reduce(alg.ANY, "xyz", alg.AND, t.named("ghixyz"),
				rel.named("ghi"));
		t = Tensor.reduce(alg.ALL, "", alg.LEQ, t.named("xyz"),
				rel.named("xyz"));

		return t.get();
	}

	public static <BOOL> Tensor<BOOL> getCompatibility33Alt(
			final BoolAlg<BOOL> alg, Tensor<BOOL> rels, Tensor<BOOL> ops) {
		assert rels.getOrder() == 4 && ops.getOrder() == 5;

		final List<Tensor<BOOL>> rs = Tensor.unconcat(rels);
		final List<Tensor<BOOL>> os = Tensor.unconcat(ops);

		return Tensor.generate(rs.size(), os.size(),
				new Func2<BOOL, Integer, Integer>() {
					@Override
					public BOOL call(Integer a, Integer b) {
						return isCompatible33(alg, rs.get(a), os.get(b));
					}
				});
	}

	public static void printMatrix(String what, Tensor<Boolean> rel) {
		assert rel.getOrder() == 2;

		System.out.println(what + ":");
		for (int i = 0; i < rel.getDim(0); i++) {
			for (int j = 0; j < rel.getDim(1); j++)
				System.out.print(rel.getElem(i, j) ? "1" : "0");
			System.out.println();
		}
	}

	public static <BOOL> BOOL isClosedSubset(BoolAlg<BOOL> alg,
			Tensor<BOOL> subset, Tensor<BOOL> galois) {
		Tensor<BOOL> t;

		t = Tensor.reduce(alg.ALL, "y", alg.LEQ, subset.named("x"),
				galois.named("xy"));
		t = Tensor.reduce(alg.ALL, "x", alg.LEQ, t.named("y"),
				galois.named("xy"));
		t = Tensor.map2(alg.EQU, subset, t);
		t = Tensor.fold(alg.ALL, 1, t);

		return t.get();
	}

	public static <SBOOL> Tensor<Boolean> collectClosedSubsets(
			SatSolver<SBOOL> solver, final Tensor<Boolean> galois) {
		Problem prob = new Problem("sub", new int[] { galois.getDim(0) }) {
			@Override
			public <BOOL> BOOL compute(BoolAlg<BOOL> alg,
					Map<String, Tensor<BOOL>> tensors) {
				Tensor<BOOL> sub = tensors.get("sub");
				Tensor<BOOL> rel = Tensor.map(alg.LIFT, galois);
				return isClosedSubset(alg, sub, rel);
			}
		};

		return prob.solveAll(solver, LIMIT).get("sub");
	}

	public static <BOOL> Tensor<BOOL> transpose(Tensor<BOOL> matrix) {
		assert matrix.getOrder() == 2;
		return Tensor.reshape(matrix,
				new int[] { matrix.getDim(1), matrix.getDim(0) }, new int[] {
						1, 0 });
	}

	public static <BOOL> BOOL isMonoid(BoolAlg<BOOL> alg, Tensor<BOOL> monoid) {
		assert monoid.getOrder() == 3;

		Tensor<BOOL> t;
		t = Tensor.reduce(alg.ANY, "xzij", alg.AND, monoid.named("xyi"),
				monoid.named("yzj"));
		t = Tensor.reduce(alg.ALL, "kij", alg.EQU, t.named("xzij"),
				monoid.named("xzk"));

		return isFunction(alg, t);
	}

	public static void checkMonoid(int size, String monoid) {
		final Tensor<Boolean> mon = decodeMonoid(size, monoid);
		if (!isMonoid(BoolAlg.BOOLEAN, mon))
			throw new RuntimeException("This is not a monoid: " + monoid);
	}

	protected static DecimalFormat TIME_FORMAT = new DecimalFormat("0.00");

	public static String[] INFINITE_MONOIDS = new String[] { "012", "000 012",
			"002 012", "000 001 012", "000 002 012", "000 011 012",
			"000 012 021", "002 012 022", "000 001 002 012", "000 001 010 012",
			"000 001 011 012", "000 001 012 111", "000 001 002 010 012",
			"000 001 002 011 012", "000 001 002 012 111",
			"000 001 010 011 012", "000 001 010 012 111",
			"000 001 011 012 111", "000 001 012 110 111",
			"000 001 012 111 112", "000 001 012 111 222",
			"000 001 002 010 011 012", "000 001 002 010 012 111",
			"000 001 002 011 012 022", "000 001 002 011 012 111",
			"000 001 002 012 110 111", "000 001 002 012 111 112",
			"000 001 002 012 111 222", "000 001 010 011 012 111",
			"000 001 010 012 110 111", "000 001 010 012 111 222",
			"000 001 011 012 111 112", "000 001 011 012 111 222",
			"000 001 012 102 110 111", "000 001 012 110 111 222",
			"000 001 012 111 112 222", "000 002 010 012 101 111",
			"000 002 010 012 111 222", "000 001 002 010 011 012 111",
			"000 001 002 010 012 110 111", "000 001 002 010 012 111 222",
			"000 001 002 011 012 110 111", "000 001 002 011 012 111 112",
			"000 001 002 011 012 111 222", "000 001 002 012 110 111 112",
			"000 001 002 012 110 111 222", "000 001 002 012 111 112 222",
			"000 001 010 011 012 110 111", "000 001 010 011 012 111 222",
			"000 001 010 012 101 110 111", "000 001 010 012 110 111 222",
			"000 001 011 012 111 112 222", "000 001 012 102 110 111 222",
			"000 001 002 010 011 012 110 111",
			"000 001 002 010 011 012 111 222",
			"000 001 002 010 012 101 110 111",
			"000 001 002 010 012 110 111 112",
			"000 001 002 010 012 110 111 222",
			"000 001 002 011 012 100 110 111",
			"000 001 002 011 012 110 111 222",
			"000 001 002 011 012 111 112 222",
			"000 001 002 012 102 110 111 112",
			"000 001 002 012 110 111 112 222",
			"000 001 002 012 110 111 220 222",
			"000 001 010 011 012 110 111 222",
			"000 001 010 012 101 110 111 222",
			"000 001 002 010 011 012 020 021 022",
			"000 001 002 010 011 012 110 111 112",
			"000 001 002 010 011 012 110 111 222",
			"000 001 002 010 012 101 110 111 112",
			"000 001 002 010 012 101 110 111 222",
			"000 001 002 010 012 110 111 112 222",
			"000 001 002 010 012 110 111 220 222",
			"000 001 002 011 012 100 110 111 222",
			"000 001 002 011 012 110 111 220 222",
			"000 001 002 012 102 110 111 112 222",
			"000 001 010 011 012 100 101 110 111",
			"000 001 002 010 011 012 100 101 110 111",
			"000 001 002 010 011 012 110 111 112 222",
			"000 001 002 010 011 012 110 111 220 222",
			"000 001 002 010 012 101 110 111 112 222",
			"000 001 002 010 012 101 110 111 220 222",
			"000 001 002 011 012 100 110 111 220 222",
			"000 001 002 012 110 111 112 220 221 222",
			"000 001 010 011 012 100 101 102 110 111",
			"000 001 010 011 012 100 101 110 111 222",
			"000 001 002 010 011 012 020 021 022 111 222",
			"000 001 002 010 011 012 100 101 110 111 112",
			"000 001 002 010 011 012 100 101 110 111 222",
			"000 001 002 010 012 110 111 112 220 221 222",
			"000 001 002 012 102 110 111 112 220 221 222",
			"000 001 010 011 012 100 101 102 110 111 222",
			"000 001 002 010 011 012 100 101 102 110 111 112",
			"000 001 002 010 011 012 100 101 110 111 112 222",
			"000 001 002 010 011 012 100 101 110 111 220 222",
			"000 001 002 010 011 012 110 111 112 220 221 222",
			"000 001 002 010 012 101 110 111 112 220 221 222",
			"000 001 002 010 011 012 100 101 102 110 111 112 222",
			"000 001 002 010 011 012 100 101 110 111 112 220 221 222",
			"000 001 002 010 011 012 100 101 102 110 111 112 220 221 222",
			"000 001 002 010 011 012 020 021 022 100 101 110 111 200 202 220 222" };

	public static String[] FINITE_MONOIDS = new String[] {
			"000 012 111",
			"012 120 201",
			"000 002 012 111 222",
			"000 012 021 102 111 120 201 210 222",
			"000 001 002 011 012 022 111 112 122 222",
			"000 001 002 010 011 012 020 022 100 101 110 111 112 121 122 200 202 211 212 220 221 222",
			"000 001 002 010 011 012 020 021 022 100 101 110 111 112 121 122 200 202 211 212 220 221 222",
			"000 001 002 010 011 012 020 022 100 101 110 111 112 120 121 122 200 201 202 211 212 220 221 222",
			"000 001 002 010 011 012 020 021 022 100 101 102 110 111 112 120 121 122 200 201 202 210 211 212 220 221 222" };

	public static String[] UNKNOWN_MONOIDS = new String[] { "012 021",
			"002 012 112", "002 012 220", "000 002 010 012", "000 002 012 022",
			"000 002 012 111", "000 002 012 222", "000 011 012 022",
			"002 012 102 112", "000 002 010 012 111", "000 002 012 022 222",
			"000 002 012 111 112", "000 002 012 220 222",
			"000 011 012 021 022", "002 012 022 200 220",
			"002 012 112 220 221", "000 001 002 010 012 020",
			"000 002 010 012 101 111", "000 002 010 012 111 222",
			"000 002 012 022 111 222", "000 002 012 102 111 112",
			"000 002 012 111 112 222", "000 002 012 111 220 222",
			"002 012 022 200 210 220", "002 012 102 112 220 221",
			"000 001 002 010 012 020 021", "000 002 010 012 101 111 222",
			"000 002 012 022 200 220 222", "000 002 012 102 111 112 222",
			"000 001 002 010 011 012 020 022",
			"000 001 002 010 012 020 111 222",
			"000 001 002 011 012 022 111 222",
			"000 001 011 012 111 112 122 222",
			"000 002 010 012 101 111 220 222",
			"000 002 012 022 111 200 220 222",
			"000 002 012 022 200 210 220 222",
			"000 002 012 111 112 220 221 222",
			"000 001 002 010 012 020 021 111 222",
			"000 002 012 022 111 200 210 220 222",
			"000 002 012 102 111 112 220 221 222",
			"000 001 002 010 011 012 020 022 111 222",
			"000 001 002 010 011 012 020 022 100 101 110 111 200 202 220 222" };

	public static void printStatistics(int size, String monoid) {
		SatSolver<Integer> solver = new Sat4J();
		solver.debugging = false;

		long time = System.currentTimeMillis();
		System.out.println("monoid: " + monoid);
		checkMonoid(size, monoid);

		System.out.println("unary relations:        "
				+ collectUnaryRels(solver, size, monoid).getDim(1));

		System.out.println("binary relations:       "
				+ collectBinaryRels(solver, size, monoid).getDim(2));

		System.out.println("ternary relations:      "
				+ collectTernaryRels(solver, size, monoid).getDim(3));

		System.out.println("essential binary rels:  "
				+ collectEssentialBinaryRels(solver, size, monoid).getDim(2));

		Tensor<Boolean> ternaryRels = collectEssentialTernaryRels(solver, size,
				monoid);
		System.out.println("essential ternary rels: " + ternaryRels.getDim(3));

		System.out.println("quasiorder relations:   "
				+ collectQuasiorderRels(solver, size, monoid).getDim(2));

		Tensor<Boolean> binaryOps = collectBinaryOps(solver, size, monoid);
		System.out.println("binary ops:             " + binaryOps.getDim(3));

		System.out.println("ternary ops:            "
				+ collectTernaryOps(solver, size, monoid).getDim(4));

		System.out.println("essential binary ops:   "
				+ collectEssentialBinaryOps(solver, size, monoid).getDim(3));

		System.out.println("essential ternary ops:  "
				+ collectEssentialTernaryOps(solver, size, monoid).getDim(4));

		System.out.println("majority ops:           "
				+ collectMajorityOps(solver, size, monoid).getDim(4));

		System.out.println("maltsev ops:            "
				+ collectMaltsevOps(solver, size, monoid).getDim(4));

		Tensor<Boolean> compat = getCompatibility32Alt(BoolAlg.BOOLEAN,
				ternaryRels, binaryOps);
		compat = transpose(compat);
		System.out.println("galois connection:      " + compat.getDim(0) + " "
				+ compat.getDim(1));

		Tensor<Boolean> closed = collectClosedSubsets(solver, compat);
		System.out.println("closed sets (clones):   " + closed.getDim(1));

		time = System.currentTimeMillis() - time;
		System.out.println("finished in " + TIME_FORMAT.format(0.001 * time)
				+ " seconds\n");
	}

	public static void main(String[] args) {
		System.out.println("FINITE MONOIDS:");
		for (String monoid : FINITE_MONOIDS)
			printStatistics(3, monoid);

		System.out.println("INFINITE MONOIDS:");
		for (String monoid : INFINITE_MONOIDS)
			printStatistics(3, monoid);

		System.out.println("UNKNOWN MONOIDS:");
		for (String monoid : UNKNOWN_MONOIDS)
			printStatistics(3, monoid);
	}

	public static void main2(String[] args) {
		SatSolver<Integer> solver = new Sat4J();
		solver.debugging = false;

		int size = 3;
		String monoid = "000 002 012 022 111 200 210 220 222";
		System.out.println("monoid: " + monoid);

		Tensor<Boolean> rels = collectTernaryRels(solver, size, monoid);
		System.out.println("rels: " + rels.info());
		// printTernaryRels(rels);

		Tensor<Boolean> ops = collectTernaryOps(solver, size, monoid);
		System.out.println("ops: " + ops.info());
		// printTernaryOps(ops);

		long time = System.currentTimeMillis();
		Tensor<Boolean> compat = getCompatibility33Alt(BoolAlg.BOOLEAN, rels,
				ops);
		System.out.println("compat: " + compat.info());
		time = System.currentTimeMillis() - time;
		System.err.println("finished in " + (0.001 * time) + " seconds");

		compat = transpose(compat);
		// printMatrix("compat", compat);

		Tensor<Boolean> closed = collectClosedSubsets(solver, compat);
		System.out.println("closed: " + closed.info());
		// printMatrix("closed", transpose(closed));

		System.out.println();
	}

	public static final int LIMIT = 5000;
}
