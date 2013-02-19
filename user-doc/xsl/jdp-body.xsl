<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
version="2.0"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:f="urn:internal.function"
extension-element-prefixes="ixsl"
>

<xsl:template match="node()|@*" mode="jdp">
<xsl:copy>
<xsl:apply-templates mode="jdp" select="node()|@*"/>
</xsl:copy>
</xsl:template>

<xsl:template match="a" mode="jdp">
<xsl:copy>
<xsl:attribute name="target" select="'_blank'"/>
<xsl:apply-templates mode="jdp" select="node()|@* except @target"/>
</xsl:copy>
</xsl:template>

<xsl:template match="a[@class = 'javalink']" mode="jdp">
<span id="{@href}" class="javalink" data-href="{@href}">
<xsl:value-of select="f:last-substring-after(. , 46)"/>
</span>
</xsl:template>

<xsl:template match="section" mode="pkg-header">
<h1><xsl:value-of select="@id"/></h1>
<xsl:apply-templates mode="jdp" select="p[1]"/>
<xsl:if test="count(p) gt 1">
<p><em>(More details at foot of page)</em></p>
</xsl:if>
</xsl:template>

<xsl:template match="body" mode="smy">
<xsl:apply-templates mode="jdp"/>
</xsl:template>

<xsl:variable name="s-titles"
select="('Interface','Class', 'Enum','Exception')" as="xs:string+"/>

<xsl:template match="document-node()" mode="summarise-pkg">
<xsl:variable name="classes" select="*/class"/>
<xsl:for-each select="1 to 4">
<xsl:variable name="i" select="." as="xs:integer"/>
<xsl:variable name="f-classes"
select="if ($i eq 1) then $classes[@interface='true']
else if ($i eq 2) then
    $classes[not(
        @interface='true'
        or @superclass='Enum'
        or ends-with(@superclass,'Exception'))]
else if ($i eq 3) then $classes[@superclass='Enum']
else $classes[ends-with(@superclass,'Exception')]"/>
<xsl:if test="exists($f-classes)">
<table border="1" style="width:100%">
<thead>
<tr>
<td colspan="2" style="text-align:center">
<h3><xsl:value-of select="concat($s-titles[$i],' Summary')"/></h3>
</td>
</tr>
<tr>
<td><p>Name</p></td>
<td><p>Description</p></td>
</tr>
</thead>
<tbody>
<xsl:for-each select="$f-classes">
<tr>
<td><xsl:sequence select="f:showType(. , true(), true())"/></td>
<td><p><xsl:apply-templates select="comment/sentence/body/node()" mode="jdp"/></p></td>
</tr>
</xsl:for-each>
</tbody>
</table>

<p>&#160;</p>
</xsl:if>
</xsl:for-each>
</xsl:template>

<xsl:template match="article" mode="jdp">
<h1>Saxon9 Java API Documentation</h1>
<xsl:variable name="article" select="."/>
<xsl:variable name="titles"
select="('s9api Interface','Other Interfaces', 'External Interfaces'
,'Other Packages')" as="xs:string+"/>

<xsl:variable name="p1" select="'net.sf.saxon.s9api'" />
<xsl:variable name="p2" select="'com.saxonica.config','com.saxonica.jaxp','com.saxonica.schema',
'net.sf.saxon','net.sf.saxon.lib', 'net.sf.saxon.om',
'net.sf.saxon.query','net.sf.saxon.sxpath','net.sf.saxon.type',
'net.sf.saxon.type','net.sf.saxon.value','net.sf.xaxon.xpath',
'net.sf.saxon.xqj'
"/>
<xsl:variable name="p3" select="'javax.xml.xquery'"/>
<xsl:variable name="p4" select="$article/section[not(@id = ($p1, $p2, $p3))]/@id"/>


<xsl:for-each select="1 to 4">
<xsl:variable name="i" select="." as="xs:integer"/>
<table border="1" style="width:100%">
<thead>
<tr>
<td colspan="2" style="text-align:center">
<h3><xsl:value-of select="$titles[$i]"/></h3>
</td>
</tr>
<tr>
<td><p>Package</p></td>
<td><p>Description</p></td>
</tr>
</thead>
<tbody>
<xsl:for-each
select="$article/section[@id = 
(if ($i eq 1) then $p1
else if ($i eq 2) then $p2
else if ($i eq 3) then $p3
else $p4)]">
<tr>
<!--
<td><p class="javapackage"><xsl:value-of select="@id"/></p></td>
-->
<td>
<span class="javalink" data-href="{@id}"><xsl:value-of select="@id"/></span>
</td>

<td><p><xsl:apply-templates select="p[1]" mode="jdp"/></p></td>
</tr>
</xsl:for-each>
</tbody>
</table>

<p>&#160;</p>
</xsl:for-each>

</xsl:template>

</xsl:transform>

