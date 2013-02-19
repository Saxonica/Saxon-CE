<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:js="http://saxonica.com/ns/globalJS"
xmlns:style="http://saxonica.com/ns/html-style-property"
xmlns:f="urn:internal.function"
xmlns:def="http://www.saxonica.com/ns/doc/functions"
extension-element-prefixes="ixsl"
xmlns:svg="http://www.w3.org/2000/svg"
xmlns:fx="http://local/functions"
xmlns:cat="http://www.saxonica.com/ns/doc/catalog"
version="2.0"
>
<xsl:import href="body.xsl"/>
<xsl:import href="checklinks.xsl"/>
<xsl:import href="findtext.xsl"/>
<xsl:import href="functions-body.xsl"/>
<xsl:import href="functions-data.xsl"/>
<xsl:import href="jdp-body.xsl"/>
<xsl:import href="jdc-body.xsl"/>

<xsl:variable name="location" select="'../doc'"/>
<xsl:variable name="fndoc"
select="doc(concat($location, '/functions.xml'))" as="document-node()"/>
<xsl:variable name="jd-path" select="'../javadoc-xml/javadoc-tree.xml'"/>
<xsl:variable name="jd-search" select="'../javadoc-xml/javadoc-types.xml'"/>
<xsl:variable name="jpackage-path" select="'../javadoc-xml/javadoc-packages.xml'"/>
<xsl:variable name="jclass-path" select="'../javadoc-xml/packages/'"/>
<xsl:variable name="jdoctree" select="doc($jd-path)" as="document-node()"/>
<xsl:variable name="navlist" as="node()"
 select="ixsl:page()/html/body/div/div[@id = 'nav']"/>
<xsl:variable name="args" select="f:parse-uri()"/>
<xsl:variable name="usesclick" select="js:usesClick()" as="xs:boolean"/>

<xsl:template name="main">

<xsl:result-document href="#list" method="replace-content">
<xsl:apply-templates select="doc(concat($location, '/catalog.xml'))/cat:catalog/cat:section"/>
</xsl:result-document>

<ixsl:schedule-action wait="1">
<xsl:call-template name="init"/>
</ixsl:schedule-action>
</xsl:template>

<xsl:template name="init">
<xsl:call-template name="show-tools"/>
<xsl:call-template name="process-hashchange"/>
</xsl:template>

<xsl:template match="cat:section">
<li class="closed" id="{@ref}">
<span class="item"><xsl:value-of select="."/></span>
</li>
</xsl:template>


<xsl:template match="ixsl:window()" mode="ixsl:onhashchange">
<xsl:call-template name="process-hashchange"/>
</xsl:template>

<xsl:template match="*" mode="ixsl:onkeydown" ixsl:prevent-default="yes" ixsl:event-property="keyCode 13 33 34">
<xsl:variable name="event" select="ixsl:event()"/>
<xsl:variable name="keycode" select="ixsl:get($event,'keyCode')" as="xs:double"/>
<xsl:variable name="class" select="if ($keycode eq 33) then 'arrowLeft'
else if ($keycode eq 34) then 'arrowRight'
else if ($keycode eq 13) then 'enter'
else ()"/>
<xsl:if test="exists($class)">
<xsl:choose>
<xsl:when test="$class eq 'enter'">
<xsl:call-template name="run-search"/>
</xsl:when>
<xsl:otherwise>

<!--
<xsl:sequence select="ixsl:call(ixsl:event(),'preventDefault')"/>

-->
<xsl:sequence select="f:navpage($class)"/>
</xsl:otherwise>
</xsl:choose>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class eq 'arrowNone']" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:for-each select="$navlist/ul/li">
<ixsl:set-attribute name="class" select="'closed'"/>
</xsl:for-each>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class eq 'arrowNone']" mode="ixsl:ontouchend">
<xsl:for-each select="$navlist/ul/li">
<ixsl:set-attribute name="class" select="'closed'"/>
</xsl:for-each>
</xsl:template>

