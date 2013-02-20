<html xsl:version="2.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Summary</title>
  </head>
  <body>
    <p>Total Amount: <xsl:value-of select="sum(doc/body/price)"/></p>
  </body>
</html>
