<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">
  <xsl:param name="use.id.as.filename" select="1"/>
  <xsl:param name="admon.graphics" select="1"/>
  <xsl:param name="admon.graphics.path"></xsl:param>
  <xsl:param name="chunk.section.depth" select="1"></xsl:param>
  <xsl:param name="html.stylesheet" select="'sonews.css'"/>
<xsl:param name="annotate.toc" select="1"></xsl:param>
<xsl:param name="toc.max.depth">2</xsl:param>
<xsl:template name="user.footer.navigation">
  <p class="copyright">
  <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a> This work by <span xmlns:cc="http://creativecommons.org/ns#" property="cc:attributionName">Christian Lins</span> is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/">Creative Commons Attribution-Share Alike 3.0 License</a>.
  </p>
</xsl:template>

</xsl:stylesheet>

