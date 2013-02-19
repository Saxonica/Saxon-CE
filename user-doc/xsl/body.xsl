<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
version="2.0"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
extension-element-prefixes="ixsl"
>

<xsl:template match="section|article" mode="primary">
<xsl:variable name="title" select="(h1|h2|h3)[1]"/>
<xsl:if test="exists($title)">
<ixsl:set-property name="document.title" select="concat('Saxon ', $title)"/>
</xsl:if>
<xsl:apply-templates mode="secondary"/>
</xsl:template>

<xsl:template match="node()|@*" mode="secondary">
<xsl:copy>
<xsl:apply-templates select="node()|@*" mode="secondary"/>
</xsl:copy>
</xsl:template>

<xsl:template match="section|article" mode="secondary"/>

<xsl:template match="dfn" mode="secondary">
<xsl:apply-templates/>
</xsl:template>

<xsl:template match="nav[ul]" mode="secondary">
<ul>
<xsl:for-each select="../(article|section)">
<li>
<p>
<span class="link" data-href="{@id}">
<xsl:value-of select="@title"/>
</span>
<xsl:if test="@summary">
<xsl:value-of select="':', @summary" separator=" "/>
</xsl:if>
</p>
</li>
</xsl:for-each>
</ul>
</xsl:template>

<xsl:template match="img[@src]" mode="secondary">
<xsl:variable name="src" select="if (starts-with(@src, '/')) then substring(@src, 2) else ."/>
<xsl:message>found @src <xsl:value-of select="$src"/></xsl:message>
<img src="{$src}">
<xsl:apply-templates select="(@* except @src)" mode="secondary"/>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="a[@class = 'apilink']" mode="secondary">
<xsl:copy-of select="."/>
</xsl:template>

<xsl:template match="a[@class ='javalink']" mode="secondary">
<span data-href="{@href}">
<xsl:copy-of select="@* except @href"/>
<xsl:apply-templates/>
</span>
</xsl:template>

<xsl:template match="a" mode="secondary">
<xsl:choose>
<xsl:when test="substring(@href, 1, 5) = ('file:','http:')
or starts-with(@href, 'https:')">
<xsl:copy-of select="."/>
</xsl:when>
<xsl:when test="substring-after(@href, '.') = ('zip','pdf','txt','xsl','xsd','xml')">
<xsl:variable name="href" select="if (starts-with(@href, '/')) then substring(@href, 2) else @href"/>
<a href="{$href}">
<xsl:apply-templates select="@* except @href" mode="secondary"/>
<xsl:apply-templates/>
</a>
</xsl:when>
<xsl:otherwise>
<span class="link" data-href="{@href}">
<xsl:apply-templates select="node()" mode="secondary"/>
</span>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

</xsl:transform>
