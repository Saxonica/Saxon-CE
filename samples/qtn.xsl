<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="2.0">
<xsl:template match="/" name="main"> 
 
<html>
<head>
<title>Transfomed</title>
</head>
<body>
<p><xsl:value-of select="//book" separator=" [sep] "/></p>
<xsl:apply-templates select="//book"/>
</body>
</html>
    
</xsl:template>

<xsl:template match="book">
<p>
<span style="color:blue"><xsl:value-of select="title"/></span>
&#160;
<span style="color:green"><xsl:value-of select="isbn"/></span>
</p>
</xsl:template>
     
</xsl:transform>	
