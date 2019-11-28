package com.minecolonies.coremod.entity.pathfinding;

import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.MineColonies;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Static class the handles all the Pathfinding.
 */
public final class Pathfinding
{
    private static final BlockingQueue<Runnable> jobQueue = new LinkedBlockingDeque<>();
    private static ThreadPoolExecutor executor;
    
    
    /**
     * Creates a new thread pool for pathfinding jobs
     */
    public static void start()
    {
        executor = new ThreadPoolExecutor(1, MineColonies.getConfig().getCommon().pathfindingMaxThreadCount.get(), 10, TimeUnit.SECONDS, jobQueue);
    }
    
    /**
     * Waits until all running pathfinding requests are finished
     * Then stops all running threads in this thread pool
     */
    public static void shutdown()
    {
    	executor.shutdown();
    }
    
    private Pathfinding()
    {
        //Hides default constructor.
    }

    /**
     * Add a job to the queue for processing.
     *
     * @param job PathJob
     * @return a Future containing the Path
     */
    public static Future<Path> enqueue(@NotNull final AbstractPathJob job)
    {
        return executor.submit(job);
    }

    /**
     * Render debugging information for the pathfinding system.
     *
     * @param frame entity movement weight.
     */
    @OnlyIn(Dist.CLIENT)
    public static void debugDraw(final double frame)
    {
        if (AbstractPathJob.lastDebugNodesNotVisited == null)
        {
            return;
        }

        final Entity entity = Minecraft.getInstance().getRenderViewEntity();
        final double dx = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * frame;
        final double dy = entity.lastTickPosY + (double)entity.getEyeHeight() + (entity.posY - entity.lastTickPosY) * frame;
        final double dz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * frame;

        GlStateManager.pushMatrix();
        GlStateManager.pushTextureAttributes();
        GlStateManager.translated(-dx, -dy, -dz);

        GlStateManager.disableTexture();
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.enableDepthTest();

        final Set<Node> debugNodesNotVisited;
        final Set<Node> debugNodesVisited;
        final Set<Node> debugNodesPath;

        synchronized (AbstractPathJob.debugNodeMonitor)
        {
            debugNodesNotVisited = AbstractPathJob.lastDebugNodesNotVisited;
            debugNodesVisited = AbstractPathJob.lastDebugNodesVisited;
            debugNodesPath = AbstractPathJob.lastDebugNodesPath;
        }

        try
        {
            for (@NotNull final Node n : debugNodesNotVisited)
            {
                debugDrawNode(n, 1.0F, 0F, 0F);
            }

            for (@NotNull final Node n : debugNodesVisited)
            {
                debugDrawNode(n, 0F, 0F, 1.0F);
            }

            for (@NotNull final Node n : debugNodesPath)
            {
                debugDrawNode(n, 0F, 1.0F, 0F);
            }
        }
        catch (final ConcurrentModificationException exc)
        {
            Log.getLogger().catching(exc);
        }

        GlStateManager.popAttributes();
        GlStateManager.popMatrix();
    }

    @OnlyIn(Dist.CLIENT)
    private static void debugDrawNode(@NotNull final Node n, final float r, final float g, final float b)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translated((double) n.pos.getX() + 0.375, (double) n.pos.getY() + 0.375, (double) n.pos.getZ() + 0.375);

