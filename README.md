# texty3

A minimal text editor, third in a series - the first in [C](https://github.com/CraigFoote/ca.footeware.c.texty), the second using [python-Gtk](https://github.com/CraigFoote/ca.footeware.py.texty2) and now this one written in Java and using [Java-GI](https://jwharm.github.io/java-gi/) Gtk/Adw bindings. Each is packaged as a flatpak. The first two were developed in GNOME Builder and this one I developed in eclipse and uses the [maven-flatpak-plugin](https://github.com/bithatch/maven-flatpak-plugin) to create flatpak artifacts.

## Prerequisites

You'll need [flatpak](https://flathub.org/setup) installed.

Along the way you may get some errors about missing flatpak runtimes. These can be fixed by installing them via, e.g.:

`flatpak install flathub org.freedesktop.Platform`

## Building

- If you're using eclipse, use the `texty3-BUILD.launch` run configuration. Or run `mvn clean package uk.co.bithatch:maven-flatpak-plugin:generate`. This populates the *target* folder, including an *app* folder where we'll do some more work. The *app* folder should look like this:

```
❯ tree ./app
./app
├── ca.footeware.java.texty3.desktop
├── ca.footeware.java.texty3.metainfo.xml
├── ca.footeware.java.texty3.svg
├── ca.footeware.java.texty3.yml
├── texty3
└── texty3.jar
```

- Edit `ca.footeware.java.texty3.metainfo.xml`  to add `<categories><category>Utility</category></categories>` before the closing `</component>`, i.e. `...</url><categories><category>Utility</category></categories></component>`. 


- Edit `ca.footeware.java.texty3.yml` to change *runtime-version* to "24.08" and to add these two lines to the end:

```
- "--env=PATH=/app/jre/bin:/app/bin:/usr/bin"
- "--env=JAVA_HOME=/app/jre"
```


- Running as root for the next command seems to be required. This will leave some items in your *apps* folder with root permission so you'll need to `sudo rm -r ./app` if you need to clean the project. Anyway, from *app* folder, run the following to build the flatpak and install it locally.


```
sudo flatpak-builder --force-clean --verbose --install build-dir ca.footeware.java.texty3.yml
```

If you get errors, running this can provide better explanations. Some *pom.xml* tags are used but I think I've got the required ones.

```
flatpak run --command=flatpak-builder-lint org.flatpak.Builder appstream ca.footeware.java.texty3.metainfo.xml
```

## Running

To run the installed flatpak, use:

```
flatpak run ca.footeware.java.texty3
```

This may take a long time to come back with the error I'm getting:

```
java.lang.UnsatisfiedLinkError: no libgtk-4.so.1 in java.library.path: :/usr/java/packages/lib:/usr/lib64:/lib64:/lib:/usr/lib
```

So it seems the texty3 flatpak container is missing its dependency on GTK libraries.

And that's as far as I've gotten! I've seen some talk of depending on libs in the host but that seems to be breaking most of the point of flatpaks - container sandboxing. I'll keep hacking on it and hopefully update this page. If you're reading this and have any ideas please open an issue and describe it. Open source rocks.


## Debugging

[Warehouse](https://flathub.org/apps/io.github.flattool.Warehouse) is a great program to manage flatpaks, including verifying installation and removal.

A couple commands I've found that might help:

- Source verification: `flatpak run --command=flatpak-builder-lint org.flatpak.Builder appstream ca.footeware.java.texty3.metainfo.xml`
- Container info: `flatpak info ca.footeware.java.texty3`
- Attach to running container: `flatpak run --command=sh ca.footeware.java.texty3`. Once connected, `cd /app` to see your files. You can check the version of java installed in `/app/jre`.

## Removing

To remove this fine piece of work, use Warehouse or run:

```
flatpak uninstall --delete-data ca.footeware.java.texty3
```

***
