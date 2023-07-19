package geometry.batch;

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
    @JsonProperty("NAME")
    private final List<String> name = new ArrayList<>();
    @JsonProperty("FILE_NAME")
    private final List<String> fileName = new ArrayList<>();
    //private final HashMap<String, List<GaiaBatchValue>> batchValues = new HashMap<>();
}
