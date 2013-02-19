<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:f="urn:internal.function"
xmlns:def="http://www.saxonica.com/ns/doc/functions"
xmlns:fx="http://local/functions"
xmlns:cat="http://www.saxonica.com/ns/doc/catalog"
exclude-result-prefixes="f xs def cat fx"
version="2.0"
>

<!-- Main stylesheet for transforming cetree-optimised XML files to static
 HTML files for use with systems without JavaScript enabled
 1. Create HTML file for each page/subpage defined in files linked to by
    the catalog.xml file    
-->

<xsl:import href="s-body.xsl"/>
<xsl:import href="s-functions-body.xsl"/>
<xsl:import href="s-functions-data.xsl"/>

<xsl:output method="html" doctype-system="about:legacy-compat" indent="no"/>

<xsl:variable name="location" select="'../doc'"/>
<xsl:variable name="fndoc"
select="doc(concat($location, '/functions.xml'))" as="document-node()"/>
<xsl:variable name="navlist" as="node()"
 select="doc(concat('','catalog.xml'))"/>

<xsl:variable name="sections" select="/cat:catalog/cat:section[not(@ref eq 'javadoc')]"/>

<xsl:variable name="section-docs" as="document-node()+">
<xsl:for-each select="$sections">
<xsl:sequence select="doc(resolve-uri(concat(@ref, '.xml'),base-uri()))"/>
</xsl:for-each>
</xsl:variable>

<xsl:template match="/">
<xsl:apply-templates select="$sections"/>
</xsl:template>

<xsl:template match="cat:section">
<xsl:variable name="doc" select="doc(resolve-uri(concat(@ref, '.xml'),base-uri()))"/>
<xsl:apply-templates select="$doc//(article|section|def:functions|def:function)" mode="makepage"/>
</xsl:template>

<xsl:function name="f:get-section-path" as="xs:string">
<xsl:param name="section" as="element()?"/>
<xsl:param name="hash-path" as="xs:string*"/>
<xsl:if test="empty($section)">
<xsl:value-of select="''"/>
</xsl:if>
<xsl:for-each select="$section">
<xsl:variable name="hash-parts" as="xs:string*"
select="(ancestor::article|ancestor::section)/@id|@id"/>
<xsl:variable name="ext" select="if (article|section|def:function) then '' else '.html'" as="xs:string"/>
<xsl:value-of select="concat(
$hash-path,
string-join($hash-parts,
'/'),
$ext)"/>
</xsl:for-each>
</xsl:function>

<xsl:template match="article|section|def:functions|def:function" mode="makepage">

<xsl:variable name="hash-parts" as="xs:string*"
select="(ancestor::article|ancestor::section|parent::def:functions)/@id|@id|*:name"/>

<xsl:variable name="hash-depth" as="xs:integer"
select="for $c in count($hash-parts) return
if (section|article|def:function) then $c + 1
else $c"/>

<xsl:variable name="hash-path" as="xs:string"
select="concat(
string-join(
(for $h in 2 to $hash-depth return '..')
,'/')
,'/')"/>

<xsl:variable name="css-path" as="xs:string"
select="concat($hash-path,'../')"/>

<xsl:variable name="top-posn" as="xs:integer"
select="for $sc in 1 to count($sections) return
if ($sections[$sc]/@ref eq $hash-parts[1]) then $sc
else ()"/>

<xsl:variable name="ns" as="node()?"
select="(section union following::section)[1]"/>
<xsl:variable name="next-section" as="node()?"
select="if (exists($ns)) then $ns else $section-docs[$top-posn + 1]/*"/>
<xsl:variable name="next-path" select="f:index-fix(
f:get-section-path($next-section,$hash-path))"/>

<xsl:variable name="ps" as="node()?"
select="((ancestor::section|ancestor::article) union preceding::section)[last()]"/>
<xsl:variable name="prev-section" as="node()?"
select="(if (exists($ps)) then $ps else $section-docs[$top-posn - 1]//(article|section))[last()]"/>
<xsl:variable name="prev-path" select="f:index-fix(
f:get-section-path($prev-section,$hash-path))"/>

<!--
<xsl:message>
top: <xsl:value-of select="$top-posn"/>
next: <xsl:value-of select="$next-section/@id"/>
next-path: <xsl:value-of select="$next-path"/>
next-exists: <xsl:value-of select="exists($next-section)"/>
prev: <xsl:value-of select="$prev-section/@id"/>
prev-path: <xsl:value-of select="$prev-path"/>
prev-exists: <xsl:value-of select="exists($prev-section)"/>
docs-count: <xsl:value-of select="count($section-docs/*)"/>
</xsl:message>
-->

