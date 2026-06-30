package com.gaia3d.process.tileprocess.multithread;

import com.gaia3d.basic.exchangable.SceneInfo;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.HalfEdgeScene;
import com.gaia3d.basic.magogl.backend.SoftwareRenderingBackend;
import com.gaia3d.basic.remesher.ReMeshParameters;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.render.MagoReTextureByObliqueCamera;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public final class IntegralReMeshMT {

    private final int threadCount;

    public IntegralReMeshMT() {
        this(2);
    }

    public IntegralReMeshMT(
            int threadCount
    ) {
        this.threadCount =
                Math.max(1, threadCount);
    }

    public List<NodeResult> process(
            List<NodeJob> jobs,
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
            if (job == null
                    || job.node() == null
                    || job.sceneInfos() == null
                    || job.sceneInfos().isEmpty()
                    || job.nodeBBoxLC() == null
                    || job.nodeTransformMatrix() == null) {
                continue;
            }

            validJobs.add(job);
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
                "Integral reMesh MT started. "
                        + "nodes={}, threads={}",
                validJobs.size(),
                realThreadCount
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        realThreadCount
                );

        CompletionService<NodeResult> completionService =
                new ExecutorCompletionService<>(
                        executor
                );

        int submittedTasks = 0;

        for (NodeJob job : validJobs) {
            completionService.submit(
                    () -> processSingleNode(
                            job,
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
                        "Integral reMesh MT completed: {} / {}",
                        i + 1,
                        submittedTasks
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();

            throw new RuntimeException(
                    "Integral reMesh MT interrupted",
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
                    "Integral reMesh MT worker failed",
                    cause
            );
        }

        completedResults.sort(
                Comparator.comparingInt(
                        NodeResult::order
                )
        );

        return completedResults;
    }

    private NodeResult processSingleNode(
            NodeJob job,
            ReMeshParameters baseReMeshParameters
    ) {
        ReMeshParameters localReMeshParameters =
                copyReMeshParameters(
                        baseReMeshParameters
                );

        List<HalfEdgeScene> localResultScenes =
                new ArrayList<>(1);

        /*
         * Backend, sesión, FBO y renderizador
         * exclusivos del worker.
         */
        MagoReTextureByObliqueCamera processor =
                new MagoReTextureByObliqueCamera(
                        new SoftwareRenderingBackend()
                );

        processor.integralReMeshByObliqueCameraV2(
                job.sceneInfos(),
                localResultScenes,
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
                    "Integral reMesh produced no scene. "
                            + "node={}",
                    job.nodeName()
            );

            return null;
        }

        if (localResultScenes.size() > 1) {
            log.warn(
                    "Integral reMesh produced multiple scenes. "
                            + "node={}, scenes={}",
                    job.nodeName(),
                    localResultScenes.size()
            );
        }

        return new NodeResult(
                job.order(),
                job.node(),
                localResultScenes.getFirst()
        );
    }

    private static ReMeshParameters copyReMeshParameters(
            ReMeshParameters source
    ) {
        if (source == null) {
            return new ReMeshParameters();
        }

        /*
         * Preferiblemente:
         *
         * return source.copyForWorker();
         */

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
         * Valores propios del worker.
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
                    Math.max(
                            maxScreenSize,
                            1
                    );

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