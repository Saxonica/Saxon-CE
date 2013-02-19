<?xml version="1.0" encoding="utf-8"?>
<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:fn="http://www.saxonica.com/ns/doc/functions" 
version="2.0">

<xsl:function name="fn:path-prefix" as="xs:string">
<xsl:param name="node" as="element()"/>
<xsl:sequence
select="if ($node/(/table)) then '../' (: generated feature keys :)
else if ($node/ancestor::subpage) then '../../'
else if ($node/ancestor::page) then '../'
else ''"
/>
</xsl:function>

<xsl:template match="node()|@*" mode="f">
<xsl:copy>
<xsl:apply-templates select="node()|@*" mode="f"/>
</xsl:copy>
</xsl:template>

<!--
<xsl:template match="a" mode="f">
<xsl:choose>
<xsl:when test="substring(@href, 1, 5) = ('file:','http:')">
<xsl:copy-of select="."/>
</xsl:when>
<xsl:otherwise>
<span class="link" data-href="{@href}">
<xsl:apply-templates select="node()" mode="secondary"/>
</span>
</xsl:otherwise>
</xsl:choose>
</xsl:template>
-->

<xsl:template match="a" mode="f">
<xsl:apply-templates select="." mode="secondary"/>
</xsl:template>

<xsl:template match="code[@java]" mode="f">
<xsl:apply-templates select="." mode="secondary"/>
</xsl:template>

<!--
<xsl:template match="code[@java]" mode="f">
<xsl:variable name="link" select="translate(@java, '.', '/')"/>
<xsl:variable name="file" select="if (contains($link, '#')) 
then concat(substring-before($link, '#'), '.html#', substring-after($link, '#'))
else concat($link, '.html')"/>

<a class="bodylink" href="{fn:path-prefix(.)}javadoc/{$file}">
<code><xsl:apply-templates/></code>
</a>
</xsl:template>
-->

<xsl:template match="code[pre]" mode="f">
<code><xsl:apply-templates select="pre/*"/></code>
</xsl:template>

<xsl:template match="code" mode="f">
<code><xsl:apply-templates/></code>
</xsl:template>

<xsl:template match="pre" mode="f">
<div class="codeblock">
<xsl:next-match/>
</div>
</xsl:template>

<xsl:template match="example" priority="2" mode="f">
<samp><xsl:apply-templates/></samp>
</xsl:template>

<xsl:template match="box" priority="2" mode="f">
<div class="boxed">
<xsl:apply-templates/>
</div>
</xsl:template>

<xsl:template match="status-ok" mode="f">
<p>The function is fully implemented according to the W3C specifications.</p>
</xsl:template>

<xsl:template match="notes" mode="f">
<p>
<b>Note:</b>
<xsl:apply-templates mode="f"/>
</p>
</xsl:template>

</xsl:transform>