<xsl:variable name="html-blocks" as="element()+">
<xsl:call-template name="process-hashchange">
<xsl:with-param name="hash-parts" select="$hash-parts"/>
<xsl:with-param name="hash-path" select="$hash-path" tunnel="yes"/>
</xsl:call-template>
</xsl:variable>

<!-- index.html should be created for an id of 'index' because this will clash
     with the 'intro' file name -->
<xsl:variable name="file-parts" as="xs:string*"
select="if ($hash-parts[last()] eq 'index') then
('html', subsequence($hash-parts, 1, count($hash-parts) - 1), 'index-1')
else ('html', $hash-parts)"/>

<xsl:variable name="filepath" as="xs:string"
select="for $p in string-join($file-parts,'/') return
if (not(section|article|def:function)) then concat($p, '.html')
else concat($p, '/index.html')"/>

<xsl:message>filepath: <xsl:value-of select="$filepath"/></xsl:message>
<xsl:variable name="redirect" select="concat($css-path, '#!', string-join($hash-parts, '/'))"/>

<xsl:result-document href="{$filepath}">
<html>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<title>Saxon Documentation</title>
<meta name="viewport" content="width=768px, minimum-scale=1.0, maximum-scale=1.0"/>
<link href="{concat($css-path, 'cetree.css')}" rel="stylesheet" type="text/css"/>

<!--
<script type="text/javascript">
    window.location = '<xsl:value-of select="$redirect"/>';
</script>
-->

</head>

<body>
<div id="wrap">
<div id="header" class="unselectable">

<!-- bread-crumb trail -->
<ul class="trail" id="trail">
<li id="trail1"><a href="http://saxonica.com">Saxonica ▷</a></li>
<li id="trail2"><a href="http://www.saxonica.com/documentation/documentation.xml">Saxon ▷</a></li>
<!-- add other items here -->
<xsl:sequence select="$html-blocks[name() = 'trail']/*"/>
</ul>

</div>

<div id="nav" class="unselectable">

<!-- logo and search -->
<div id="logo" style="">
<p class="cetree" style="padding-left:10px">Saxon</p>
<!--
<p class="arrowNone"></p>
-->
<p class="search"></p>
<!--
<input id="search" style="float:right;padding-top:2px;margin-top:3px;width:90px"/>
-->
</div>

<!-- tree-view -->
<ul id="list">
<xsl:sequence select="$html-blocks[name() = 'tree']/*"/>
</ul>

</div>

<!-- main page -->
<div id="main" style="float:left">
<xsl:sequence select="$html-blocks[name() = 'main']/*"/>
</div>

<!-- footnote anchored to bottom of display -->

<div id="footer">
<div id="info" class="footnote">
<p>Saxon Developer Guide (Non-JavaScript Version)</p>
</div>
<div>
<a href="{$next-path}"><p class="arrowRight"></p></a>
<a href="{$prev-path}"><p class="arrowLeft"></p></a>
</div>
</div>

</div>

</body>

</html>
</xsl:result-document>
</xsl:template>

<!--
<xsl:template name="show-fn">
<xsl:param name="span"/>
<xsl:variable name="href" select="$span/@data-href"/>
<xsl:variable name="fn"
select="$fndoc/def:functions/def:function[*:name = $href]"/>
<h4><xsl:value-of select="$href"/></h4>
<xsl:apply-templates select="$fn/def:description|$fn/def:signatures" mode="fn-description"/>
</xsl:template>

-->

<xsl:template name="process-hashchange" as="element()*">
<xsl:param name="hash-parts" as="xs:string*"/>
<xsl:param name="hash-path" as="xs:string" tunnel="yes"/>
<xsl:variable name="start" select="$hash-parts[1]"/>
<xsl:variable name="docName" select="resolve-uri(concat($start,'.xml'), base-uri())"/>
<xsl:variable name="doc" select="if (doc-available($docName)) then doc($docName) else ()"/>
<xsl:variable name="count" select="count($hash-parts)"/>
<xsl:choose>
<xsl:when test="exists($doc)">
<tree>
<xsl:call-template name="show-listitems">
<xsl:with-param name="doc" select="$doc"/>
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</tree>

<main>
<xsl:choose>

<xsl:when test="$start eq 'functions' and $count eq 1">
<xsl:apply-templates select="$doc/def:functions" mode="f"/>
</xsl:when>
<xsl:when test="$start eq 'functions' and $count gt 1">
<xsl:apply-templates select="$doc/def:functions/def:function[def:name eq $hash-parts[2]]" mode="f"/>
</xsl:when>

