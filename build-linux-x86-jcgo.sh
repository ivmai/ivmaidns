#!/bin/sh
# @(#) build-linux-x86-jcgo.sh - Linux/x86 build script for dnslook, dnszcon.
# Used tools: JCGO, GNU/GCC.

proj_unix_name="ivmaidns"
dist_dir=".dist-linux-x86-jcgo"

echo "Building Linux/x86 executables using JCGO+GCC..."

if [ "$jcgo_home" != "" ] ; then : ; else : ; jcgo_home="/usr/share/JCGO" ; fi

rm -rf "$dist_dir"
mkdir "$dist_dir"

mkdir "$dist_dir/.jcgo_Out-dnslook"
$jcgo_home/jcgo -d "$dist_dir/.jcgo_Out-dnslook" -src $~/goclsp/clsp_asc \
    -src src net.sf.$proj_unix_name.dnslook @$~/stdpaths.in || exit 1

mkdir "$dist_dir/.jcgo_Out-dnszcon"
$jcgo_home/jcgo -d "$dist_dir/.jcgo_Out-dnszcon" -src $~/goclsp/clsp_asc \
    -src src net.sf.$proj_unix_name.dnszcon @$~/stdpaths.in || exit 1

mkdir -m 0755 "$dist_dir/$proj_unix_name"

echo "Compiling dnslook..."
gcc -o "$dist_dir/$proj_unix_name/dnslook" -I $jcgo_home/include \
    -I $jcgo_home/native -Os -fwrapv -fno-strict-aliasing -DJCGO_INTFIT \
    -DJCGO_UNIX -DJCGO_UNIFSYS -DJCGO_NOGC -DJCGO_NOJNI -DJCGO_NOSEGV \
    -DJCGO_INET -DJNIIMPORT=static/**/inline -DJNIEXPORT=JNIIMPORT \
    -DJNUBIGEXPORT=static -DJCGO_NOFP -s \
    "$dist_dir/.jcgo_Out-dnslook/Main.c" || exit 1
chmod 0755 "$dist_dir/$proj_unix_name/dnslook"

echo "Compiling dnszcon..."
gcc -o "$dist_dir/$proj_unix_name/dnszcon" -I $jcgo_home/include \
    -I $jcgo_home/include/boehmgc -I $jcgo_home/native -O2 -fwrapv \
    -fno-strict-aliasing -DJCGO_INTFIT -DJCGO_UNIX -DJCGO_UNIFSYS \
    -D_REENTRANT -DJCGO_THREADS -DJCGO_USEGCJ -DJCGO_INET -DJCGO_GNUNETDB \
    -DGC_NO_THREAD_REDIRECTS -DJCGO_NOJNI -DJCGO_NOSEGV -DGCSTATICDATA= \
    -DATTRIBNONGC=__attribute__\(\(section\(\".dataord\"\)\)\) \
    -DJCGO_GCRESETDLS -DJCGO_NOFP -s "$dist_dir/.jcgo_Out-dnszcon/Main.c" \
    -lpthread $jcgo_home/libs/x86/linux/libgcmt.a || exit 1
chmod 0755 "$dist_dir/$proj_unix_name/dnszcon"

cp -p GNU_GPL.txt README.txt "$dist_dir/$proj_unix_name"
echo ""

"$dist_dir/$proj_unix_name/dnslook"
echo ""

"$dist_dir/$proj_unix_name/dnszcon"
echo ""

echo "BUILD SUCCESSFUL"
exit 0
