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
package dev.equo.ide;

import com.diffplug.common.swt.os.OS;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class IdeHookBranding implements IdeHook {
	private static final String DEFAULT_TITLE = "Equo IDE";
	private String title = DEFAULT_TITLE;
	private @Nullable File icon;
	private @Nullable File splash;

	public IdeHookBranding title(String title) {
		this.title = title == null ? DEFAULT_TITLE : title;
		return this;
	}

	public IdeHookBranding icon(File iconImg) {
		this.icon = iconImg;
		return this;
	}

	public IdeHookBranding splash(File splashImg) {
		this.splash = splashImg;
		return this;
	}

	@Override
	public IdeHookInstantiated instantiate() {
		return new Instantiated();
	}

	class Instantiated implements IdeHookInstantiated {
		private Shell splash;

		private Image loadImage(Display display, File file, String defaultResource) {
			try {
				if (file != null) {
					var imageData = new ImageData(file.getAbsolutePath());
					return new Image(display, imageData);
				}
			} catch (Exception e) {
				LoggerFactory.getLogger(IdeHookBranding.class)
						.warn(
								"Unable to load image " + file.getAbsolutePath() + ", falling back to default.", e);
			}
			try (var input =
					IdeHookBranding.class.getClassLoader().getResource(defaultResource).openStream()) {
				var imageData = new ImageData(input);
				return new Image(display, imageData);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void afterDisplay(Display display) {
			var cursor = display.getCursorLocation();
			var monitors = display.getMonitors();
			var bestMonitor =
					Arrays.stream(monitors)
							.filter(monitor -> monitor.getBounds().contains(cursor))
							.findAny()
							.orElse(monitors[0])
							.getBounds();

			Image image = loadImage(display, IdeHookBranding.this.splash, "dev/equo/ide/equo_splash.png");
			int imgX = image.getBounds().width;
			int imgY = image.getBounds().height;
			splash = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
			splash.setText("Branding");
			splash.setBounds(
					bestMonitor.x + (bestMonitor.width - imgX / 2) / 2,
					bestMonitor.y + (bestMonitor.height - imgY / 2) / 2,
					imgX / 2,
					imgY / 2);
			splash.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
			splash.addListener(
					SWT.Paint,
					e -> {
						e.gc.setAdvanced(true);
						e.gc.setAntialias(SWT.ON);
						e.gc.drawImage(image, 0, 0, imgX, imgY, 0, 0, imgX / 2, imgY / 2);
					});
			splash.open();
			splash.forceActive();

			Display.setAppName(title);
			while (display.readAndDispatch())
				// pump the event loop enough to show the branding
				;
		}

		@Override
		public void postStartup() {
			splash.dispose();
			splash = null;

			Display.getDefault()
					.asyncExec(
							() -> {
								var logger = LoggerFactory.getLogger(IdeHookBranding.class);
								var display = Display.getCurrent();
								if (display == null) {
									// early shutdown
									return;
								}
								display = Display.getDefault();
								Display.setAppName(title);
								Image icon =
										loadImage(display, IdeHookBranding.this.icon, "dev/equo/ide/equo_icon.png");

								var bounds = icon.getBounds();
								if (bounds.width != bounds.height) {
									logger.warn(
											"Icon should be square, but is instead {} by {}",
											bounds.width,
											bounds.height);
								}
								var sizes = new int[] {16, 32, 48, 64, 128, 256};
								Image[] images = new Image[sizes.length];
								for (int i = 0; i < sizes.length; ++i) {
									var size = sizes[i];
									images[i] = new Image(display, size, size);
									GC gc = new GC(images[i]);
									gc.setBackground(new Color(0, 0, 0, 0));
									gc.setAntialias(SWT.ON);
									gc.drawImage(icon, 0, 0, bounds.width, bounds.height, 0, 0, size, size);
									gc.dispose();
								}
								icon.dispose();

								if (OS.getRunning().isMac()) {
									try {
										var dockImageField = display.getClass().getDeclaredField("dockImage");
										dockImageField.setAccessible(true);
										var dockImage = dockImageField.get(display);
										if (dockImage != null) {
											var release = dockImage.getClass().getDeclaredMethod("release");
											release.invoke(dockImage);
											dockImageField.set(display, null);
										}
									} catch (Exception e) {
										logger.warn("problem releasing default mac icon", e);
									}
								}

								Shell[] shells = display.getShells();
								for (var shell : shells) {
									shell.setText(title);
									shell.setImages(images);
									shell.forceActive();
								}
							});
		}
	}
}
