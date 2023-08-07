package basic.extension;

import basic.structure.*;
import org.joml.Vector3d;
import process.preprocess.PreProcess;
import process.tileprocess.tile.TileInfo;

import java.util.ArrayList;
import java.util.List;

public class GeometryOptimizer implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        return null;
    }

    public void optimize(ArrayList<GaiaScene> gaiaScenes) {
        System.out.println("Optimize TEST");

        // delete faces with normal aprox to (0, 0, -1).
        Vector3d normalReference = new Vector3d(0, 0, -1);
        double error = 0.1;
        for (GaiaScene gaiaScene : gaiaScenes) {
            deleteFacesWithNormalInScene(gaiaScene, normalReference, error);
        }

        /*
        // optimize scenes.
        GeometryOptimizer geometryOptimizer = new GeometryOptimizer();
        geometryOptimizer.optimize((ArrayList<GaiaScene>) scenes);
        // end optimize scenes.

         */
    }

    public void deleteFacesWithNormalInNode(GaiaNode gaiaNode, Vector3d normalReference, double error) {
        if(gaiaNode.getMeshes().size() > 0) {
            List<GaiaMesh> meshes = gaiaNode.getMeshes();
            ArrayList<GaiaMesh> meshesToRemove = new ArrayList<>();
            for (GaiaMesh mesh : meshes) {
                boolean faceRemoved = false;
                ArrayList<GaiaPrimitive> primitives = mesh.getPrimitives();
                ArrayList<GaiaPrimitive> primitivesToRemove = new ArrayList<>();
                for (GaiaPrimitive primitive : primitives) {
                    List<GaiaSurface> surfaces = primitive.getSurfaces();
                    ArrayList<GaiaSurface> surfacesToRemove = new ArrayList<>();
                    for (GaiaSurface surface : surfaces) {
                        ArrayList<GaiaFace> faces = surface.getFaces();
                        ArrayList<GaiaFace> facesToRemove = new ArrayList<>();
                        for (GaiaFace face : faces) {
                            Vector3d faceNormal = face.getFaceNormal();
                            double dotProd = faceNormal.dot(normalReference);
                            if(dotProd > 1 - error) {
                                facesToRemove.add(face);
                                faceRemoved = true;
                            }
                        }
                        if(faceRemoved) {
                            // remove faces.
                            for (GaiaFace face : facesToRemove) {
                                faces.remove(face);
                            }

                            // check if the surface has faces.
                            if(surface.getFaces().size() == 0) {
                                surfacesToRemove.add(surface);
                            }
                        }
                    }
                    if(faceRemoved) {
                        // remove surfaces.
                        for (GaiaSurface surface : surfacesToRemove) {
                            surfaces.remove(surface);
                        }
                        // check if the primitive has surfaces.
                        if(primitive.getSurfaces().size() == 0) {
                            primitivesToRemove.add(primitive);
                        }
                    }
                }
                if(faceRemoved) {
                    // remove primitives.
                    for (GaiaPrimitive primitive : primitivesToRemove) {
                        primitives.remove(primitive);
                    }
                    // check if the mesh has primitives.
                    if(mesh.getPrimitives().size() == 0) {
                        meshesToRemove.add(mesh);
                    }
                }
            }
            // remove meshes.
            for (GaiaMesh mesh : meshesToRemove) {
                meshes.remove(mesh);
            }
        }

        // check if exist children.
        if(gaiaNode.getChildren().size() > 0) {
            gaiaNode.getChildren().forEach((child) -> deleteFacesWithNormalInNode(child, normalReference, error));
        }
    }

    public void deleteFacesWithNormalInScene(GaiaScene gaiaScene, Vector3d normalReference, double error) {
        List<GaiaNode> nodes = gaiaScene.getNodes();
        for (GaiaNode node : nodes) {
            deleteFacesWithNormalInNode(node, normalReference, error);
        }
    }

}
