SUMMARY = "Documentation generator for glib-based software"
DESCRIPTION = "Gtk-doc is a set of scripts that extract specially formatted comments \
               from glib-based software and produce a set of html documentation files from them"
HOMEPAGE = "https://www.gtk.org/docs/"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=94d55d512a9ba36caa9b7df079bae19f"

GNOMEBASEBUILDCLASS = "meson"
inherit gnomebase

# Configure the scripts correctly (and build their dependencies) only if they are actually
# going to be used; otheriwse we need only the m4/makefile includes from the gtk-doc tarball.
PACKAGECONFIG ??= "${@bb.utils.contains("DISTRO_FEATURES", "api-documentation", "working-scripts", "", d)}"
DEPENDS += "itstool-native libxslt-native python3-pygments-native gettext-native"

# This will cause target gtk-doc to hardcode paths of native dependencies
# into its scripts. This means that target gtk-doc package is broken;
# hopefully no one minds because its scripts are not used for anything during build
# and shouldn't be used on targets.
PACKAGECONFIG[working-scripts] = ",,docbook-xml-dtd4-native docbook-xsl-stylesheets"
PACKAGECONFIG[tests] = "-Dtests=true,-Dtests=false,glib-2.0"

SRC_URI[archive.sha256sum] = "3b84bac36efbe59017469040dfee58f17cf0853b5f54dfae26347daf55b6d337"
SRC_URI += "file://0001-Do-not-hardocode-paths-to-perl-python-in-scripts.patch \
           file://no-clobber.patch \
           "
SRC_URI:append:class-native = " file://pkg-config-native.patch"

BBCLASSEXTEND = "native nativesdk"

do_install:append () {
    # configure values for python3 xsltproc and pkg-config encoded in scripts
    for fn in ${bindir}/gtkdoc-depscan \
        ${bindir}/gtkdoc-mkhtml2 \
        ${datadir}/gtk-doc/python/gtkdoc/config_data.py \
        ${datadir}/gtk-doc/python/gtkdoc/config.py; do
        sed -e 's,${RECIPE_SYSROOT_NATIVE}/usr/bin/pkg-config,${bindir}/pkg-config,' \
            -e 's,${RECIPE_SYSROOT_NATIVE}/usr/bin/xsltproc,${bindir}/xsltproc,' \
            -e 's,${HOSTTOOLS_DIR}/python3,${bindir}/python3,' \
            -e '1s|^#!.*|#!/usr/bin/env python3|' \
            -i ${D}$fn
    done
}

FILES:${PN}-doc = "${datadir}/help"

SYSROOT_PREPROCESS_FUNCS:append:class-native = " gtkdoc_makefiles_sysroot_preprocess"
gtkdoc_makefiles_sysroot_preprocess() {
        # Patch the gtk-doc makefiles so that the qemu wrapper is used to run transient binaries
        # instead of libtool wrapper or running them directly
        sed -i \
           -e "s|GTKDOC_RUN =.*|GTKDOC_RUN = \$(top_builddir)/gtkdoc-qemuwrapper|" \
           ${SYSROOT_DESTDIR}${datadir}/gtk-doc/data/gtk-doc*make
}