<xsl:template match="span[@data-href]|svg:g[@data-href]" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:sequence select="f:anchor-navigation(.)"/>
</xsl:if>
</xsl:template>

<xsl:template match="span[@data-href]" mode="ixsl:onmouseover">
<xsl:if test="@class eq 'flink'">
<ixsl:schedule-action wait="200">
<xsl:call-template name="show-fn">
<xsl:with-param name="span" select="."/>
</xsl:call-template>
</ixsl:schedule-action>
</xsl:if>
</xsl:template>

<xsl:template name="show-fn">
<xsl:param name="span"/>
<xsl:variable name="href" select="$span/@data-href"/>
<xsl:variable name="fn"
select="$fndoc/def:functions/def:function[*:name = $href]"/>
<xsl:result-document href="#fn-desc" method="replace-content">
<h4><xsl:value-of select="$href"/></h4>
<xsl:apply-templates select="$fn/def:description|$fn/def:signatures" mode="fn-description"/>
</xsl:result-document>
</xsl:template>

<xsl:template match="span[@data-href]|svg:g[@data-href]" mode="ixsl:ontouchend">
<xsl:sequence select="f:anchor-navigation(.)"/>
</xsl:template>

<xsl:template name="scrollpage">
<xsl:param name="id"/>
<xsl:variable name="subnode" select="(ixsl:page()/html/body/div[@id = 'wrap']/div[@id = 'main']/div[@class = 'section']/div[@class = 'method']/h3[. eq $id])[1]" as="node()?"/>

<xsl:choose>
<xsl:when test="exists($subnode)">
<xsl:sequence select="js:scrollToElement($subnode)"/>
<xsl:for-each select="$subnode">
<ixsl:set-attribute name="class" select="'hot'"/>
</xsl:for-each>
</xsl:when>
<xsl:otherwise>
<ixsl:set-property object="ixsl:page()/html/body/div[@id = 'wrap']/div[@id = 'main']" name="scrollTop" select="0"/>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:function name="f:scrollpage">
<xsl:param name="id"/>
<ixsl:schedule-action wait="1">
<xsl:call-template name="scrollpage">
<xsl:with-param name="id" select="$id"/>
</xsl:call-template>
</ixsl:schedule-action>
</xsl:function>


<xsl:template name="process-hashchange">
<xsl:variable name="hash-parts" select="tokenize(f:get-hash(),'/')"/>
<xsl:variable name="start" select="$hash-parts[1]"/>
<xsl:variable name="docName" select="if ($start eq 'javadoc') then $jd-path
else concat($location, '/', $start,'.xml')"/>
<xsl:variable name="doc" select="if (doc-available($docName)) then doc($docName) else ()"/>
<xsl:variable name="first-item" select="f:get-first-item($start)" as="node()?"/>


<xsl:choose>
<xsl:when test="exists($doc) and exists($first-item)">
<xsl:call-template name="show-listitems">
<xsl:with-param name="doc" select="$doc"/>
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="index" select="1"/>
<xsl:with-param name="item" select="$first-item"/>
</xsl:call-template>
<xsl:variable name="count" select="count($hash-parts)"/>
<xsl:variable name="jdocpath" select="if ($start eq 'javadoc' and $count gt 1)
then concat($jclass-path, $hash-parts[2],'.xml')
else ()"/>
<xsl:variable name="jpackageDoc" select="if (exists($jdocpath) and doc-available($jdocpath))
then doc($jdocpath)
else ()"/>
<xsl:variable name="hp" select="$hash-parts[$count]"/>
<xsl:variable name="isjavadoc" select="$start eq 'javadoc'" as="xs:boolean"/>
<xsl:variable name="subpage" select="if ($isjavadoc) then substring-after($hp, '@') else ''"/>
<xsl:variable name="hpj" select="if ($subpage ne '') then
substring($hp , 1, (string-length($hp) - string-length($subpage)) - 1)
else $hp"/>
<xsl:result-document href="#main" method="replace-content">
<xsl:choose>
<xsl:when test="$isjavadoc and $count eq 1">
<xsl:apply-templates mode="jdp"
select="doc($jpackage-path)/article" />
</xsl:when>
<xsl:when test="$isjavadoc and $count eq 2">
<xsl:if test="empty($jpackageDoc)">
<xsl:sequence select="f:pkgnotfound($hash-parts)"/>
</xsl:if>
<xsl:for-each select="doc($jpackage-path)/article/section
[@id eq $hpj]">
<xsl:apply-templates mode="pkg-header" select="." />
<xsl:apply-templates mode="summarise-pkg" select="$jpackageDoc"/>
<xsl:apply-templates mode="jdp" select="." />
</xsl:for-each>

