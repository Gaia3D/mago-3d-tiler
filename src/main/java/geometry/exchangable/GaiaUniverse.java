package geometry.exchangable;

import geometry.structure.GaiaScene;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class GaiaUniverse {
    private final String name;
    private final Path inputRoot;
    private final Path outputRoot;
    private final List<GaiaScene> scenes;
    private final List<GaiaSet> sets;

    public GaiaUniverse(String name, File inputRoot, File outputRoot) {
        this.name = name;
        if (!(inputRoot.isDirectory() && outputRoot.isDirectory())) {
            throw new NullPointerException();
        }
        this.inputRoot = inputRoot.toPath();
        this.outputRoot = outputRoot.toPath();
        this.scenes = new ArrayList<>();
        this.sets = new ArrayList<>();
    }

    public GaiaUniverse(String name, Path inputRoot, Path outputRoot) {
        this.name = name;
        if (!(inputRoot.toFile().isDirectory() && outputRoot.toFile().isDirectory())) {
            throw new NullPointerException();
        }
        this.inputRoot = inputRoot;
        this.outputRoot = outputRoot;
        this.scenes = new ArrayList<>();
        this.sets = new ArrayList<>();
    }
    public List<GaiaSet> getGaiaSets() {
        List<GaiaSet> gaiaSets = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            gaiaSets.add(gaiaSet);
        }
        return gaiaSets;
    }
    public void convertGaiaSet() {
        List<GaiaSet> sets = this.getScenes().stream()
                .map(GaiaSet::new)
                .collect(Collectors.toList());
        this.sets.removeAll(new ArrayList<>());
        this.sets.addAll(sets);
    }
}
