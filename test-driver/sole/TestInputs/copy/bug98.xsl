<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
version="2.0">

  <!-- Bug 1930319 -->


	<xsl:output method="xml" indent="yes"/>

	<xsl:strip-space elements="*"/>
	<xsl:param name="grp-size" select="number(20)"/>
	
	<xsl:template match="/">
	  <out>
	    <xsl:apply-templates/>
	  </out>
	</xsl:template>

	<xsl:template match="//table">
	  <!-- Original bug was in optimizing this select expression (recursed indefinitely). -->
		<xsl:apply-templates mode="stategroup" select="//tbody/row[position() mod $grp-size=1]"/>
	</xsl:template>

	<xsl:template match="//tbody/row" mode="stategroup">
	 <xsl:variable name="num">
			<xsl:number count="row[position() mod $grp-size=1]"/>
	</xsl:variable>
		 <xsl:variable name="filename" select="concat('output/preprocerrs/preprocerr',$num)"/>
		
			<xsl:variable name ="ref-id" select="concat('preproc', $num,'- ref' )"/>
			<xsl:variable name ="ttl-id" select="concat('preproc', $num,'- ttl' )"/>
			<xsl:variable name ="tbl-id" select="concat('preproc', $num,'- tbl' )"/>

				<reference id="{$ref-id}">
					<title id="{$ttl-id}">Preprocessing Errors
						<xsl:variable name="start" select="./entry[@colname='col2']"/>
						<xsl:value-of select="$start[position() mod $grp-size=1]"/> through
						<xsl:apply-templates mode="varstate" select=". | following-sibling::row[position() &lt; $grp-size]">
						</xsl:apply-templates>
					</title>
					<shortdesc></shortdesc>
					<refbody>
						<table id="{$ttl-id}">
						<tgroup cols="4">
							<colspec column="1" colname="col1" colwidth="1*"/>
							<colspec column="2" colname="col2" colwidth="2.25*"/>
							<colspec column="3" colname="col3" colwidth="1*"/>
							<colspec column="4" colname="col5" colwidth="2.25*"/>
						<thead>
							<row>
								<entry colname="col1" align="center">Error Code</entry>
								<entry colname="col1" align="center">Type</entry>
								<entry colname="col1" align="center">Message</entry>
								<entry colname="col1" align="center">Possible Cause</entry>
							</row>
						</thead>
             				<tbody>
             					<xsl:apply-templates mode="stateitem" select=". |
following-sibling::row[position() &lt; $grp-size]">
             					</xsl:apply-templates>
             				</tbody>
					</tgroup>
				</table>
				</refbody>
			</reference>
	</xsl:template>
	
	
	<xsl:template match="//row" mode="stateitem">
		<xsl:variable name="msg-type" select="entry[@colname='col1']"/>
		<xsl:variable name="errnum" select="entry[@colname='col2']"/>
		<xsl:variable name="errmsg" select="entry[@colname='col3']"/>
		<xsl:variable name="poss-cause" select="entry[@colname='col4']"/>
		
		<row>

			<entry colname="col1" align="left" valign="top">
				<xsl:value-of select="$msg-type"/>
			</entry>
			<entry colname="col2" align="left" valign="top">
				<xsl:value-of select="$errnum"/>
			</entry>
				<entry colname="col3" align="left" valign="top">
				<xsl:value-of select="$errmsg"/>
				</entry>
			<entry colname="col4" align="left" valign="top">
				<xsl:value-of select="$poss-cause"/>
			</entry>
		</row>
	</xsl:template>
	
	
	<xsl:template match="//row" mode="varstate">
		<xsl:choose>
			<xsl:when test="position()=last()">
				<xsl:value-of select="entry[@colname='col2']"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	
	
</xsl:stylesheet>
