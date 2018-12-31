package com.SirBlobman.blockrenderer;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

@Mod(
        modid = ModInfo.MODID, 
        name = ModInfo.NAME, 
        version = ModInfo.VERSION, 
        acceptableRemoteVersions = ModInfo.WILDCARD, 
        acceptableSaveVersions = ModInfo.WILDCARD, 
        clientSideOnly = true
        )
public class BlockRenderer {
    @Instance
    public static BlockRenderer INSTANCE;
    
    public static final Logger LOG = LogManager.getLogger("Block Renderer");
    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    
    protected KeyBinding keyBind;
    protected boolean keyDown = false;
    
    protected String pendingBulkRender;
    protected int pendingBulkRenderSize;
    
    private int size;
    private float oldZLevel;
    
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        keyBind = new KeyBinding("key.blockrenderer.render", Keyboard.KEY_GRAVE, "key.categories.blockrenderer");
        ClientRegistry.registerKeyBinding(keyBind);
        
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public void onRenderTick(RenderTickEvent e) {
        Phase phase = e.phase;
        if(phase == Phase.START) {
            if(pendingBulkRender != null) {
                bulkRender(pendingBulkRender, pendingBulkRenderSize);
                pendingBulkRender = null;
            }
            
            int code = keyBind.getKeyCode();
            if(code > 256) return;
            
            if(Keyboard.isKeyDown(code)) {
                if(!keyDown) {
                    keyDown = true;
                    
                    Minecraft minecraft = Minecraft.getMinecraft();
                    Slot hoveredSlot = null;
                    GuiScreen currentGUI = minecraft.currentScreen;
                    GuiIngame ingameGUI = minecraft.ingameGUI;
                    GuiNewChat chatGUI = ingameGUI.getChatGUI();
                    
                    if(currentGUI instanceof GuiContainer) {
                        int guiWidth = currentGUI.width;
                        int guiHeight = currentGUI.height;
                        
                        final int mouseX = (Mouse.getX() * guiWidth / minecraft.displayWidth);
                        final int mouseY = (guiHeight - Mouse.getY() * guiHeight / minecraft.displayHeight - 1);
                        
                        GuiContainer currentContainer = (GuiContainer) currentGUI;
                        hoveredSlot = currentContainer.getSlotAtPosition(mouseX, mouseY);
                    }
                    
                    if(GuiScreen.isCtrlKeyDown()) {
                        String itemModid = null;
                        if(hoveredSlot != null && hoveredSlot.getHasStack()) {
                            ItemStack hoveredStack = hoveredSlot.getStack();
                            Item hoveredItem = hoveredStack.getItem();
                            ResourceLocation hoveredResource = Item.REGISTRY.getNameForObject(hoveredItem);
                            itemModid = hoveredResource.getResourceDomain();
                        }
                        
                        EnterModidGUI customGUI = new EnterModidGUI(currentGUI, itemModid);
                        minecraft.displayGuiScreen(customGUI);
                    } 
                    
                    else if(currentGUI instanceof GuiContainer) {
                        if(hoveredSlot != null) {
                            ItemStack hoveredStack = hoveredSlot.getStack();
                            if(hoveredStack != null && !hoveredStack.isEmpty()) {
                                int renderSize = 512;
                                if(GuiScreen.isShiftKeyDown()) {
                                    ScaledResolution scaledResolution = new ScaledResolution(minecraft);
                                    int scaleFactor = scaledResolution.getScaleFactor();
                                    renderSize = (16 * scaleFactor);
                                }
                                
                                setupRenderState(renderSize);
                                
                                File folder_renders = new File("renders");
                                String renderString = render(hoveredStack, folder_renders, true);
                                
                                TextComponentString text = new TextComponentString(renderString);
                                chatGUI.printChatMessage(text);
                                
                                tearDownRenderState();
                            } else {
                                TextComponentTranslation text = new TextComponentTranslation("message.blockrenderer.slot_empty");
                                chatGUI.printChatMessage(text); 
                            }
                        } else {
                            TextComponentTranslation text = new TextComponentTranslation("message.blockrenderer.slot_absent");
                            chatGUI.printChatMessage(text); 
                        }
                    } 
                    
                    else {
                        TextComponentTranslation text = new TextComponentTranslation("message.blockrenderer.not_container");
                        chatGUI.printChatMessage(text); 
                    }
                }
            } else keyDown = false;
        }
    }
    
