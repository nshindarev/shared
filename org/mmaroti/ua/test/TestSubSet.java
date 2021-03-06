/**
 *	Copyright (C) Miklos Maroti, 2005
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

package org.mmaroti.ua.test;

import org.mmaroti.ua.alg.*;

public class TestSubSet {
	public static class MySubSet extends SubAlgebra {
		public MySubSet(Algebra base) {
			super(base);
		}

		public void print() {
			System.out.println("elements (size=" + size + "):");
			for (int i = 0; i < keys.length; ++i) {
				if (keys[i] == null) {
					System.out.println(i + ". empty");
					continue;
				}

				int hash = hashCode(keys[i]) & Integer.MAX_VALUE;
				int slot = hash % table.length;
				int step = 1 + (hash % (table.length - 2));

				System.out.println(i + ". hash=" + hashCode(keys[i]) + " slot="
						+ slot + " step=" + step + " object=" + keys[i]);
			}

			System.out.println("table (emptySlots=" + emptySlots + "):");
			for (int i = 0; i < table.length; ++i)
				System.out.println(i + ". " + table[i]);

			System.out.println();
		}
	}

	public static class Objects extends Algebra {
		public int getSize() {
			throw new IllegalStateException("not enumerable");
		}

		public int getIndex(Object element) {
			throw new IllegalStateException("not enumerable");
		}

		public Object getElement(int index) {
			throw new IllegalStateException("not enumerable");
		}

		public Object parse(String string) {
			return null;
		}

		public Operation[] getOperations() {
			return new Operation[0];
		}

		public Relation[] getRelations() {
			return new Relation[0];
		}
	}

	public static class Element {
		int value;
		int id;
		int hash;

		static int totalInstances = 0;

		public Element(int v) {
			value = v;
			id = ++totalInstances;
			hash = value;
		}

		public int hashCode() {
			return hash;
		}

		public boolean equals(Object o) {
			return value == ((Element) o).value;
		}

		public String toString() {
			return "(value=" + value + ", hash=" + hash + ", id=" + id + ")";
		}
	}

	MySubSet subset = new MySubSet(new Objects());

	public void add(int i) {
		System.out.println("adding " + i);
		subset.add(new Element(i));
		subset.print();
	}

	public void remove(int i) {
		System.out.println("removing " + i);
		subset.remove(new Element(i));
		subset.print();
	}

	public void resize() {
		System.out.println("resizing");
		subset.resize(subset.getSize());
		subset.print();
	}

	public void test() {
		System.out.println("empty:");
		subset.print();

		add(1);
		add(1);
		add(2);
		add(3);
		add(4);
		remove(1);
		remove(2);
		remove(2);
		add(2);
		add(5);
		add(6);
		remove(4);
		add(7);
		remove(3);
		add(8);
		remove(5);
		add(9);
		remove(6);
		add(10);
		remove(7);
		add(11);
		remove(8);
		add(12);
		resize();
	}

	public static void main(String[] _) {
		TestSubSet tss = new TestSubSet();
		tss.test();
	}
}
