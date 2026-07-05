package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Vector3d;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ThinScenesScaler {
    public static ThinExpansionPlan createThinExpansionPlan(
            GaiaBoundingBox sceneBBox,
            GaiaBoundingBox nodeBBox,
            double cellSize,
            boolean lockX,
            boolean lockY,
            double scaleFactor
    ) {
        if (sceneBBox == null
                || nodeBBox == null
                || cellSize <= 0.0
                || scaleFactor <= 1.0) {

            return new ThinExpansionPlan(
                    false,
                    0.0,
                    false,
                    0.0,
                    1.0
            );
        }

        /*
         * Only consider a side attached to the node boundary when it
         * is reasonably close to that boundary.
         */
        double boundaryTolerance =
                Math.max(
                        1e-4,
                        cellSize * 0.05
                );

        boolean expandX =
                false;

        boolean expandY =
                false;

        double fixedX =
                0.0;

        double fixedY =
                0.0;

        if (lockX) {
            double distanceToMinX =
                    Math.abs(
                            sceneBBox.getMinX()
                                    - nodeBBox.getMinX()
                    );

            double distanceToMaxX =
                    Math.abs(
                            sceneBBox.getMaxX()
                                    - nodeBBox.getMaxX()
                    );

            double nearestDistanceX =
                    Math.min(
                            distanceToMinX,
                            distanceToMaxX
                    );

            if (nearestDistanceX <= boundaryTolerance) {
                expandX = true;

                /*
                 * Preserve the actual fragment side, rather than
                 * snapping it to the node boundary.
                 */
                fixedX =
                        distanceToMinX <= distanceToMaxX
                                ? sceneBBox.getMinX()
                                : sceneBBox.getMaxX();
            }
        }

        if (lockY) {
            double distanceToMinY =
                    Math.abs(
                            sceneBBox.getMinY()
                                    - nodeBBox.getMinY()
                    );

            double distanceToMaxY =
                    Math.abs(
                            sceneBBox.getMaxY()
                                    - nodeBBox.getMaxY()
                    );

            double nearestDistanceY =
                    Math.min(
                            distanceToMinY,
                            distanceToMaxY
                    );

            if (nearestDistanceY <= boundaryTolerance) {
                expandY = true;

                fixedY =
                        distanceToMinY <= distanceToMaxY
                                ? sceneBBox.getMinY()
                                : sceneBBox.getMaxY();
            }
        }

        return new ThinExpansionPlan(
                expandX,
                fixedX,
                expandY,
                fixedY,
                scaleFactor
        );
    }

    public static int applyThinExpansion(
            Collection<GaiaVertex> vertices,
            ThinExpansionPlan plan
    ) {
        if (vertices == null
                || vertices.isEmpty()
                || plan == null
                || !plan.isEnabled()) {
            return 0;
        }

        /*
         * IdentityHashMap prevents modifying the same GaiaVertex
         * object twice if it appears in multiple collections.
         */
        Set<GaiaVertex> visitedVertices =
                Collections.newSetFromMap(
                        new IdentityHashMap<>()
                );

        int modifiedVertices =
                0;

        for (GaiaVertex vertex : vertices) {
            if (vertex == null
                    || !visitedVertices.add(vertex)
                    || vertex.getPosition() == null) {
                continue;
            }

            Vector3d position =
                    vertex.getPosition();

            boolean modified =
                    false;

            if (plan.expandX()) {
                position.x =
                        plan.fixedX()
                                + (position.x - plan.fixedX())
                                * plan.scaleFactor();

                modified = true;
            }

            if (plan.expandY()) {
                position.y =
                        plan.fixedY()
                                + (position.y - plan.fixedY())
                                * plan.scaleFactor();

                modified = true;
            }

            if (modified) {
                modifiedVertices++;
            }
        }

        return modifiedVertices;
    }

    public record ThinExpansionPlan(
            boolean expandX,
            double fixedX,
            boolean expandY,
            double fixedY,
            double scaleFactor
    ) {
        public boolean isEnabled() {
            return expandX || expandY;
        }
    }
}
