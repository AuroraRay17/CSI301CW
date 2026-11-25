package com.example.team18;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import androidx.core.graphics.PathParser;

final class RabbitDrawing {
    static final float CUSHION_LIFT = 8f;
    private static final int OUTLINE = 0xFFDBA267;
    private static final int EYE = 0xFF8D431E;
    private static final int PAW_LIGHT = 0xFFFFF0D8;
    private static final int CLAW = 0xFFEFC89E;
    private static final float HEAD_X = 449f;
    private static final float HEAD_Y = 1038f;
    private static final float HINGE_X = 422f;
    private static final float HINGE_Y = 959f;
    private static final int CROWN_X = 419;
    private static final int CROWN_Y = 912;
    private static final float DEGREES_PER_RADIAN = 180f / (float) Math.PI;

    private static final Path BODY = path("M362 992 C414 975 461 971 487 958 C521 940 552 930 587 932 "
            + "C648 933 693 969 712 1017 C727 1050 734 1115 703 1145 C688 1158 635 1157 592 1157 "
            + "L372 1157 C349 1148 336 1112 342 1073 C346 1046 355 1018 362 992 Z");
    private static final Path BACK_EDGE = path("M472 966 C512 941 553 931 587 932 C648 933 693 969 712 1017 "
            + "C727 1050 734 1115 703 1145 C688 1158 635 1157 592 1157 L427 1157");
    private static final Path HAUNCH = path("M543 1110 C567 1110 584 1100 611 1089 C639 1077 672 1080 696 1091 "
            + "C709 1098 719 1095 728 1088 C729 1114 715 1136 700 1146 C678 1158 635 1157 589 1157 "
            + "H520 C513 1140 527 1116 543 1110 Z");
    private static final Path TAIL = path("M704 1019 C715 1004 724 995 736 997 C748 999 754 1012 747 1026 "
            + "C761 1024 769 1033 769 1045 C769 1054 763 1062 755 1066 C763 1081 751 1093 727 1094 "
            + "L703 1084 Z");
    private static final Path FAR_PAW = path("M151 1148 C144 1138 145 1125 154 1116 C174 1099 219 1095 252 1090 "
            + "C281 1087 320 1091 337 1108 C356 1128 339 1155 313 1155 L178 1156 "
            + "C164 1156 155 1153 151 1148 Z");
    private static final Path FAR_PAW_LIGHT = path("M151 1148 C144 1138 145 1125 154 1116 C169 1104 202 1100 225 1097 "
            + "C242 1103 255 1119 255 1135 C255 1144 251 1152 246 1155 L178 1156 C164 1156 155 1153 151 1148 Z");
    private static final Path CLOSE_PAW = path("M319 1151 C309 1141 312 1127 324 1117 C340 1104 368 1101 398 1104 "
            + "C424 1105 443 1118 450 1137 C455 1152 442 1159 424 1159 L354 1160 C337 1160 326 1157 319 1151 Z");
    private static final Path CLOSE_PAW_LIGHT = path("M319 1151 C309 1141 312 1127 324 1117 C338 1107 356 1105 371 1105 "
            + "C396 1107 414 1124 414 1140 C414 1150 408 1157 397 1159 L354 1160 C337 1160 326 1157 319 1151 Z");
    private static final Path FAR_CLAW = path("M173 1128 Q159 1137 169 1150");
    private static final Path CLOSE_CLAW = path("M343 1132 Q328 1140 338 1153");
    private static final Path FAR_EAR = path("M273 956 C297 938 319 949 308 971 C290 1012 264 1058 249 1081 "
            + "C229 1079 218 1067 220 1047 C222 1013 245 980 273 956 Z");
    private static final Path FAR_EAR_INNER = path("M273 970 C292 957 299 961 295 975 C281 1010 259 1056 245 1073 "
            + "C233 1070 225 1059 228 1047 C231 1019 251 989 273 970 Z");
    private static final Path FACE = path("M245 1025 C268 979 288 942 332 921 C367 903 415 905 454 927 "
            + "C484 945 505 970 512 1003 C521 1049 494 1101 461 1124 C423 1148 365 1144 319 1132 "
            + "C273 1119 225 1083 245 1025 Z");
    private static final Path FACE_EDGE = path("M" + CROWN_X + " " + CROWN_Y
            + " C389 904 358 906 332 921 C288 942 268 979 245 1025 C225 1083 273 1119 319 1132 "
            + "C345 1140 372 1144 397 1143");
    private static final Path SMILE = path("M286 1049 C289 1060 303 1067 314 1054");
    private static final float[][] EAR_CURVES = {
            {94, 30, 116, 74, 129, 118},
            {142, 160, 150, 190, 140, 207},
            {130, 226, 92, 223, 67, 214},
            {37, 204, 20, 183, 11, 154},
            {-1, 118, -6, 90, -9, 61}
    };
    private static final Path INNER_EAR = path("M13 7 C66 34 104 96 125 150 C135 180 145 202 126 213 "
            + "C104 222 71 214 49 198 C19 174 13 125 12 88 C10 54 12 27 13 7 Z");

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path eyeLine = new Path();
    private final Path earRim = new Path();
    private final Path earCoat = new Path();
    private final Matrix earTransform = new Matrix();
    private final float[] points = new float[6];
    private final Shader bodyFill = new LinearGradient(500, 939, 620, 1157,
            0xFFFFEED7, 0xFFFEECD2, Shader.TileMode.CLAMP);
    private final Shader faceFill = new LinearGradient(250, 907, 366, 1143,
            new int[]{0xFFFFEED8, 0xFFFEEED8, 0xFFF4D8B4}, new float[]{0f, .75f, 1f}, Shader.TileMode.CLAMP);
    private final Shader earFill = new LinearGradient(-12, -37, 123, 222,
            0xFFFEEED8, 0xFFFFE9CF, Shader.TileMode.CLAMP);
    private final Shader shadow = new RadialGradient(0, 0, 327,
            new int[]{0x9181482A, 0x7093522E, 0x0093522E}, new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP);

