<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:js="http://saxonica.com/ns/globalJS"
xmlns:f="urn:internal.function"
extension-element-prefixes="ixsl"
xmlns:svg="http://www.w3.org/2000/svg"
xmlns:style="http://saxonica.com/ns/html-style-property"
xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
version="2.0"
>

<xsl:template match="p[@class eq 'search']" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:call-template name="run-search"/>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class eq 'search']" mode="ixsl:ontouchend">
<xsl:call-template name="run-search"/>
</xsl:template>

<xsl:template name="run-search">
<xsl:variable name="text" select="normalize-space(ixsl:get($navlist/div/input, 'value'))"/>
<xsl:if test="string-length($text) gt 0">
<xsl:for-each select="$navlist/../div[@class eq 'found']">
<ixsl:set-attribute name="style:display" select="'block'"/>
</xsl:for-each>
<xsl:result-document href="#findstatus" method="replace-content">
searching...
</xsl:result-document>
<ixsl:schedule-action wait="16">
<xsl:call-template name="check-text"/>
</ixsl:schedule-action>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class = ('foundNext','foundPrev','foundClosed')]" mode="ixsl:onclick">
<xsl:if test="$usesclick">
<xsl:apply-templates mode="found-action" select="."/>
</xsl:if>
</xsl:template>

<xsl:template match="p[@class = ('foundNext','foundPrev','foundClosed')]" mode="ixsl:ontouchend">
<xsl:apply-templates mode="found-action" select="."/>
</xsl:template>

<xsl:template match="p" mode="found-action">
<xsl:variable name="status-element" select="../p[@id = 'findstatus']"/>
<xsl:variable name="paths" select="js:getPaths()"/>
<xsl:variable name="index" as="xs:integer"
select="xs:integer(substring-before($status-element,' of'))"/>
<xsl:if test="@class eq 'foundClosed'">
<xsl:for-each select="$navlist/../div[@class eq 'found']">
<ixsl:set-attribute name="style:display" select="'none'"/>
</xsl:for-each>
</xsl:if>
<xsl:variable name="newindex" as="xs:integer"
select="if (@class eq 'foundNext' and $index lt count($paths)) then $index + 1
else if (@class eq 'foundPrev' and $index gt 0) then $index - 1
else 0"/>
<xsl:if test="$newindex gt 0">
<xsl:for-each select="$status-element">
<xsl:result-document href="?select=." method="replace-content">
<xsl:value-of select="$newindex, ' of ', substring-after(., 'of ')"/>
</xsl:result-document>
</xsl:for-each>
<xsl:sequence select="f:set-hash($paths[$newindex])"/>
</xsl:if>
</xsl:template>

<xsl:function name="f:highlight-finds">
<xsl:variable name="findtext"
select="ixsl:get($navlist/div/input, 'value')"/>
<xsl:sequence select="js:findit($findtext)"/>
</xsl:function>

<xsl:template name="check-text">
<xsl:variable name="search"
select="lower-case(
normalize-space(ixsl:get($navlist/div/input, 'value'))
)"/>
<xsl:variable name="found" as="xs:string*">
<xsl:choose>
<xsl:when test="starts-with($search, '#')">
<xsl:variable name="doc" select="doc($jd-search)"/>
<xsl:variable name="s" select="lower-case(substring($search, 2))"/>
<xsl:variable name="hitElements" select="$doc/*//*[@qy eq $s]" as="node()*"/>
<xsl:for-each select="$hitElements">
<xsl:variable name="member" select="if (@in ne 'c') then concat('@',@id) else ''"/>
<xsl:value-of select="concat(
'javadoc/',
string-join(
(self::*[@in eq 'c']|ancestor::*)/@id, '/'),
$member)"/>
</xsl:for-each>
</xsl:when>
<xsl:otherwise>
<xsl:for-each select="$navlist/ul/li">
<xsl:if test="@id ne 'javadoc'">
<xsl:variable name="doc" select="doc(concat($location, '/', @id,'.xml'))"/>
<xsl:apply-templates select="$doc/*" mode="check-text">
<xsl:with-param name="search" select="$search"/>
</xsl:apply-templates>
</xsl:if>
</xsl:for-each>
</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<xsl:variable name="count" select="count($found)"/>

<xsl:if test="$count gt 0">
<xsl:if test="f:get-hash() eq $found[1]">
<xsl:sequence select="f:highlight-finds()"/>
</xsl:if>
<xsl:sequence select="f:set-hash($found[1])"/>
</xsl:if>

<xsl:result-document href="#findstatus" method="replace-content">
<xsl:value-of select="if ($count gt 0) then concat('1 of ',$count) else '0 of 0'"/>
</xsl:result-document>
<xsl:sequence select="js:setPaths($found)"/>
</xsl:template>

<xsl:template match="fnd:functions" mode="check-text">
<xsl:param name="search"/>
<xsl:apply-templates mode="check-text" select="fnd:function">
<xsl:with-param name="search" select="$search"/>
</xsl:apply-templates>
</xsl:template>

<xsl:template match="fnd:function" mode="check-text">
<xsl:param name="search"/>
<xsl:variable name="newpath" select="concat('functions/', fnd:name)"/>
<xsl:variable name="text" select="lower-case(.)"/>
<xsl:sequence select="if (contains($text, $search))
then $newpath
else ()"/>

</xsl:template>

<xsl:template match="section|article" mode="check-text">
<xsl:param name="search"/>
<xsl:param name="path" as="xs:string" select="''"/>
<xsl:variable name="newpath" select="concat($path, '/', @id)"/>
<xsl:variable name="text" select="lower-case(
string-join(
*[not(local-name() = ('section','article'))]
,'!')
)"/>
<xsl:sequence select="if (contains($text, $search))
then substring($newpath,2)
else ()"/>
<xsl:apply-templates mode="check-text" select="section|article">
<xsl:with-param name="search" select="$search"/>
<xsl:with-param name="path" select="$newpath"/>
</xsl:apply-templates>
</xsl:template>

</xsl:transform>
