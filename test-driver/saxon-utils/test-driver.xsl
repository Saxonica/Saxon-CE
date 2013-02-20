<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:cat="http://www.w3.org/2005/05/xslt20-test-catalog"
xmlns:xs="http://www.w3.org/2001/XMLSchema"
xmlns:local="http://local.functions/"
xmlns:js="http://saxonica.com/ns/globalJS"
xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
xmlns:f="http://www.saxonica.com/ce-testing/functions"
xmlns:style="http://saxonica.com/ns/html-style-property"
extension-element-prefixes="ixsl"
version="2.0">

<!-- 
 Process all the tests in the XSLT20 test suite for Saxon-CE.
 The source document is the XSLT20 test suite catalog file (because this is the largest file -
 so benefits from async loading, other tests may be loaded using doc() ).
 For each test case, the transform command is built and then run. The result is then
 compared with the expected result, using the imported compare-utils.xsl template

 This XSLT is run from an embedded xslt2.0 script element in test-driver.html - the host html page
 Both Saxonce and SaxonceDebug builds should be tested - but only SaxonceDebug will provide details on
 compile-time errors

 Pre-processing required for w3c test suite:
 The required expanded-catalog.xml file is generated using an identity transform on the w3c catalog.xml
 which references a collection external general parsed entities in catname.entity files located in a 'cat' directory 

 Required files and directories:
 1. test-driver.xsl              this file
 2. test-driver.html             host html page
 3. expanded-catalog.xml         w3c catalog file with entities expanded (see above)
 4. test-driver.js               hosts javascript functions called from here to run each test case
 5. test-driver.css              style html table etc.
 6. compare-utils.xsl            imported xslt - does 'smart' deep-equal and serializes results
 7. TestInputs                   directory containing xslt and data files required for each test
 8. ExpectedTestResults          directory containing a file for each test showing the expected output
 9. categories.xml               w3c suite file - used only to generate dropdown list of categories
 10. exceptions.xml              saxon-ce file - used to exclude non-applicable w3c tests from test run

 For preprocessing - only when w3c testsuite changes:
 11. catalog.xml                  w3c catalog files with unresolved general entity references
 12. cat                          w3c directory containing .entity files
-->


<xsl:import href="compare-utils.xsl"/>

<xsl:variable name="xslt20testdir"      select="'../../w3c-xslt20/TestInputs/'"/>
<xsl:variable name="xslt20resultsdir"   select="'../../w3c-xslt20/ExpectedTestResults/'"/>
<xsl:variable name="xsltcategories"   select="'../w3c-tests/categories.xml'"/>
<xsl:variable name="xsltexceptions"   select="doc('../w3c-tests/exceptions.xml')" as="document-node()"/>


<xsl:variable name="internaltestdir"      select="'../internal/TestInputs/'"/>
<xsl:variable name="internalresultsdir"   select="'../internal/ExpectedTestResults/'"/>
<xsl:variable name="internalcat"   select="'../internal/internal-catalog.xml'"/>
<xsl:variable name="internalcategories"   select="'../internal/internal-categories.xml'"/>

<xsl:variable name="solecat"   select="'../sole/sole-catalog.xml'"/>
<xsl:variable name="solecategories"   select="'../sole/sole-categories.xml'"/>
<xsl:variable name="soletestdir"      select="'../sole/TestInputs/'"/>
<xsl:variable name="soleresultsdir"   select="'../sole/ExpectedTestResults/'"/>

<xsl:variable name="saxon" select="ixsl:get(ixsl:window(), 'Saxon')"/>

<xsl:template match="/">
<xsl:variable name="version" select="ixsl:call($saxon, 'getVersion')"/>
<xsl:result-document href="#version">
<xsl:value-of select="$version"/>
</xsl:result-document>
<xsl:call-template name="updateCategories"/>
</xsl:template>

<xsl:template name="updateCategories">

<xsl:variable name="testType" select="xs:integer(ixsl:get(ixsl:page()/html/body/div[@id='heading']/div/select[@id='testtype']
,'selectedIndex'))" as="xs:integer"/>

