package texty3;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.Properties;

import org.gnome.adw.AboutDialog;
import org.gnome.adw.Application;
import org.gnome.gdk.Display;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.gnome.glib.Variant;
import org.gnome.gobject.GObject;
import org.gnome.gtk.GtkBuilder;
import org.gnome.gtk.IconTheme;
import org.gnome.gtk.ShortcutsWindow;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.gobject.annotations.InstanceInit;
import io.github.jwharm.javagi.gobject.annotations.RegisteredType;
import io.github.jwharm.javagi.gtk.types.TemplateTypes;

@RegisteredType(name = "Texty3Application")
public class Texty3Application extends Application {

	public static Type gtype = TemplateTypes.register(Texty3Application.class);
	private GtkBuilder builder;

	public Texty3Application(MemorySegment address) {
		super(address);
	}

	public static Texty3Application create() {
		Texty3Application app = GObject.newInstance(gtype);
		app.setApplicationId("ca.footeware.java.texty3");
		app.onActivate(app::activate);
		return app;
	}

	@Override
	public void activate() {
		var display = Display.getDefault();
		var iconTheme = IconTheme.getForDisplay(display);
		iconTheme.addResourcePath("/texty3");

		var win = this.getActiveWindow();
		if (win == null) {
			win = Texty3Window.create(this);
		}
		win.present();
	}

	// @formatter:off
	private void onAboutAction(Variant parameter) {
		String version = "unknown";
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("/project.properties"));
			version = (String) properties.get("version");
		} catch (IOException e) {
			// ignore and use initial value
		}
        var about = AboutDialog.builder()
            .setApplicationName("texty3")
            .setApplicationIcon("texty3")
            .setDeveloperName("Another fine mess by Footeware.ca")
            .setDevelopers(new String[]{"Craig Foote"})
            .setVersion(version)
            .setCopyright("©2024 Craig Foote")
            .build();
        about.present(this.getActiveWindow());
 	}
 	// @formatter:on

	@InstanceInit
	public void init() {
		var aboutAction = new SimpleAction("about", null);
		aboutAction.onActivate(this::onAboutAction);
		addAction(aboutAction);

		var newWindowAction = new SimpleAction("new-window", null);
		newWindowAction.onActivate(this::onNewWindowAction);
		setAccelsForAction("app.new-window", new String[] { "<primary><shift>n" });
		addAction(newWindowAction);

		var shortcutsAction = new SimpleAction("keyboard-shortcuts", null);
		shortcutsAction.onActivate(this::onShortcutsAction);
		addAction(shortcutsAction);

		builder = new GtkBuilder();
	}

	private void onNewWindowAction(Variant variant1) {
		var win = Texty3Window.create(this);
		win.present();
	}

	private void onShortcutsAction(Variant variant1) {
		try {
			builder.addFromResource("/texty3/help_overlay.ui");
			((ShortcutsWindow) builder.getObject("help-overlay")).setVisible(true);
		} catch (GErrorException ignored) {
		}
	}
}