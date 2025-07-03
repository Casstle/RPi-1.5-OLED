package com.casstle.pi.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {

    public static BufferedImage LoadImageRessource(String name) throws IOException
    {
        InputStream stream = ResourceLoader.class.getResourceAsStream("/"+name);
        return ImageIO.read(stream);
    }

}
