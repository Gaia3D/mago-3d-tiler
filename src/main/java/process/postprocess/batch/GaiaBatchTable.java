package process.postprocess.batch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GaiaBatchTable {
    @JsonProperty("BatchId")
    private final List<String> batchId = new ArrayList<>();
    @JsonProperty("Name")
    private final List<String> fileName = new ArrayList<>();
}
