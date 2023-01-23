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
package dev.equo.ide.maven;

import dev.equo.ide.Launcher;
import java.io.IOException;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class LaunchTest extends MavenHarness {
	@Test
	public void integrationTestAtomos() throws IOException, InterruptedException {
		integrationTestUseAtomos(true);
	}

	@Test
	public void integrationTestSolstice() throws IOException, InterruptedException {
		integrationTestUseAtomos(false);
	}

	private void integrationTestUseAtomos(boolean useAtomos)
			throws IOException, InterruptedException {
		setPom(
				"<p2repos>\n"
						+ "  <p2repo>https://download.eclipse.org/eclipse/updates/4.26/</p2repo>\n"
						+ "</p2repos>\n"
						+ "<installs>\n"
						+ "  <install>org.eclipse.swt</install>\n"
						+ "</installs>");
		var output = mvnw("equo-ide:launch -DinitOnly=true -DuseAtomos=" + useAtomos);
		Assertions.assertThat(output).matches("(?s)(.*)Loaded (\\d+) bundles(.*)");
		Assertions.assertThat(output).contains(useAtomos ? "using Atomos" : "not using Atomos");
	}

	@Disabled
	@Test
	public void integrationTestReal() throws IOException, InterruptedException {
		setPom("");
		Launcher.launchAndInheritIO(rootFolder(), Arrays.asList("mvn", "equo-ide:launch", "-q"));
	}
}