package com.casstle.pi.oled;

import com.casstle.pi.oled.OLED.OLEDContent;
import com.casstle.pi.util.ResourceLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Demo diplay content: Auto refresh enabled, image output, loading image ressources
 * <br> <br>
 * Shows a flying bird animation
 */
public class FlyingBird extends OLEDContent
{
    int frame = 0;
    final BufferedImage[] BirdAsset = new BufferedImage[8];

    public FlyingBird() throws IOException
    {
        BirdAsset[0] = ResourceLoader.LoadImageRessource("frame-1.png");
        BirdAsset[1] = ResourceLoader.LoadImageRessource("frame-2.png");
        BirdAsset[2] = ResourceLoader.LoadImageRessource("frame-3.png");
        BirdAsset[3] = ResourceLoader.LoadImageRessource("frame-4.png");
        BirdAsset[4] = ResourceLoader.LoadImageRessource("frame-5.png");
        BirdAsset[5] = ResourceLoader.LoadImageRessource("frame-6.png");
        BirdAsset[6] = ResourceLoader.LoadImageRessource("frame-7.png");
        BirdAsset[7] = ResourceLoader.LoadImageRessource("frame-8.png");

        EnableAutoRefresh(20);
    }

    @Override
    protected void Paint(Graphics g)
    {
        g.drawImage(BirdAsset[frame], 0, 0, null);
        frame++;
        if(frame >= 8) frame = 0;
    }

}