<xsl:variable name="categoryFile"
select="if ($testType eq 0) 
    then $xsltcategories
else
    if ($testType eq 1) 
        then $internalcategories
    else $solecategories"
as="xs:string"/>
<xsl:message>category file: <xsl:value-of select="$categoryFile"/></xsl:message>
<xsl:variable name="docfile" as="document-node()" select="doc($categoryFile)"/>

<xsl:result-document href="#category" method="replace-content">
<option value="[All]" data-title="Run tests for all categories">[All]</option>
<xsl:for-each select="$docfile/categories/category">
<option value="{@name}" data-title="{@desc}"><xsl:value-of select="@name"/></option>
</xsl:for-each>
</xsl:result-document>
</xsl:template>

<xsl:template match="select[@id = 'testtype']" mode="ixsl:onchange">
<xsl:call-template name="updateCategories"/>
</xsl:template>

<xsl:template match="select[@id = 'category']" mode="ixsl:onclick">
<xsl:variable name="index" select="ixsl:get(.,'selectedIndex')" as="xs:double"/>
<xsl:result-document href="#description" method="replace-content">
<xsl:value-of select="option[$index + 1]/@data-title"/>
</xsl:result-document>
<xsl:result-document href="#option" method="replace-content">
<xsl:value-of select="option[$index + 1]/@value"/>
</xsl:result-document>
</xsl:template>

<xsl:template match="select[@id = 'show']" mode="ixsl:onclick">
<xsl:variable name="index" select="ixsl:get(.,'selectedIndex')" as="xs:double"/>
<xsl:result-document href="#showOption" method="replace-content">
<xsl:value-of select="option[$index + 1]/@value"/>
</xsl:result-document>
</xsl:template>

<xsl:template match="button[@id = 'stop']" mode="ixsl:onclick">
<ixsl:set-attribute name="data-cancel" select="'yes'"/>
<xsl:for-each select="parent::*/button[@id = 'start']">
<ixsl:remove-attribute name="disabled"/>
</xsl:for-each>
</xsl:template>

<xsl:template match="button[@id = 'clear']" mode="ixsl:onclick">
<xsl:result-document href="#results" method="replace-content"/>
</xsl:template>

<xsl:template match="button[@id eq 'start']" mode="ixsl:onclick">

<ixsl:set-attribute name="disabled" select="'disabled'"/>
<xsl:for-each select="parent::*/button[@id eq 'stop']">
<ixsl:remove-attribute name="disabled"/>
<ixsl:set-attribute name="data-cancel" select="'no'"/>
</xsl:for-each>
<xsl:result-document href="#count" method="replace-content">
<xsl:value-of select="1"/>
</xsl:result-document>
<xsl:for-each select="/html/body/div[1][@id='heading']/h1/span[@id='status']">
<ixsl:set-attribute name="style:display" select="'inline'"/>
</xsl:for-each>
<xsl:variable name="failsonly" select="/html/body/div[@id = 'control']/span[@id = 'showOption'] = 'fails'" as="xs:boolean"/>
<xsl:variable name="testType" select="xs:integer(ixsl:get(/html/body/div[@id='heading']/div/select[@id='testtype']
,'selectedIndex'))" as="xs:integer"/>

<xsl:variable name="altpaths"
select="if ($testType eq 0) 
    then ('', $xslt20testdir, $xslt20resultsdir)
else
    if ($testType eq 1) 
        then ($internalcat, $internaltestdir , $internalresultsdir)
    else ($solecat, $soletestdir, $soleresultsdir)"
as="xs:string*"/>

<xsl:result-document href="#test" method="replace-content">
<p>[Empty]</p>
</xsl:result-document>

<ixsl:schedule-action wait="1">
<xsl:call-template name="loadTests">
<xsl:with-param name="startButton" select="."/>
<xsl:with-param name="failsonly" select="$failsonly" tunnel="yes"/>
<xsl:with-param name="altpaths" select="$altpaths"/>
</xsl:call-template>
</ixsl:schedule-action>
</xsl:template>

