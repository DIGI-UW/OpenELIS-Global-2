package org.openelisglobal.common.service.servlet.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Logo validation capped the encoded file at 500 KB and then decoded it, which
 * allocates a raster of the size the header declares (SCAN-100).
 */
public class LogoUploadServiceImplTest {

    private final LogoUploadServiceImpl service = new LogoUploadServiceImpl();

    @Test
    public void aLogoDeclaringAHugeRaster_isRefusedBeforeDecoding() throws Exception {
        byte[] png = pngHeaderDeclaring(20000, 20000);

        try {
            service.validateLogo(new MockMultipartFile("logo", "logo.png", "image/png", png));
            fail("a 20000 by 20000 logo was accepted");
        } catch (IllegalArgumentException refused) {
            assertTrue(refused.getMessage(), refused.getMessage().contains("at most 2048"));
        }
    }

    @Test
    public void anOrdinaryLogo_isAccepted() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(128, 96, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();

        assertEquals(png.length,
                service.validateLogo(new MockMultipartFile("logo", "logo.png", "image/png", png)).length);
    }

    /**
     * A PNG signature and IHDR chunk only; nothing to decode, just a declared size.
     */
    private static byte[] pngHeaderDeclaring(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(17);
        ihdr.put("IHDR".getBytes()).putInt(width).putInt(height).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0)
                .put((byte) 0);
        CRC32 crc = new CRC32();
        crc.update(ihdr.array());
        ByteBuffer png = ByteBuffer.allocate(8 + 4 + 17 + 4);
        png.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n' }).putInt(13).put(ihdr.array())
                .putInt((int) crc.getValue());
        return png.array();
    }
}
