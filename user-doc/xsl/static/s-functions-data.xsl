<xsl:stylesheet id="ss" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="2.0"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:f="http://local/functions"
exclude-result-prefixes="xs f"
xpath-default-namespace="http://www.saxonica.com/ns/doc/functions">


<xsl:template match="xsl:stylesheet" mode="f"/>

<xsl:key name="fk" match="function" use="name"/>

<xsl:template match="description" mode="fn-description">
<p><xsl:value-of select="."/></p>
</xsl:template>

<xsl:template match="signatures" mode="fn-description">
<xsl:if test="proto/description">
<ol>
<xsl:for-each select="proto/description">
<li><xsl:value-of select="."/></li>
</xsl:for-each>
</ol>
</xsl:if>
</xsl:template>

<xsl:function name="f:fn-list" as="element()*">
<xsl:param name="fns" as="element()"/>
<xsl:perform-sort select="$fns/function[. is key('fk', name)[1]]">
<xsl:sort select="lower-case(name)" lang="en"/>
</xsl:perform-sort>
</xsl:function>

<xsl:template match="functions" mode="f">
<article id="functions" title="XSLT 2.0 and XPath 2.0 Functions">

<h2>XSLT 2.0 and XPath 2.0 Functions</h2>
<p class="small-note">The information in this section indicates which functions are implemented in this Saxon release, and any restrictions in the current implementation. It includes both the core functions defined in XPath, and the additional
functions defined in the XSLT specification.</p>
   
<!-- <xsl:variable name="fns" select="function[starts-with(name, 'fn:') or starts-with(name, 'xslt:')]"/> -->
<xsl:variable name="sfns" as="element()*" select="f:fn-list(.)">
</xsl:variable>

<xsl:variable name="colsize" select="xs:integer(ceiling(count($sfns) div 3))"/>
<div style="overflow:auto; margin-top:5px;margin-bottom:5px">
<table>
<tr>
<td valign="top">
<p>
<xsl:apply-templates select="$sfns[(position()-1) idiv $colsize eq 0]" mode="index"/>
</p>
</td>
<td valign="top">
<p>
<xsl:apply-templates select="$sfns[(position()-1) idiv $colsize eq 1]" mode="index"/>
</p>
</td>
<td valign="top">
<p>
<xsl:apply-templates select="$sfns[(position()-1) idiv $colsize eq 2]" mode="index"/>
</p>
</td>
</tr>
</table>
</div>
    
         

</article>
</xsl:template>

<xsl:template match="function" mode="index">
<a class="flink" href="{concat(name, '.html')}">
<xsl:value-of select="name"/>
</a>
<br/>
</xsl:template>

<xsl:template match="function[name]" priority="5" mode="f">

<xsl:variable name="MyFunction" select="." />
<h1><xsl:value-of select="name"/></h1>
<!--
<xsl:copy-of select="description/*"/>
-->
<xsl:apply-templates select="description/*"/>
<xsl:variable name="same-spec-references" as="xs:boolean"
select="every $proto in subsequence(signatures/proto, 2)
satisfies deep-equal(signatures/proto[1]/in-spec, $proto/in-spec)"/>
<xsl:for-each select="signatures/proto">
<xsl:variable name="MyProto" select="."/>
<p style="color:#98b7c0;margin-bottom:3px;">
<xsl:value-of select="concat(@name,'(',string-join(for $arg in arg return concat('$',$arg/@name, ' as ', $arg/@type), ', ') ,')', ' → ',@return-type)" />
</p>
<!--
<xsl:copy-of select="description/*"/>
-->
<xsl:apply-templates select="description/*"/>
<table class="fn-prototype" style="margin-bottom:5px;">
<tr>
<td width="470" align="left" colspan="4" style="border-top:solid 1px;">
<p><i><xsl:value-of select="if (exists(arg)) then 'Arguments' else 'There are no arguments'"/></i></p>
</td>
</tr>
<xsl:for-each select="arg">
<tr>
<td width="40"><p>&#xa0;</p></td>
<td width="80" valign="top"><p>$<xsl:value-of select="@name"/></p></td>
<td valign="top" width="150"><p>
<xsl:value-of select="@type"/></p>
</td>
<td valign="top" width="200">
<p><xsl:value-of select="@desc"/></p>
</td>
</tr>
</xsl:for-each>
<tr>
<td colspan="2" style="border-top:solid 1px; border-bottom:solid 1px;">
<p><i>Result</i></p>
</td>

