package com.SirBlobman.blockrenderer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;

public class Rendering {
    private Rendering() {}
    private static final DummyGUI DUMMY_GUI = new DummyGUI();
    private static class DummyGUI extends GuiScreen {
        @Override
        public void drawGradientRect(int left, int top, int bottom, int right, int startColor, int endColor) {
            super.drawGradientRect(left, top, right, bottom, startColor, endColor);
        }
        
        @Override
        public void drawHorizontalLine(int startX, int endX, int y, int color) {
            super.drawHorizontalLine(startX, endX, y, color);
        }
        
        @Override
        public void drawVerticalLine(int x, int startY, int endY, int color) {
            super.drawVerticalLine(x, startY, endY, color);
        }
        
        @Override
        public void drawHoveringText(List<String> lines, int x, int y) {
            super.drawHoveringText(lines, x, y);
        }
        
        @Override
        public void drawHoveringText(List<String> lines, int x, int y, FontRenderer fontRenderer) {
            super.drawHoveringText(lines, x, y, fontRenderer);
        }
        
        @Override
        public void renderToolTip(ItemStack stack, int x, int y) {
            super.renderToolTip(stack, x, y);
        }
    }
    
    public static void drawCenteredString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        DUMMY_GUI.drawCenteredString(fontRenderer, text, x, y, color);
    }
    
    public static void drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        DUMMY_GUI.drawString(fontRenderer, text, x, y, color);
    }
    
    public static void drawHoveringText(List<String> textLines, int x, int y) {
        DUMMY_GUI.drawHoveringText(textLines, x, y);
    }
    
    public static void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        DUMMY_GUI.drawHoveringText(textLines, x, y, font);
    }
    
    public static void drawHorizontalLine(int startX, int endX, int y, int color) {
        DUMMY_GUI.drawHorizontalLine(startX, endX, y, color);
    }
    
    public static void drawVerticalLine(int x, int startY, int endY, int color) {
        DUMMY_GUI.drawVerticalLine(x, startY, endY, color);
    }
    
    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }
    
    public static void drawGradientRectangle(int left, int top, int right, int bottom, int startColor, int endColor) {
        DUMMY_GUI.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }
    
    public static void drawTexturedModalRect(float xCoord, float yCoord, int minU, int minV, int maxU, int maxV) {
        DUMMY_GUI.drawTexturedModalRect(xCoord, yCoord, minU, minV, maxU, maxV);
    }
    
    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        DUMMY_GUI.drawTexturedModalRect(x, y, textureX, textureY, width, height);
    }
    
    public static void drawTexturedModalRect(int xCoord, int yCoord, TextureAtlasSprite textureSprite, int widthIn, int heightIn) {
        DUMMY_GUI.drawTexturedModalRect(xCoord, yCoord, textureSprite, widthIn, heightIn);
    }
    
    public static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight, int width, int height, float tileWidth, float tileHeight) {
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, uWidth, vHeight, width, height, tileWidth, tileHeight);
    }
    
    public static void drawModalRectWithCustomSizedTexture(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }
    
    public static void renderToolTip(ItemStack stack, int x, int y) {
        DUMMY_GUI.renderToolTip(stack, x, y);
    }
    
    public static void drawBackground(int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        DUMMY_GUI.setWorldAndResolution(minecraft, width, height);
        DUMMY_GUI.drawBackground(0);
    }
}