</xsl:when>
<xsl:when test="$isjavadoc and $count gt 2">
<xsl:variable name="showclass"
select="if ($count eq 3) then $jpackageDoc/package/class[@id eq $hpj]
else $jpackageDoc/package/class/class[@id eq concat($hash-parts[$count - 1],'.',
$hpj)]"/>
<xsl:if test="empty($showclass)">
<xsl:sequence select="f:classnotfound($hash-parts)"/>
</xsl:if>
<xsl:apply-templates
select="$showclass" mode="show-class"/>

</xsl:when>
<xsl:when test="$start eq 'functions' and $count eq 1">
<xsl:apply-templates select="$doc/def:functions" mode="f"/>
</xsl:when>
<xsl:when test="$start eq 'functions' and $count gt 1">
<xsl:apply-templates select="$doc/def:functions/def:function[def:name eq $hp]" mode="f"/>
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

<xsl:sequence select="f:scrollpage($subpage)"/>

</xsl:result-document>


<xsl:result-document href="#trail" method="replace-content">
<xsl:variable name="ul" select="ixsl:page()/html/body/div/div[@id = 'header']/ul[@class='trail']"/>
<xsl:copy-of select="$ul/li[@id eq 'trail1']"/>
<xsl:copy-of select="$ul/li[@id eq 'trail2']"/>
<xsl:call-template name="get-trail">
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="parent" select="$doc"/>
<xsl:with-param name="index" select="1"/>
</xsl:call-template>
</xsl:result-document>

<ixsl:schedule-action wait="1">
<xsl:call-template name="highlight-item">
<xsl:with-param name="parent" select="$navlist"/>
<xsl:with-param name="ids" select="$hash-parts"/>
<xsl:with-param name="index" select="1"/>
</xsl:call-template>
</ixsl:schedule-action>
</xsl:when>
<xsl:otherwise>
<xsl:result-document href="#main" method="replace-content">
<h1>Page Not Found</h1>
<p>Error in URI hash-path:</p>
<p><xsl:value-of select="if (exists($doc)) then ('List Item ''', $start)
else ('Document ''', $docName)"/>' not found</p>
</xsl:result-document>
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
<p>Error in URI hash-path:</p>
<p>Section '<xsl:value-of select="$ids[$index]"/>' not found in path:
<xsl:value-of select="$ids" separator="/"/></p>
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


<xsl:template match="span[@class eq 'item']|li[@class = ('closed','open','empty')]"
mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:apply-templates select="." mode="handle-itemclick"/>
</xsl:if>
</xsl:template>

<xsl:template match="span[@class eq 'item']|li[@class = ('closed','open','empty')]"
mode="ixsl:ontouchend">
<xsl:apply-templates select="." mode="handle-itemclick"/>
</xsl:template>

<xsl:template match="*" mode="handle-itemclick">
<xsl:variable name="ids" select="(., ancestor::li)/@id" as="xs:string*"/>
<xsl:variable name="new-hash" select="string-join($ids, '/')"/>
<xsl:variable name="isSpan" select="@class eq 'item'" as="xs:boolean"/>
<xsl:for-each select="if ($isSpan) then .. else .">
<xsl:choose>
<xsl:when test="@class eq 'open' and not($isSpan)">
<ixsl:set-attribute name="class" select="'closed'"/>
</xsl:when>
<xsl:otherwise>
<xsl:sequence select="js:disableScroll()"/>
<xsl:choose>
<xsl:when test="f:get-hash() eq $new-hash">
<xsl:variable name="new-class" select="f:get-open-class(@class)"/>
<ixsl:set-attribute name="class" select="$new-class"/>
<xsl:if test="empty(ul)">
<xsl:call-template name="process-hashchange"/>
</xsl:if>
</xsl:when>
<xsl:otherwise>
<xsl:sequence select="f:set-hash($new-hash)"/>
</xsl:otherwise>
</xsl:choose>
</xsl:otherwise>
</xsl:choose>
</xsl:for-each>
</xsl:template>

