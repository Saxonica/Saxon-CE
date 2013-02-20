<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs='http://www.w3.org/2001/XMLSchema'
  exclude-result-prefixes="xs">

  <!-- Creator: Phil Fearon -->
  <!-- Purpose: Test names and namespaces of attributes and elements -->

<xsl:template match="/">
<out>
<root_element><xsl:value-of select="node-name(*)"/></root_element>
<xsl:variable name="child" select="*/*[1]" as="element()"/>
<child_element><xsl:value-of select="node-name($child)"/></child_element>
<xsl:variable name="name" select="name(*/*[1])"/>
<xsl:variable name="attributeOfChild" select="$child/@*[1]"/>
<child_element>
<xsl:value-of select="'{',namespace-uri($child),'}', local-name($child)" separator=""/>
</child_element>
<child_attribute>
<xsl:value-of select="'{',namespace-uri($attributeOfChild),'}', local-name($attributeOfChild)" separator=""/>
</child_attribute>

</out>
</xsl:template>

</xsl:stylesheet>