<xsl:template name="loadTests">
<xsl:param name="startButton" as="element()"/>
<xsl:param name="altpaths" as="xs:string*"/>

<xsl:variable name="altsource" as="document-node()"
select="if ($altpaths[1] eq '') then ixsl:source()
else doc($altpaths[1])"/>

<!-- Not yet exploited -->
<xsl:variable name="scripts" as="xs:string*"
select="$altsource/cat:testcases/cat:js-scripts/cat:script"/>

<xsl:for-each select="$scripts">
<xsl:sequence select="js:includeJS(.)"/>
</xsl:for-each>

<xsl:for-each select="$startButton">

<xsl:variable name="requestedStart" select="number(ixsl:get(parent::*/input[@id eq 'testStartPos'], 'value'))" as="xs:double"/>
<xsl:variable name="requestedCount" select="number(ixsl:get(parent::*/input[@id eq 'maxTestCount'], 'value'))" as="xs:double"/>
<xsl:variable name="requestedCategory" select="parent::*/span[@id eq 'option']" as="xs:string"/>

<xsl:for-each select="/html/body/div[1][@id='heading']/h1/span[@id='status']">
<ixsl:set-attribute name="style:display" select="'none'"/>
</xsl:for-each>

<xsl:variable name="testCases" as="element()*"
select="if ($requestedCategory ne '[All]')
    then subsequence($altsource/cat:testcases/cat:testcase[cat:category = $requestedCategory and local:is-included(.)], $requestedStart, $requestedCount)
    else subsequence($altsource/cat:testcases/cat:testcase[local:is-included(.)], $requestedStart, $requestedCount)"/>
<xsl:variable name="batchSize" select="5"/>

<xsl:variable name="numberOfBatchesEx" select="count($testCases) idiv $batchSize" as="xs:integer"/>
<xsl:variable name="increment" select="if (count($testCases) mod $batchSize ne 0) then 1 else 0" as="xs:integer"/>
<xsl:variable name="numberOfBatches" select="$numberOfBatchesEx + $increment" as="xs:integer"/>


<xsl:result-document href="#total" method="replace-content">
<xsl:value-of select="count($testCases)"/>
</xsl:result-document>

<ixsl:schedule-action wait="1">

<xsl:call-template name="runBatch">
<xsl:with-param name="batchPos" select="1"/>
<xsl:with-param name="batchCount" select="$numberOfBatches" as="xs:integer"/>
<xsl:with-param name="testCases" select="$testCases"/>
<xsl:with-param name="batchSize" select="$batchSize"/>
<xsl:with-param name="passesFails" select="(0, 0)" as="xs:integer*"/>
<xsl:with-param name="altpaths" select="$altpaths" as="xs:string*" tunnel="yes"/>
</xsl:call-template>

</ixsl:schedule-action>

</xsl:for-each>

</xsl:template>

<xsl:template name="runBatch">
<xsl:param name="batchPos" as="xs:integer"/>
<xsl:param name="batchCount" as="xs:integer"/>
<xsl:param name="testCases" as="element()*"/>
<xsl:param name="batchSize" as="xs:integer"/>
<xsl:param name="passesFails" as="xs:integer*"/>
<xsl:param name="failsonly" as="xs:boolean" tunnel="yes"/>

<!-- Get items for this batch and process: -->
<xsl:variable name="batchItems"
select="subsequence($testCases, ($batchPos * $batchSize) - ($batchSize - 1), $batchSize)"
as="element()*"/>
<xsl:variable name="rows" as="element()*">
<xsl:for-each select="1 to count($batchItems)">
<xsl:variable name="index" as="xs:double" select="number(.)"/>
<xsl:call-template name="test">
<xsl:with-param name="testCase" as="element()" select="$batchItems[$index]"/>
<xsl:with-param name="index" as="xs:integer" select="$passesFails[1] + $passesFails[2] + ."/>
</xsl:call-template>
</xsl:for-each>
</xsl:variable>

