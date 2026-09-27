package dev.rdh.argentum.impl.render.entity;

import dev.rdh.argentum.impl.Argentum;
import dev.rdh.argentum.impl.render.terrain.ArgentumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.Texture;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.VertexFormat;
import net.minecraft.resource.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NameTagBatch {
    private static final int STACK_SIZE = 32;
    private static final VertexFormat FORMAT = DefaultVertexFormat.PARTICLE;
    private static final int STRIDE = FORMAT.getIntSize();
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final Comparator<Pass> ORDER = Comparator.comparingInt(Pass::order);

    private final Matrix4fStack matrices = new Matrix4fStack(STACK_SIZE);
    private final Matrix4f multiplied = new Matrix4f();
    private final Vector3f position = new Vector3f();
    private final int[] quad = new int[4 * STRIDE];
    private final List<Pass> passes = new ArrayList<>();
    private final BufferUploader uploader = new BufferUploader();
    private Matrix4fc camera;
    private boolean active;
    private int depth;
    private int pushes;
    private int matrixMode;
    private boolean lost;
    private int passCount;
    private float lightU = 240.0F;
    private float lightV = 240.0F;

    public static NameTagBatch active() {
        ArgentumWorldRenderer renderer = ArgentumWorldRenderer.instanceNullable();
        NameTagBatch batch = renderer == null ? null : renderer.getNameTagBatch();
        return batch != null && batch.active ? batch : null;
    }

    public static NameTagBatch capturing() {
        NameTagBatch batch = active();
        return batch != null && batch.depth > 0 ? batch : null;
    }

    public void begin(Matrix4fc camera) {
        this.discard();
        this.camera = camera;
        this.active = Argentum.CONFIG.nameTagBatching && Argentum.CONFIG.fontBatching;
    }

    public void end() {
        this.flush();
        this.active = false;
    }

    public void discard() {
        for (int i = 0; i < this.passCount; i++) {
            BufferBuilder buffer = this.passes.get(i).buffer;
            buffer.end();
            buffer.clear();
        }
        this.passCount = 0;
        this.depth = 0;
        this.active = false;
    }

    public void enter() {
        if (this.depth++ == 0) {
            this.matrices.clear().set(this.camera);
            this.pushes = 0;
            this.matrixMode = GL11.GL_MODELVIEW;
            this.lost = false;
        }
    }

    public void exit() {
        this.depth--;
    }

    public void setLight(float u, float v) {
        this.lightU = u;
        this.lightV = v;
    }

    public void matrixMode(int mode) {
        this.matrixMode = mode;
    }

    public void pushMatrix() {
        if (!this.tracks()) return;
        if (this.pushes == STACK_SIZE - 1) {
            this.lost = true;
            return;
        }
        this.matrices.pushMatrix();
        this.pushes++;
    }

    public void popMatrix() {
        if (!this.tracks()) return;
        if (this.pushes == 0) {
            this.lost = true;
            return;
        }
        this.matrices.popMatrix();
        this.pushes--;
    }

    public void loadIdentity() {
        if (this.tracks()) this.matrices.identity();
    }

    public void translate(float x, float y, float z) {
        if (this.tracks()) this.matrices.translate(x, y, z);
    }

    public void rotate(float angle, float x, float y, float z) {
        float length = (float)Math.sqrt(x * x + y * y + z * z);
        if (this.tracks() && length != 0.0F) {
            this.matrices.rotate((float)Math.toRadians(angle), x / length, y / length, z / length);
        }
    }

    public void scale(float x, float y, float z) {
        if (this.tracks()) this.matrices.scale(x, y, z);
    }

    public void multiply(FloatBuffer matrix) {
        if (this.tracks()) this.matrices.mul(this.multiplied.set(matrix));
    }

    public boolean canCaptureText() {
        return this.canCapture(true);
    }

    public void text(Identifier location, int[] vertices, float x, float y) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Texture texture = textureManager.get(location);
        if (texture == null) {
            textureManager.bind(location);
            texture = textureManager.get(location);
        }

        Pass pass = this.pass(texture.getGlId());
        int light = this.packedLight();
        int stride = DefaultVertexFormat.POSITION_TEX_COLOR.getIntSize();
        for (int offset = 0; offset + 4 * stride <= vertices.length; offset += 4 * stride) {
            for (int vertex = 0; vertex < 4; vertex++) {
                int source = offset + vertex * stride;
                this.vertex(vertex, Float.intBitsToFloat(vertices[source]) + x,
                        Float.intBitsToFloat(vertices[source + 1]) + y, Float.intBitsToFloat(vertices[source + 2]),
                        vertices[source + 3], vertices[source + 4], vertices[source + 5], light);
            }
            pass.buffer.vertices(this.quad);
        }
    }

    public boolean capture(BufferBuilder buffer) {
        VertexFormat format = buffer.getFormat();
        boolean textured = DefaultVertexFormat.POSITION_TEX_COLOR.equals(format);
        if (buffer.getDrawMode() != GL11.GL_QUADS || !textured && !DefaultVertexFormat.POSITION_COLOR.equals(format)
                || !this.canCapture(textured)) {
            this.flush();
            return false;
        }

        buffer.end();
        int count = buffer.getVertexCount();
        if (count >= 4) {
            Pass pass = this.pass(textured ? GlStateManager.TEXTURES[0].texture : 0);
            IntBuffer source = buffer.getBuffer().asIntBuffer();
            int stride = format.getIntSize();
            int color = textured ? 5 : 3;
            int light = this.packedLight();
            for (int first = 0; first + 4 <= count; first += 4) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = (first + vertex) * stride;
                    this.vertex(vertex, Float.intBitsToFloat(source.get(offset)),
                            Float.intBitsToFloat(source.get(offset + 1)), Float.intBitsToFloat(source.get(offset + 2)),
                            textured ? source.get(offset + 3) : 0, textured ? source.get(offset + 4) : 0,
                            source.get(offset + color), light);
                }
                pass.buffer.vertices(this.quad);
            }
        }
        buffer.clear();
        return true;
    }

    public void flush() {
        if (this.passCount == 0) return;

        GlStateManager.Depth depth = GlStateManager.DEPTH;
        GlStateManager.Blend blend = GlStateManager.BLEND;
        GlStateManager.Alpha alpha = GlStateManager.ALPHA_TEST;
        GlStateManager.Color color = GlStateManager.COLOR;
        boolean depthTest = depth.state.enabled;
        boolean depthMask = depth.mask;
        int depthFunc = depth.func;
        boolean blending = blend.state.enabled;
        int sourceRgb = blend.sfactorRGB;
        int destinationRgb = blend.dfactorRGB;
        int sourceAlpha = blend.sfactorAlpha;
        int destinationAlpha = blend.dfactorAlpha;
        boolean alphaTest = alpha.state.enabled;
        int alphaFunc = alpha.func;
        float alphaRef = alpha.ref;
        boolean cull = GlStateManager.CULL.state.enabled;
        boolean lighting = GlStateManager.LIGHTING.enabled;
        float red = color.r;
        float green = color.g;
        float blue = color.b;
        float opacity = color.a;
        int unit = GlStateManager.texture;
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        boolean texture = GlStateManager.TEXTURES[0].state.enabled;
        int boundTexture = GlStateManager.TEXTURES[0].texture;
        int matrixMode = this.depth > 0 ? this.matrixMode : GL11.GL_MODELVIEW;

        if (matrixMode != GL11.GL_MODELVIEW) GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GlStateManager.disableLighting();
        this.passes.subList(0, this.passCount).sort(ORDER);
        for (int i = 0; i < this.passCount; i++) {
            Pass pass = this.passes.get(i);
            pass.apply();
            pass.buffer.end();
            this.uploader.end(pass.buffer);
        }
        this.passCount = 0;
        GL11.glPopMatrix();
        if (matrixMode != GL11.GL_MODELVIEW) GL11.glMatrixMode(matrixMode);

        Pass.setDepth(depthTest, depthMask, depthFunc);
        Pass.setBlend(blending, sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        Pass.setAlpha(alphaTest, alphaFunc, alphaRef);
        Pass.setCull(cull);
        if (lighting) GlStateManager.enableLighting();
        GlStateManager.bindTexture(boundTexture);
        if (texture) GlStateManager.enableTexture();
        else GlStateManager.disableTexture();
        GlStateManager.activeTexture(GLX.GL_TEXTURE0 + unit);
        GLX.multiTexCoord2f(GLX.GL_TEXTURE1, this.lightU, this.lightV);
        GlStateManager.color4f(red, green, blue, opacity);
    }

    private boolean tracks() {
        return this.matrixMode == GL11.GL_MODELVIEW && !this.lost;
    }

    private boolean canCapture(boolean textured) {
        return this.tracks() && GlStateManager.texture == 0 && !GlStateManager.LIGHTING.enabled
                && GlStateManager.TEXTURES[0].state.enabled == textured;
    }

    private int packedLight() {
        int u = (int)this.lightU & 0xFFFF;
        int v = (int)this.lightV & 0xFFFF;
        return LITTLE_ENDIAN ? u | v << 16 : v | u << 16;
    }

    private void vertex(int index, float x, float y, float z, int u, int v, int color, int light) {
        this.matrices.transformPosition(this.position.set(x, y, z));
        int offset = index * STRIDE;
        this.quad[offset] = Float.floatToRawIntBits(this.position.x);
        this.quad[offset + 1] = Float.floatToRawIntBits(this.position.y);
        this.quad[offset + 2] = Float.floatToRawIntBits(this.position.z);
        this.quad[offset + 3] = u;
        this.quad[offset + 4] = v;
        this.quad[offset + 5] = color;
        this.quad[offset + 6] = light;
    }

    private Pass pass(int texture) {
        for (int i = 0; i < this.passCount; i++) {
            Pass pass = this.passes.get(i);
            if (pass.matches(texture)) return pass;
        }
        if (this.passCount == this.passes.size()) this.passes.add(new Pass());
        Pass pass = this.passes.get(this.passCount++);
        pass.capture(texture);
        pass.buffer.begin(GL11.GL_QUADS, FORMAT);
        return pass;
    }

    private static final class Pass {
        private final BufferBuilder buffer = new BufferBuilder(16 * 1024);
        private int texture;
        private boolean depthTest;
        private boolean depthMask;
        private int depthFunc;
        private boolean blend;
        private int sourceRgb;
        private int destinationRgb;
        private int sourceAlpha;
        private int destinationAlpha;
        private boolean alphaTest;
        private int alphaFunc;
        private float alphaRef;
        private boolean cull;

        private void capture(int texture) {
            this.texture = texture;
            this.depthTest = GlStateManager.DEPTH.state.enabled;
            this.depthMask = GlStateManager.DEPTH.mask;
            this.depthFunc = GlStateManager.DEPTH.func;
            this.blend = GlStateManager.BLEND.state.enabled;
            this.sourceRgb = GlStateManager.BLEND.sfactorRGB;
            this.destinationRgb = GlStateManager.BLEND.dfactorRGB;
            this.sourceAlpha = GlStateManager.BLEND.sfactorAlpha;
            this.destinationAlpha = GlStateManager.BLEND.dfactorAlpha;
            this.alphaTest = GlStateManager.ALPHA_TEST.state.enabled;
            this.alphaFunc = GlStateManager.ALPHA_TEST.func;
            this.alphaRef = GlStateManager.ALPHA_TEST.ref;
            this.cull = GlStateManager.CULL.state.enabled;
        }

        private boolean matches(int texture) {
            return this.texture == texture
                    && this.depthTest == GlStateManager.DEPTH.state.enabled
                    && this.depthMask == GlStateManager.DEPTH.mask
                    && this.depthFunc == GlStateManager.DEPTH.func
                    && this.blend == GlStateManager.BLEND.state.enabled
                    && this.sourceRgb == GlStateManager.BLEND.sfactorRGB
                    && this.destinationRgb == GlStateManager.BLEND.dfactorRGB
                    && this.sourceAlpha == GlStateManager.BLEND.sfactorAlpha
                    && this.destinationAlpha == GlStateManager.BLEND.dfactorAlpha
                    && this.alphaTest == GlStateManager.ALPHA_TEST.state.enabled
                    && this.alphaFunc == GlStateManager.ALPHA_TEST.func
                    && this.alphaRef == GlStateManager.ALPHA_TEST.ref
                    && this.cull == GlStateManager.CULL.state.enabled;
        }

        private int order() {
            return (this.texture != 0 ? 4 : 0) | (this.depthTest ? 2 : 0) | (this.depthMask ? 1 : 0);
        }

        private void apply() {
            setDepth(this.depthTest, this.depthMask, this.depthFunc);
            setBlend(this.blend, this.sourceRgb, this.destinationRgb, this.sourceAlpha, this.destinationAlpha);
            setAlpha(this.alphaTest, this.alphaFunc, this.alphaRef);
            setCull(this.cull);
            if (this.texture != 0) {
                GlStateManager.enableTexture();
                GlStateManager.bindTexture(this.texture);
            } else {
                GlStateManager.disableTexture();
            }
        }

        private static void setDepth(boolean test, boolean mask, int func) {
            if (test) GlStateManager.enableDepthTest();
            else GlStateManager.disableDepthTest();
            GlStateManager.depthMask(mask);
            GlStateManager.depthFunc(func);
        }

        private static void setBlend(boolean enabled, int sourceRgb, int destinationRgb, int sourceAlpha, int destinationAlpha) {
            if (enabled) GlStateManager.enableBlend();
            else GlStateManager.disableBlend();
            GlStateManager.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
        }

        private static void setAlpha(boolean enabled, int func, float ref) {
            if (enabled) GlStateManager.enableAlphaTest();
            else GlStateManager.disableAlphaTest();
            GlStateManager.alphaFunc(func, ref);
        }

        private static void setCull(boolean enabled) {
            if (enabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
        }
    }
}
