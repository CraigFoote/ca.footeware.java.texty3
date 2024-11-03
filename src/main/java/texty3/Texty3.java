package texty3;

import org.gnome.adw.Application;
import org.gnome.gio.ApplicationFlags;

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
    	openWindow();
    }
    
    public void openWindow() {
    	var window = new Texty3Window(this);
    	window.present();
    }
}