<xsl:variable name="passes" select="$passesFails[1] + count($rows[td[5] = 'Pass'])"/>
<xsl:variable name="fails" select="$passesFails[2] + count($rows[td[5] = 'Fail'])"/>

<xsl:result-document href="#passes" method="replace-content">
<xsl:value-of select="concat($passes, ' Fails: ', $fails)"/>
</xsl:result-document>


<xsl:result-document href="#results" method="append-content">
<xsl:sequence select="if ($failsonly) then $rows[td[5] = 'Fail'] else $rows"/>
</xsl:result-document>

<xsl:result-document href="#count" method="replace-content">
<xsl:value-of select="$passes + $fails"/>
</xsl:result-document>

<xsl:variable name="continue" as="xs:boolean"
select="$batchPos lt $batchCount
and exists(ixsl:page()/html/body/div[@id='control']/button[@id='start']/@disabled)"
/>

<xsl:if test="not($continue)">
<xsl:call-template name="resetButtons"/>
</xsl:if>

<xsl:if test="$continue">
<ixsl:schedule-action wait="1">

<xsl:call-template name="runBatch">
<xsl:with-param name="batchPos" select="$batchPos + 1"/>
<xsl:with-param name="batchCount" select="$batchCount" as="xs:integer"/>
<xsl:with-param name="testCases" select="$testCases"/>
<xsl:with-param name="batchSize" select="$batchSize"/>
<xsl:with-param name="passesFails" select="($passes, $fails)" as="xs:integer*"/>
</xsl:call-template>

</ixsl:schedule-action>
</xsl:if>
</xsl:template>

<xsl:template name="resetButtons">
<xsl:for-each select="ixsl:page()/html/body/div[@id eq 'control']/button[@id eq 'start']">
<ixsl:remove-attribute name="disabled"/>
<xsl:for-each select="parent::*/button[@id eq 'stop']">
<ixsl:set-attribute name="disabled" select="'disabled'"/>
</xsl:for-each>
</xsl:for-each>
</xsl:template>

<xsl:function name="local:stringValue" as="xs:string">
<xsl:param name="node" as="node()?"/>
<xsl:value-of select="if (exists($node)) then $node else ''"/>
</xsl:function>


<xsl:template name="test" as="element()">
<xsl:param name="testCase" as="element()"/>
<xsl:param name="index" as="xs:integer"/>
<xsl:param name="altpaths" as="xs:string*" tunnel="yes"/>

<xsl:for-each select="$testCase">

<xsl:variable name="stylesheet"         select="concat($altpaths[2],cat:input/cat:stylesheet[@role='principal']/@file)"
as="xs:string"/>

<xsl:variable name="rawdir"             select="substring-before($stylesheet, '/')"                      as="xs:string"/>
<xsl:variable name="dir"                select="if ($rawdir) then $rawdir else 'catalog'"                as="xs:string"/>

<xsl:variable name="initial-template"   select="local:stringValue(cat:input/cat:entry-named-template/@qname)"  as="xs:string"/>
<xsl:variable name="initial-mode"       select="local:stringValue(cat:input/cat:initial-mode/@qname)"          as="xs:string"/>

<xsl:variable name="file" as="node()?"   select="cat:input/cat:source-document[@role='principal']/@file"/>
<xsl:variable name="source"             select="if (exists($file)) then concat($altpaths[2], $file) else ''" 
as="xs:string"/>

<xsl:variable name="isHTMLUpdate"
select="exists(cat:discretionary-items/cat:discretionary-feature[@name='update-html'])"
as="xs:boolean"/>

<xsl:variable name="cmd"  select="js:makeCommand((
'stylesheet',        $stylesheet,
'source',            $source,
'initialTemplate',    $initial-template,
'initialMode',       $initial-mode
))"/>

<xsl:variable name="result" select="if ($isHTMLUpdate) then js:update($cmd) else js:transform($cmd)" as="node()?"/>

