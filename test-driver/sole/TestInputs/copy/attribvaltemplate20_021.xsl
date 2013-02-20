<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- FileName: attribvaltemplate20_021 -->
  <!-- Document: http://www.w3.org/TR/xslt20/ -->
  <!-- DocVersion: 20051103 -->
  <!-- Section: 13.1 xsl:sort-->
  <!-- Section: 5.6 Attribute Value Templates-->
  <!-- Creator: Vesela Parapounska -->
  <!-- Purpose: Test of single AVT in @stable of xsl:sort. AVT value has 
  				no literal element and comes from a node.-->

<xsl:template match="/doc">
  <out>
  	<xsl:for-each select="item">
  		<xsl:sort lang="en" stable="{/doc/case1}"/>
  		<xsl:sort select="@attr" lang="en" case-order="lower-first"/>
  		<xsl:value-of select="@num" />
  		<xsl:value-of select="." /><xsl:text>@</xsl:text>
  		<xsl:value-of select="@attr" />
  		<xsl:text> * </xsl:text>
  	</xsl:for-each>
  </out>
</xsl:template>


  <!-- Copyright IBM Corp. 2004, 2005. -->

</xsl:stylesheet>