package com.gaia3d.process.tileprocess.multithread;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.magogl.backend.SoftwareRenderingBackend;
import com.gaia3d.basic.remesher.ReMeshParameters;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.render.MagoReTextureByObliqueCamera;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Slf4j
public final class IntegralDecimateMT {

    private final int threadCount;

    public IntegralDecimateMT() {
        this(2);
    }

    public IntegralDecimateMT(
            int threadCount
    ) {
        this.threadCount =
                Math.max(1, threadCount);
    }

    public void process(
            List<NodeJob> jobs,
            DecimateParameters baseDecimateParameters,
            ReMeshParameters baseReMeshParameters,
            Consumer<NodeResult> resultConsumer
    ) {
        Objects.requireNonNull(
                jobs,
                "jobs must not be null"
        );

        Objects.requireNonNull(
                resultConsumer,
                "resultConsumer must not be null"
        );

        if (jobs.isEmpty()) {
            return;
        }

        ExecutorService executorService =
                Executors.newFixedThreadPool(
                        threadCount
                );

        CompletionService<NodeResult> completionService =
                new ExecutorCompletionService<>(
                        executorService
                );

        /*
         * Keep only a small number of jobs in flight.
         * This provides backpressure and prevents finished
         * scenes from accumulating in memory.
         */
        int maxInFlight =
                Math.max(
                        threadCount * 2,
                        1
                );

        Iterator<NodeJob> jobIterator =
                jobs.iterator();

        int submittedCount = 0;
        int completedCount = 0;
        int inFlightCount = 0;

        try {
            while (jobIterator.hasNext()
                    && inFlightCount < maxInFlight) {

                NodeJob job =
                        jobIterator.next();

                completionService.submit(
                        () -> processSingleNode(
                                job,
                                baseDecimateParameters,
                                baseReMeshParameters
                        )
                );

                submittedCount++;
                inFlightCount++;
            }

            while (completedCount < jobs.size()) {
                Future<NodeResult> completedFuture =
                        completionService.take();

                inFlightCount--;

                NodeResult result;

                try {
                    result =
                            completedFuture.get();
                } catch (ExecutionException e) {
                    Throwable cause =
                            e.getCause();

                    throw new RuntimeException(
                            "Integral decimate MT worker failed",
                            cause
                    );
                }

                /*
                 * This runs in the caller thread, not in a worker.
                 * Result processing therefore remains sequential.
                 */
                if (result != null) {
                    resultConsumer.accept(
                            result
                    );
                }

                completedCount++;

                log.info(
                        "Integral decimate MT completed: {} / {}",
                        completedCount,
                        jobs.size()
                );

                if (jobIterator.hasNext()) {
                    NodeJob job =
                            jobIterator.next();

                    completionService.submit(
                            () -> processSingleNode(
                                    job,
                                    baseDecimateParameters,
                                    baseReMeshParameters
                            )
                    );

                    submittedCount++;
                    inFlightCount++;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Integral decimate MT was interrupted",
                    e
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    private NodeResult processSingleNode(
            NodeJob job,
            DecimateParameters baseDecimateParameters,
            ReMeshParameters baseReMeshParameters
    ) {
        DecimateParameters localDecimateParameters =
                copyDecimateParameters(
                        baseDecimateParameters
                );

        ReMeshParameters localReMeshParameters =
                copyReMeshParameters(
                        baseReMeshParameters
                );

        List<HalfEdgeScene> localResultScenes =
                new ArrayList<>(1);

        /*
         * Cada worker tiene:
         *
         * - backend propio;
         * - sesión propia;
         * - FBOs propios;
         * - parámetros mutables propios.
         */
        MagoReTextureByObliqueCamera processor =
                new MagoReTextureByObliqueCamera(
                        new SoftwareRenderingBackend()
                );

        processor.integralDecimateByObliqueCamera(
                job.sceneInfos(),
                localResultScenes,
                localDecimateParameters,
                localReMeshParameters,
                job.nodeBBoxLC().clone(),
                new Matrix4d(
                        job.nodeTransformMatrix()
                ),
                job.maxScreenSize(),
                job.outputPath(),
                job.nodeName(),
                job.lod()
        );

        if (localResultScenes.isEmpty()) {
            log.info(
                    "Integral decimate produced no scene. "
                            + "node={}",
                    job.nodeName()
            );

            return null;
        }

        return new NodeResult(
                job.order(),
                job.node(),
                localResultScenes.getFirst()
        );
    }

    private static DecimateParameters copyDecimateParameters(
            DecimateParameters source
    ) {
        if (source == null) {
            return new DecimateParameters();
        }

        /*
         * Requiere constructor de copia.
         */
        return source.clone();
    }

    private static ReMeshParameters copyReMeshParameters(
            ReMeshParameters source
    ) {
        if (source == null) {
            return new ReMeshParameters();
        }

        ReMeshParameters result =
                new ReMeshParameters();

        /*
         * Compartidos como estructuras de solo lectura.
         */
        result.setCellGrid(
                source.getCellGrid()
        );

        result.setGlobalBoundaryAnchors(
                source.getGlobalBoundaryAnchors()
        );

        /*
         * Valores privados de cada worker.
         */
        result.setAngleDeg(
                source.getAngleDeg()
        );

        result.setTexturePixelsForMeter(
                source.getTexturePixelsForMeter()
        );

        return result;
    }

    public record NodeJob(
            int order,
            Node node,
            List<SceneInfo> sceneInfos,
            GaiaBoundingBox nodeBBoxLC,
            Matrix4d nodeTransformMatrix,
            int maxScreenSize,
            String outputPath,
            String nodeName,
            int lod
    ) {
        public NodeJob {
            Objects.requireNonNull(
                    node,
                    "node must not be null"
            );

            Objects.requireNonNull(
                    sceneInfos,
                    "sceneInfos must not be null"
            );

            Objects.requireNonNull(
                    nodeBBoxLC,
                    "nodeBBoxLC must not be null"
            );

            Objects.requireNonNull(
                    nodeTransformMatrix,
                    "nodeTransformMatrix must not be null"
            );

            sceneInfos =
                    List.copyOf(sceneInfos);

            nodeBBoxLC =
                    nodeBBoxLC.clone();

            nodeTransformMatrix =
                    new Matrix4d(
                            nodeTransformMatrix
                    );

            maxScreenSize =
                    Math.max(1, maxScreenSize);

            nodeName =
                    nodeName == null
                            ? "node_" + order
                            : nodeName;
        }
    }

    public record NodeResult(
            int order,
            Node node,
            HalfEdgeScene halfEdgeScene
    ) {
    }
}