<xsl:variable name="eresult" as="node()?">
<xsl:variable name="out" select="cat:output/cat:result-document[@role='principal']" as="node()?"/>
<xsl:variable name="result-uri" select="concat($altpaths[3], $out/@file)" as="xs:string"/>
<xsl:if test="doc-available(trace($result-uri,'result-uri'))">
<xsl:choose>
<xsl:when test="$out/@type = 'xml'">
<xsl:sequence select="doc($result-uri)"/>
</xsl:when>
<xsl:when test="$out/@type = 'xml-frag'">
<xsl:variable name="frag" select="unparsed-text($result-uri)"/>
<xsl:variable name="frag2" select="if (starts-with($frag, '&lt;?')) then substring-after($frag, '?&gt;') else $frag"/>
<xsl:sequence select="js:parseXML(concat('&lt;a&gt;',$frag2,'&lt;/a&gt;'))"/>
</xsl:when>
</xsl:choose>
</xsl:if>                   
</xsl:variable>

<!-- TODO: Enhance to get xsl:message output - needs Saxon setErrorHandler -->
<xsl:variable name="message-output" as="xs:string?"
select="ixsl:get($saxon, 'message')"/>

<xsl:variable name="uresult"
select="if ($isHTMLUpdate)
then ixsl:page()/html/body/div[@id='test']
else $result" as="node()?"/>

<xsl:variable name="uEresult" select="if ($isHTMLUpdate) then $eresult/*[1] else $eresult" as="node()?"/>


<xsl:variable name="comparison" as="xs:string?"
select="if(exists($uresult) and exists($uEresult))
then f:deep-equal($uresult, $uEresult, '/')
else concat('(E) ', $message-output)"/>

<!-- This message is useful for diagnostics when test case fails suddenly in a batch -->
<xsl:if test="empty($uresult)">
<xsl:message>Stylesheet error: <xsl:value-of select="$stylesheet"/></xsl:message>
</xsl:if>

<xsl:variable name="passed" select="empty($comparison)" as="xs:boolean"/>

<tr>

<td><xsl:value-of select="$index"/></td>
<td><xsl:value-of select="cat:category"/></td>

<td><a href="{$stylesheet}" target="_blank"><xsl:value-of select="cat:name"/></a></td>
<td><a href="{$source}" target="_blank"><xsl:value-of select="tokenize($source,'/')[last()]"/></a></td>
<td>
<xsl:if test="empty($result)">
<xsl:attribute name="class" select="'error'"/>
</xsl:if>
<xsl:value-of select="if ($passed) then 'Pass' else 'Fail'"/></td>
<td>
<xsl:value-of select="if (string-length($comparison) gt 150)
then concat(substring($comparison, 0, 150), '...')
else $comparison"/>
</td>

<xsl:choose>
<xsl:when test="$passed">
<td></td><td></td>
</xsl:when>
<xsl:otherwise>
<td><xsl:value-of select="if (exists($uresult)) then js:serializeXML($uresult) else '[no result node]'"/></td>
<td>
<xsl:value-of select="if (exists($eresult)) then js:serializeXML($eresult) else '[file not available or corrupt]'"/>
</td>
</xsl:otherwise>
</xsl:choose>

</tr>
</xsl:for-each>

</xsl:template>


<xsl:function name="local:is-included" as="xs:boolean">
<xsl:param name="test" as="element(cat:testcase)"/>
<xsl:sequence select="
$test/cat:input/cat:stylesheet and
not($test/cat:output/cat:error) and
$test/cat:output/cat:result-document/@type='xml' and
not($test/cat:discretionary-items/cat:discretionary-version[@spec = 'XSLT30']) and 
not($test/cat:output/cat:result-document/@type=('html-output', 'text')) and
not($test/cat:discretionary-items/cat:discretionary-feature[@name='schema_aware'][@behavior='on']) and
not($xsltexceptions/*/test[name=$test/cat:name and @run='no']) "/>
</xsl:function>
   
</xsl:stylesheet>

