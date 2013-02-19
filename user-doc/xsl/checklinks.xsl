<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:js="http://saxonica.com/ns/globalJS"
xmlns:f="urn:internal.function"
extension-element-prefixes="ixsl"
xmlns:svg="http://www.w3.org/2000/svg"
xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
version="2.0"
>

<xsl:template match="span[@class eq 'button']" mode="ixsl:onclick">
<xsl:sequence select="f:check()"/>
</xsl:template>

<xsl:template match="span[@class eq 'button']" mode="ixsl:ontouchend">
<xsl:sequence select="f:check()"/>
</xsl:template>

<xsl:function name="f:check">
<xsl:result-document href="#status" method="replace-content">
Checking...
</xsl:result-document>
<ixsl:schedule-action wait="16">
<xsl:call-template name="check-links"/>
</ixsl:schedule-action>
</xsl:function>

<xsl:template name="check-links">
<xsl:result-document href="#status" method="replace-content">
Complete: results below
</xsl:result-document>
<xsl:result-document href="#results" method="replace-content">
<xsl:variable name="validpaths" as="xs:string*">
<xsl:for-each select="$navlist/ul/li">
<xsl:if test="@id ne 'javadoc'">
<xsl:variable name="doc" select="doc(concat($location, '/', @id,'.xml'))"/>
<xsl:apply-templates select="$doc/*" mode="check-refs"/>
</xsl:if>
</xsl:for-each>
</xsl:variable>
<xsl:variable name="foundlinks" as="node()*">
<xsl:for-each select="$navlist/ul/li">
<xsl:if test="@id ne 'javadoc'">
<xsl:variable name="doc" select="doc(concat($location, '/', @id,'.xml'))"/>
<xsl:apply-templates select="$doc/*" mode="get-refs"/>
</xsl:if>
</xsl:for-each>
</xsl:variable>
<xsl:variable name="erroritems" as="element()*">
<xsl:for-each select="$foundlinks">
<xsl:if test="not(starts-with(.,'http:') or starts-with(.,'https:'))">
<xsl:variable name="ancestorPath" select="string-join(ancestor::*/@id,'/')"/>
<xsl:variable name="pathtolink"
select="if ($ancestorPath eq 'functions') then
string-join(($ancestorPath,ancestor::fnd:function/*:name),'/')
else $ancestorPath"/>
<xsl:variable name="abs-path" select="concat('/',$pathtolink)"/>
<xsl:choose>
<xsl:when test="contains(.,'>')">
<div>
<p><span class="bl">Anchor text: </span>'<xsl:value-of select=".."/>'</p>
<p><span class="bl">Anchor page: </span>
<span data-href="{$abs-path}" class="link">
<xsl:value-of select="$abs-path"/>
</span>
</p>
<p><span class="bl">HREF link: </span> <xsl:value-of select="."/></p>
<p style="color:red"><span class="bl">Invalid character in href</span></p>
<hr style="margin-bottom:5px"/>
</div>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="href"
select="resolve-uri(self::attribute(),
concat('http://a.com/', $pathtolink,'/'))"/>
<xsl:variable name="s-href" select="for $h in substring($href, 14) return
if (starts-with($h, '/')) then $h
else concat('/',$h)"/>
<xsl:if test="not($s-href = $validpaths)">
<!--
<xsl:if test="not(contains($s-href, 'dotnetdoc') or contains($s-href, 'javadoc'))">
-->
<div>
<p><span class="bl">Anchor text: </span>'<xsl:value-of select=".."/>'</p>
<p><span class="bl">Anchor page: </span>
<span data-href="{$abs-path}" class="link">
<xsl:value-of select="$abs-path"/>
</span>
</p>
<p><span class="bl">HREF link: </span> <xsl:value-of select="."/></p>
<p style="color:red"><span class="bl">Not found at: </span> <xsl:value-of select="$s-href"/></p>
<hr style="margin-bottom:5px"/>
</div>
<!--
</xsl:if>
-->
</xsl:if>
</xsl:otherwise>
</xsl:choose>
</xsl:if>
</xsl:for-each>
</xsl:variable>
<xsl:variable name="failedcount" select="count($erroritems)" as="xs:integer"/>
<p class="bl">Links: <xsl:value-of select="count($foundlinks) - $failedcount"/>
Resolved - <xsl:value-of select="$failedcount"/> Failed</p>
<hr style="margin-bottom:5px"/>
<xsl:sequence select="$erroritems"/>
</xsl:result-document>
</xsl:template>

<xsl:template match="functions" mode="check-refs"
xpath-default-namespace="http://www.saxonica.com/ns/doc/functions">
<xsl:variable name="newpath" select="concat('/', @id)"/>
<xsl:sequence select="$newpath"/>
<xsl:apply-templates mode="check-refs" select="function">
<xsl:with-param name="path" select="$newpath"/>
</xsl:apply-templates>
</xsl:template>

<xsl:template match="function" mode="check-refs"
xpath-default-namespace="http://www.saxonica.com/ns/doc/functions">
<xsl:param name="path" as="xs:string" select="''"/>
<xsl:variable name="newpath" select="concat($path, '/', *:name)"/>
<xsl:sequence select="$newpath"/>
</xsl:template>

<xsl:template match="section|article" mode="check-refs">
<xsl:param name="path" as="xs:string" select="''"/>
<xsl:variable name="newpath" select="concat($path, '/', @id)"/>
<xsl:sequence select="$newpath"/>
<xsl:apply-templates mode="check-refs" select="section|article">
<xsl:with-param name="path" select="$newpath"/>
</xsl:apply-templates>
</xsl:template>

<xsl:template match="node()" mode="get-refs" as="node()*">
<xsl:apply-templates select="*|@*" mode="get-refs"/>
</xsl:template>

<xsl:template match="attribute()" mode="get-refs"/>

<xsl:template match="@data-href|@href" mode="get-refs" as="node()*">
<xsl:if test="parent::*[not(@class = ('javalink','apilink'))]">
<xsl:sequence select="."/>
</xsl:if>
</xsl:template>

</xsl:transform>
