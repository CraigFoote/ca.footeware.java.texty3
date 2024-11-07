package texty3;

import java.io.IOException;
import java.util.HashSet;

import org.gnome.adw.AboutDialog;
import org.gnome.adw.AlertDialog;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.HeaderBar;
import org.gnome.adw.ResponseAppearance;
import org.gnome.adw.SplitButton;
import org.gnome.adw.Toast;
import org.gnome.adw.ToastOverlay;
import org.gnome.adw.WindowTitle;
import org.gnome.gdk.Display;
import org.gnome.gio.File;
import org.gnome.gio.FileCreateFlags;
import org.gnome.gio.Menu;
import org.gnome.gio.MenuItem;
import org.gnome.gio.Settings;
import org.gnome.gio.SimpleAction;
import org.gnome.glib.Variant;
import org.gnome.glib.VariantType;
import org.gnome.gtk.Box;
import org.gnome.gtk.CssProvider;
import org.gnome.gtk.FileDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.MenuButton;
import org.gnome.gtk.Orientation;
import org.gnome.gtk.ScrolledWindow;
import org.gnome.gtk.StyleContext;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextView;
import org.gnome.gtk.WrapMode;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.base.Out;

/**
 * texty3's window with menus and text view.
 *
 * @author Footeware.ca
 *
 */
public class Texty3Window extends ApplicationWindow {

	private File file;
	private boolean modified = false;
	private Settings settings;
	private TextView textView;
	private ToastOverlay toastOverlay;
	private WindowTitle windowTitle;

	/**
	 * Constructor.
	 *
	 * @param app {@link Texty3} the enclosing application
	 */
	public Texty3Window(Texty3 app) {
		super(app);

		settings = new Settings("ca.footeware.java.texty3");

		// set window sized based on last resize
		int width = settings.getInt("window-width");
		int height = settings.getInt("window-height");
		setDefaultSize(width, height);

		// connect to window size change signals
		onNotify("default-width", paramSpec -> onWindowSizeChange());
		onNotify("default-height", paramSpec -> onWindowSizeChange());

		createActions();

		var vbox = new Box(Orientation.VERTICAL, 0);

		var headerBar = new HeaderBar();
		createSplitButton(headerBar);
		vbox.append(headerBar);

		textView = new TextView();
		textView.setMonospace(true);
		// set initial state
		boolean wrapMode = settings.getBoolean("wrap-mode");
		textView.setWrapMode(wrapMode ? WrapMode.WORD : WrapMode.NONE);

		loadCss();
		int initialSize = settings.getInt("font-size");
		textView.addCssClass("font-size-" + initialSize);

		textView.getBuffer().onModifiedChanged(() -> {
			modified = textView.getBuffer().getModified();
			updateWindowTitle();
		});

		var scrolledWindow = new ScrolledWindow();
		scrolledWindow.setChild(textView);
		scrolledWindow.setVexpand(true);

		toastOverlay = new ToastOverlay();
		toastOverlay.setVexpand(true);
		toastOverlay.setHexpand(true);
		toastOverlay.setChild(scrolledWindow);
		vbox.append(toastOverlay);

		windowTitle = new WindowTitle("texty3", "a minimal text editor");
		headerBar.setTitleWidget(windowTitle);

		createHamburgerMenu(headerBar);

		setContent(vbox);

		textView.grabFocus();
	}

	private void clear() {
		file = null;
		textView.getBuffer().setText("", 0);
		textView.getBuffer().setModified(false);
		modified = false;
		updateWindowTitle();
		textView.grabFocus();
	}