<td style="border-top:solid 1px #3D5B96; border-bottom:solid 1px;" colspan="2">
<p><xsl:value-of select="$MyProto/@return-type"/></p>
</td>
                        
</tr>
</table>

                
<xsl:if test="not($same-spec-references)">
<xsl:variable name="fname" select="$MyFunction/name"/>
<p>Applies to: <xsl:value-of select="f:specs(in-spec)"/></p>
<xsl:if test="in-spec='xpath20'"><p><a href="http://www.w3.org/TR/xpath-functions/#func-{$fname}">XPath 2.0 Specification</a></p></xsl:if>
<xsl:if test="in-spec='xpath30'"><p><a href="http://www.w3.org/TR/xpath-functions-11/#func-{$fname}">XPath 3.0 Specification</a></p></xsl:if>
<xsl:if test="in-spec='xslt20' and not(in-spec='xpath20')"><p><a href="http://www.w3.org/TR/xslt20/#function-{$fname}">XSLT 2.0 Specification</a></p></xsl:if>
<xsl:if test="in-spec='xslt30' and not(in-spec='xpath30')"><p><a href="http://www.w3.org/TR/xslt-21/#function-{$fname}">XSLT 2.1 Specification</a></p></xsl:if>
</xsl:if>
</xsl:for-each>

<xsl:if test="$same-spec-references">
<h3 class="subtitle">Links to W3C specifications</h3>
<xsl:variable name="fname" select="$MyFunction/name"/>
<xsl:variable name="p" select="signatures/proto[1]"/>
<p>Namespace: <xsl:value-of select="$MyFunction/name/@namespace"/></p>
<p>Applies to: <xsl:value-of select="f:specs($p/in-spec)"/></p>
<xsl:if test="$p/in-spec='xpath20'"><p><a href="http://www.w3.org/TR/xpath-functions/#func-{$fname}">XPath 2.0 Functions and Operators</a></p></xsl:if>
<xsl:if test="$p/in-spec='xpath30'"><p><a href="http://www.w3.org/TR/xpath-functions-30/#func-{$fname}">XPath 3.0 Functions and Operators</a></p></xsl:if>
<xsl:if test="$p/in-spec='xslt20' and not($p/in-spec='xpath20')"><p><a href="http://www.w3.org/TR/xslt20/#function-{$fname}">XSLT 2.0 Specification</a></p></xsl:if>
<xsl:if test="$p/in-spec='xslt30' and not($p/in-spec='xpath30')"><p><a href="http://www.w3.org/TR/xslt-21/#function-{$fname}">XSLT 2.1 Specification</a></p></xsl:if>
</xsl:if>
<h3 class="subtitle">Notes on the Saxon implementation</h3>            
<xsl:apply-templates select="status-ok" mode="f"/>
<xsl:apply-templates select="status/*" mode="f"/>
<xsl:apply-templates select="notes" mode="f"/>

</xsl:template>

<xsl:template match="div" xpath-default-namespace="" priority="8" mode="function-doc">
<xsl:apply-templates select="child::node()" mode="function-doc"/>
</xsl:template>

<xsl:template match="*[namespace-uri()='']" priority="7" mode="function-doc">
<xsl:copy-of select="."/>
</xsl:template>
    
<!--
<xsl:template match="status" mode="f">
<xsl:copy-of select="child::node()"/>
</xsl:template>

<xsl:template match="status[p]" mode="f">
<xsl:copy-of select="child::node()"/>
</xsl:template>
-->

<xsl:function name="f:specs" as="xs:string">
<xsl:param name="in" as="element(in-spec)*"/>
<xsl:choose>
<xsl:when test="$in = 'xpath20'">
<xsl:text>XPath 2.0, XSLT 2.0, XQuery 1.0 and later versions</xsl:text>
</xsl:when>
<xsl:when test="$in = 'xpath30'">
<xsl:text>XPath 3.0, XSLT 3.0, XQuery 3.0 (if enabled in Saxon: requires Saxon-PE or Saxon-EE)</xsl:text>
</xsl:when>
<xsl:when test="$in = 'xslt20'">
<xsl:text>XSLT 2.0 and later versions</xsl:text>
</xsl:when>
<xsl:when test="$in = 'xslt30'">
<xsl:text>XSLT 3.0 only (if enabled in Saxon: requires Saxon-PE or Saxon-EE)</xsl:text>
</xsl:when>
<xsl:otherwise>
<xsl:sequence select="''"/>
</xsl:otherwise>
</xsl:choose>
</xsl:function>
    
</xsl:stylesheet>