<xsl:template match="li[@class eq 'trail']" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:sequence select="f:crumb-navigation(.)"/>
</xsl:if>
</xsl:template>

<xsl:template match="li[@class eq 'trail']" mode="ixsl:ontouchend">
<xsl:sequence select="f:crumb-navigation(.)"/>
</xsl:template>

<xsl:template match="p[@class = ('arrowLeft','arrowRight')]" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:sequence select="f:navpage(@class)"/>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class = ('arrowLeft','arrowRight')]" mode="ixsl:ontouchend">
<xsl:sequence select="f:navpage(@class)"/>
</xsl:template>

<xsl:function name="f:navpage">
<xsl:param name="class"/>
<ixsl:schedule-action wait="16">
<xsl:call-template name="navpage">
<xsl:with-param name="class" select="$class"/>
</xsl:call-template>
</ixsl:schedule-action>
</xsl:function>

<xsl:template name="navpage">
<xsl:param name="class" as="xs:string"/>
<xsl:variable name="ids" select="tokenize(f:get-hash(),'/')"/>
<xsl:variable name="start" select="$ids[1]"/>
<xsl:variable name="push" as="xs:string">
<xsl:choose>
<xsl:when test="$start eq 'functions' and count($ids) gt 1">
<xsl:variable name="fns" as="element()*"
 select="fx:fn-list($fndoc/def:functions)"/>
