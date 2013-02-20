<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- FileName: accessor20_003 -->
  <!-- Document: http://www.w3.org/TR/xpath-functions -->
  <!-- DocVersion: 20051103 -->
  <!-- Section: 2.1 fn:node-name -->
  <!-- Creator: David Marston, Joe Kesseman, Joanne Tong -->
  <!-- Purpose: Test of node-name() accessor with node stored in variable -->

<xsl:template match="doc">
  <out>
    <t1>
      <xsl:variable name="var1" select="processing-instruction()"/>
      <xsl:value-of select="node-name($var1)"/>
    </t1>

    <t2>
		<xsl:variable name="var2" select="comment()"/>
		<xsl:value-of select="node-name($var2)"/>
    </t2>
    <t3>
		<xsl:variable name="var3" select="text()[1]"/>
		<xsl:value-of select="node-name($var3)"/>
    </t3>
    <t4>
		<xsl:variable name="var4" select="element()"/>
		<xsl:value-of select="node-name($var4)"/>
    </t4>
    <t5>
		<xsl:variable name="var5" select="attribute()"/>
		<xsl:value-of select="node-name($var5)"/>
    </t5>
    
    <t6>
		<xsl:variable name="var6" select="namespace::*"/>
		<xsl:for-each select="$var6[name(.)='mynamespace']">
			<xsl:value-of select="node-name(.)"/>
		</xsl:for-each>
    </t6>
  </out>
</xsl:template>

  <!-- Copyright IBM Corp. 2004, 2005. -->

</xsl:stylesheet>
