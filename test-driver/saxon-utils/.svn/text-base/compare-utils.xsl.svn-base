<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:f="http://www.saxonica.com/ce-testing/functions"
    exclude-result-prefixes="xs f"
    version="2.0">
    
    <!-- Compare two nodes for deep equality. Unlike deep-equals(), this returns
    a diagnostic to indicate where the nodes are different-->

<xsl:function name="f:deep-equal" as="xs:string?">
<!-- return empty sequence if nodes are deep-equal, or an explanation of why not -->
<xsl:param name="n1" as="node()*"/>
<xsl:param name="n2" as="node()*"/>
<xsl:param name="path" as="xs:string"/>
<xsl:choose>
<xsl:when test="count($n1) ne count($n2)">
<xsl:value-of select="'At', $path, 'n1 has ', count($n1), 'nodes but n2 has', count($n2), '.',
string-join($n1/f:show-node(.), '|'), 'vs.', string-join($n2/f:show-node(.), '|')"/>
</xsl:when>
<xsl:otherwise>
<xsl:variable name="child-comparisons" as="item()*">
<xsl:for-each select="1 to count($n1)">
<xsl:variable name="p" as="xs:integer" select="."/>
<xsl:variable name="node1" select="$n1[$p]"/>
<xsl:variable name="node2" select="$n2[$p]"/>
<xsl:variable name="atts1" as="attribute()*">
<xsl:perform-sort select="$node1/@*">
<xsl:sort select="namespace-uri()"/>
<xsl:sort select="local-name()"/>
</xsl:perform-sort>
</xsl:variable>
<xsl:variable name="atts2" as="attribute()*">
<xsl:perform-sort select="$node2/@*">
<xsl:sort select="namespace-uri()"/>
<xsl:sort select="local-name()"/>
</xsl:perform-sort>
</xsl:variable>
<xsl:variable name="compare-atts" as="xs:string?"
select="f:deep-equal($atts1, $atts2, concat($path, '/@*'))"/>
<xsl:choose>
<xsl:when test="f:node-kind($node1) ne f:node-kind($node2)">
<xsl:value-of select="'At', $path, 'item', $p, 'n1 has kind', f:node-kind($node1), 
'but n2 has kind', f:node-kind($node2)"/>
</xsl:when>
<xsl:when test="not(deep-equal(node-name($node1), node-name($node2)))">
<xsl:value-of select="'At', $path, 'item', $p, 'n1 has name {', namespace-uri($node1),'}', name($node1), 
'but n2 has name {', namespace-uri($node2), '}', name($node2)"/>
</xsl:when>
<xsl:when test="not($node1 instance of element(*)) and 
not($node1 instance of document-node()) and 
not(string($node1) eq string($node2))">
<xsl:value-of select="'At ', $path, ' item ', $p, ' n1 has value {', string($node1), 
'} but n2 has value {', string($node2), '}.', 
' Difference is at ', f:show-difference(string($node1), string($node2))" separator=""/>
</xsl:when>
<xsl:when test="exists($compare-atts)">
<xsl:sequence select="$compare-atts"/>
</xsl:when>
<xsl:otherwise>
<xsl:sequence select="f:deep-equal(
$node1/child::node()[not(. instance of text()) or normalize-space(.)], 
$node2/child::node()[not(. instance of text()) or normalize-space(.)], 
concat($path, '/', name($node1)))"/>
</xsl:otherwise>                       
</xsl:choose>
</xsl:for-each>
</xsl:variable>
<!--<xsl:message select="'count', count($child-comparisons)"/>
<xsl:message select="'value', f:node-kind($child-comparisons[1]), '[', string($child-comparisons[1]), ']'"/>
-->
<xsl:sequence select="if (empty($child-comparisons)) then () else string-join($child-comparisons, '; ')"/>
</xsl:otherwise>
</xsl:choose>
</xsl:function>

<xsl:function name="f:node-kind" as="xs:string">
<xsl:param name="n" as="node()"/>
<xsl:choose>
<xsl:when test="$n instance of document-node()">document</xsl:when>
<xsl:when test="$n instance of element()">element</xsl:when>
<xsl:when test="$n instance of attribute()">attribute</xsl:when>
<xsl:when test="$n instance of text()">text</xsl:when>
<xsl:when test="$n instance of comment()">comment</xsl:when>
<xsl:when test="$n instance of processing-instruction()">pi</xsl:when>
<xsl:otherwise>namespace</xsl:otherwise>
</xsl:choose>
</xsl:function>

<xsl:function name="f:show-node" as="xs:string">
<xsl:param name="n" as="node()"/>
<xsl:value-of>
<xsl:choose>
<xsl:when test="$n instance of document-node()">doc()</xsl:when>
<xsl:when test="$n instance of element()">element(<xsl:value-of select="name($n)"/>)</xsl:when>
<xsl:when test="$n instance of attribute()">attribute(<xsl:value-of select="name($n)"/>)</xsl:when>
<xsl:when test="$n instance of text()">text(<xsl:value-of select="$n"/>)</xsl:when>
<xsl:when test="$n instance of comment()">comment(<xsl:value-of select="$n"/>)</xsl:when>
<xsl:when test="$n instance of processing-instruction()">pi(<xsl:value-of select="name($n)"/>)</xsl:when>
<xsl:otherwise>namespace()</xsl:otherwise>
</xsl:choose>
</xsl:value-of>
</xsl:function>

<xsl:function name="f:show-difference" as="xs:string">
<xsl:param name="s1" as="xs:string"/>
<xsl:param name="s2" as="xs:string"/>

<xsl:variable name="p" as="xs:integer" select="
min(((1 to (string-length($s1) + 1))[substring($s1, ., 1) ne substring($s2, ., 1)]))"/>

<xsl:sequence select="concat(
if (string-length($s1) ne string-length($s2))
then concat('length(', string-length($s1), ',', string-length($s2), ')') else '',
'at ', $p, ' {', substring($s1, $p, 5), '}~{', substring($s2, $p, 5), '}')"/>
</xsl:function>
  
<!-- Simple serializer to create an HTML rendition of an XML document -->

<xsl:template match="/" mode="serialize">
<div style="background-color:gray">
<xsl:apply-templates mode="#current"/>
</div>
</xsl:template>
<xsl:template match="*" mode="serialize">
<div style="position:relative; left:+10px">
<xsl:value-of select="'&lt;', name(.)" separator=""/>
<xsl:for-each select="@*">
<xsl:value-of select="concat(' ', name(), '=''', string(), '''')"/>
</xsl:for-each>
<xsl:value-of select="'&gt;'"/>
<xsl:apply-templates mode="#current"/>
<xsl:value-of select="'&lt;/', name(.), '&gt;'" separator=""/>
</div>    
</xsl:template>
<xsl:template match="text()" mode="serialize">
<xsl:value-of select="replace(., ' ', '_')" separator=""/>
</xsl:template>
<xsl:template match="comment()" mode="serialize">
<xsl:value-of select="'&lt;!--', replace(., ' ', '_'), '--&gt;'" separator=""/>
</xsl:template>
<xsl:template match="processing-instruction()" mode="serialize">
<xsl:value-of select="'&lt;?', name(), ' ', replace(., ' ', '_'), '?&gt;'" separator=""/>
</xsl:template>
    
</xsl:stylesheet>

