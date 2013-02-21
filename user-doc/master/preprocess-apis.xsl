<xsl:stylesheet id="ss" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:f="http://local/functions"
    exclude-result-prefixes="xs f"
    xpath-default-namespace="http://www.saxonica.com/ns/doc/functions">
    
    <!-- Stylesheet to generate api.xml from api-data.xml -->
    <!-- Note that this stylesheet is schema-aware and that the schema requires XSD 1.1 -->

    <xsl:import-schema namespace="http://www.saxonica.com/ns/doc/functions"
        schema-location="apis-schema.xsd"/>
    <xsl:output indent="yes"/>

    <xsl:template match="xsl:stylesheet" mode="f"/>

    <xsl:key name="fk" match="function|property" use="name"/>

    <xsl:template match="/">
        <xsl:result-document validation="strip">
            <xsl:apply-templates select="//apis" mode="f"/>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="apis" mode="f">
        <section xml:id="{@section}">
            <title>
                <xsl:value-of select="@title"/>
            </title>

            <xsl:apply-templates mode="table" select="api[@name eq 'intro']"/>

            <xsl:apply-templates mode="table" select="api[@name eq 'using']"/>

            <xsl:apply-templates mode="table" select="api[@name eq 'command']"/>

            <xsl:apply-templates mode="table" select="api[@name eq 'saxon']"/>

            <xsl:apply-templates mode="table" select="api[@name eq 'xslt20processor']"/>

            <xsl:apply-templates mode="table" select="api[@name eq 'resultdocuments']"/>

        </section>
    </xsl:template>

    <xsl:template match="api" mode="table">


        <page xml:id="{@name}">

            <title>
                <xsl:value-of select="title"/>
            </title>

            <xsl:apply-templates select="description" mode="f"/>

            <xsl:apply-templates select="functions|properties" mode="table"/>

            <xsl:if test="@name='intro'">
                <pageindex/>
            </xsl:if>

            <xsl:if test="footer">
                <subtitle>Notes</subtitle>
                <xsl:apply-templates select="footer" mode="f"/>
            </xsl:if>


        </page>
    </xsl:template>

    <xsl:template match="functions" mode="table">
        <xsl:variable name="sfns" select="f:process-functions(function)" as="element()*"/>
        <subtitle>Functions</subtitle>
        <table>
            <thead>
                <tr>
                    <td>
                        <p>
                            <b>Function</b>
                        </p>
                    </td>
                    <td>
                        <p>
                            <b>Description</b>
                        </p>
                    </td>
                </tr>
            </thead>
            <xsl:apply-templates select="$sfns" mode="function-index"/>
        </table>

        <xsl:apply-templates select="$sfns" mode="f"/>
    </xsl:template>

    <xsl:function name="f:process-functions" as="element()*">
        <xsl:param name="fns" as="element()*"/>

        <xsl:perform-sort select="$fns[. is key('fk', name)[1]]">
            <xsl:sort select="name" lang="en"/>
        </xsl:perform-sort>
    </xsl:function>

    <xsl:template match="*" mode="function-index">
        <tr>
            <td valign="top">
                <p>
                    <code>
                        <xref section="api" page="{../../@name}" subpage="{name}">
                            <xsl:value-of
                                select="concat(signatures/proto[1]/@name,'(',string-join(signatures/proto[1]/arg/@name,', '),')')"
                            />
                        </xref>
                    </code>
                </p>
            </td>
            <td valign="top">
                <p>
                    <xsl:value-of select="signatures/proto/description/*"/>
                </p>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="properties" mode="table">
        <xsl:variable name="spns" select="f:process-functions(property)" as="element()*"/>
        <subtitle>Properties</subtitle>
        <table>
            <thead>
                <tr>
                    <td>
                        <p>
                            <b>Property</b>
                        </p>
                    </td>
                    <td>
                        <p>
                            <b>Type</b>
                        </p>
                    </td>
                    <td>
                        <p>
                            <b>Description</b>
                        </p>
                    </td>
                </tr>
            </thead>
            <xsl:apply-templates select="$spns" mode="property-index"/>
        </table>
    </xsl:template>

    <xsl:template match="*" mode="property-index">
        <tr>
            <td valign="top">
                <p>
                    <xsl:choose>
                        <xsl:when test="@required eq 'yes'">
                            <i>
                                <xsl:value-of select="name"/>
                            </i>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="name"/>
                        </xsl:otherwise>
                    </xsl:choose>

                </p>
            </td>
            <td valign="top">
                <p>
                    <xsl:value-of select="@type"/>
                </p>
            </td>
            <td valign="top">
                <p>
                    <xsl:value-of select="description/*"/>
                </p>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="function[name]" priority="5" mode="f">
        <xsl:variable name="MyFunction" select="."/>
        <subpage xml:id="{name}">
            <title>
                <xsl:value-of select="name"/>
            </title>
            <xsl:copy-of select="description/*"/>
            <xsl:variable name="same-spec-references" as="xs:boolean"
                select="
