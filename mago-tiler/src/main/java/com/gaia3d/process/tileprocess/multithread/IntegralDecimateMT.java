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

    public List<NodeResult> process(
            List<NodeJob> jobs,
            DecimateParameters baseDecimateParameters,
            ReMeshParameters baseReMeshParameters
    ) {
        List<NodeResult> completedResults =
                new ArrayList<>();

        if (jobs == null || jobs.isEmpty()) {
            return completedResults;
        }

        List<NodeJob> validJobs =
                new ArrayList<>(jobs.size());

        for (NodeJob job : jobs) {
            if (job != null
                    && job.node() != null
                    && job.sceneInfos() != null
                    && !job.sceneInfos().isEmpty()
                    && job.nodeBBoxLC() != null
                    && job.nodeTransformMatrix() != null) {

                validJobs.add(job);
            }
        }

        if (validJobs.isEmpty()) {
            return completedResults;
        }

        int realThreadCount =
                Math.min(
                        threadCount,
                        validJobs.size()
                );

        log.info(
                "Integral decimate MT started. "
                        + "nodes={}, threads={}",
                validJobs.size(),
                realThreadCount
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        realThreadCount
                );

        CompletionService<NodeResult>
                completionService =
                new ExecutorCompletionService<>(
                        executor
                );

        int submittedTasks = 0;

        for (NodeJob job : validJobs) {
            completionService.submit(
                    () -> processSingleNode(
                            job,
                            baseDecimateParameters,
                            baseReMeshParameters
                    )
            );

            submittedTasks++;
        }

        executor.shutdown();

        try {
            for (int i = 0; i < submittedTasks; i++) {
                Future<NodeResult> future =
                        completionService.take();

                NodeResult result =
                        future.get();

                if (result != null
                        && result.halfEdgeScene() != null) {
                    completedResults.add(result);
                }

                log.info(
                        "Integral decimate MT completed: {} / {}",
                        i + 1,
                        submittedTasks
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            throw new RuntimeException(
                    "Integral decimate MT interrupted",
                    e
            );

        } catch (ExecutionException e) {
            executor.shutdownNow();

            Throwable cause =
                    e.getCause();

            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(
                    "Integral decimate MT worker failed",
                    cause
            );
        }

        /*
         * CompletionService devuelve los resultados
         * según terminan. Restauramos el orden original.
         */
        completedResults.sort(
                Comparator.comparingInt(
                        NodeResult::order
                )
        );

        return completedResults;
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

    private static Map<Vector3i, Vector3d>
    copyCellAveragePositions(
            Map<Vector3i, Vector3d> source
    ) {
        Map<Vector3i, Vector3d> result =
                new HashMap<>();

        if (source == null || source.isEmpty()) {
            return result;
        }

        for (Map.Entry<Vector3i, Vector3d> entry
                : source.entrySet()) {

            Vector3i key =
                    entry.getKey();

            Vector3d value =
                    entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            result.put(
                    new Vector3i(key),
                    new Vector3d(value)
            );
        }

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