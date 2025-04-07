/**
 * 
 */
package texty3;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;

import org.gnome.adw.AlertDialog;
import org.gnome.adw.Application;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.ResponseAppearance;
import org.gnome.adw.Toast;
import org.gnome.adw.ToastOverlay;
import org.gnome.adw.WindowTitle;
import org.gnome.gdk.Display;
import org.gnome.gio.File;
import org.gnome.gio.FileCreateFlags;
import org.gnome.gio.Settings;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Type;
import org.gnome.glib.Variant;
import org.gnome.glib.VariantType;
import org.gnome.gobject.GObject;
import org.gnome.gtk.CssProvider;
import org.gnome.gtk.FileDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextView;
import org.gnome.gtk.WrapMode;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.base.Out;
import io.github.jwharm.javagi.gobject.annotations.InstanceInit;
import io.github.jwharm.javagi.gtk.annotations.GtkChild;
import io.github.jwharm.javagi.gtk.annotations.GtkTemplate;
import io.github.jwharm.javagi.gtk.types.TemplateTypes;

/**
 * @author Footeware.ca
 *
 */
@GtkTemplate(name = "Texty3Window", ui = "/texty3/window.ui")
public class Texty3Window extends ApplicationWindow {

	public static Type gtype = TemplateTypes.register(Texty3Window.class);
	private static Application app;

	/**
	 * Constructor.
	 *
	 * @param app {@link Texty3Application} the enclosing application
	 */
	public static Texty3Window create(Application app) {
		Texty3Window.app = app;
		Texty3Window win = GObject.newInstance(gtype);
		win.setApplication(app);
		return win;
	}

	private File file;
	private Settings settings;
	private CssProvider provider = new CssProvider();
	@GtkChild(name = "text_view")
	public TextView textView;
	@GtkChild(name = "toast_overlay")
	public ToastOverlay toastOverlay;
	@GtkChild(name = "window_title")
	public WindowTitle windowTitle;

	public Texty3Window(MemorySegment address) {
		super(address);
	}

	private void clear() {
		file = null;
		textView.getBuffer().setText("", 0);
		textView.getBuffer().setModified(false);
		updateWindowTitle();
		textView.grabFocus();
	}

	@InstanceInit
	public void init() {
		settings = new Settings("ca.footeware.java.texty3");

		// set window sized based on last resize
		int width = settings.getInt("window-width");
		int height = settings.getInt("window-height");
		setSizeRequest(width, height);

		// connect to window size change signals
		onNotify("default-width", _ -> onWindowSizeChange());
		onNotify("default-height", _ -> onWindowSizeChange());

		// Save action
		var saveAction = new SimpleAction("save", null);
		saveAction.onActivate((SimpleAction.ActivateCallback) _ -> onSaveAction());
		addAction(saveAction);

		// New action
		var newAction = new SimpleAction("new", null);
		newAction.onActivate((SimpleAction.ActivateCallback) _ -> onNewAction());
		app.setAccelsForAction("win.new", new String[] { "<primary>n" });
		addAction(newAction);

		// Open action
		var openAction = new SimpleAction("open", null);
		openAction.onActivate((SimpleAction.ActivateCallback) _ -> onOpenAction());
		app.setAccelsForAction("win.open", new String[] { "<primary>o" });
		addAction(openAction);

		// Save As action
		var saveAsAction = new SimpleAction("save-as", null);
		saveAsAction.onActivate((SimpleAction.ActivateCallback) _ -> onSaveAsAction());
		app.setAccelsForAction("win.save-as", new String[] { "<primary><shift>s" });
		addAction(saveAsAction);

		// New Window action
		var newWindowAction = new SimpleAction("new-win", null);
		newWindowAction.onActivate((SimpleAction.ActivateCallback) _ -> onNewWindowAction());
		app.setAccelsForAction("app.new-win", new String[] { "<primary><shift>n" });
		app.addAction(newWindowAction);

		// Toggle Wrap action with initial state
		var toggleWrapAction = SimpleAction.stateful("toggle-wrap", null,
				new Variant("b", settings.getBoolean("wrap-mode")));
		toggleWrapAction.onActivate(this::onToggleWrapAction);
		app.setAccelsForAction("win.toggle-wrap", new String[] { "<primary><shift>w" });
		addAction(toggleWrapAction);

		// Font Size action
		var fontSizeAction = SimpleAction.stateful("set-font-size", new VariantType("i"),
				new Variant("i", settings.getInt("font-size")));
		fontSizeAction.onActivate((SimpleAction.ActivateCallback) parameter -> onFontSizeAction(parameter));
		// no shortcut, add to window
		addAction(fontSizeAction);
		// fire once to set font-size as per settings
		fontSizeAction.activate(new Variant("i", settings.getInt("font-size")));

		// init font-size
		textView.setName("journalTextView");
		int fontSize = settings.getInt("font-size");
		String css = "#journalTextView { font-family: Mono; font-size: " + fontSize + "px; }";
		provider.loadFromString(css);
		Gtk.styleContextAddProviderForDisplay(Display.getDefault(), provider, 600);
	}

