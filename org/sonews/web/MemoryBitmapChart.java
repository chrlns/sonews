/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sonews.web;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.IAxis.AxisTitle;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * A chart rendered to a memory bitmap.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
class MemoryBitmapChart extends Chart2D
{

  public MemoryBitmapChart()
  {
    setGridColor(Color.LIGHT_GRAY);
    getAxisX().setPaintGrid(true);
    getAxisY().setPaintGrid(true);
    getAxisX().setAxisTitle(new AxisTitle("time of day"));
    getAxisY().setAxisTitle(new AxisTitle("processed news"));
  }
  
  public String getContentType()
  {
    return "image/png";
  }
  
  public byte[] getRawData(final int width, final int height)
    throws IOException
  {
    setSize(width, height);
    BufferedImage img = snapShot(width, height);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(img, "png", out);
    return out.toByteArray();
  }
  
}