    private void bulkRender(String modidSpec, int size) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GuiIngameMenu menuGUI = new GuiIngameMenu();
        minecraft.displayGuiScreen(menuGUI);
        
        Set<String> modids = new HashSet<>();
        for(String modid : modidSpec.split(",")) {
            String trim = modid.trim();
            modids.add(trim);
        }
        
        List<ItemStack> toRender = new ArrayList<>();
        NonNullList<ItemStack> nonNullItems = NonNullList.create();
        int renderedCount = 0;
        
        Set<ResourceLocation> itemKeys = Item.REGISTRY.getKeys();
        for(ResourceLocation resource : itemKeys) {
            if(resource != null) {
                String resourceDomain = resource.getResourceDomain();
                if(modids.contains("*") || modids.contains(resourceDomain)) {
                    nonNullItems.clear();
                    Item item = Item.REGISTRY.getObject(resource);
                    
                    CreativeTabs itemTab = item.getCreativeTab();
                    if(itemTab != null) {
                        item.getSubItems(itemTab, nonNullItems);
                    }
                    
                    toRender.addAll(nonNullItems);
                }
            }
        }
        
        Date nowDate = new Date();
        String nowDateString = DATE_FORMAT.format(nowDate);
        String sanitizedModids = sanitize(modidSpec);
        File folder_renders = new File("renders");
        File folder = new File(folder_renders, nowDateString + "_" + sanitizedModids);
        
        long lastUpdateTime = 0;
        String joinedModids = String.join(", ", modids);
        setupRenderState(size);
        
        for(ItemStack stack : toRender) {
            if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) break;
            
            render(stack, folder, false);
            renderedCount++;
            