	private void loadFile() {
		try {
			var contents = new Out<byte[]>();
			if (file.loadContents(null, contents, null)) {
				var buffer = textView.getBuffer();
				String str = new String(contents.get());
				buffer.setText(str, str.length());
				buffer.setModified(false);
				updateWindowTitle();
				textView.grabFocus();
				String message = "File opened: " + file.getBasename();
				showToast(message);
			}
		} catch (GErrorException e) {
			showToast(e.getMessage());
		}
	}

	private void onFontSizeAction(Variant parameter) {
		int size = parameter.getInt32();
		settings.setInt("font-size", size);

		String css = "#journalTextView { font-family: Mono; font-size: " + size + "px; }";
		provider.loadFromString(css);
		Gtk.styleContextAddProviderForDisplay(Display.getDefault(), provider, 500);

		SimpleAction action = (SimpleAction) lookupAction("set-font-size");
		action.setState(new Variant("i", size));
	}

	private void onNewAction() {
		if (textView.getBuffer().getModified()) {
			promptToSaveModified("new");
		} else {
			clear();
		}
	}

	private void onNewWindowAction() {
		Texty3Application.create();
	}

	private void onOpenAction() {
		if (textView.getBuffer().getModified()) {
			promptToSaveModified("open"); // newAction = "open"
		} else {
			// prompt for file to open
			var dialog = new FileDialog();
			dialog.open(this, null, (_, result, _) -> {
				try {
					file = dialog.openFinish(result);
					if (file != null) {
						loadFile();
					}
				} catch (GErrorException ignored) {
					// user clicked cancel
				}
			});
		}
	}

	private void onSaveAction() {
		save();
	}

	private void onSaveAsAction() {
		// prompt for file to open
		var dialog = new FileDialog();
		dialog.save(this, null, (_, result, _) -> {
			try {
				file = dialog.saveFinish(result);
				if (file != null && file.queryExists(null)) {
					saveFile();
				} else if (file != null) {
					// create the file so we can later write to it
					file.create(new HashSet<>(), null);
					saveFile();
				}
			} catch (GErrorException ignored) {
				// user clicked cancel
			} 
		});
	}

	private void onToggleWrapAction(Variant parameter) {
		// get action's state
		SimpleAction action = (SimpleAction) lookupAction("toggle-wrap");
		boolean currentState = action.getState().getBoolean();
		// toggle action state
		boolean newState = !currentState;
		action.setState(new Variant("b", newState));
		// set wrap mode in textview
		textView.setWrapMode(newState ? WrapMode.WORD : WrapMode.NONE);
		// save as preference
		settings.setBoolean("wrap-mode", newState);
	}

	private void onWindowSizeChange() {
		int width = getWidth();
		int height = getHeight();
		// save window size to prefs
		settings.setInt("window-width", width);
		settings.setInt("window-height", height);
	}

	private void performNextAction(String nextAction) {
		if (nextAction.equals("new")) {
			clear();
		} else if (nextAction.equals("open")) {
			// prompt for file to open
			var dialog = new FileDialog();
			dialog.open(this, null, (_, result2, _) -> {
				try {
					file = dialog.openFinish(result2);
					if (file != null) {
						loadFile();
					}
				} catch (GErrorException ignored) {
					// user clicked cancel
				} 
			});
		}
	}

	private void promptToSaveModified(String nextAction) {
		AlertDialog alert = new AlertDialog("Save?", "Would you like to save modifications?");
		alert.addResponse("cancel", "Cancel");
		alert.addResponse("discard", "Discard");
		alert.addResponse("save", "Save");
		alert.setResponseAppearance("save", ResponseAppearance.SUGGESTED);
		alert.setResponseAppearance("discard", ResponseAppearance.DESTRUCTIVE);
		alert.choose(this, null, (_, result, _) -> {
			String response = alert.chooseFinish(result);
			if (response.equals("save")) {
				save();
			} else if (response.equals("discard")) {
				performNextAction(nextAction);
			}
		});
	}

	private void save() {
		if (file != null) {
			saveFile();
		} else {
			var dialog = new FileDialog();
			dialog.save(this, null, (_, result, _) -> {
				try {
					file = dialog.saveFinish(result);
					if (file != null) {
						saveFile();
					}
				} catch (GErrorException ignored) {
					// user clicked cancel
				} 
			});
		}
	}

	private void saveFile() {
		write();
		textView.getBuffer().setModified(false);
		textView.grabFocus();
		updateWindowTitle();
		String message = "File saved: " + file.getBasename();
		showToast(message);
	}

	private void showToast(String message) {
		var toast = new Toast(message);
		toastOverlay.addToast(toast);
	}

	private void updateWindowTitle() {
		windowTitle.setTitle(
				(textView.getBuffer().getModified() ? "• " : "") + (file != null ? file.getBasename() : "texty3"));
		windowTitle.setSubtitle(file != null ? file.getPath() : "a minimal text editor");
	}

	private void write() {
		// Get the contents of the textview buffer as a byte array
		TextIter start = new TextIter();
		TextIter end = new TextIter();
		textView.getBuffer().getBounds(start, end);
		byte[] contents = textView.getBuffer().getText(start, end, false).getBytes();
		try {
			// Write the byte array to the file
			file.replaceContents(contents, "", false, FileCreateFlags.NONE, null, null);
		} catch (GErrorException e) {
			showToast(e.getMessage());
		}
	}
}