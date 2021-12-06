package com.serenegiant.usb.processor;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

public class GLUtils {

    private static final String TAG = "GLUtils";

    private static int GL_VERSION = 2;

    public static final String TEXTURE_VS =
            "attribute vec2 a_pos;\n" +
            "attribute vec2 a_tex;\n" +
            "varying vec2 v_tex_coord;\n" +
            "uniform mat4 u_mvp;\n" +
            "uniform mat4 u_tex_trans;\n" +
            "void main() {\n" +
            "   gl_Position = u_mvp * vec4(a_pos, 0.0, 1.0);\n" +
            "   v_tex_coord = (u_tex_trans * vec4(a_tex, 0.0, 1.0)).st;\n" +
            "}\n";

    public static final String TEXTURE_2D_FS =
            "precision mediump float;\n" +
            "uniform sampler2D u_tex;\n" +
            "varying vec2 v_tex_coord;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(u_tex, v_tex_coord);\n" +
            "}\n";

    public static final String TEXTURE_EXTERNAL_FS =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES u_tex;\n" +
                    "varying vec2 v_tex_coord;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(u_tex, v_tex_coord);\n" +
                    "}\n";

    public static float[] VERTEX_POSITION = {
            -1.0f, -1.0f,
            -1.0f,  1.0f,
            1.0f, -1.0f,
            1.0f,  1.0f,
    };

    public static float[] TEXTURE_COORDINATE = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public static float[] TEXTURE_COORDINATE_FLIP = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

    /**
     * Identity matrix for general use.  Don't modify or life will get weird.
     */
    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    public static boolean isGL3() {
        return GL_VERSION > 2;
    }

    public static int createProgram(String vertexShader, String fragmentShader) {
        int vertexShaderId = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fragmentShaderId = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            return -1;
        }
        int program = GLES20.glCreateProgram();
        /**
         * link vertex shade and fragment shade
         */
        GLES20.glAttachShader(program, vertexShaderId);
        GLES20.glAttachShader(program, fragmentShaderId);
        GLES20.glLinkProgram(program);
        /**
         * Check compile result
         */
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program);
            Log.d(TAG, "Linking of program failed !");
            return -1;
        }
        if (!validateProgram(program)) {
            return -1;
        }
        return program;
    }

    public static int genFrameBufferTextureID(int width, int height) {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        return texture[0];
    }

    private static boolean validateProgram(int programObjectId) {
        GLES20.glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        Log.d(TAG, "Results of validating program: " + validateStatus[0]
                + "\nLog:" + GLES20.glGetProgramInfoLog(programObjectId));
        return validateStatus[0] != 0;
    }

    private static int compileShader(int type, String shaderCode) {
        /**
         * create shader
         */
        int objectId = GLES20.glCreateShader(type);
        /**
         * bind shader source code
         */
        GLES20.glShaderSource(objectId, shaderCode);
        /**
         * compile shader
         */
        GLES20.glCompileShader(objectId);
        /**
         * Check compile result
         */
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(objectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(objectId);
            Log.e(TAG, "Compilation of shader failed.");
            return 0;
        }
        return objectId;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static boolean checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            return false;
        }
        return true;
    }

    /**
     * Create a VAO
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static int createVAO() {
        int[] vao = new int[1];
        GLES30.glGenVertexArrays(1, vao, 0);
        return vao[0];
    }

    /**
     * Create a FBO
     */
    public static int createFBO() {
        int[] fbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        return fbo[0];
    }
}
