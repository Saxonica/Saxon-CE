<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

  <!-- FileName: embed20_020 -->
  <!-- Document: http://www.w3.org/TR/xslt20 -->
  <!-- DocVersion: 20051103 -->
  <!-- Section: 3.11 Embedded Stylesheet Modules -->
  <!-- Creator: Atosa Khoddamhazrati -->
  <!-- Purpose: Test case in which a standalone standard stylesheet imports a standalone simplified stylesheet. -->

	<xsl:import href="standalone_simplified.xsl"/>
	
	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:template match="doc">
	  <out>
	    <xsl:apply-templates/>
	  </out>
	</xsl:template>
	
	<!-- Copyright IBM Corp. 2006. -->

</xsl:stylesheet>
