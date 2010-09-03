<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" omit-xml-declaration="yes"/>

<xsl:template match="/picstory">
	<h1><xsl:apply-templates select="title"/></h1>
	<div id="story">
		<xsl:apply-templates select="story/*"/>
	</div>
	<div id="storyfinal">%%STORYFINAL%%</div>
	<div id="back">
		<a href="../">&#x25c0; Back to index</a>
	</div>
	<div id="plug">
	  Created with <a href="http://www.leafdigital.com/software/picstory/">
	  <i>leafdigital</i><xsl:text> </xsl:text><span>picstory</span> 2.0
	  </a>
	</div>
</xsl:template>

<xsl:template match="p">
  <p>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="subhead">
  <h2>
    <xsl:apply-templates/>
  </h2>
</xsl:template>

<xsl:template match="pic">
  <xsl:variable name="ODDEVEN">
    <xsl:choose>
      <xsl:when test="count(preceding::pic) mod 2 = 0">
        <xsl:text>odd</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>even</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:variable name="FOLLOWING">
    <xsl:if test="preceding-sibling::*[position()=1 and local-name()='pic']">
      <xsl:text>following</xsl:text>
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="SHAPE">
    <xsl:choose>
      <xsl:when test="@width * 3 > @height * 4">
        <xsl:text>landscape</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>portrait</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <div id="{@id}" class="pic size{@width}x{@height} {$ODDEVEN} {$FOLLOWING} {$SHAPE}">
  	<noscript><img src="{@src}.{@hash}.w800.jpg" /></noscript>
  	<div class="belowpic">
  	<div class="caption"><div class="inner">
  	  <xsl:apply-templates/>
  	</div></div>
  	<div class="infobox">
  	  <xsl:if test="@shutterSpeed">
  	    <span class="boringbit"><xsl:value-of select="@shutterSpeed"/>
  	    <xsl:if test="@aperture">
  	      <xsl:text>&#xa0;at&#xa0;</xsl:text>
  	      <xsl:value-of select="@aperture"/>
  	    </xsl:if>
  	    <xsl:if test="@focalLength">
  	      <xsl:text>, </xsl:text>
  	      <xsl:value-of select="@focalLength"/>
  	    </xsl:if>
  	    <xsl:if test="@iso">
  	      <xsl:text>, ISO</xsl:text>
  	      <xsl:value-of select="@iso"/>
  	    </xsl:if>
  	    <xsl:if test="@locationDisplay">
  	      <xsl:text> </xsl:text>
  	      <a href="http://maps.google.com/maps?q={@latitude}+{@longitude}&amp;z=17"><xsl:value-of select="@locationDisplay"/></a>
  	    </xsl:if>
  	    </span>
  	  </xsl:if>
  	</div>
  	<div class="clear"></div>
  	</div>
  </div>
</xsl:template>

<xsl:template match="em">
  <em><xsl:apply-templates/></em>
</xsl:template>

<xsl:template match="a">
<a href="{@href}"><xsl:apply-templates/></a>
</xsl:template>

<xsl:template match="ellipsis">…</xsl:template>

<xsl:template match="endash">–</xsl:template>

</xsl:stylesheet>