every $proto in subsequence(signatures/proto, 2)
satisfies deep-equal(signatures/proto[1]/in-spec, $proto/in-spec)"/>
            <xsl:for-each select="signatures/proto">
                <xsl:variable name="MyProto" select="."/>
                <subtitle>
                    <xsl:value-of
                        select="concat(@name,'(',string-join(for $arg in arg return concat('$',$arg/@name, ' as ', $arg/@type), ', ') ,')', ' → ',@return-type)"
                    />
                </subtitle>
                <xsl:copy-of select="description/*"/>
                <table>
                    <tr>
                        <td width="470" align="left" colspan="4"
                            style="border-top:solid 1px #3D5B96; ">
                            <p>
                                <i>
                                    <xsl:value-of
                                        select="if (exists(arg)) then 'Arguments' else 'There are no arguments'"
                                    />
                                </i>
                            </p>
                        </td>
                    </tr>
                    <xsl:for-each select="arg">
                        <tr>
                            <td width="40">
                                <p>&#xa0;</p>
                            </td>
                            <td width="80" valign="top">
                                <p>$<xsl:value-of select="@name"/></p>
                            </td>
                            <td valign="top" width="150">
                                <p>
                                    <xsl:value-of select="@type"/>
                                </p>
                            </td>
                            <td valign="top" width="200">
                                <p>
                                    <xsl:value-of select="@desc"/>
                                </p>
                            </td>
                        </tr>
                    </xsl:for-each>
                    <tr>
                        <td colspan="2"
                            style="border-top:solid 1px #3D5B96; border-bottom:solid 1px #3D5B96;">
                            <p>
                                <i>Result</i>
                            </p>
                        </td>

                        <td style="border-top:solid 1px #3D5B96; border-bottom:solid 1px #3D5B96;"
                            colspan="2">
                            <p>
                                <xsl:value-of select="$MyProto/@return-type"/>
                            </p>
                        </td>

                    </tr>
                </table>

            </xsl:for-each>

            <subtitle>Details</subtitle>
            <!--<xsl:apply-templates select="status|status-ok" mode="f"/>-->
            <xsl:apply-templates select="details" mode="f"/>


        </subpage>
    </xsl:template>

    <xsl:template match="div" xpath-default-namespace="" priority="8" mode="function-doc">
        <xsl:apply-templates select="child::node()" mode="function-doc"/>
    </xsl:template>

    <xsl:template match="*[namespace-uri()='']" priority="7" mode="function-doc">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="*" mode="fr">
        <xsl:element name="{local-name(.)}">
            <xsl:apply-templates mode="fr" select="node()|@*"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*" mode="fr">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="status-ok" mode="f">
        <p>The function is fully implemented according to the W3C specifications.</p>
    </xsl:template>

    <xsl:template match="details|description|footer" mode="f">
        <xsl:apply-templates mode="fr" select="node()|@*"/>
    </xsl:template>

</xsl:stylesheet>