<xsl:when test="$count eq 1">
<xsl:apply-templates select="$doc" mode="primary"/>
</xsl:when>

<xsl:otherwise>
<xsl:call-template name="get-section">
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="parent" select="$doc/*"/>
<xsl:with-param name="index" select="2"/>
</xsl:call-template>
</xsl:otherwise>

</xsl:choose>
</main>


<trail>
<xsl:call-template name="get-trail">
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="parent" select="$doc"/>
<xsl:with-param name="index" select="1"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</trail>

</xsl:when>
<xsl:otherwise>
<xsl:message>Warning: doc or first-item not found
docName <xsl:value-of select="$docName"/>
docExists <xsl:value-of select="exists($doc)"/>
</xsl:message>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template name="get-section">
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="parent" as="node()?"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:variable name="section" select="$parent/section[@id eq $ids[$index]]"/>
<xsl:choose>
<xsl:when test="empty($section)">
<xsl:message>
<p>Get-Section Error in URI hash-path:</p>
<p>Section '<xsl:value-of select="$ids[$index]"/>' not found in path:
<xsl:value-of select="$ids" separator="/"/></p>
</xsl:message>
</xsl:when>
<xsl:when test="$index gt count($ids)"/>
<xsl:when test="$index eq count($ids)">
<xsl:apply-templates select="$section" mode="primary"/>
</xsl:when>
<xsl:otherwise>
<xsl:call-template name="get-section">
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="parent" select="$section"/>
<xsl:with-param name="index" select="$index + 1"/>
</xsl:call-template>
</xsl:otherwise>
</xsl:choose> 
</xsl:template>

<xsl:template name="get-section-element" as="element()?">
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="parent" as="node()?"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:variable name="section-all"
select="$parent/section[@id eq $ids[$index]]|
$parent/def:function[*:name eq $ids[$index]]"/>
<xsl:variable name="section"
select="$section-all[1]"/>
<xsl:if test="count($section-all) gt 1">
<xsl:message>
Warning: Ambiguous path - found <xsl:value-of select="count($section-all)"/> sections at id-path
'<xsl:value-of select="string-join($ids,'/')"/>'
 the path should reference only 1 section - first section found was used.
</xsl:message>
</xsl:if>
<xsl:choose>
<xsl:when test="count($ids) eq 1">
<xsl:sequence select="$parent"/>
</xsl:when>
<xsl:when test="empty($section)">
<xsl:message>
<p>GSE: Error in URI hash-path:</p>
<p>Section '<xsl:value-of select="$ids[$index]"/>' not found in path:
<xsl:value-of select="$ids" separator="/"/></p>
</xsl:message>
</xsl:when>
<xsl:when test="$index gt count($ids)"/>
<xsl:when test="$index eq count($ids)">
<xsl:sequence select="$section"/>
</xsl:when>
<xsl:otherwise>
<xsl:call-template name="get-section-element">
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="parent" select="$section"/>
<xsl:with-param name="index" select="$index + 1"/>
</xsl:call-template>
</xsl:otherwise>
</xsl:choose> 
</xsl:template>


<xsl:function name="f:get-item" as="node()">
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="item" as="node()"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:choose>
<xsl:when test="$index eq count($ids)">
<xsl:sequence select="$item"/>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="new-item" select="$item/ul/li[@id eq $ids[$index+1]]"/>
<xsl:sequence select="f:get-item($ids, $new-item, $index + 1)"/>
</xsl:otherwise>
</xsl:choose> 
</xsl:function>

<xsl:template name="get-trail">
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="parent" as="node()?"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:param name="hash-path" as="xs:string*"/>
<xsl:variable name="section" select="$parent/*[@id eq $ids[$index]]"/>
<xsl:variable name="ext" select="if ($section/(article|section|def:function)) then '' else '.html'" as="xs:string"/>
<xsl:variable name="href" as="xs:string"
select="concat(
$hash-path,
string-join(
subsequence($ids,1,$index),
'/'),
$ext)"/>
<xsl:choose>
<xsl:when test="$index gt count($ids)"/>
<xsl:when test="$index eq count($ids) and empty($section)">
<li id="{$section/@id}" class="trail">
<a href="{$href}">
<xsl:value-of select="$ids[$index]"/>
</a>
</li>
</xsl:when>
<xsl:when test="$index eq count($ids)">
<li id="{$section/@id}" class="trail">
<a href="{$href}">
<xsl:value-of select="$section/@title"/>
</a>
</li>
</xsl:when>
<xsl:otherwise>
<li id="{$section/@id}" class="trail">
<a href="{$href}">
<xsl:value-of select="$section/@title"/> &#x25b7;
</a>
</li>
<xsl:call-template name="get-trail">
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="parent" select="$section"/>
<xsl:with-param name="index" select="$index + 1"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</xsl:otherwise>
</xsl:choose> 
</xsl:template>

