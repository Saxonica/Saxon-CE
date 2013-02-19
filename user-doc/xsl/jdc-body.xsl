<xsl:transform
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
version="2.0"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:js="http://saxonica.com/ns/globalJS"
xmlns:f="urn:internal.function"
extension-element-prefixes="ixsl"
>

<xsl:variable name="object" select="('getClass','hashCode','equals','toString','notify','notifyAll','wait')"/>

<xsl:template match="node()|@*" mode="jdc">
<xsl:copy>
<xsl:apply-templates mode="jdc" select="node()|@*"/>
</xsl:copy>
</xsl:template>

<xsl:template match="tag[@kind]" mode="jdc">
<xsl:variable name="local" select="starts-with(. , '#')" as="xs:boolean"/>
<xsl:variable name="afterhash" select="if (not($local)) then substring-after(., '#') else ''"/>
<span class="javalink" data-href="{if ($local) then
concat(ancestor::class[1]/@fulltype, '@', substring(. , 2))
 else translate(. , '#', '@')}">
<xsl:sequence select="if ($local) then substring(. , 2)
else if ($afterhash ne '') then $afterhash
else f:last-substring-after(. , 46)"/>
</span>
</xsl:template>

<xsl:function name="f:last-substring-after">
<xsl:param name="text"/>
<xsl:param name="char" as="xs:double"/>
<xsl:variable name="last" as="xs:integer?"
select="index-of(string-to-codepoints($text), $char)[last()]"/>
<xsl:sequence select="if (exists($last)) then substring($text, $last + 1) else $text"/>
</xsl:function>

<xsl:template match="pre" mode="jdc">
<samp>
<xsl:apply-templates/>
</samp>
</xsl:template>

<xsl:function name="f:class-parts">
<xsl:param name="class" as="node()?"/>
<xsl:for-each select="$class">
<xsl:sequence select="@visibility,
if (@abstract and not(@interface)) then 'abstract' else (),
if (@static) then 'static' else (),
if (@final) then 'final' else (),
if (@interface) then 'interface' 
    else if (name(.) eq 'class') then 'class' else ()"/>
</xsl:for-each>
</xsl:function>

<xsl:template match="extends" mode="extends">

<ul>
<li>
<xsl:sequence select="f:showType(interface , true(), false())"/>
<xsl:choose>
<xsl:when test="parent::interface">
<xsl:apply-templates select="parent::interface/parent::extends" mode="extends"/>
</xsl:when>
<xsl:otherwise>
<ul><li><xsl:value-of select="ancestor::class[1]/@fulltype"/></li></ul>
</xsl:otherwise>
</xsl:choose>
</li></ul>
</xsl:template>

<xsl:function name="f:sig" as="node()*">
<xsl:param name="classid" as="xs:string"/>
<xsl:param name="method" as="node()"/>
<xsl:param name="for-detail" as="xs:boolean"/>
<xsl:variable name="class" select="if ($for-detail) then 'fblock' else 'javalink'"/>
<xsl:for-each select="$method">
<span class="{if ($for-detail) then 'fullsignature' else 'signature'}">
<span class="{$class}" data-href="{$classid}@{@id}">
<xsl:if test="$for-detail">
<xsl:value-of select="(f:class-parts(.),' ')"/>
<xsl:sequence select="f:showMethodType(.)"/>
<xsl:text> </xsl:text>
</xsl:if>
<xsl:value-of select="@id"/>
<xsl:text> (</xsl:text>
</span>
<span class="{if ($for-detail) then 'fblock' else 'normal'}">
<xsl:for-each select="params/param">
<xsl:sequence select="f:showMethodType(.)"/>
<xsl:text> </xsl:text>
<code><xsl:value-of select="@name"/></code>
<xsl:if test="not(position() eq last())">
<xsl:text>, </xsl:text>
<xsl:if test="$for-detail"><br/></xsl:if>
</xsl:if>
</xsl:for-each>
<xsl:text>)</xsl:text>
<xsl:if test="$for-detail and exceptions">
<br/><xsl:text>throws </xsl:text>
<xsl:for-each select="exceptions/exception">
<br/>
<xsl:sequence select="f:showType(., false(), true())"/>
</xsl:for-each>
</xsl:if>
</span>
</span>
<div class="{if ($for-detail) then 'fcomment' else 'fnormal'}">
<xsl:apply-templates
select="if ($for-detail and comment/body) then comment/body/node()
else comment/sentence/body/node()" mode="jdc"/>
</div>
<xsl:if test="$for-detail">

