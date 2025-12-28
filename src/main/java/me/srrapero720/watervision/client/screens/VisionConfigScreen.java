package me.srrapero720.watervision.client.screens;

import me.srrapero720.watervision.VisionConfig;
import me.srrapero720.watervision.client.render.ShaderCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * WaterVision 配置屏幕
 * 使用命令 /watervision config 打开
 */
@OnlyIn(Dist.CLIENT)
public class VisionConfigScreen extends Screen {
    
    private final Screen parent;
    private Checkbox hdrToSdrCheckbox;
    
    private boolean hdrToSdrValue;
    
    public VisionConfigScreen(Screen parent) {
        super(Component.translatable("watervision.config.title"));
        this.parent = parent;
        
        // 读取当前配置值
        this.hdrToSdrValue = VisionConfig.isHdrToSdrEnabled();
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;
        
        // HDR to SDR 选项
        this.hdrToSdrCheckbox = Checkbox.builder(
                Component.translatable("watervision.config.hdr.enableHdrToSdr"),
                this.font)
            .pos(centerX - 100, startY)
            .selected(this.hdrToSdrValue)
            .onValueChange((checkbox, value) -> this.hdrToSdrValue = value)
            .build();
        this.addRenderableWidget(this.hdrToSdrCheckbox);
        
        // 完成按钮
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(centerX - 100, this.height - 40, 200, 20)
            .build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // 绘制说明文本
        int infoY = this.height / 4 + 40;
        graphics.drawCenteredString(this.font, 
            Component.translatable("watervision.config.hdr.description"), 
            this.width / 2, infoY, 0xAAAAAA);
        
        // 显示光影状态
        if (ShaderCompat.isShaderModPresent()) {
            String shaderStatus = ShaderCompat.areShadersActive() 
                ? "§e" + Component.translatable("watervision.config.shader.active").getString()
                : "§a" + Component.translatable("watervision.config.shader.inactive").getString();
            graphics.drawCenteredString(this.font, shaderStatus, this.width / 2, infoY + 20, 0xFFFFFF);
            
            if (ShaderCompat.areShadersActive()) {
                graphics.drawCenteredString(this.font, 
                    Component.translatable("watervision.config.shader.note"), 
                    this.width / 2, infoY + 35, 0x888888);
            }
        }
        
        // 绘制配置文件路径
        graphics.drawCenteredString(this.font, 
            Component.literal("§7config/watervision-client.toml"), 
            this.width / 2, this.height - 60, 0xAAAAAA);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        // 保存配置
        VisionConfig.setHdrToSdrEnabled(this.hdrToSdrValue);
        
        this.minecraft.setScreen(this.parent);
    }
}
