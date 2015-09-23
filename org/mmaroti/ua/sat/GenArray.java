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

public class GenArray<ELEM> {
	private final int[] shape;
	private final int[] steps;
	private final int offset;
	private final ELEM[] elems;

	public int[] getShape() {
		return shape;
	}

	public int getDim() {
		return shape.length;
	}

	public ELEM get(int[] index) {
		assert index.length == steps.length;

		int pos = offset;
		for (int i = 0; i < steps.length; i++)
			pos += steps[i] * index[i];

		return elems[pos];
	}

	private GenArray(final int[] shape, final int[] steps, final int offset,
			final ELEM[] elems) {
		assert shape.length == steps.length;

		this.shape = shape;
		this.steps = steps;
		this.offset = offset;
		this.elems = elems;
	}

	public interface Gen<ELEM> {
		public ELEM elem(final int[] index);
	}

	public static <ELEM> GenArray<ELEM> generate(final int[] shape,
			final Gen<ELEM> gen) {
		int[] steps = new int[shape.length];

		int size = 1;
		for (int i = 0; i < shape.length; i++) {
			assert 0 <= shape[i];
			steps[i] = size;
			size *= shape[i];
		}

		@SuppressWarnings("unchecked")
		ELEM[] elems = (ELEM[]) new Object[size];

		int[] index = new int[shape.length];
		int pos = 0;
		while (pos < size) {
			elems[pos] = gen.elem(index);

			pos += 1;
			for (int i = 0; i < index.length && ++index[i] >= shape[i]; i++)
				index[i] = 0;
		}

		return new GenArray<ELEM>(shape, steps, 0, elems);
	}

	public static <ELEM> GenArray<ELEM> constant(final int[] shape,
			final ELEM elem) {
		@SuppressWarnings("unchecked")
		ELEM[] elems = (ELEM[]) new Object[1];
		elems[0] = elem;

		return new GenArray<ELEM>(shape, new int[shape.length], 0, elems);
	}

	public static <ELEM> GenArray<ELEM> vector(final Collection<ELEM> elems) {
		int size = elems.size();

		@SuppressWarnings("unchecked")
		ELEM[] es = (ELEM[]) new Object[size];

		int pos = 0;
		for (ELEM elem : elems)
			es[pos++] = elem;

		return new GenArray<ELEM>(new int[] { size }, new int[] { 1 }, 0, es);
	}

	public static <ELEM> GenArray<ELEM> matrix(final int[] shape,
			final Collection<ELEM> elems) {
		int[] steps = new int[shape.length];

		int size = 1;
		for (int i = 0; i < shape.length; i++) {
			assert 0 <= shape[i];
			steps[i] = size;
			size *= shape[i];
		}
		assert size == elems.size();

		@SuppressWarnings("unchecked")
		ELEM[] es = (ELEM[]) new Object[size];

		int pos = 0;
		for (ELEM elem : elems)
			es[pos++] = elem;

		return new GenArray<ELEM>(shape, steps, 0, es);
	}

	public GenArray<ELEM> reshape(int[] shape2, int[] map) {
		assert shape2.length == map.length;

		int[] steps2 = new int[shape2.length];

		for (int i = 0; i < map.length; i++) {
			assert map[i] >= -1;
			if (map[i] >= 0) {
				assert map[i] < shape.length;
				assert shape2[i] == shape[map[i]];

				steps2[i] += shape[map[i]];
			}
		}

		return new GenArray<ELEM>(shape2, steps2, offset, elems);
	}

	public interface Fun<ELEM, ELEM2> {
		public ELEM2 map(ELEM elem);
	}

	public <ELEM2> GenArray<ELEM2> map(Fun<ELEM, ELEM2> fun) {
		int[] steps2 = new int[shape.length];

		int size = 1;
		for (int i = 0; i < shape.length; i++) {
			assert 0 <= shape[i];
			steps2[i] = size;
			size *= shape[i];
		}

		@SuppressWarnings("unchecked")
		ELEM2[] elems2 = (ELEM2[]) new Object[size];

		int[] index = new int[shape.length];
		int pos1 = offset;
		int pos2 = 0;
		while (pos2 < size) {
			elems2[pos2] = fun.map(elems[pos1]);
			pos2 += 1;

			for (int i = 0; i < index.length; i++) {
				pos1 += steps[i];
				index[i] += 1;

				if (index[i] == shape[i]) {
					pos1 -= shape[i] * steps[i];
					index[i] = 0;
				} else
					break;
			}
		}

		return new GenArray<ELEM2>(shape, steps2, 0, elems2);
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		str.append("Array [");
		for (int i = 0; i < shape.length; i++) {
			if (i != 0)
				str.append(',');
			str.append(shape[i]);
		}
		str.append("] [");
		for (int i = 0; i < shape.length; i++) {
			if (i != 0)
				str.append(',');
			str.append(steps[i]);
		}
		str.append("] [");
		for (int i = 0; i < elems.length; i++) {
			if (i != 0)
				str.append(',');
			str.append(elems[i]);
		}
		str.append("]");

		return str.toString();
	}

	public static void main(String[] args) {
		GenArray<Integer> array = GenArray.generate(new int[] { 3, 3 },
				new Gen<Integer>() {
					@Override
					public Integer elem(int[] index) {
						return index[0] + index[1];
					}
				});
		System.out.println(array);

		array = GenArray.constant(new int[] { 2, 3 }, 0);
		System.out.println(array);
	}
}