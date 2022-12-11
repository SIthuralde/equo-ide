/*******************************************************************************
 * Copyright (c) 2022 EquoTech, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EquoTech, Inc. - initial API and implementation
 *******************************************************************************/
package dev.equo.solstice.p2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.internal.framework.FilterImpl;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.InvalidSyntaxException;

/** In-memory store of all p2 metadata, especially provides/requires dependency information. */
public class P2Session {
	List<P2Unit> units = new ArrayList<>();

	public void populateFrom(P2Client client, String url) throws Exception {
		client.addUnits(this, url);
		sort();
	}

	private void sort() {
		units.sort(Comparator.naturalOrder());
		for (var namespace : requirements.values()) {
			for (Requirement requirement : namespace.values()) {
				requirement.sortProviders();
			}
		}
	}

	/**
	 * Returns the unit with the given id. If there are multiple units with the same id, returns the
	 * one with the greatest version number. If there are none, throws an exception.
	 */
	public P2Unit getUnitById(String id) {
		for (var unit : units) {
			if (id.equals(unit.id)) {
				return unit;
			}
		}
		throw new IllegalArgumentException("No such unit id " + id);
	}

	public P2Query query() {
		return new P2Query(this);
	}

	/** Keeps track of every unit which provides the given capability. */
	public static class Requirement implements Comparable<Requirement> {
		final String name;
		private Object providers;

		private Requirement(String name) {
			this.name = name;
		}

		public String name() {
			return name;
		}

		private void add(P2Unit unit) {
			providers = add(providers, unit);
		}

		public boolean hasOnlyOneProvider() {
			return providers instanceof P2Unit;
		}

		public P2Unit getOnlyProvider() {
			return (P2Unit) providers;
		}

		public List<P2Unit> getProviders() {
			return getProviders(providers);
		}

		private void sortProviders() {
			if (providers instanceof ArrayList) {
				((ArrayList<P2Unit>) providers).sort(Comparator.naturalOrder());
			}
		}

		/** FYI, profiling against Eclipse 4.25 shows that 95% of these don't need a list. */
		private static Object add(Object existing, P2Unit toAdd) {
			if (existing == null) {
				return toAdd;
			} else if (existing instanceof P2Unit) {
				var list = new ArrayList<P2Unit>();
				list.add((P2Unit) existing);
				list.add(toAdd);
				return list;
			} else {
				((ArrayList<P2Unit>) existing).add(toAdd);
				return existing;
			}
		}

		private static List<P2Unit> getProviders(Object existing) {
			if (existing == null) {
				return Collections.emptyList();
			} else if (existing instanceof P2Unit) {
				return Collections.singletonList((P2Unit) existing);
			} else {
				return (ArrayList<P2Unit>) existing;
			}
		}

		@Override
		public int compareTo(@NotNull Requirement o) {
			return name.compareTo(o.name);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private final Map<String, Map<String, Requirement>> requirements = new HashMap<>();

	Requirement requires(String namespace, String name) {
		var perName = requirements.computeIfAbsent(namespace, unused -> new HashMap<>());
		return perName.computeIfAbsent(name, unused -> new Requirement(name));
	}

	void provides(String namespace, String name, P2Unit unit) {
		requires(namespace, name).add(unit);
	}

	private final Map<String, FilterImpl> filterCache = new HashMap<>();

	FilterImpl parseFilter(String filter) {
		return filterCache.computeIfAbsent(
				filter,
				f -> {
					try {
						return FilterImpl.newInstance(f);
					} catch (InvalidSyntaxException e) {
						throw new RuntimeException(e);
					}
				});
	}
}