<xsl:if test="override-of">
<p class="methodLabel">Overrides:</p>
<xsl:for-each select="override-of">
<div class="params">
<span class="javalink" data-href="{concat(@fulltype, '@', @method)}">
<xsl:value-of select="substring-before(@method, '(')"/></span>
<xsl:text> in class </xsl:text>
<xsl:sequence select="f:showType(., false(), false())"/>
</div>
</xsl:for-each>

</xsl:if>

<xsl:if test="params">
<p class="methodLabel">Parameters:</p>
<xsl:for-each select="params/param">
<div class="params">
<code><xsl:value-of select="@name, (if (body) then ' - ' else ())"/></code>
<xsl:apply-templates
select="body/node()" mode="jdc"/>
</div>
</xsl:for-each>
</xsl:if>

<xsl:if test="comment/return">
<p class="methodLabel">Returns:</p>
<div class="params">
<xsl:apply-templates
select="comment/return/body/node()" mode="jdc"/>
</div>
</xsl:if>

<xsl:if test="exceptions">
<p class="methodLabel">Throws:</p>
<xsl:for-each select="exceptions/exception">
<div class="params">
<xsl:sequence select="f:showType(., false(), true())"/>
<xsl:if test="exists(body|sentence)"> - </xsl:if>
<xsl:apply-templates select="(body/node()|sentence/body/node())" mode="jdc"/>
</div>
</xsl:for-each>
</xsl:if>

<xsl:for-each select="comment/attribute[@name='@since']">
<p class="methodLabel">Since</p>
<div class="params">
<xsl:apply-templates select="body/node()" mode="jdc"/>
</div>
</xsl:for-each>
</xsl:if>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:nested" as="node()*">
<xsl:param name="class" as="node()"/>
<xsl:sequence select="f:showType($class, true(), true())"/><br/>
<xsl:apply-templates select="$class/comment/sentence/body"/>
</xsl:function>

<xsl:function name="f:comment" as="node()*">
<xsl:param name="method" as="node()"/>
<xsl:apply-templates select="$method/comment/sentence/body"/>
</xsl:function>

<xsl:function name="f:fieldDetail" as="node()*">
<xsl:param name="class"/>
<xsl:param name="field" as="node()"/>
<xsl:for-each select="$field">
<span class="javalink" data-href="{$class/@fulltype}@{@id}"><xsl:value-of select="@id"/></span><br/>
<xsl:apply-templates select="$field/comment/sentence/body"/>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:addLink">
<xsl:param name="href"/>
<xsl:param name="text"/>
<xsl:param name="style"/>
<span class="{$style}" data-href="{$href}"><xsl:value-of select="$text"/></span>
</xsl:function>

<xsl:function name="f:showType">
<xsl:param name="class" as="node()"/>
<xsl:param name="showFull" as="xs:boolean"/>
<xsl:param name="isClass" as="xs:boolean"/>
<xsl:for-each select="$class">
<span class="javalink" data-href="{@fulltype}">
<xsl:value-of select="if ($isClass) then @id else if ($showFull) then @fulltype else @type"/>
</span>
<xsl:sequence select="f:showParamTypes(.)"/>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:showParamTypes">
<xsl:param name="class"/>
<xsl:for-each select="$class/paramtypes">
<span class="operator">
<xsl:text>&lt;</xsl:text>
<xsl:for-each select="type">
<span class="javalink" data-href="{@fulltype}">
<xsl:value-of select="@name"/>
</span>
<xsl:if test="bounds">
<xsl:text> extends </xsl:text>
<xsl:for-each select="bounds/limit">
<xsl:sequence select="f:showMethodType(.)"/>
</xsl:for-each>
</xsl:if>
<xsl:value-of select="if (not(position() eq last())) then ', ' else ''"/>
</xsl:for-each>
<xsl:text>&gt;</xsl:text>
</span>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:showMethodType">
<xsl:param name="method" as="node()"/>
<xsl:for-each select="$method">
<xsl:variable name="wrap" select="false()"/>
<xsl:if test="@extendby">
<xsl:value-of select="concat(
if ($wrap) then '&lt;xx' else '',
@extendby, ' extends ')"/>
</xsl:if>
<span class="javalink" data-href="{@fulltype}">
<xsl:value-of select="@type"/>
</span>
<xsl:if test="$wrap">&gt;</xsl:if>
<xsl:if test="exists(type)">
<xsl:text>&lt;</xsl:text>
<xsl:for-each select="type">
<span class="operator">
<xsl:sequence select="f:showMethodType(.)"/>
</span>
<xsl:if test="position() ne last()">, </xsl:if>
</xsl:for-each>
<xsl:text>&gt;</xsl:text>
</xsl:if>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:modifiers">
<xsl:param name="member" as="node()"/>
<xsl:for-each select="$member">
<xsl:value-of select="string-join((
if (@visibility eq 'protected') then 'protected' else '',
if (@abstract) then 'abstract' else '',
if (@static) then 'static' else ''),
' '), ' '"/>
</xsl:for-each>
</xsl:function>

