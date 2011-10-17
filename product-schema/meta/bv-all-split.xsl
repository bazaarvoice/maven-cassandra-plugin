<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
        version="2.0"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:meta="http://www.bazaarvoice.com/xs/meta/1.0"
        exclude-result-prefixes="meta"
        >
    <xsl:output indent="yes"/>
    <xsl:strip-space elements="*"/>

    <!-- name of the output .xsd file, for example "product-feed-1.0.xsd" -->
    <xsl:param name="outputxsd"/>

    <!-- name of the output namespace, for example "http://www.bazaarvoice.com/PRR/ProductFeed/1.0" -->
    <xsl:param name="outputns"/>

    <!-- add header comment to the top of the document -->
    <xsl:template match="/">
        <xsl:comment> ************ GENERATED CONTENT ************ </xsl:comment>
        <xsl:text>&#10;</xsl:text>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <!-- add the configured targetNamespace attribute to the root element of the schema -->
    <xsl:template match="/xs:schema">
        <xsl:copy copy-namespaces="no">
            <xsl:attribute name="targetNamespace"><xsl:value-of select="$outputns"/></xsl:attribute>
            <xsl:apply-templates select="node() | @*[name() != 'targetNamespace']"/>
        </xsl:copy>
    </xsl:template>

    <!-- if allowed, change xs:sequence (with strict order requirements) to xs:all (with loose ordering requirements) -->
    <xsl:template match="xs:sequence[@meta:changeToAll = 'true']" priority="1">
        <xs:all><xsl:apply-templates select="node() | @*"/></xs:all>
    </xsl:template>

    <xsl:template match="xs:sequence[not(@meta:POOR_BACKWARD_COMPATIBILITY = 'true' or @meta:SEQUENCE_SPECIAL_CASE = 'true') and
                                     not(count(*) = 1 and xs:element[@maxOccurs = 'unbounded'])]" priority="0.75">
        <xsl:message terminate="yes">
            An xs:sequence must have a single maxOccurs="unbounded" child or be annotated with meta:changeToAll="true" or meta:POOR_BACKWARD_COMPATIBILITY="true": <xsl:value-of select="ancestor::*[@name]/@name"/>
        </xsl:message>
    </xsl:template>

    <!-- omit meta elements that are explicity not included or excluded from the output schema
        Note: com.bazaarvoice.cca.xml.xmlbean.AbstractXmlMarshaller has logic that needs to match this.
    -->
    <xsl:template match="*[(@meta:includeIn and not(matches(@meta:includeIn, concat('.*(^|\s+)', $outputxsd, '($|\s+).*'))))
                             or matches(@meta:excludeFrom, concat('.*(^|\s+)', $outputxsd, '($|\s+).*'))]">
    </xsl:template>

    <!-- omit meta attributes in the output document. -->
    <xsl:template match="@meta:*">
    </xsl:template>

    <!-- omit comments in the output document.  they're usually dev-oriented and not appropriate for customers -->
    <xsl:template match="comment()">
    </xsl:template>

    <!-- default matching rule recursively copies input to output as-is -->
    <xsl:template match="node() | @*" priority="-1">
        <xsl:copy copy-namespaces="no"><xsl:apply-templates select="node() | @*"/></xsl:copy>
    </xsl:template>

</xsl:stylesheet>