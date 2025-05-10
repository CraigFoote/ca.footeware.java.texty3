package texty3;

import java.io.IOException;

import org.gnome.gio.Resource;

import io.github.jwharm.javagi.base.GErrorException;

/**
 * The Main class registers the compiled gresource bundle and runs a new
 * Texty3Application instance.
 */
public class Main {

	/**
	 * Run texty3.
	 *
	 * @param args passed to AdwApplication.run()
	 * @throws GErrorException thrown while loading and registering the compiled
	 *                         resource bundle
	 */
	public static void main(String[] args) throws GErrorException, IOException {
		byte[] bytes = Main.class.getResourceAsStream("/texty3.gresource").readAllBytes();
		var resources = Resource.fromData(bytes);
		resources.resourcesRegister();

		var app = Texty3Application.create();
		app.run(args);
	}
}