<xsl:function name="f:classnotfound">
<xsl:param name="parts" as="xs:string*"/>
<xsl:variable name="subparts" select="subsequence($parts, 2)"/>
<xsl:variable name="dotname" select="string-join($subparts,'.')"/>
<xsl:variable name="path" select="concat(translate($dotname,'.','/'),'.html')"/>
<h1>Non-Saxon Type</h1>
<p>Type <code><xsl:value-of select="$dotname"/></code> is not defined here</p>
<p>See the Java Specification on <a href="http://docs.oracle.com/javase/1.5.0/docs/api/{$path}" target="_blank"><xsl:value-of select="$parts[last()]"/></a> for more detail.</p>
<p class="history">Back</p>
</xsl:function>

<xsl:function name="f:pkgnotfound">
<xsl:param name="parts" as="xs:string*"/>
<h1>Non-Saxon Type</h1>
<p>Type: <code><xsl:value-of select="$parts[2]"/></code> is not defined within this documentation.</p>
<p>See the Java Specification <a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.2" target="_blank">Primitive Types and Values</a> section for more detail.</p>
<p class="history">Back</p>
</xsl:function>

<xsl:template match="p[@class eq 'history']" mode="ixsl:onclick">
<xsl:sequence select="js:goback()"/>
</xsl:template>


<xsl:template match="class" mode="show-class">

<h3><span class="javalink" data-href="{../@id}"><xsl:value-of select="../@id"/></span></h3>
<xsl:variable name="interface" select="exists(@interface)" as="xs:boolean"/>
<h1><span class="classLabel">
<xsl:value-of select="if ($interface) then 'Interface ' else if (@superclass='Enum') then 'Enum ' else 'Class '"/>
</span><xsl:value-of select="@id"/>
<xsl:sequence select="f:showParamTypes(.)"/>
</h1>

<xsl:if test="empty(@interface)">
<ul class="extends"><li>java.lang.Object
<xsl:variable name="inner-count" select="count(.//class[1])" as="xs:integer"/>
<xsl:apply-templates select="(extends|extends//extends)[last()]" mode="extends"/>

<xsl:if test="empty(extends)">
<ul>
<li>
<xsl:value-of select="@fulltype"/>
</li>
</ul>
</xsl:if>
</li></ul>
</xsl:if>

<xsl:if test="exists(implements|extends/inherits/interface)">
<dl>
<dt>All <xsl:value-of select="if ($interface) then ' Superinterfaces' else 'Implemented Interfaces '"/></dt>
<dd><xsl:for-each select="implements/interface|extends/inherits/interface">
<xsl:sequence select="f:showType(. , false() , false())"/>
<xsl:if test="position() ne last()">, </xsl:if>
</xsl:for-each>
</dd>
</dl>
</xsl:if>
<hr style="margin:8px;"/>
<p><code><xsl:value-of select="string-join((f:class-parts( . )),' ')"/></code>&#160;<span class="classLabel"><strong><xsl:value-of select="@id"/></strong></span>
<xsl:for-each select="extends/interface">
<br/>extends <xsl:sequence select="f:showType(., false() , false())"/>
</xsl:for-each>
<xsl:for-each select="implements/interface">
<xsl:if test="position() = 1"><br/><xsl:value-of select="if ($interface) then 'extends ' else 'implements '"/></xsl:if>
<xsl:sequence select="f:addLink(@fulltype, @type, 'javalink')"/>
<xsl:if test="position() ne last()">, </xsl:if>
</xsl:for-each>
</p>