<xsl:variable name="fnCount" select="count($fns)" as="xs:integer"/>
<xsl:variable name="posn" as="xs:integer"
select="for $a in 1 to $fnCount return
if ($fns[$a]/*:name eq $ids[2]) then $a
else ()"/>
<xsl:variable name="new-posn" as="xs:integer"
select="if ($class eq 'arrowLeft') then
    if ($posn gt 1) then $posn - 1 else 1
    else if ($posn lt $fnCount) then $posn + 1 else $fnCount"/>
<xsl:sequence select="concat('functions/',$fns[$new-posn]/*:name)"/>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="c" as="node()"
select="f:get-item($ids, f:get-first-item($start), 1)"/>
<xsl:variable name="new-li"
select="if ($class eq 'arrowLeft') then
($c/preceding::li[1] union $c/parent::ul/parent::li)[last()]
else ($c/ul/li union $c/following::li)[1]"/>

<xsl:sequence select="string-join(($new-li/ancestor::li union $new-li)/@id,'/')"/>
</xsl:otherwise>
</xsl:choose>
</xsl:variable>

<xsl:sequence select="f:set-hash($push)"/>
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
<xsl:variable name="section" select="$parent/*[@id eq $ids[$index]]"/>
<xsl:variable name="title" select="if ($section/@title) then $section/@title else $section/@id"/>
<xsl:choose>

<xsl:when test="$index gt count($ids)"/>
<xsl:when test="$index eq count($ids) and empty($section)">
<xsl:variable name="all" select="$ids[$index]"/> 
<xsl:variable name="pre" select="substring-before($all, '@')"/>

<xsl:choose>
<xsl:when test="$pre eq ''">
<li id="{$section/@id}" class="trail"><xsl:value-of select="$all"/></li>
</xsl:when>
<xsl:otherwise>
<li id="{$pre}" class="trail"><xsl:value-of select="$pre"/> &#x25b7;</li>
<li class="trail">
<xsl:value-of select="substring($all, string-length($pre) + 2)"/>
</li>
</xsl:otherwise>
</xsl:choose>
</xsl:when>
<xsl:when test="$index eq count($ids)">
<li id="{$section/@id}" class="trail"><xsl:value-of select="$title"/></li>
</xsl:when>
<xsl:otherwise>
<li id="{$section/@id}" class="trail">
<xsl:value-of select="$title"/> &#x25b7;</li>
<xsl:call-template name="get-trail">
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="parent" select="$section"/>
<xsl:with-param name="index" select="$index + 1"/>
</xsl:call-template>
</xsl:otherwise>
</xsl:choose> 
</xsl:template>

<xsl:template name="show-listitems">
<xsl:param name="doc" as="node()"/>
<xsl:param name="ids"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:param name="item" as="node()?"/>
<xsl:variable name="id" select="$ids[$index]"/>

<xsl:for-each select="$item">
<ixsl:set-attribute name="class" select="f:get-open-class(@class)"/>
<xsl:choose>
<xsl:when test="$index eq 1 and $item/@id eq 'functions'">
<xsl:result-document href="?select=." method="replace-content">
<xsl:call-template name="add-list-fn">
<xsl:with-param name="top-name" select="span"/>
<xsl:with-param name="fn-name" select="$ids[2]"/>
</xsl:call-template>
</xsl:result-document>
</xsl:when>
<xsl:when test="not(empty(ul))">
<xsl:if test="$index lt count($ids)">
<xsl:call-template name="show-listitems">
<xsl:with-param name="doc" select="$doc/*[@id eq $id]"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="$index + 1"/>
<xsl:with-param name="item" select="ul/li[@id eq $ids[$index + 1]]"/>
</xsl:call-template>
</xsl:if>
</xsl:when>
<xsl:otherwise>
<xsl:result-document href="?select=." method="append-content">
<xsl:call-template name="add-list">
<xsl:with-param name="section" select="$doc/*[@id eq $id]"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="$index"/>
</xsl:call-template>
</xsl:result-document>
</xsl:otherwise>
</xsl:choose>
</xsl:for-each>
</xsl:template>

<xsl:template name="add-list">
<xsl:param name="section" as="node()"/>
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="index" as="xs:integer"/>

<xsl:if test="exists($section/(section|j))">
<ul>
<xsl:for-each select="$section/(section|j)">
<xsl:variable name="onpath" as="xs:boolean*"
select="$index lt count($ids) and @id eq $ids[$index + 1]"/>
<xsl:variable name="contains" select="exists(section|j)"/>
<li id="{@id}">
<xsl:attribute name="class"
select="if ($onpath and $contains) then 'open'
else if ($contains) then 'closed'
else 'empty'"/>
<span class="item"><xsl:value-of select="if (@title) then @title else @id"/></span>
<xsl:if test="$onpath">
<xsl:call-template name="add-list">
<xsl:with-param name="section" select="$section/(section|j)[@id = $ids[$index + 1]]"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="$index + 1"/>
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
<span class="item"><xsl:value-of select="$top-name"/></span>
<xsl:if test="exists($fn-name)">
<ul>
<li id="{$fn-name}" class="empty">
<span class="item">[<xsl:value-of select="$fn-name"/>]</span>
</li>
</ul>
</xsl:if>
</xsl:template>

<xsl:template name="highlight-item">
<xsl:param name="parent" as="node()?"/>
<xsl:param name="ids" as="xs:string*"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:variable name="hitem" select="$parent/ul/li[@id eq $ids[$index]]"/>
<xsl:choose>
<xsl:when test="$index lt count($ids)">

<!--
<xsl:when test="$index lt count($ids) and not($index eq 1 and $hitem/@id eq 'functions')">
-->
<xsl:call-template name="highlight-item">
<xsl:with-param name="parent" select="$hitem"/>
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="index" select="$index + 1"/>
</xsl:call-template>
</xsl:when>
<xsl:otherwise>
<xsl:for-each select="$hitem/span">
<xsl:for-each select="js:swapItem(.)">
<ixsl:set-attribute name="class" select="'item'"/>
</xsl:for-each>
<xsl:sequence select="js:enableScroll()"/>
<ixsl:set-attribute name="class" select="'hot'"/>
</xsl:for-each>
<xsl:if test="$navlist/../div[@class eq 'found']/@style:display ne 'none'">
<xsl:sequence select="f:highlight-finds()"/>
</xsl:if>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:function name="f:crumb-navigation">
<xsl:param name="c" as="node()"/>
<xsl:variable name="seq" select="subsequence($c/preceding-sibling::*/@id, 3)|$c/@id" as="xs:string*"/>
<xsl:variable name="new-hash" select="string-join($seq,'/')"/>
<xsl:sequence select="f:set-hash($new-hash)"/>
</xsl:function>