    RabbitDrawing() {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    void draw(Canvas canvas, PetItem action, float phase, float breath, boolean cushion) {
        float greet = action == PetItem.GREET ? smooth(0f, .24f, phase) * (1f - smooth(.60f, 1f, phase)) : 0f;
        float pat = action == PetItem.PET ? smooth(0f, .20f, phase) * (1f - smooth(.64f, 1f, phase)) : 0f;
        float headX = HEAD_X + 9f * greet - 12f * pat;
        float headY = HEAD_Y - 17f * greet + 9f * pat - 2.6f * breath;
        float headTilt = (.19f * greet - .065f * pat) * DEGREES_PER_RADIAN;
        float earSwing = (-.94f * greet + .18f * (float) Math.sin(phase * Math.PI * 10) * pat) * DEGREES_PER_RADIAN;

        drawFloor(canvas, cushion);
        canvas.save();
        if (cushion) canvas.translate(0, -CUSHION_LIFT);
        drawBody(canvas, breath);
        drawPaw(canvas, FAR_PAW, FAR_PAW_LIGHT, FAR_CLAW, 0xFFF2D8B8);

        canvas.save();
        positionHead(canvas, headX, headY, headTilt);
        fill(canvas, FAR_EAR, 0xFFFFEED8);
        fill(canvas, FAR_EAR_INNER, 0xFFF8BFAC);
        line(canvas, FAR_EAR, OUTLINE, 3f);
        fill(canvas, FACE, faceFill);
        line(canvas, FACE_EDGE, OUTLINE, 3f);
        float opening = smooth(.18f, .78f, smooth(.26f, .72f, greet));
        drawEye(canvas, 277, 1004, 12, 7, opening, pat);
        drawEye(canvas, 371, 1028, 45, 15, opening, pat);
        line(canvas, SMILE, 0xFFCF7041, 4.2f);
        canvas.restore();

        drawPaw(canvas, CLOSE_PAW, CLOSE_PAW_LIGHT, CLOSE_CLAW, 0xFFF3D7B5);
        canvas.save();
        positionHead(canvas, headX, headY, headTilt);
        drawNearEar(canvas, earSwing);
        canvas.restore();
        canvas.restore();
    }

    private void drawFloor(Canvas canvas, boolean cushion) {
        canvas.save();
        canvas.translate(458f, 1156f);
        canvas.scale(1f, .075f);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setShader(shadow);
        canvas.drawCircle(0, 0, 327f, paint);
        paint.setShader(null);
        canvas.restore();
        if (cushion) {
            oval(canvas, 131, 1119, 785, 1196, 0xFF719DE6);
            oval(canvas, 137, 1114, 779, 1184, 0xFFA8CCFF);
            oval(canvas, 166, 1123, 750, 1175, 0xFF8DB6F9);
        }
    }

    private void drawBody(Canvas canvas, float breath) {
        canvas.save();
        canvas.scale(1f, 1f + .032f * breath, 0f, 1157f);
        fill(canvas, TAIL, 0xFFFFEED8);
        line(canvas, TAIL, OUTLINE, 3f);
        fill(canvas, BODY, bodyFill);
        fill(canvas, HAUNCH, 0xFFF8DFBF);
        line(canvas, BACK_EDGE, OUTLINE, 3f);
        canvas.restore();
    }

    private void drawPaw(Canvas canvas, Path paw, Path light, Path claw, int color) {
        fill(canvas, paw, color);
        fill(canvas, light, PAW_LIGHT);
        line(canvas, paw, OUTLINE, 3f);
        line(canvas, claw, CLAW, 2.6f);
    }

    private static void positionHead(Canvas canvas, float x, float y, float tilt) {
        canvas.translate(x, y);
        canvas.rotate(tilt);
        canvas.translate(-HEAD_X, -HEAD_Y);
    }

    private void drawNearEar(Canvas canvas, float swing) {
        earTransform.setRotate(swing);
        earTransform.postTranslate(HINGE_X, HINGE_Y);
        earRim.rewind();
        earRim.moveTo(CROWN_X, CROWN_Y);
        // The ear and face share the same crown point, including during rotation.
        points[0] = 43f;
        points[1] = -14f;
        points[2] = 59f;
        points[3] = 0f;
        earTransform.mapPoints(points, 0, points, 0, 2);
        earRim.cubicTo(434f, 916f, points[0], points[1], points[2], points[3]);
        for (float[] curve : EAR_CURVES) {
            earTransform.mapPoints(points, curve);
            earRim.cubicTo(points[0], points[1], points[2], points[3], points[4], points[5]);
        }
        earCoat.set(earRim);
        points[0] = -14f;
        points[1] = 22f;
        earTransform.mapPoints(points, 0, points, 0, 1);
        earCoat.cubicTo(points[0], points[1], 410f, 932f, CROWN_X, CROWN_Y);
        earCoat.close();

        earFill.setLocalMatrix(earTransform);
        fill(canvas, earCoat, earFill);
        canvas.save();
        canvas.concat(earTransform);
        fill(canvas, INNER_EAR, 0xFFFDCAB4);
        canvas.restore();
        line(canvas, earRim, OUTLINE, 3f);
    }

    private void drawEye(Canvas canvas, float x, float y, float span, float rise, float opening, float squint) {
        boolean far = span < 20f;
        if (opening < 1f) {
            float halfSpan = span * (.5f - .38f * opening);
            eyeLine.rewind();
            eyeLine.moveTo(x - halfSpan, y + (far ? -5f : 0f) * (1f - opening));
            eyeLine.quadTo(x + 3f, y - (rise + 6f * squint) * (1f - opening), x + halfSpan, y + 5f);
            line(canvas, eyeLine, alpha(EYE, 1f - opening), far ? 5f : 6f);
        }
        if (opening > 0f) {
            float rx = far ? 4.4f : 7.2f;
            float ry = 2f + 8f * opening;
            oval(canvas, x - rx, y - 2f - ry, x + rx, y - 2f + ry, alpha(EYE, opening));
            float glintX = x + rx * .24f;
            oval(canvas, glintX - rx * .29f, y - 7.2f, glintX + rx * .29f, y - 2.8f, alpha(0xFFFFF2DA, opening));
        }
    }

    private static float smooth(float start, float end, float value) {
        float p = Math.max(0f, Math.min(1f, (value - start) / (end - start)));
        return p * p * (3f - 2f * p);
    }

    private static int alpha(int color, float opacity) {
        return (color & 0x00FFFFFF) | (Math.round(255 * opacity) << 24);
    }

    private static Path path(String commands) {
        return PathParser.createPathFromPathData(commands);
    }

    private void fill(Canvas canvas, Path path, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private void fill(Canvas canvas, Path path, Shader shader) {
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setShader(shader);
        canvas.drawPath(path, paint);
        paint.setShader(null);
    }

    private void line(Canvas canvas, Path path, int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(width);
        canvas.drawPath(path, paint);
    }

    private void oval(Canvas canvas, float left, float top, float right, float bottom, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawOval(left, top, right, bottom, paint);
    }
}