<div class="jcomments" style="margin-bottom: 8px;">
<xsl:apply-templates select="if (exists(comment/body)) then comment/body/node()
else comment/sentence/body/node()" mode="jdc"/>
</div>
<xsl:variable name="s-titles"
select="('Nested Classes',
'Field Summary',
'Constructor Summary',
'Method Summary')"
as="xs:string+"/>


<xsl:variable name="class" select="."/>
<xsl:variable name="classid" select="@fulltype"/>
<xsl:for-each select="1 to 4">
<xsl:variable name="i" select="." as="xs:integer"/>
<xsl:variable name="members"
select="if ($i eq 1) then $class/class
else if ($i eq 2) then $class/fields/field
else if ($i eq 3) then $class/methods/constructor
else $class/methods/method" as="node()*"/>
<xsl:if test="exists($members)">
<table border="1" style="width:100%">
<thead>
<tr>
<td colspan="2" style="text-align:center">
<h3><xsl:value-of select="$s-titles[$i]"/></h3>
</td>
</tr>
</thead>
<tbody>
<xsl:for-each select="$members">
<tr>
<td class="col-left">
<p class="javaclassmember">
<xsl:choose>
<xsl:when test="$i eq 1">
<xsl:value-of select="'class'"/>
</xsl:when>
<xsl:when test="$i eq 2">
<xsl:value-of select="f:modifiers(.)"/>
<xsl:sequence select="f:showMethodType(.)"/>
</xsl:when>
<xsl:when test="$i eq 3">
<xsl:sequence select="if (@visibility = 'protected') then 'protected' else ()"/>

</xsl:when>
<xsl:otherwise>
<xsl:value-of select="f:modifiers(.)"/>
<xsl:sequence select="f:showMethodType(.)"/>
</xsl:otherwise>
</xsl:choose>
</p></td>
<td class="col-right">
<xsl:sequence select="if ($i = 1) then f:nested(.)
else if ($i = 2) then f:fieldDetail($class, .)
else if ($i = (3,4)) then f:sig($classid, ., false())
else f:comment(.)"/>
</td>
</tr>
</xsl:for-each>
</tbody>
</table>

<p>&#160;</p>

</xsl:if>
</xsl:for-each>

<xsl:if test="exists($class/fields/field)">
<div class="section">
<h2>Field Detail</h2>

<xsl:for-each select="$class/fields/field">
<div class="method">
<h3><xsl:value-of select="@id"/></h3>
<p>
<xsl:value-of select="f:class-parts(.),' '"/>
<xsl:sequence select="f:showMethodType(.)"/>&#160;<code><xsl:value-of select="@id"/></code></p>
<xsl:apply-templates
select="if (comment/body) then comment/body/node()
else comment/sentence/body/node()" mode="jdc"/>
<xsl:if test="comment/body|comment/sentence">
<p>&#160;</p>
</xsl:if>
</div>
</xsl:for-each>
</div>
</xsl:if>

<xsl:if test="exists($class/methods/constructor)">
<div class="section">
<h2>Constructor Detail</h2>

<xsl:for-each select="$class/methods/constructor">
<div class="method">
<h3><xsl:value-of select="@id"/></h3>
<p><xsl:sequence select="f:sig($classid, ., true())"/></p>
</div>
</xsl:for-each>
</div>
</xsl:if>

<xsl:if test="exists($class/methods/method)">
<div class="section">
<h2>Method Detail</h2>
<xsl:for-each select="$class/methods/method">
<div class="method">
<h3><xsl:value-of select="@id"/></h3>
<p><xsl:sequence select="f:sig($classid, ., true())"/></p>
</div>
</xsl:for-each>
</div>
</xsl:if>
</xsl:template>

</xsl:transform>

