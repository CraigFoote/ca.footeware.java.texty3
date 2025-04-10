# texty3

A minimal text editor, third in a series - the first in [C](https://github.com/CraigFoote/ca.footeware.c.texty), the second using [python-Gtk](https://github.com/CraigFoote/ca.footeware.py.texty2) and now this one written in Java and using [Java-GI](https://jwharm.github.io/java-gi/) Gtk/Adw bindings. Each is packaged as a flatpak. The first two were developed in GNOME Builder and this one I developed in eclipse using a [fork](https://github.com/CraigFoote/flatpak-maven-plugin) of the [uk.co.bithatch:maven-flatpak-plugin](https://github.com/bithatch/maven-flatpak-plugin) to create flatpak artifacts. The plugin is built locally for now until I get it in Sonatype snapshots like the original did.

The code is compiled with Java 22 (the minimum for Java-GI) and is packaged in a container with flatpak runtime *org.gnome.Platform* and *org.gnome.Sdk* 48 that includes the openjdk-23.0.2 JRE that runs the application.

## Prerequisites

You'll need [flatpak](https://flathub.org/setup) and a Java JDK >= 22 installed.

Along the way you may get some errors about missing flatpak runtimes. These can be fixed by installing them via, e.g.:

`flatpak install flathub org.gnome.Platform`

## Building

If you're using eclipse, use the `texty3-BUILD.launch` run configuration. Or run:

```
mvn clean package ca.footeware:flatpak-maven-plugin:generate
```

This populates the *target* folder, including an *app* folder with artifacts needed for a flatpak-builder call. The *app* folder should look like this:

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

From the *app* folder, run the following to build the flatpak and install it locally.

```
flatpak-builder --force-clean --user --verbose --install build-dir ca.footeware.java.texty3.yml
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
GLib-GIO-ERROR **: 13:43:41.272: Settings schema 'ca.footeware.java.texty3' is not installed
```

The texty3 application is missing its dependency on its GSettings schema.

And that's as far as I've gotten!  
If you're reading this and have any ideas please open an issue and describe it. Open source rocks.

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

## TODO

- Get GPG signing of artifacts working and deploy to sonatype central snapshots.
- Provide gsettings schema.
- Build to *texty3.flatpak* rather than installing, the same way Gnome Builder builds.
- Release *texty3.flatpak* as installable on clients.

---