<xsl:function name="f:anchor-navigation">
<xsl:param name="c" as="node()"/>
<xsl:variable name="href">
<xsl:choose>
<xsl:when test="$c/@class = 'javalink'">
<xsl:variable name="href" select="$c/@data-href"/>
<xsl:variable name="ref" select="for $a in substring-before($href,'(') return
if ($a eq '') then $href else $a"/>
<xsl:variable name="pageref" select="if (ends-with($ref, ']')) then substring-before($ref, '[') else $ref"/>
<xsl:variable name="tokens" select="tokenize($pageref,'\.')" as="xs:string*"/>
<xsl:variable name="paths" as="xs:string*"
select="for $t in 1 to count($tokens),
$ch in substring($tokens[$t],1,1) return
if (upper-case($ch) eq $ch) then
concat('/',$tokens[$t]) else concat('.',$tokens[$t])
"/>
<xsl:value-of select="concat('javadoc/',
substring(string-join($paths,''),2))"/>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="ahref"
select="resolve-uri($c/@data-href, concat('http://a.com/', f:get-hash(),'/'))"/>
<xsl:value-of select="substring($ahref, 14)"/>
</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<xsl:sequence select="f:set-hash(translate($href, '#','@'))"/>
</xsl:function>

<xsl:function name="f:set-hash">
<xsl:param name="hash"/>
<ixsl:set-property name="location.hash" select="concat('!',$hash)"/>
</xsl:function>

<xsl:function name="f:get-open-class" as="xs:string">
<xsl:param name="class" as="xs:string"/>
<xsl:sequence select="if ($class eq 'empty') then 'empty'
 else 'open'"/>
</xsl:function>

<xsl:function name="f:get-first-item" as="node()?">
<xsl:param name="start"/>
<xsl:sequence select="$navlist/ul/li[@id = $start]"/>
</xsl:function>

<!-- hash is prefixed with ! as the 'hashbang' SEO measure: eg. http:/a.com#!about/gwt -->
<xsl:function name="f:get-hash">
<xsl:variable name="hash"
select="substring(ixsl:get(ixsl:window() , 'location.hash'),3)"/>
<xsl:sequence select="if (string-length($hash) gt 0)
then $hash else ($navlist/ul/li)[1]/@id"/>
</xsl:function>

<xsl:template name="show-tools">
<xsl:if test="$args/@*[name() eq 'test'] eq 'ON'">
<xsl:for-each select="ixsl:page()/html/body/div/div[@id eq 'footer']
/div[@id eq 'test']">
<ixsl:set-attribute name="style:display" select="'block'"/>
</xsl:for-each>
<xsl:for-each select="$navlist">
<ixsl:set-attribute name="style:bottom" select="'210px'"/>
</xsl:for-each>
<xsl:for-each select="$navlist/../div[@id eq 'main']">
<ixsl:set-attribute name="style:bottom" select="'210px'"/>
</xsl:for-each>
</xsl:if>
</xsl:template>

<xsl:function name="f:parse-uri">
<args>
<xsl:analyze-string regex="([^=&amp;]+)=([^&amp;]*)"
select="substring(ixsl:get(ixsl:window(), 'location.search'),2)">
<xsl:matching-substring>
<xsl:attribute name="{regex-group(1)}" select="regex-group(2)"/>
</xsl:matching-substring>
</xsl:analyze-string>
</args>
</xsl:function>

</xsl:transform>
