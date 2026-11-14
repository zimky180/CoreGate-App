/*
 * Derived from Telegram Android, org.telegram.ui.Components.ProfileMetaballView.
 * Source: https://github.com/DrKLO/Telegram/blob/master/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileMetaballView.java
 * GPL-2.0; see app/src/main/assets/licenses/Telegram-GPL-2.0.txt.
 * Changes: standalone geometry wrapper; Math.hypot replaces ZXing MathUtils.distance.
 */
package com.zimky.coregateUS.ui;
import android.graphics.Path;
import android.graphics.PointF;

public final class TelegramMetaballPath {
    private final PointF c1 = new PointF(), c2 = new PointF();
    private final PointF p1 = new PointF(), p2 = new PointF(), p3 = new PointF(), p4 = new PointF();
    private final PointF h1 = new PointF(), h2 = new PointF(), h3 = new PointF(), h4 = new PointF();
    private final PointF a1 = new PointF(), a2 = new PointF();
    private Path path;
    public boolean build(Path output, float x1, float y1, float radius1,
                         float x2, float y2, float radius2, float v) {
        path = output;
        path.rewind();
        c1.set(x1, y1);
        c2.set(x2, y2);
        return createMetaballPath(radius1, radius2, v);
    }
    private boolean createMetaballPath(float radius1, float radius2, float v) {
        final float HALF_PI = (float) (Math.PI / 2);
        final float HANDLE_SIZE = 20.0f;

        float d = dist(c1, c2);
        float maxDist = radius1 + radius2 * 2.25f;
        float u1 = 0f, u2 = 0f;

        if (radius1 == 0 || radius2 == 0 || d > maxDist || d <= Math.abs(radius1 - radius2)) {
            return false;
        }

        if (d < radius1 + radius2) {
            u1 = (float) Math.acos((radius1 * radius1 + d * d - radius2 * radius2) / (2 * radius1 * d));
            u2 = (float) Math.acos((radius2 * radius2 + d * d - radius1 * radius1) / (2 * radius2 * d));
        }

        float angleBetweenCenters = angle(c2, c1);
        float maxSpread = (float) Math.acos((radius1 - radius2) / d);

        float angle1 = angleBetweenCenters + u1 + (maxSpread - u1) * v;
        float angle2 = angleBetweenCenters - u1 - (maxSpread - u1) * v;
        float angle3 = angleBetweenCenters + (float) Math.PI - u2 - ((float) Math.PI - u2 - maxSpread) * v;
        float angle4 = angleBetweenCenters - (float) Math.PI + u2 + ((float) Math.PI - u2 - maxSpread) * v;

        getVector(c1, angle1, radius1, p1);
        getVector(c1, angle2, radius1, p2);
        getVector(c2, angle3, radius2, p3);
        getVector(c2, angle4, radius2, p4);

        float totalRadius = radius1 + radius2;
        float d2Base = Math.min(v * HANDLE_SIZE, dist(p1, p3) / totalRadius);
        float d2 = d2Base * Math.min(1, d * 2 / totalRadius);

        float r1 = radius1 * d2;
        float r2 = radius2 * d2;

        getVector(p1, angle1 - HALF_PI, r1, h1);
        getVector(p2, angle2 + HALF_PI, r1, h2);
        getVector(p3, angle3 + HALF_PI, r2, h3);
        getVector(p4, angle4 - HALF_PI, r2, h4);

        path.rewind();
        path.moveTo(p1.x, p1.y);
        path.cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y);

        getVector(p3, angle3 + HALF_PI, r2 * 0.55f, a1);
        getVector(p4, angle4 - HALF_PI, r2 * 0.55f, a2);
        path.cubicTo(a1.x, a1.y, a2.x, a2.y, p4.x, p4.y);

        path.cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y);
        path.close();
        return true;
    }

    private float dist(PointF a, PointF b) {
        return (float) Math.hypot(a.x - b.x, a.y - b.y);
    }

    private float angle(PointF a, PointF b) {
        return (float) Math.atan2(a.y - b.y, a.x - b.x);
    }

    private void getVector(PointF center, float angle, float radius, PointF out) {
        out.x = (float) (center.x + radius * Math.cos(angle));
        out.y = (float) (center.y + radius * Math.sin(angle));
    }
}
