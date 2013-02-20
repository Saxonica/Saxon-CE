<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:xs="http://www.w3.org/2001/XMLSchema">
<xsl:template match="test">
<test>
<cmt><xsl:value-of select="comment()"/></cmt>
<pi><xsl:value-of select="processing-instruction('mode')"/></pi>
<piname><xsl:value-of select="name(processing-instruction('mode')[1])"/></piname>
</test>
</xsl:template>
</xsl:stylesheet>

