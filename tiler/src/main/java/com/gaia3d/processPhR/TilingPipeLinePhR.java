package com.gaia3d.processPhR;

import com.gaia3d.converter.loader.FileLoader;
import com.gaia3d.process.tileprocess.Pipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class TilingPipeLinePhR  implements Pipeline {
    @Override
    public void process(FileLoader fileLoader) throws IOException {
        /* Pre-process */
        //try {
            //createTemp();
            //startPreProcesses(fileLoader);
            /* Main-process */
            //startTilingProcess();
            /* Post-process */
            //startPostProcesses();
            /* Delete temp files */
            //deleteTemp();
//        } catch (InterruptedException e) {
//            log.error("Error : {}", e.getMessage());
//            throw new RuntimeException(e);
//        }
    }
}
