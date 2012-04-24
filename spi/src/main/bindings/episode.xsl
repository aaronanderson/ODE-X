<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:j="http://java.sun.com/xml/ns/jaxb" xmlns:e="http://ode.apache.org/executable"
	exclude-result-prefixes="j e">

	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" />
	<xsl:strip-space elements="*" />


	<!-- identity template -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

	<xsl:template match="//j:bindings[@scd='x-schema::tns']">
		<xsl:copy>
			<xsl:attribute name="if-exists"></xsl:attribute>
			<xsl:apply-templates select="@*|node()" />

			<xsl:choose>
				<xsl:when test="j:schemaBindings/j:package[@name='org.apache.ode.spi.exec.xml']">
					<!-- <xsl:variable name="xjc" select="document('executable.xjb')" /> <xsl:for-each select="$xjc//j:bindings[@node]"> <xsl:if test="j:class[@ref]"> <xsl:copy> 
						<xsl:apply-templates select="@*|node()" /> </xsl:copy> </xsl:if> </xsl:for-each> -->
					<bindings scd="~tns:srcIdType">
						<class ref="org.apache.ode.spi.exec.xml.SrcId" />
					</bindings>

					<bindings scd="~tns:srcIdRefType">
						<class ref="org.apache.ode.spi.exec.xml.SrcIdRef" />
					</bindings>

					<bindings scd="~tns:memAddressType">
						<class ref="org.apache.ode.spi.exec.xml.MemAdd" />
					</bindings>

					<bindings scd="~tns:memAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.MemAddRef" />
					</bindings>

					<bindings scd="~tns:insAddressType">
						<class ref="org.apache.ode.spi.exec.xml.InsAdd" />
					</bindings>

					<bindings scd="~tns:insAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.InsAddRef" />
					</bindings>

					<bindings scd="~tns:blcAddressType">
						<class ref="org.apache.ode.spi.exec.xml.BlcAdd" />
					</bindings>

					<bindings scd="~tns:blcAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.BlcAddRef" />
					</bindings>

				</xsl:when>
			</xsl:choose>

		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>