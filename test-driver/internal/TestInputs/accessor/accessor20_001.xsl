<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- FileName: accessor20_001 -->
  <!-- Document: http://www.w3.org/TR/xpath-functions -->
  <!-- DocVersion: 20051103 -->
  <!-- Section: 2.1 fn:node-name -->
  <!-- Creator: Yan Luo -->
  <!-- Purpose: Test of node-name() accessor with document, element, attr, txt, comment, and PI nodes -->

<xsl:template match="/">
  <out>
    <doc_node>doc_name="<xsl:value-of select="node-name(.)"/>"
    </doc_node>
    <xsl:apply-templates select="*"/>
    <xsl:text>|done|</xsl:text>
  </out>
</xsl:template>

<xsl:template match="doc">
  <xsl:text>&#10;</xsl:text>
  <Elmt>name="<xsl:value-of select="node-name(.)"/>"
  </Elmt>
  <xsl:apply-templates select="@*|node()"/>
  <xsl:text>|up|</xsl:text>
  <xsl:for-each select="namespace::*[name(.)='mynamespace']">
    <xsl:value-of select="node-name(.)"/>
  </xsl:for-each>

</xsl:template>

<xsl:template match="element()">
  <xsl:text>&#10;</xsl:text>
  <Elmt>name="<xsl:value-of select="node-name(.)"/>"
  </Elmt>
  <xsl:apply-templates select="@*|node()"/>
  <xsl:text>|up|</xsl:text>
</xsl:template>

<xsl:template match="processing-instruction()">
  <PI>name="<xsl:value-of select="node-name(.)"/>"
  </PI>
</xsl:template>

<xsl:template match="attribute()">
  <Attr>name="<xsl:value-of select="node-name(.)"/>"
  </Attr>
</xsl:template>

<xsl:template match="text()">
  
  <Txt>name="<xsl:value-of select="node-name(.)"/>"
  </Txt>
</xsl:template>

<xsl:template match="comment()">
  <Cmt>name="<xsl:value-of select="node-name(.)"/>"
  </Cmt>
</xsl:template>

  <!-- Copyright IBM Corp. 2004, 2005. -->

</xsl:stylesheet>
