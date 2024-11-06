package texty3;

import java.io.IOException;
import java.io.InputStream;

import org.gnome.adw.Application;
import org.gnome.gdk.Display;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gio.Resource;
import org.gnome.glib.Bytes;
import org.gnome.gtk.IconTheme;

import io.github.jwharm.javagi.base.GErrorException;

public class Texty3 extends Application {

	public static void main(String[] args) {
		var app = new Texty3("ca.footeware.java.texty3", ApplicationFlags.NON_UNIQUE);
	    app.onActivate(app::activate);
	    app.run(args);
	}

	public Texty3(String applicationId, ApplicationFlags flags) {
		super(applicationId, flags);
	}

	@Override
	public void activate() {
		loadGResource();
        var display = Display.getDefault();
        var iconTheme = IconTheme.getForDisplay(display);
        iconTheme.addResourcePath("/ca/footeware/java/texty3");
		openWindow();
	}

	public void openWindow() {
		var window = new Texty3Window(this);
		window.present();
	}

	private void loadGResource() {
		try (InputStream is = getClass().getResourceAsStream("/texty3.gresource")) {
			byte[] bytes = is.readAllBytes();
			Resource resource = Resource.fromData(new Bytes(bytes));
			resource.resourcesRegister();
		} catch (IOException | GErrorException e) {
			e.printStackTrace();
		}
	}
}