<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:f="urn:internal.function"
xmlns:def="http://www.saxonica.com/ns/doc/functions"
exclude-result-prefixes="f xsl def xs"
version="2.0"
>

<xsl:output method="html"/>

<xsl:variable name="javadoc-types" select="doc('../../jdoc/javadoc-types.xml')/types"/>

<xsl:key name="jd-types" match="types" use="*"/>

<xsl:template match="section|article" mode="primary">
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
<xsl:variable name="href" select="f:index-fix(
if (article|section) then
@id else concat(@id, '.html'))"/>
<li>
<p>
<a href="{$href}">
<xsl:value-of select="@title"/>
</a>
<xsl:if test="@summary">
<xsl:value-of select="':', @summary" separator=" "/>
</xsl:if>
</p>
</li>
</xsl:for-each>
</ul>
</xsl:template>

<xsl:template match="img[@src]" mode="secondary">
<xsl:param name="hash-path" tunnel="yes" required="no" as="xs:string" select="''"/>
<xsl:variable name="src" select="if (starts-with(@src, '/')) then substring(@src, 2) else ."/>
<img src="{concat('../',$hash-path, $src)}">
<xsl:apply-templates select="(@* except @src)" mode="secondary"/>
<xsl:apply-templates/>
</img>
</xsl:template>

<xsl:template match="a[@class = 'apilink']" mode="secondary">
<xsl:param name="hash-path" tunnel="yes" required="no" as="xs:string" select="''"/>
<xsl:copy copy-namespaces="no">
<xsl:attribute name="href" select="concat($hash-path, @href)"/>
<xsl:apply-templates select="(@* except @href)" mode="secondary"/>
<xsl:apply-templates select="node()" mode="secondary"/>
</xsl:copy>
</xsl:template>

<xsl:template match="a[@class ='javalink']" mode="secondary">
<xsl:param name="hash-path" tunnel="yes" required="no" as="xs:string" select="''"/>
<xsl:variable name="item-id" select="translate(@href, '#','.')"/>
<xsl:variable name="found" select="exists(key('jd-types',$item-id, $javadoc-types))" as="xs:boolean"/>
<xsl:if test="not($found)">
<xsl:message>Warning: javadoc ref <xsl:value-of select="$item-id"/> not found</xsl:message>
</xsl:if>
<xsl:variable name="link" select="translate(@href, '.', '/')"/>
<xsl:variable name="file" select="if (contains($link, '#')) 
then concat(substring-before($link, '#'), '.html#', substring-after($link, '#'))
else concat($link, '.html')"/>
<xsl:variable name="pfx" select="substring(hash-path, 4)"/>
<a class="javalink" href="{concat($hash-path, 'javadoc/', $file)}">
<code><xsl:apply-templates/></code>
</a>
</xsl:template>

<xsl:template match="a" mode="secondary">
<xsl:param name="hash-path" tunnel="yes" required="no" as="xs:string" select="''"/>
<xsl:choose>
<xsl:when test="substring(@href, 1, 5) = ('file:','http:')
or contains(@href,'>') or starts-with(@href, 'https:')">
<xsl:copy-of select="." copy-namespaces="no"/>
</xsl:when>
<xsl:when test="substring-after(@href, '.') = ('zip','pdf','txt','xsl','xsd','xml')">
<xsl:variable name="href" select="if (starts-with(@href, '/')) then substring(@href, 2) else @href"/>
<a href="{concat('../',$hash-path, $href)}">
<xsl:apply-templates select="@* except @href" mode="secondary"/>
<xsl:apply-templates/>
</a>
</xsl:when>
<xsl:otherwise>
<xsl:copy>
<xsl:attribute name="href" select="f:anchor-navigation(., $hash-path)"/>
<xsl:apply-templates select="(@* except @href)" mode="secondary"/>
<xsl:apply-templates select="node()" mode="secondary"/>
</xsl:copy>
</xsl:otherwise>
</xsl:choose>
<!--
<xsl:if test="@href eq 'instructions'">
<xsl:message terminate="yes">
terminated on 'instructions'
</xsl:message>
</xsl:if>
-->
</xsl:template>

