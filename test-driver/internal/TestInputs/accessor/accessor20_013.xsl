<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs='http://www.w3.org/2001/XMLSchema'
  exclude-result-prefixes="xs">

  <!-- FileName: accessor20_013 -->
  <!-- Document: http://www.w3.org/TR/xpath-functions -->
  <!-- DocVersion: 20051103 -->
  <!-- Section: 2.4 fn:data -->
  <!-- Creator: Yan Luo -->
  <!-- Purpose: Test of fn:data accessor with no schema validation, context node set to document, element, attr, txt, comment, and PI nodes -->

<xsl:template match="/">
  <out>
    <doc_node>doc_typed_value="<xsl:value-of select="if (data(.) instance of xs:untypedAtomic) then data(.) else 'WROONG TYPE returned'"/>"</doc_node><!--same as string(), type is untypedAtomic -->
    <xsl:apply-templates select="*"/>
    <xsl:text>|done|</xsl:text>
  </out>
</xsl:template>

<xsl:template match="doc">
  <xsl:text>&#10;</xsl:text>
  <Elmt>typed_value="<xsl:value-of select="if (data(.) instance of xs:untypedAtomic) then data(.) else 'WROONG TYPE returned'"/>"</Elmt>
  <xsl:apply-templates select="@*|node()"/>
  <xsl:text>|up|</xsl:text>
  <xsl:for-each select="namespace::*[name(.)='sub']">
	    <xsl:value-of select="if (data(.) instance of xs:string) then data(.) else 'WROONG TYPE returned'"/>
  </xsl:for-each>
</xsl:template>

<xsl:template match="element()">
  <xsl:text>&#10;</xsl:text>
  <Elmt>typed_value="<xsl:value-of select="if (data(.) instance of xs:untypedAtomic) then data(.) else 'WROONG TYPE returned'"/>"</Elmt>
  <xsl:apply-templates select="@*|node()"/>
  <xsl:text>|up|</xsl:text>
</xsl:template>

<xsl:template match="attribute()">
  <Attr>typed_value="<xsl:value-of select="if (data(.) instance of xs:untypedAtomic) then data(.) else 'WROONG TYPE returned'"/>"</Attr>
</xsl:template>

<xsl:template match="processing-instruction()"><!--same as string()-->
  <PI>typed_value="<xsl:value-of select="if (data(.) instance of xs:string) then data(.) else 'WROONG TYPE returned'"/>"</PI>
</xsl:template>

<xsl:template match="comment()"><!--same as string()-->
  <Cmt>typed_value="<xsl:value-of select="if (data(.) instance of xs:string) then data(.) else 'WROONG TYPE returned'"/>"</Cmt>
</xsl:template>

<xsl:template match="text()">
  
  <Txt>typed_value="<xsl:value-of select="if (data(.) instance of xs:untypedAtomic) then data(.) else 'WROONG TYPE returned'"/>"</Txt><!--same as string() type is untypedAtomic-->
</xsl:template>


  <!-- Copyright IBM Corp. 2004, 2005. -->

</xsl:stylesheet>