	private void createActions() {
		// Save action
		var saveAction = new SimpleAction("save", null);
		saveAction.onActivate((SimpleAction.ActivateCallback) parameter -> onSaveAction());
		addAction(saveAction);

		// New action
		var newAction = new SimpleAction("new", null);
		newAction.onActivate((SimpleAction.ActivateCallback) parameter -> onNewAction());
		getApplication().setAccelsForAction("win.new", new String[] { "<primary>n" });
		addAction(newAction);

		// Open action
		var openAction = new SimpleAction("open", null);
		openAction.onActivate((SimpleAction.ActivateCallback) parameter -> onOpenAction());
		getApplication().setAccelsForAction("win.open", new String[] { "<primary>o" });
		addAction(openAction);

		// Save As action
		var saveAsAction = new SimpleAction("save-as", null);
		saveAsAction.onActivate((SimpleAction.ActivateCallback) parameter -> onSaveAsAction());
		getApplication().setAccelsForAction("win.save-as", new String[] { "<primary><shift>s" });
		addAction(saveAsAction);

		// New Window action
		var newWindowAction = new SimpleAction("new-win", null);
		newWindowAction.onActivate((SimpleAction.ActivateCallback) parameter -> onNewWindowAction());
		getApplication().setAccelsForAction("app.new-win", new String[] { "<primary><shift>n" });
		getApplication().addAction(newWindowAction);

		// Toggle Wrap action with initial state
		var toggleWrapAction = SimpleAction.stateful("toggle-wrap", null,
				new Variant("b", settings.getBoolean("wrap-mode")));
		toggleWrapAction.onActivate(this::onToggleWrapAction);
		getApplication().setAccelsForAction("win.toggle-wrap", new String[] { "<primary><shift>w" });
		addAction(toggleWrapAction);

		// Font Size action
		var fontSizeAction = SimpleAction.stateful("font-size", new VariantType("i"),
				new Variant("i", settings.getInt("font-size")));
		fontSizeAction.onActivate((SimpleAction.ActivateCallback) parameter -> onFontSizeAction(parameter));
		// no shortcut, add to window
		addAction(fontSizeAction);

		// About dialog
		var aboutAction = new SimpleAction("about", null);
		aboutAction.onActivate(this::onAboutAction);
		// no shortcut key, add to application
		getApplication().addAction(aboutAction);
	}

	private void createHamburgerMenu(HeaderBar headerBar) {
		MenuButton hamburgerButton = new MenuButton();
		hamburgerButton.setIconName("open-menu-symbolic");

		var hamburgerMenu = new Menu();

		var primaryMenu = new Menu();

		primaryMenu.appendItem(new MenuItem("Wrap Text", "win.toggle-wrap"));
		Menu fontMenu = new Menu();
		fontMenu.appendItem(new MenuItem("14px", "win.font-size(14)"));
		fontMenu.appendItem(new MenuItem("16px", "win.font-size(16)"));
		fontMenu.appendItem(new MenuItem("18px", "win.font-size(18)"));
		fontMenu.appendItem(new MenuItem("20px", "win.font-size(20)"));
		fontMenu.appendItem(new MenuItem("22px", "win.font-size(22)"));
		fontMenu.appendItem(new MenuItem("24px", "win.font-size(24)"));
		fontMenu.appendItem(new MenuItem("26px", "win.font-size(26)"));
		fontMenu.appendItem(new MenuItem("28px", "win.font-size(28)"));
		primaryMenu.appendSubmenu("Font Size", fontMenu);

		hamburgerMenu.appendSection(null, primaryMenu);

		var secondaryMenu = new Menu();
		secondaryMenu.appendItem(new MenuItem("About texty3", "app.about"));
		hamburgerMenu.appendSection(null, secondaryMenu);

		hamburgerButton.setMenuModel(hamburgerMenu);
		headerBar.packEnd(hamburgerButton);
	}

	/**
	 * Creates the splitbutton and its menuitems.
	 *
	 * @param headerBar {@link HeaderBar} the parent of the splitbutton
	 */
	private void createSplitButton(HeaderBar headerBar) {
		var splitButton = new SplitButton();
		splitButton.setLabel("Save");
		splitButton.setActionName("win.save");

		var actionMenu = new Menu();

		var windowActions = new Menu();
		windowActions.appendItem(new MenuItem("New", "win.new"));
		windowActions.appendItem(new MenuItem("Save As", "win.save-as"));
		windowActions.appendItem(new MenuItem("Open", "win.open"));
		actionMenu.appendSection(null, windowActions);

		var appActions = new Menu();
		appActions.appendItem(new MenuItem("New Window", "app.new-win"));
		actionMenu.appendSection(null, appActions);

		splitButton.setMenuModel(actionMenu);
		headerBar.packStart(splitButton);
	}

