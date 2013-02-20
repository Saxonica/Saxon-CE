<?xml version="1.0" encoding="utf-8" standalone="no"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">

<?spec xslt#copy-of?>    
  <!-- Creator: David Marston -->
  <!-- Purpose: Test copy-of a string constant containing character entity -->

<xsl:output method="xml" encoding="ISO-8859-1"/>
<!-- With this output encoding, should get one byte of xE8 for the &egrave -->

<xsl:template match="/">
<out>
<xsl:copy-of select="'abcdèfgh'"/>
</out>
</xsl:template>

</xsl:stylesheet>