            long minecraftTime = Minecraft.getSystemTime();
            if((minecraftTime - lastUpdateTime) > 33) {
                tearDownRenderState();
                
                int toRenderSize = toRender.size();
                String guiRenderingString = I18n.format("gui.blockrenderer.rendering", toRenderSize, joinedModids);
                String guiProgressString = I18n.format("gui.blockrenderer.progress", renderedCount, toRenderSize, (toRenderSize - renderedCount));
                
                float fRenderedCount = renderedCount;
                float fToRenderSize = toRenderSize;
                float progress = (fRenderedCount / fToRenderSize);
                
                renderLoading(guiRenderingString, guiProgressString, stack, progress);
                
                lastUpdateTime = Minecraft.getSystemTime();
                setupRenderState(size);
            }
        }
        
        if(renderedCount >= toRender.size()) {
            String guiRenderedString = I18n.format("gui.blockrenderer.rendered", toRender.size(), joinedModids);
            renderLoading(guiRenderedString, "", null, 1.0F);
        } else {
            String guiRenderCancelledString = I18n.format("gui.blockrenderer.renderCancelled");
            String guiProgressString = I18n.format("gui.blockrenderer.progress", renderedCount, toRender.size(), (toRender.size() - renderedCount));
            
            float fRenderedCount = renderedCount;
            float fToRenderSize = toRender.size();
            float progress = (fRenderedCount / fToRenderSize);
            
            renderLoading(guiRenderCancelledString, guiProgressString, null, progress);
        }
        
        tearDownRenderState();
        try {Thread.sleep(1500);} catch(InterruptedException ignored) {};
    }
    
    private void renderLoading(String title, String subtitle, ItemStack stack, float progress) {
        Minecraft minecraft = Minecraft.getMinecraft();
        Framebuffer frameBuffer = minecraft.getFramebuffer();
        frameBuffer.unbindFramebuffer();
        
        GlStateManager.pushMatrix();
            ScaledResolution scaledRes = new ScaledResolution(minecraft);
            minecraft.entityRenderer.setupOverlayRendering();
            
            int scaledWidth = scaledRes.getScaledWidth();
            int scaledHeight = scaledRes.getScaledHeight();
            
            Rendering.drawBackground(scaledWidth, scaledHeight);
            
            final int noColor = -1;
            final int color1 = 0xFF001100;
            final int color2 = 0xFF55FF55;
            Rendering.drawCenteredString(minecraft.fontRenderer, title, (scaledWidth / 2), (scaledWidth / 2 - 24), noColor);
            Rendering.drawRect((scaledWidth / 2 - 50), (scaledHeight / 2 - 1), (scaledWidth / 2 + 50), (scaledHeight / 2 + 1), color1);
            Rendering.drawRect((scaledWidth / 2 - 50), (scaledHeight / 2 - 1), ((scaledWidth / 2 - 50) + ((int) (progress * 100))), (scaledHeight / 2 + 1), color2);
            GlStateManager.pushMatrix();
                GlStateManager.scale(0.5F, 0.5F, 1.0F);
                Rendering.drawCenteredString(minecraft.fontRenderer, subtitle, scaledWidth, scaledHeight - 20, noColor);
                
                if(stack != null) {
                    List<String> loreList = stack.getTooltip(minecraft.player, TooltipFlags.NORMAL);
                    
                    for(int i = 0; i < loreList.size(); i++) {
                        if(i == 0) loreList.set(i, stack.getRarity().rarityColor + loreList.get(i));
                        else loreList.set(i, TextFormatting.GRAY + loreList.get(i));
                    }
                    
                    FontRenderer fontRenderer = stack.getItem().getFontRenderer(stack);
                    if(fontRenderer == null) fontRenderer = minecraft.fontRenderer;
                    
                    int width = 0;
                    for(String lore : loreList) {
                        int loreWidth = fontRenderer.getStringWidth(lore);
                        if(loreWidth > width) width = loreWidth;
                    }
                    
                    GlStateManager.translate(((scaledWidth - width / 2.0D) - 12.0D), (scaledHeight + 30.0D), 0.0D);
                    Rendering.drawHoveringText(loreList, 0, 0, fontRenderer);
                }
            GlStateManager.popMatrix();
        GlStateManager.popMatrix();
        
        minecraft.updateDisplay();
        frameBuffer.bindFramebuffer(false);
    }
    
    private String render(ItemStack stack, File folder, boolean includeDateInFileName) {
        Minecraft minecraft = Minecraft.getMinecraft();
        
        Date now = new Date();
        String fileName = (includeDateInFileName ? (DATE_FORMAT.format(now) + "_") : "") + sanitize(stack.getDisplayName());
        
        GlStateManager.pushMatrix();
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            
            RenderItem renderItem = minecraft.getRenderItem();
            renderItem.renderItemAndEffectIntoGUI(stack, 0, 0);
        GlStateManager.popMatrix();
        
        try {
            BufferedImage readPixels = readPixels(size, size);
            BufferedImage flipped = createFlipped(readPixels);
            
            File file = new File(folder, fileName + ".png");
            int i = 2;
            while(file.exists()) {
                file = new File(folder, fileName + "_" + i + ".png");
                i++;
            }
            
            file.getParentFile().mkdirs();
            file.createNewFile();
            
            ImageIO.write(flipped, "PNG", file);
            return I18n.format("message.blockrenderer.render.success", file.getPath());
        } catch(Throwable ex) {
            ex.printStackTrace();
            return I18n.format("message.blockrenderer.render.fail");
        }
    }
    
    private void setupRenderState(int desiredSize) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledRes = new ScaledResolution(minecraft);
        
        int min1 = Math.min(minecraft.displayWidth, minecraft.displayHeight);
        size = Math.min(min1, desiredSize);
        
        minecraft.entityRenderer.setupOverlayRendering();
        RenderHelper.enableGUIStandardItemLighting();
        
        float scale = (size / (16.0F * scaledRes.getScaleFactor()));
        GlStateManager.translate(0, 0, -(scale * 100.0F));
        GlStateManager.scale(scale, scale, scale);
        
        oldZLevel = minecraft.getRenderItem().zLevel;
        minecraft.getRenderItem().zLevel = -50.0F;
        
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
    }
    
    private void tearDownRenderState() {
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        
        Minecraft.getMinecraft().getRenderItem().zLevel = oldZLevel;
    }
    
    private String sanitize(String string) {
        final String regex = "[^A-Za-z0-9-_ ]";
        return string.replaceAll(regex, "_");
    }
    
    public BufferedImage readPixels(int width, int height) throws InterruptedException {
        GL11.glReadBuffer(GL11.GL_BACK);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, (Minecraft.getMinecraft().displayHeight - height), width, height, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, byteBuffer);
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        byteBuffer.asIntBuffer().get(pixels);
        
        image.setRGB(0, 0, width, height, pixels, 0, width);
        return image;
    }
    
    private static BufferedImage createFlipped(BufferedImage image) {
        AffineTransform transformer = new AffineTransform();
        transformer.concatenate(AffineTransform.getScaleInstance(1, -1));
        transformer.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
        return createTransformed(image, transformer);
    }
    
    private static BufferedImage createTransformed(BufferedImage image, AffineTransform transform) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D graphics = newImage.createGraphics();
        graphics.transform(transform);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        
        return newImage;
    }
}