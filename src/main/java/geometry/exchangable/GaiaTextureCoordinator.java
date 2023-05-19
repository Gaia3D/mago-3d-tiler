package geometry.exchangable;

import geometry.basic.GaiaRectangle;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Getter
@Setter
public class GaiaTextureCoordinator {
    List<GaiaMaterial> materials = new ArrayList<>();
    List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
    List<GaiaRectangle> boundingRectangles = new ArrayList<>();

    BufferedImage batchImage;
    GaiaRectangle batchBoundingRectangle;
    int batchWidth;
    int batchHeight;

    public GaiaTextureCoordinator(List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.initBatchImage(0, 0);
    }

    private void initBatchImage(int width, int height) {
        if (width > 0 || height > 0) {
            this.batchImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            this.batchImage = null;
        }
        this.batchWidth = width;
        this.batchHeight = height;
        this.batchBoundingRectangle = new GaiaRectangle();
        this.batchBoundingRectangle.setInit(new Vector2d(0, 0));
        this.batchBoundingRectangle.addPoint(new Vector2d(width, height));
    }

    public void batchTextures() {
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);
            if (texture.getBufferedImage() == null) {
                texture.loadImage();
            }

            BufferedImage bufferedImage = texture.getBufferedImage();
            bufferedImage = ImageUtils.resizeImageGraphic2D(bufferedImage, 256, 256);

            addImage(bufferedImage);

            log.info("material: {}", material.getName());
            log.info("texture: {}", texture.getName());
            //TODO
        }


    }

    public void clipImage(BufferedImage bufferedImage) {
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
        //graphics.
    }

    public void addImage(BufferedImage bufferedImage) {
        GaiaRectangle boundingRectangle = new GaiaRectangle();
        BufferedImage tempImage = this.copyImage(this.batchImage);
        Graphics2D graphics = null;

        if (this.batchWidth <= 0 || this.batchHeight <= 0) {
            initBatchImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            boundingRectangle.setInit(new Vector2d(0, 0));
            boundingRectangle.addPoint(new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight()));
            graphics = (Graphics2D) this.batchImage.getGraphics();
            graphics.drawImage(bufferedImage, 0, 0, null);
            graphics.dispose();
        } else {
            int preWidth = this.batchWidth;
            int preHeight = this.batchHeight;

            int calcWidth = this.batchWidth;
            int calcHeight = this.batchHeight;
            boolean heightFlag = (calcHeight >= calcWidth);
            boolean widthFlag = (calcHeight < calcWidth);
            if (heightFlag) {
                calcWidth += bufferedImage.getWidth();
            } else if (widthFlag) {
                calcHeight += bufferedImage.getHeight();
            } else {
                calcWidth += bufferedImage.getWidth();
                calcHeight += bufferedImage.getHeight();
            }

            initBatchImage(calcWidth, calcHeight);
            graphics = (Graphics2D) this.batchImage.getGraphics();
            graphics.setBackground(Color.WHITE);
            graphics.drawImage(tempImage, 0, 0, null);

            boundingRectangle.setInit(new Vector2d(preWidth, preHeight));
            boundingRectangle.addPoint(new Vector2d(preWidth + bufferedImage.getWidth(), preHeight + bufferedImage.getHeight()));

            if (heightFlag) {
                graphics.drawImage(bufferedImage, preWidth, 0, null);
            } else if (widthFlag) {
                graphics.drawImage(bufferedImage, 0, preHeight, null);
            } else {
                graphics.drawImage(bufferedImage, preWidth, preHeight, null);
            }
            graphics.dispose();
        }
        log.info(graphics.toString());
        boundingRectangles.add(boundingRectangle);
    }

    //private void drawRight;

    private BufferedImage copyImage(BufferedImage source){
        if (source == null) {
            return null;
        }
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics graphics = image.getGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return image;
    }
}


