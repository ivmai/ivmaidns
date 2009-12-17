#!/bin/sh
# @(#) build-solaris-amd64-jcgo.sh - Solaris/amd64 build script for dnslook, dnszcon.
# Used tools: JCGO, SunCC.

proj_unix_name="ivmaidns"
dist_dir=".dist-solaris-amd64-jcgo"

echo "Building Solaris/amd64 executables using JCGO+SunCC..."

if [ "$jcgo_home" != "" ] ; then : ; else : ; jcgo_home="/usr/share/JCGO" ; fi

rm -rf "$dist_dir"
mkdir "$dist_dir"

mkdir "$dist_dir/.jcgo_Out-dnslook"
$jcgo_home/jcgo -d "$dist_dir/.jcgo_Out-dnslook" -src $~/goclsp/clsp_asc \
    -src src net.sf.$proj_unix_name.dnslook @$~/stdpaths.in || exit 1

mkdir "$dist_dir/.jcgo_Out-dnszcon"
$jcgo_home/jcgo -d "$dist_dir/.jcgo_Out-dnszcon" -src $~/goclsp/clsp_asc \
    -src src net.sf.$proj_unix_name.dnszcon @$~/stdpaths.in || exit 1

mkdir "$dist_dir/$proj_unix_name"

echo "Compiling dnslook..."
cc -o "$dist_dir/$proj_unix_name/dnslook" -I $jcgo_home/include \
    -I $jcgo_home/native -m64 -O2 -erroff=E_WHITE_SPACE_IN_DIRECTIVE \
    -erroff=E_INTEGER_OVERFLOW_DETECTED -erroff=E_STATEMENT_NOT_REACHED \
    -DJCGO_USELONG -DJCGO_INTFIT -DJCGO_UNIX -DJCGO_UNIFSYS -DJCGO_NOGC \
    -DJCGO_NOJNI -DJCGO_NOSEGV -DJCGO_INET -DJCGO_NOFP -s \
    "$dist_dir/.jcgo_Out-dnslook/Main.c" -lsocket -lnsl || exit 1

echo "Compiling dnszcon..."
cc -o "$dist_dir/$proj_unix_name/dnszcon" -I $jcgo_home/include \
    -I $jcgo_home/include/boehmgc -I $jcgo_home/native -m64 -O2 -mt \
    -erroff=E_WHITE_SPACE_IN_DIRECTIVE -erroff=E_INTEGER_OVERFLOW_DETECTED \
    -erroff=E_STATEMENT_NOT_REACHED -D_FPU_CONTROL_H -DJCGO_USELONG \
    -DJCGO_INTFIT -DJCGO_UNIX -DJCGO_MATHEXT -DJCGO_UNIFSYS -DJCGO_THREADS \
    -DJCGO_USEGCJ -DJCGO_INET -DJCGO_SYSVNETDB -s \
    "$dist_dir/.jcgo_Out-dnszcon/Main.c" -lm -lsocket -lnsl \
    $jcgo_home/libs/amd64/solaris/libgcmt.a || exit 1

cp -p GNU_GPL.txt README.txt "$dist_dir/$proj_unix_name"
echo ""

"$dist_dir/$proj_unix_name/dnslook"
echo ""

"$dist_dir/$proj_unix_name/dnszcon"
echo ""

echo "BUILD SUCCESSFUL"
exit 0
