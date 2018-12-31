package com.SirBlobman.blockrenderer;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class EnterModidGUI extends GuiScreen implements GuiResponder {
    private String prefill;
    private GuiTextField text;
    private GuiSlider size;
    private GuiScreen old;
    
    public EnterModidGUI(GuiScreen old, String prefill) {
        this.old = old;
        this.prefill = ((prefill == null) ? "" : prefill);
    }
    
    private int round(float value) {
        int intValue = (int) value;
        
        int nearestTwoPower = ((int) Math.pow(2, Math.ceil(Math.log(intValue) / Math.log(2))));
        int minSize = Math.min(this.mc.displayHeight, this.mc.displayWidth);
        
        if(nearestTwoPower < minSize && Math.abs(intValue - nearestTwoPower) < 32) intValue = nearestTwoPower;
        
        return Math.min(intValue, minSize);
    }
    
    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        String oldText = (this.text == null ? this.prefill : this.text.getText());
        float oldSize = (this.size == null ? 512 : this.size.getSliderValue());
        
        this.text = new GuiTextField(0, this.mc.fontRenderer, (this.width / 2 - 100), (this.height / 6 + 50), 200, 20);
        this.text.setText(oldText);
        
        GuiButton cancelButton = new GuiButton(2, (this.width / 2 - 100), (this.height / 6 + 120), 98, 20, I18n.format("gui.blockrenderer.cancel"));
        this.buttonList.add(cancelButton);
        
        GuiButton renderButton = new GuiButton(1, (this.width / 2 + 2), (this.height / 6 + 120), 98, 20, I18n.format("gui.blockrenderer.render"));
        this.buttonList.add(renderButton);
        
        int minSize = Math.min(this.mc.displayWidth, this.mc.displayHeight);
        this.size = new GuiSlider(this, 3, (this.width / 2 - 100), (this.height / 6 + 80), I18n.format("gui.blockrenderer.rendersize"), 16, Math.min(2048, minSize), Math.min(oldSize, minSize), (id, name, value) -> {
            int round = round(value);
            String roundString = Integer.toString(round);
            return (name + ": " + roundString + "x" + roundString);
        });
        this.size.width = 200;
        this.buttonList.add(size);
        
        this.text.setFocused(true);
        this.text.setCanLoseFocus(false);
        
        boolean enabled = (this.mc.world != null);
        renderButton.enabled = enabled;
        this.text.setEnabled(enabled);
        this.size.enabled = enabled;
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, ticks);
        
        String enterModidString = I18n.format("gui.blockrenderer.enter_modid");
        drawCenteredString(this.mc.fontRenderer, enterModidString, (this.width / 2), (this.height / 6), -1);
        
        if(this.mc.world != null) {
            boolean widthCap = (this.mc.displayWidth < 2048);
            boolean heightCap = (this.mc.displayHeight < 2048);
            
            String string = null;
            if(widthCap && heightCap) {
                if(this.mc.displayWidth > this.mc.displayHeight) string = "gui.blockrenderer.capped_height";
                else if(this.mc.displayWidth == this.mc.displayHeight) string = "gui.blockrenderer.capped_both";
                else if(this.mc.displayHeight > this.mc.displayWidth) string = "gui.blockrenderer.capped_width";
            } 
            
            else if(widthCap) string = "gui.blockrenderer.capped_width";
            else if(heightCap) string = "gui.blockrender.capped_height";
            
            if(string != null) {
                int minRes = Math.min(this.mc.displayHeight, this.mc.displayWidth);
                string = I18n.format(string, minRes);
                drawCenteredString(this.mc.fontRenderer, string, (this.width / 2), (this.height / 6 + 104), 0xFFFFFFFF);
            }
        } else {
            String noWorldString = I18n.format("gui.blockrenderer.no_world");
            drawCenteredString(this.mc.fontRenderer, noWorldString, (this.width / 2), (this.height / 6 + 30), 0xFF5555);
        }
        
        this.text.drawTextBox();
    }
    
    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        
        if(button.id == 1) {
            if(this.mc.world != null) {
                BlockRenderer.INSTANCE.pendingBulkRender = this.text.getText();
                BlockRenderer.INSTANCE.pendingBulkRenderSize = round(this.size.getSliderValue());
            }
            
            this.mc.displayGuiScreen(this.old);
        } else if(button.id == 2) {
            this.mc.displayGuiScreen(this.old);
        }
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        this.text.updateCursorCounter();
    }
    
    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.text.textboxKeyTyped(typedChar, keyCode);
    }
    
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.text.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    public void setEntryValue(int id, float value) {
        int round = round(value);
        this.size.setSliderValue(round, false);
    }
    
    @Override
    public void setEntryValue(int id, boolean value) {
        // Do Nothing
    }
    
    @Override
    public void setEntryValue(int id, String value) {
        // Do Nothing
    }
}