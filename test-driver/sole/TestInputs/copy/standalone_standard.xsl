<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

<!-- FileName: standalone_standard.xsl -->
<!-- Purpose: Has a template to be called by another stylesheet that imports/includes this.-->

<xsl:template match="tag">
  <xsl:param name="size" select="31" tunnel="yes"/>
  <pre>
    <xsl:value-of select="$size"/>
    <xsl:apply-templates/>
  </pre>
</xsl:template>

</xsl:stylesheet>