	private void loadCss() {
		var cssProvider = new CssProvider();
		try {
			var cssResource = Texty3.class.getResourceAsStream("/style.css");
			byte[] cssBytes = cssResource.readAllBytes();
			cssProvider.loadFromString(new String(cssBytes));
			// TODO try updating when Java-GI 0.12.0 is released
			StyleContext.addProviderForDisplay(Display.getDefault(), cssProvider,
					Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION);
		} catch (IOException e) {
			System.err.println("Failed to load CSS: " + e.getMessage());
		}
	}

	private void loadFile() {
		try {
			var contents = new Out<byte[]>();
			if (file.loadContents(null, contents, null)) {
				var buffer = textView.getBuffer();
				String str = new String(contents.get());
				buffer.setText(str, str.length());
				buffer.setModified(false);
				modified = false;
				updateWindowTitle();
				textView.grabFocus();
				String message = "File opened: " + file.getBasename();
				showToast(message);
			}
		} catch (GErrorException e) {
			showToast(e.getMessage());
		}
	}

	private void onAboutAction(Variant variant1) {
		// @formatter:off
		var about = AboutDialog.builder()
				.setApplicationName("texty3")
				.setApplicationIcon("texty3")
				.setDeveloperName("Another fine mess by Footeware.ca")
				.setVersion("1.0.0")
				.setDevelopers(new String[] { "Craig Foote https://Footeware.ca" })
				.setCopyright("© 2024 Craig Foote")
				.build();
		// @formatter:on
		about.present(this);
	}

	private void onFontSizeAction(Variant parameter) {
		int size = parameter.getInt32();
		settings.setInt("font-size", size);

		// Remove any existing font size classes
		for (int i = 14; i <= 28; i += 2) {
			textView.removeCssClass("font-size-" + i);
		}

		// Add the new font size class
		textView.addCssClass("font-size-" + size);

		SimpleAction action = (SimpleAction) lookupAction("font-size");
		action.setState(new Variant("i", size));
	}

	private void onNewAction() {
		if (modified) {
			promptToSaveModified("new");
		} else {
			clear();
		}
	}

	private void onNewWindowAction() {
		((Texty3) getApplication()).openWindow();
	}

	private void onOpenAction() {
		if (modified) {
			promptToSaveModified("open"); // newAction = "open"
		} else {
			// prompt for file to open
			var dialog = new FileDialog();
			dialog.open(this, null, (obj, result, memSeg) -> {
				try {
					file = dialog.openFinish(result);
					if (file != null) {
						loadFile();
					}
				} catch (GErrorException ignored) {
				} // user clicked cancel
			});
		}
	}

	private void onSaveAction() {
		save();
	}

	private void onSaveAsAction() {
		// prompt for file to open
		var dialog = new FileDialog();
		dialog.save(this, null, (obj, result, memSeg) -> {
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
			} // user clicked cancel
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
			dialog.open(this, null, (obj, result2, memSeg) -> {
				try {
					file = dialog.openFinish(result2);
					if (file != null) {
						loadFile();
					}
				} catch (GErrorException ignored) {
				} // user clicked cancel
			});
		}
	}

	private void promptToSaveModified(String nextAction) {
		AlertDialog alert = new AlertDialog("Save?", "Would you like to save modifications?");
		alert.addResponses("cancel", "Cancel");
		alert.addResponses("discard", "Discard");
		alert.addResponses("save", "Save");
		alert.setResponseAppearance("save", ResponseAppearance.SUGGESTED);
		alert.setResponseAppearance("discard", ResponseAppearance.DESTRUCTIVE);
		alert.choose(this, null, (obj, result, memSeg) -> {
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
			dialog.save(this, null, (obj, result, memSeg) -> {
				try {
					file = dialog.saveFinish(result);
					if (file != null) {
						saveFile();
					}
				} catch (GErrorException ignored) {
				} // user clicked cancel
			});
		}
	}

	private void saveFile() {
		write();
		textView.getBuffer().setModified(false);
		textView.grabFocus();
		modified = false;
		updateWindowTitle();
		String message = "File saved: " + file.getBasename();
		showToast(message);
	}

	private void showToast(String message) {
		var toast = new Toast(message);
		toastOverlay.addToast(toast);
	}

	private void updateWindowTitle() {
		windowTitle.setTitle((modified ? "• " : "") + (file != null ? file.getBasename() : "texty3"));
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