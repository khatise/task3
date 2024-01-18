package rasterizer.line;

import rasterizer.Raster;
/**
 * Responsible for an interaction between user and raster
 * in case of rasterizing lines
 */
public interface Line {
    public void drawLine(Raster rastr, double x1, double y1, double x2, double y2, int color);
}
