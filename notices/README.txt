This directory contains a number of notices which are included in order to comply with
license conditions relating to various components of the Saxon product.

The bulk of the Saxon-CE software was developed by Saxonica and is issued under the
Mozilla Public License version 2. In a few cases modules contain contributions from
outside Saxonica, and these are generally acknowledged by comments in the source code.
The Mozilla Public License version 2 is included in this folder as LICENSE.txt, and is
available on the web at http://http://www.mozilla.org/MPL/2.0/.

In the majority of cases, modules issued under MPL 2.0 are marked as being "incompatible
with secondary licenses". This means you do not have the right to reissue the code under
GPL or similar licenses. The reason for this restriction is that Saxon code has a long history,
and it is difficult to contact all past contributors to get their permission to release code
under a different license from that originally envisaged when they made their contribution.

A number of modules originate from Apache projects, and are issued under the Apache version 2.0
license. In most cases the Saxon-CE version includes modifications from the original. Modules
in this category include the following:

* RECompiler, REMatcher, REProgram, RESyntaxException: regular expression library adapted from
Apache's Jakarta project. This has been fairly extensively modified to support the XPath 2.0
regular expression dialect, to work with full Unicode, and to integrate with supporting Saxon
classes.

* StringTokenizer: a simple replacement for the JDK StringTokenizer class (which is not present
in GWT), taken from the Apache Harmony project

* URI: a replacement for the JDK java.net.URI class (which is not present in GWT), taken from Apache 
Xerces 2

* DocumentImpl, NamedNodeMapImpl, NodeXml, XmlParserImplXMLDoc: implementations of parts of the XML
DOM model, modifications of classes included in the Google GWT product. Saxon does not use the
XML DOM provided by GWT because it provides too strong an encapsulation of the browser-specific
Javascript DOM implementations; instead it uses its own DOM implementation over the native Javascript.

In addition a number of modules incorporate code that was originally released under a variety of
BSD-style licenses. These include:

* Normalizer, NormalizerData, and UnicodeDataParserFromXML. These implement Unicode normalization
and denormalization, and are based on code released by the Unicode Consortium. They have been modified
to change the format in which the data needed for normalization is distributed and accessed.

* GenericSorter. Saxon's sort routine was written by Wolfgang Hoschek while working at CERN, the 
European Organization for Nuclear Research. It has been modified to integrate it more closely with 
Saxon.

* ExpressionParser, Tokenizer. The original versions of Saxon's XPath parser and tokenizer were taken
from James Clark's xt product. They have been modified beyond all recognition, but the core design remains
intact, and their provenance is duly acknowledged.

