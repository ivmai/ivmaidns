        IvmaiDNS - a java DNS client library and utilities
        --------------------------------------------------
         Brief information about IvmaiDNS dnslook/dnszcon
         ------------------------------------------------
           (c) 1999-2001 Ivan Maidanski <ivmai@mail.ru>


Project home page
-----------------

http://ivmaidns.sourceforge.net


Preface
-------

IvmaiDNS is a pure java DNS client implementation. It consists of
a library and utilities for looking up the Internet domain names. The
utilities are also able to fetch an entire domain zone and save it in
the standard DNS zone file format.

Disclaimer
----------

You use this software at your own risk.

This application is for legal use only.

All rights reserved.

Installation
------------

Download the binary distributive file appropriate to your host
platform.

Extract the contents of the distributive file (preserving the
directories structure) into any appropriate place (eg.,
"C:\Program Files" for Windows, "/usr/share" for Linux and Solaris) on
your hard drive. No other setup is required.

To uninstall the software, just remove "ivmaidns" directory.

Supported platforms
-------------------

IvmaiDNS tools are officially built for the following platforms:
- JavaVM (J2SE/JDK v1.2+);
- Windows x86 (Win95/98/Me/NT/2000/XP/2003/Vista/Win7);
- Linux x86 (tested on CentOS);
- Sun Solaris amd64.

The native binary files (for Win32, Linux, Solaris) are created using
the JCGO tool and the corresponding C/C++ compiler.

General
-------

The major goals of this project are:
- develop a pure java library implementing DNS (domain name
system) basic primitives and simplifying development of a DNS client
application;
- develop a tool (named "dnslook") similar to the well-known Unix
nslookup one but with the ability to save fetched domain zones in the
standard DNS server zone format;
- develop a multi-threaded interactive console tool getting DNS zones
in parallel and recursively equipped with a load balancing algorithm.

Usage directions for dnslook
----------------------------

Synopsis:

dnslook [-z] [-n] [-p] [<dns_server>] <name> [[-d] <out_txt_file>]

Options:
 -d <out_txt_file>  Save records to a given file
 -p  Save records in the plain format
 -z  Get all records for the domain
 -n  Look up only name server records (if used without -z)
 -n  Try all available name servers until success (if with -z)

Information (displayed if invoked without parameters):

This utility allows the user to look up Internet DNS records and entire
domains of a given name from the specified server. Every such record
consists of its name, the class (IN), time-to-live value, its type
(mostly A, NS, MX, CNAME, PTR or SOA), and its value according to its
type. Refer to RFC1035 document for more information on DNS.

Here, if <dns_server> parameter is omitted then the default server is
used. If an optional <out_txt_file> name is specified then the looked
up records are all saved to it in the standard DNS zone file format,
which is either plain (the fields of the records are tab-separated) if
-p option is set or blank-padded (with the records sorted ascending and
duplicated records removed). By default, all the records of a given
name are looked up, but setting of -n option causes the utility to look
up only name server records.

To look up an entire domain (zone) -z option must be set (but, this
operation succeeds only if zone transferring is allowed by the server).
If -n option is set in addition to -z option then all of the
authoritative name servers for a given domain are sequentially tried to
look up the entire domain from until it succeeds.

Usage directions for dnszcon
----------------------------

Synopsis:

dnszcon <out_text_file>

Options: none.

Information (displayed if invoked without parameters):

This tool allows the user to fetch (retrieve) Internet DNS records and
entire DNS zones of a given name from their authoritative name servers.
The tool is an interactive console program which processes user
retrieval queries asynchronously in the background (up to 30 queries to
different name servers at a time). All retrieved records are
immediately put into the built-in sorted storage, which may be listed
by user at run-time, and which is saved to the specified file (in
a textual tab-separated format) on exit. An entire zone retrieving
(zone transferring) is possible only if it is allowed at least by one
of its authoritative name servers. Subzones for the zone being
transferred are retrieved only in the recursive mode.

Interactive console commands:

 <zone> <server1> ... - get and list name server records for <zone>
 ?<name> <server1> ... - try to retrieve all records for <name>
 !<zone> <server1> ... - try to retrieve entire <zone>
 <zone> - list all retrieved records for <zone> and its subzones
 + - enable recursive mode
 - - disable recursive mode (default)
 ? - view retriever activity and memory utilization
 & - pause/resume activity
 ! - show statistics, save retrieved records to file and exit

License
-------

This is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

This software is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License (GPL) for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library. Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module. An independent module is a module which is not derived from
or based on this library. If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so. If you do not wish to do so, delete this
exception statement from your version.

User's feedback
---------------

Any questions, suggestions, bug reports and patches are welcomed at
the IvmaiDNS site tracker (hosted at SourceForge.net).

                          --- [ End of File ] ---
