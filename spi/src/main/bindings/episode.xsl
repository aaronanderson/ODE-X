<?xml version="1.0" encoding="UTF-8"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
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

					<bindings scd="~tns:prgAddressType">
						<class ref="org.apache.ode.spi.exec.xml.PrgAdd" />
					</bindings>

					<bindings scd="~tns:prgAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.PrgAddRef" />
					</bindings>

					<bindings scd="~tns:prcAddressType">
						<class ref="org.apache.ode.spi.exec.xml.PrcAdd" />
					</bindings>

					<bindings scd="~tns:prcAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.PrcAddRef" />
					</bindings>

					<bindings scd="~tns:thdAddressType">
						<class ref="org.apache.ode.spi.exec.xml.ThdAdd" />
					</bindings>

					<bindings scd="~tns:thdAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.ThdAddRef" />
					</bindings>

					<bindings scd="~tns:stkAddressType">
						<class ref="org.apache.ode.spi.exec.xml.StkAdd" />
					</bindings>

					<bindings scd="~tns:stkAddressRefType">
						<class ref="org.apache.ode.spi.exec.xml.StkAddRef" />
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