<xsl:function name="f:anchor-navigation">
<xsl:param name="a" as="node()"/>
<xsl:param name="hash-path" as="xs:string"/>
<xsl:variable name="href" select="$a/@href"/>
<xsl:variable name="section"
select="($a/ancestor::section[1]|$a/ancestor::article|
$a/ancestor::def:function|$a/ancestor::def:functions)[last()]"/>

<xsl:for-each select="$section">
<!-- path to the section containing the href - use for resolving relative paths -->
<xsl:variable name="path-to-section" as="xs:string*"
select="string-join(
(ancestor::article|ancestor::section|ancestor::def:functions)/@id,
'/')"/>
<!-- if this is a folder (contains other sections) then add the section name -->
<xsl:variable name="base-path" as="xs:string"
select="string-join(($path-to-section, @id, *:name), '/')"
/>
<xsl:variable name="is-absolute" select="starts-with($href, '/')" as="xs:boolean"/>
<xsl:variable name="resolved-ref">
<xsl:choose>
<xsl:when test="$is-absolute">
<xsl:value-of select="substring($href,2)"/>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="r-href"
select="resolve-uri($href, concat('http://a.com/', $base-path,'/'))"/>
<xsl:sequence select="substring($r-href, 14)"/>
</xsl:otherwise>
</xsl:choose>
</xsl:variable>
<xsl:variable name="ids" select="tokenize($resolved-ref, '/')"/>
<xsl:variable name="parent" select="$section-docs[*/@id eq $ids[1]]/*"/>

<xsl:variable name="ref-section" as="element()?">
<xsl:call-template name="get-section-element">
<xsl:with-param name="ids" select="$ids"/>
<xsl:with-param name="parent" select="$parent"/>
<xsl:with-param name="index" select="2"/>
</xsl:call-template>
</xsl:variable>

<!--
<xsl:message>
parent-id <xsl:value-of select="$parent/@id"/>
is-abs <xsl:value-of select="$is-absolute"/>
path-to-secton: <xsl:value-of select="$path-to-section"/>
base-path <xsl:value-of select="$base-path"/>
resolved-ref <xsl:value-of select="$resolved-ref"/>
</xsl:message>
-->

<!--  prefix was '../' for non absolute links -->
<xsl:for-each select="$ref-section">
<xsl:variable name="prefix" select="$hash-path"/>

<xsl:variable name="fpath" select="$resolved-ref"/>
<xsl:variable name="ext" select="if (article|section|def:function) then '' else '.html'"/>
<xsl:variable name="pre-filepath" select="concat($prefix, $fpath, $ext)"/>
<xsl:variable name="filepath" select="f:index-fix($pre-filepath)"/>
<xsl:sequence select="$filepath"/>

<!--
<xsl:message>
section-doc-ids: <xsl:value-of select="$section-docs/*/@id"/>
href <xsl:value-of select="$href"/>
base-path: <xsl:value-of select="$base-path"/>
resolved-ref <xsl:value-of select="$resolved-ref"/>
prefix: <xsl:value-of select="$prefix"/>
file-path: <xsl:value-of select="$filepath"/>
parent-id <xsl:value-of select="$parent/@id"/>
sec-id: <xsl:value-of select="@id"/>
</xsl:message>
-->

</xsl:for-each>

<!--
<xsl:if test="$href eq '../linenumber'">
<xsl:message terminate="yes">
section-doc-ids: <xsl:value-of select="$section-docs/*/@id"/>
ids[1]: <xsl:value-of select="$ids[1]"/>
terminated for ../xslt20
</xsl:message>
</xsl:if>
-->

<xsl:if test="empty($ref-section)">
<xsl:message>
warning: at base-path: <xsl:value-of select="$base-path"/>
href: <xsl:value-of select="$href"/> not resolved
</xsl:message>
<xsl:sequence select="$href"/>
</xsl:if>

</xsl:for-each>

</xsl:function>

<!-- fixes case where index.html name at the folder top-level may clash with
     an item within the folder also named index (e.g. saxon:index)  -->
<xsl:function name="f:index-fix">
<xsl:param name="path"/>
<xsl:sequence select="if (ends-with($path, '/index.html') or $path eq 'index.html') then
concat(substring($path,1,string-length($path) - 5),'-1.html') 
else $path "/>
</xsl:function>

</xsl:transform>