<xsl:template name="show-listitems">
<xsl:param name="doc" as="node()"/>
<xsl:param name="ids"/>
<xsl:param name="hash-path" as="xs:string*"/>

<xsl:variable name="id" select="$ids[1]"/>
<xsl:variable name="posn" as="xs:integer"
 select="for $sc in 1 to count($sections) return
 if ($sections[$sc]/@ref eq $id) then $sc
 else ()"/>

<xsl:for-each select="$sections[position() lt $posn]">
<li id="@ref" class="closed">
<!--
<span class="item"><xsl:value-of select="."/></span>
-->
<a class="item" href="{concat($hash-path,@ref)}"><xsl:value-of select="."/></a>
</li>
</xsl:for-each>
<xsl:for-each select="$sections[position() eq $posn]">
<li id="{@ref}" class="open">
<!--
<span class="{if (count($ids) eq 1) then 'hot' else 'item'}"><xsl:value-of select="."/></span>
-->
<a class="{if (count($ids) eq 1) then 'hot' else 'item'}" href="{concat($hash-path,@ref)}"><xsl:value-of select="."/></a>
<xsl:choose>
<xsl:when test="$id eq 'functions'">
<xsl:call-template name="add-list-fn">
<xsl:with-param name="top-name" select="."/>
<xsl:with-param name="fn-name" select="$ids[2]"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</xsl:when>
<xsl:otherwise>
<xsl:call-template name="add-list">
<xsl:with-param name="section" select="$doc/*[@id eq $id]"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="1"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</xsl:otherwise>
</xsl:choose>
</li>
</xsl:for-each>
<xsl:for-each select="$sections[position() gt $posn]">
<li id="{@ref}" class="closed">
<!--
<span class="item"><xsl:value-of select="."/></span>
-->
<a class="item" href="{concat($hash-path,@ref)}"><xsl:value-of select="."/></a>
</li>
</xsl:for-each>
</xsl:template> 

<xsl:template name="add-list">
<xsl:param name="section" as="node()"/>
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:param name="hash-path" as="xs:string*"/>
<xsl:if test="exists($section/section)">
<ul>
<!--
<span>ids: <xsl:value-of select="string-join($ids,'/')"/></span>
-->
<xsl:for-each select="$section/section">
<xsl:variable name="onpath" as="xs:boolean*"
select="$index lt count($ids) and @id eq $ids[$index + 1]"/>
<xsl:variable name="contains" select="exists(section)"/>
<li id="{@id}">
<xsl:attribute name="class"
select="if ($onpath and $contains) then 'open'
else if ($contains) then 'closed'
else 'empty'"/>
<!--
<span class="{if ($onpath and count($ids) - $index = 1) then 'hot'
else 'item'}"><xsl:value-of select="@title"/></span>
-->
<xsl:variable name="ext" select="if ($contains) then '' else '.html'" as="xs:string"/>
<a class="{if ($onpath and count($ids) - $index = 1) then 'hot'
else 'item'}"

href="{
concat(
$hash-path,
string-join(
insert-before(@id, 1, (for $sc in 1 to $index return $ids[$sc])),
'/'),
$ext)
}">

<xsl:value-of select="@title"/>
</a>

<xsl:if test="$onpath">
<xsl:call-template name="add-list">
<xsl:with-param name="section" select="$section/section[@id = $ids[$index + 1]]"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="$index + 1"/>
<xsl:with-param name="hash-path" select="$hash-path"/>
</xsl:call-template>
</xsl:if>
</li>
</xsl:for-each>
</ul>
</xsl:if>
</xsl:template>

<xsl:template name="add-list-fn">
<xsl:param name="top-name" as="xs:string"/>
<xsl:param name="fn-name" as="xs:string*"/>
<xsl:param name="hash-path" as="xs:string*"/>
<xsl:if test="exists($fn-name)">
<ul>
<li id="{$fn-name}" class="empty">
<!--
<span class="item">[<xsl:value-of select="$fn-name"/>]</span>
-->
<a class="hot"
href="{concat($hash-path, 'functions/',$fn-name)}">[<xsl:value-of select="$fn-name"/>]</a>
</li>
</ul>
</xsl:if>
</xsl:template>

<xsl:function name="f:get-open-class" as="xs:string">
<xsl:param name="section" as="element()"/>
<xsl:sequence select="if ($section/section) then 'open'
 else 'empty'"/>
</xsl:function>


</xsl:transform>
