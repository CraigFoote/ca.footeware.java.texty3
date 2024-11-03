package texty3;

import java.util.HashSet;

import org.gnome.adw.AlertDialog;
import org.gnome.adw.ApplicationWindow;
import org.gnome.adw.HeaderBar;
import org.gnome.adw.ResponseAppearance;
import org.gnome.adw.SplitButton;
import org.gnome.adw.WindowTitle;
import org.gnome.gio.File;
import org.gnome.gio.FileCreateFlags;
import org.gnome.gio.Menu;
import org.gnome.gio.MenuItem;
import org.gnome.gio.Settings;
import org.gnome.gio.SimpleAction;
import org.gnome.gtk.Box;
import org.gnome.gtk.FileDialog;
import org.gnome.gtk.Orientation;
import org.gnome.gtk.ScrolledWindow;
import org.gnome.gtk.TextIter;
import org.gnome.gtk.TextView;

import io.github.jwharm.javagi.base.GErrorException;
import io.github.jwharm.javagi.base.Out;
import io.github.jwharm.javagi.gobject.annotations.InstanceInit;

/**
 * texty3's window with menus and text view.
 * 
 * @author Footeware.ca
 *
 */
public class Texty3Window extends ApplicationWindow {

	private File file;
	private boolean modified = false;
	private TextView textView;
	private WindowTitle windowTitle;
	private Settings settings;

	@InstanceInit
	public void init() {
		settings = new Settings("ca.footeware.java.texty3");
	}

	/**
	 * Constructor.
	 * 
	 * @param app {@link Texty3} the enclosing application
	 */
	public Texty3Window(Texty3 app) {
		super(app);

		setDefaultSize(600, 400);

		// connect to window size change signals
		onNotify("default-width", (paramSpec) -> onWindowSizeChange());
		onNotify("default-height", (paramSpec) -> onWindowSizeChange());

		createActions();

		var vbox = new Box(Orientation.VERTICAL, 0);

		var headerBar = new HeaderBar();
		createSplitButton(headerBar);
		vbox.append(headerBar);

		textView = new TextView();
		textView.setMonospace(true);

		var scrolledWindow = new ScrolledWindow();
		scrolledWindow.setChild(textView);
		scrolledWindow.setVexpand(true);
		vbox.append(scrolledWindow);

		windowTitle = new WindowTitle("texty3", "a minimal text editor");
		headerBar.setTitleWidget(windowTitle);

		setContent(vbox);

		textView.getBuffer().onModifiedChanged(() -> {
			modified = textView.getBuffer().getModified();
			updateWindowTitle();
		});

		textView.grabFocus();
	}

	private void onWindowSizeChange() {
		System.err.println("onWindowSizeChange");
		int width = getWidth();
        int height = getHeight();
        // save window size to prefs
		settings.setInt("window-width", width);
		settings.setInt("window-height", height);
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
		saveAction.onActivate((SimpleAction.ActivateCallback) (parameter) -> onSaveAction());
		addAction(saveAction);

		// New action
		var newAction = new SimpleAction("new", null);
		newAction.onActivate((SimpleAction.ActivateCallback) (parameter) -> onNewAction());
		getApplication().setAccelsForAction("win.new", new String[] { "<primary>n" });
		addAction(newAction);

		// Open action
		var openAction = new SimpleAction("open", null);
		openAction.onActivate((SimpleAction.ActivateCallback) (parameter) -> onOpenAction());
		getApplication().setAccelsForAction("win.open", new String[] { "<primary>o" });
		addAction(openAction);

		// Save As action
		var saveAsAction = new SimpleAction("save-as", null);
		saveAsAction.onActivate((SimpleAction.ActivateCallback) (parameter) -> onSaveAsAction());
		getApplication().setAccelsForAction("win.save-as", new String[] { "<primary><shift>s" });
		addAction(saveAsAction);

		// New Window action
		var newWindowAction = new SimpleAction("new-win", null);
		newWindowAction.onActivate((SimpleAction.ActivateCallback) (parameter) -> onNewWindowAction());
		getApplication().setAccelsForAction("app.new-win", new String[] { "<primary><shift>n" });
		getApplication().addAction(newWindowAction);
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
			dialog.open(this, null, (_, result, _) -> {
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
			} // user clicked cancel
		});
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
			}
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
	}

	private void promptToSaveModified(String nextAction) {
		AlertDialog alert = new AlertDialog("Save?", "Would you like to save modifications?");
		alert.addResponses("cancel", "Cancel");
		alert.addResponses("discard", "Discard");
		alert.addResponses("save", "Save");
		alert.setResponseAppearance("save", ResponseAppearance.SUGGESTED);
		alert.setResponseAppearance("discard", ResponseAppearance.DESTRUCTIVE);
		alert.choose(this, null, (_, result, _) -> {
			String response = alert.chooseFinish(result);
			if (response.equals("save")) {
				save();
			} else if (response.equals("discard")) {
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
						} // user clicked cancel
					});
				}
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
			AlertDialog alert = new AlertDialog("Error", e.getMessage());
			alert.setVisible(true);
		}
	}
}