        final Entity entity = Minecraft.getInstance().getRenderViewEntity();
        final double dx = n.pos.getX() - entity.posX;
        final double dy = n.pos.getY() - entity.posY;
        final double dz = n.pos.getZ() - entity.posZ;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= 5D)
        {
            renderDebugText(n);
        }

        GlStateManager.scaled(0.25D, 0.25D, 0.25D);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        GlStateManager.color3f(r, g, b);

        //  X+
        vertexBuffer.pos(1.0, 0.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 1.0, 1.0).endVertex();
        vertexBuffer.pos(1.0, 0.0, 1.0).endVertex();

        //  X-
        vertexBuffer.pos(0.0, 0.0, 1.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 1.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(0.0, 0.0, 0.0).endVertex();

        //  Z-
        vertexBuffer.pos(0.0, 0.0, 0.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 0.0, 0.0).endVertex();

        //  Z+
        vertexBuffer.pos(1.0, 0.0, 1.0).endVertex();
        vertexBuffer.pos(1.0, 1.0, 1.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 1.0).endVertex();
        vertexBuffer.pos(0.0, 0.0, 1.0).endVertex();

        //  Y+
        vertexBuffer.pos(1.0, 1.0, 1.0).endVertex();
        vertexBuffer.pos(1.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 0.0).endVertex();
        vertexBuffer.pos(0.0, 1.0, 1.0).endVertex();

        //  Y-
        vertexBuffer.pos(0.0, 0.0, 1.0).endVertex();
        vertexBuffer.pos(0.0, 0.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 0.0, 0.0).endVertex();
        vertexBuffer.pos(1.0, 0.0, 1.0).endVertex();

        tessellator.draw();

        if (n.parent != null)
        {
            final double pdx = n.parent.pos.getX() - n.pos.getX() + 0.125;
            final double pdy = n.parent.pos.getY() - n.pos.getY() + 0.125;
            final double pdz = n.parent.pos.getZ() - n.pos.getZ() + 0.125;

            vertexBuffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            vertexBuffer.pos(0.5D, 0.5D, 0.5D).color(0.75F, 0.75F, 0.75F, 1.0F).endVertex();
            vertexBuffer.pos(pdx / 0.25, pdy / 0.25, pdz / 0.25).color(0.75F, 0.75F, 0.75F, 1.0F).endVertex();
            tessellator.draw();
        }

        GlStateManager.popMatrix();
    }

    @OnlyIn(Dist.CLIENT)
    private static void renderDebugText(@NotNull final Node n)
    {
        final String s1 = String.format("F: %.3f [%d]", n.getCost(), n.getCounterAdded());
        final String s2 = String.format("G: %.3f [%d]", n.getScore(), n.getCounterVisited());
        final FontRenderer fontrenderer = Minecraft.getInstance().fontRenderer;
        GlStateManager.pushTextureAttributes();
        GlStateManager.pushMatrix();
        GlStateManager.translated(0.0F, 0.75F, 0.0F);
        GlStateManager.normal3f(0.0F, 1.0F, 0.0F);

        final EntityRendererManager renderManager = Minecraft.getInstance().getRenderManager();
        GlStateManager.rotated(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotated(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scaled(-0.014F, -0.014F, 0.014F);
        GlStateManager.translated(0.0F, 18F, 0.0F);

        GlStateManager.depthMask(false);
        GlStateManager.disableDepthTest();

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(
          GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
          GlStateManager.SourceFactor.ONE,
          GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture();

        final int i = Math.max(fontrenderer.getStringWidth(s1), fontrenderer.getStringWidth(s2)) / 2;

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertexBuffer.pos((double) (-i - 1), -5.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        vertexBuffer.pos((double) (-i - 1), 12.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        vertexBuffer.pos((double) (i + 1), 12.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        vertexBuffer.pos((double) (i + 1), -5.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture();

        GlStateManager.translated(0.0F, -5F, 0.0F);
        fontrenderer.drawString(s1, -fontrenderer.getStringWidth(s1) / 2, 0, 0xFFFFFFFF);
        GlStateManager.translated(0.0F, 8F, 0.0F);
        fontrenderer.drawString(s2, -fontrenderer.getStringWidth(s2) / 2, 0, 0xFFFFFFFF);

        GlStateManager.enableDepthTest();
        GlStateManager.depthMask(true);
        GlStateManager.translated(0.0F, -8F, 0.0F);
        fontrenderer.drawString(s1, -fontrenderer.getStringWidth(s1) / 2, 0, 0xFFFFFFFF);
        GlStateManager.translated(0.0F, 8F, 0.0F);
        fontrenderer.drawString(s2, -fontrenderer.getStringWidth(s2) / 2, 0, 0xFFFFFFFF);

        GlStateManager.popMatrix();
        GlStateManager.popAttributes();
    }
}
